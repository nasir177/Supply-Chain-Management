package com.sorascm.backend.outbox.controller;

import com.sorascm.backend.common.dto.ApiResponse;
import com.sorascm.backend.outbox.dto.WebhookDto;
import com.sorascm.backend.outbox.entity.WebhookSubscription;
import com.sorascm.backend.outbox.repository.WebhookSubscriptionRepository;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/webhooks")
public class WebhookController {

    private final WebhookSubscriptionRepository subscriptionRepository;

    public WebhookController(WebhookSubscriptionRepository subscriptionRepository) {
        this.subscriptionRepository = subscriptionRepository;
    }

    @PostMapping("/subscriptions")
    public ResponseEntity<ApiResponse<WebhookDto.SubscriptionResponse>> createSubscription(
            @Valid @RequestBody WebhookDto.CreateSubscriptionRequest req
    ) {
        WebhookSubscription sub = new WebhookSubscription();
        sub.setClientName(req.clientName());
        sub.setTargetUrl(req.targetUrl());
        sub.setSecretToken(req.secretToken());
        sub.setSubscribedEvents(req.subscribedEvents());

        WebhookSubscription saved = subscriptionRepository.save(sub);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(new WebhookDto.SubscriptionResponse(
                        saved.getId(),
                        saved.getClientName(),
                        saved.getTargetUrl(),
                        saved.getSubscribedEvents(),
                        saved.isActive()
                ), "Webhook registered"));
    }

    @GetMapping("/subscriptions")
    public ResponseEntity<ApiResponse<List<WebhookDto.SubscriptionResponse>>> listSubscriptions() {
        List<WebhookDto.SubscriptionResponse> list = subscriptionRepository.findAll().stream()
                .map(s -> new WebhookDto.SubscriptionResponse(
                        s.getId(),
                        s.getClientName(),
                        s.getTargetUrl(),
                        s.getSubscribedEvents(),
                        s.isActive()
                )).toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}