package com.sorascm.backend.analytics.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public final class AnalyticsDto {

    public record LowStockItem(
            UUID productId,
            String productSku,
            String productName,
            UUID warehouseId,
            String warehouseName,
            int totalOnHand,
            int totalReserved,
            int available,
            int reorderThreshold
    ) {}

    public record WarehouseValuation(
            UUID warehouseId,
            String warehouseCode,
            String warehouseName,
            long totalUnits,
            BigDecimal totalValuation
    ) {}

    public record StockAlertResponse(
            Long alertId,
            UUID productId,
            String productName,
            UUID warehouseId,
            String warehouseName,
            int currentStock,
            int reorderThreshold,
            String status,
            String message,
            Instant createdAt
    ) {}
}