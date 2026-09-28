package com.sorascm.backend.inventory.dto;

import com.sorascm.backend.inventory.entity.MovementType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.UUID;

public final class InventoryDto {

    public record AdjustStockRequest(
            @NotNull UUID productId,
            @NotNull Long locationId,
            @NotNull @Min(1) Integer quantity,
            @NotNull MovementType movementType,
            @NotBlank String referenceType,
            UUID referenceId,
            String notes
    ) {}

    public record TransferStockRequest(
            @NotNull UUID productId,
            @NotNull Long fromLocationId,
            @NotNull Long toLocationId,
            @NotNull @Min(1) Integer quantity,
            String notes
    ) {}

    public record StockResponse(
            Long inventoryId,
            UUID productId,
            String productSku,
            String productName,
            Long locationId,
            String locationCode,
            int quantityOnHand,
            int quantityReserved,
            int quantityAvailable
    ) {}

    public record MovementResponse(
            Long id,
            Long inventoryId,
            String movementType,
            int deltaQuantity,
            String referenceType,
            UUID referenceId,
            String notes,
            Instant createdAt
    ) {}
}