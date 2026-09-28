package com.sorascm.backend.common.metrics;

import com.sorascm.backend.outbox.entity.OutboxStatus;
import com.sorascm.backend.outbox.repository.OutboxEventRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class ScmBusinessMetrics {

    private final Counter inventoryAdjustmentCounter;
    private final OutboxEventRepository outboxEventRepository;

    public ScmBusinessMetrics(MeterRegistry registry, OutboxEventRepository outboxEventRepository) {
        this.outboxEventRepository = outboxEventRepository;

        // Custom counter: tracks how many stock adjustments occur over time
        this.inventoryAdjustmentCounter = Counter.builder("scm.inventory.adjustments.total")
                .description("Total number of inventory adjustments executed")
                .tag("module", "inventory")
                .register(registry);

        // Custom gauge: tracks pending outbox backlog in real time
        Gauge.builder("scm.outbox.backlog.count", this, ScmBusinessMetrics::getPendingOutboxCount)
                .description("Number of pending events in transactional outbox")
                .tag("module", "outbox")
                .register(registry);
    }

    public void incrementInventoryAdjustments() {
        inventoryAdjustmentCounter.increment();
    }

    private double getPendingOutboxCount() {
        return outboxEventRepository.findPendingEventsForProcessing(
                OutboxStatus.PENDING,
                Instant.now(),
                1000
        ).size();
    }
}