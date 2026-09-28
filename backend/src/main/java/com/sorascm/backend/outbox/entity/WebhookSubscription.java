package com.sorascm.backend.outbox.entity;

import com.sorascm.backend.common.entity.BaseAuditableEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "webhook_subscriptions")
public class WebhookSubscription extends BaseAuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "client_name", nullable = false, length = 128)
    private String clientName;

    @Column(name = "target_url", nullable = false, length = 512)
    private String targetUrl;

    @Column(name = "secret_token", nullable = false)
    private String secretToken;

    @Column(name = "subscribed_events", nullable = false)
    private String subscribedEvents;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public UUID getId() { return id; }
    public String getClientName() { return clientName; }
    public void setClientName(String clientName) { this.clientName = clientName; }
    public String getTargetUrl() { return targetUrl; }
    public void setTargetUrl(String targetUrl) { this.targetUrl = targetUrl; }
    public String getSecretToken() { return secretToken; }
    public void setSecretToken(String secretToken) { this.secretToken = secretToken; }
    public String getSubscribedEvents() { return subscribedEvents; }
    public void setSubscribedEvents(String subscribedEvents) { this.subscribedEvents = subscribedEvents; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}