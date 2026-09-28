package com.sorascm.backend.logistics.dto;

import com.sorascm.backend.logistics.entity.ManifestStatus;
import com.sorascm.backend.logistics.entity.ShipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class LogisticsDto {

    public record CreateManifestRequest(
            @NotBlank String carrier,
            @NotNull UUID warehouseId,
            Instant scheduledPickupAt
    ) {}

    public record CreateShipmentRequest(
            @NotNull UUID salesOrderId,
            @NotBlank String carrier,
            @NotBlank String trackingNumber,
            String serviceLevel,
            UUID manifestId,
            Instant estimatedDeliveryAt
    ) {}

    public record TrackingWebhookEvent(
            @NotBlank String trackingNumber,
            @NotNull ShipmentStatus status,
            String locationDescription,
            String carrierMessage,
            String signedBy,
            Instant eventTimestamp
    ) {}

    public record ShipmentEventResponse(
            Long id,
            ShipmentStatus fromStatus,
            ShipmentStatus toStatus,
            String locationDescription,
            String carrierMessage,
            Instant eventTimestamp
    ) {}

    public record ShipmentResponse(
            UUID id,
            String shipmentNumber,
            UUID salesOrderId,
            String carrier,
            String trackingNumber,
            String serviceLevel,
            ShipmentStatus status,
            String recipientName,
            String destinationAddress,
            String destinationCity,
            String destinationCountryCode,
            Instant estimatedDeliveryAt,
            Instant actualDeliveryAt,
            String signedBy,
            List<ShipmentEventResponse> events
    ) {}

    public record ManifestResponse(
            UUID id,
            String manifestNumber,
            String carrier,
            UUID warehouseId,
            ManifestStatus status,
            int totalPackages,
            BigDecimal totalWeightKg,
            Instant scheduledPickupAt,
            Instant closedAt
    ) {}
}