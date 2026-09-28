package com.sorascm.backend.outbox.repository;

import com.sorascm.backend.outbox.entity.WebhookSubscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface WebhookSubscriptionRepository  extends JpaRepository<WebhookSubscription, UUID> {
    List<WebhookSubscription> findByActiveTrue();

}
