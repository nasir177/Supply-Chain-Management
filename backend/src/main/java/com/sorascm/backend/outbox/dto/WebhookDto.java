package com.sorascm.backend.outbox.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public final class WebhookDto {
    public record CreateSubscriptionRequest(
            @NotBlank String clientName,
            @NotBlank String targetUrl,
            @NotBlank String secretToken,
            @NotBlank String subscribedEvents
    ) {}

    public record SubscriptionResponse(
            UUID id,
            String clientName,
            String targetUrl,
            String SubscribedEvents,
            boolean active
    ){}

}
