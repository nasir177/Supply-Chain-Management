package com.sorascm.backend.outbox.scheduler;

import com.sorascm.backend.outbox.entity.OutboxEvent;
import com.sorascm.backend.outbox.entity.OutboxStatus;
import com.sorascm.backend.outbox.entity.WebhookSubscription;
import com.sorascm.backend.outbox.repository.OutboxEventRepository;
import com.sorascm.backend.outbox.repository.WebhookSubscriptionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Component
public class OutboxDispatcher {

    private static final Logger log = LoggerFactory.getLogger(OutboxDispatcher.class);

    private final OutboxEventRepository outboxEventRepository;
    private final WebhookSubscriptionRepository subscriptionRepository;
    private final RestTemplate restTemplate;

    public OutboxDispatcher(
            OutboxEventRepository outboxEventRepository,
            WebhookSubscriptionRepository subscriptionRepository,
            RestTemplateBuilder restTemplateBuilder
    ) {
        this.outboxEventRepository = outboxEventRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.restTemplate = restTemplateBuilder
                .connectTimeout(Duration.ofSeconds(3))
                .readTimeout(Duration.ofSeconds(3))
                .build();
    }

    @Scheduled(fixedDelay = 5000) // Polls every 5 seconds for pending events
    @Transactional
    public void processOutboxEvents() {
        List<OutboxEvent> pending = outboxEventRepository.findPendingEventsForProcessing(
                OutboxStatus.PENDING,
                Instant.now(),
                50 // batch size
        );

        if (pending.isEmpty()) {
            return;
        }

        List<WebhookSubscription> subscriptions = subscriptionRepository.findByActiveTrue();

        for (OutboxEvent event : pending) {
            dispatchSingleEvent(event, subscriptions);
        }
    }

    private void dispatchSingleEvent(OutboxEvent event, List<WebhookSubscription> subscriptions) {
        boolean allSuccess = true;
        String lastError = null;

        for (WebhookSubscription sub : subscriptions) {
            if (sub.getSubscribedEvents().contains(event.getEventType()) || sub.getSubscribedEvents().equals("*")) {
                try {
                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.APPLICATION_JSON);
                    headers.set("X-SoraSCM-Event", event.getEventType());
                    headers.set("X-SoraSCM-Signature", sub.getSecretToken());

                    HttpEntity<String> entity = new HttpEntity<>(event.getPayload(), headers);
                    ResponseEntity<String> response = restTemplate.postForEntity(sub.getTargetUrl(), entity, String.class);

                    if (!response.getStatusCode().is2xxSuccessful()) {
                        allSuccess = false;
                        lastError = "HTTP " + response.getStatusCode().value();
                    }
                } catch (Exception ex) {
                    allSuccess = false;
                    lastError = ex.getMessage();
                    log.warn("Webhook delivery failed for client {} at {}: {}", sub.getClientName(), sub.getTargetUrl(), ex.getMessage());
                }
            }
        }

        if (allSuccess) {
            event.setStatus(OutboxStatus.PUBLISHED);
            event.setProcessedAt(Instant.now());
            event.setErrorMessage(null);
        } else {
            int newRetry = event.getRetryCount() + 1;
            event.setRetryCount(newRetry);
            event.setErrorMessage(lastError);

            if (newRetry >= event.getMaxRetries()) {
                event.setStatus(OutboxStatus.FAILED);
                log.error("Outbox Event {} reached maximum retries ({}). Marked as FAILED.", event.getId(), event.getMaxRetries());
            } else {
                // Exponential backoff: 10s, 40s, 90s, 160s...
                long backoffSeconds = (long) (10 * Math.pow(newRetry, 2));
                event.setNextRetryAt(Instant.now().plusSeconds(backoffSeconds));
            }
        }

        outboxEventRepository.save(event);
    }
}