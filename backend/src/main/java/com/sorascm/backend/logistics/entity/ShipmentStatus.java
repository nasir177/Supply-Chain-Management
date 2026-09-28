package com.sorascm.backend.logistics.entity;

import java.util.Set;

public enum ShipmentStatus {
    MANIFESTED,
    TENDERED_TO_CARRIER,
    IN_TRANSIT,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED_ATTEMPT,
    RETURNED,
    CANCELLED;

    public boolean canTransitionTo(ShipmentStatus next) {
        return switch (this) {
            case MANIFESTED -> Set.of(TENDERED_TO_CARRIER, CANCELLED).contains(next);
            case TENDERED_TO_CARRIER -> Set.of(IN_TRANSIT, CANCELLED).contains(next);
            case IN_TRANSIT -> Set.of(OUT_FOR_DELIVERY, FAILED_ATTEMPT, RETURNED).contains(next);
            case OUT_FOR_DELIVERY -> Set.of(DELIVERED, FAILED_ATTEMPT).contains(next);
            case FAILED_ATTEMPT -> Set.of(OUT_FOR_DELIVERY, RETURNED).contains(next);
            case DELIVERED, RETURNED, CANCELLED -> false; // Terminal states
        };
    }
}