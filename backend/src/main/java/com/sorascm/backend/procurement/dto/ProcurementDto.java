package com.sorascm.backend.procurement.dto;

import com.sorascm.backend.procurement.entity.PurchaseOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class ProcurementDto {

    public record CreatePoLineItem(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal unitPrice
    ) {}

    public record CreatePoRequest(
            @NotNull UUID supplierId,
            @NotNull UUID warehouseId,
            Instant expectedDeliveryDate,
            String notes,
            @NotEmpty @Valid List<CreatePoLineItem> lineItems
    ) {}

    public record ReceiveItemRequest(
            @NotNull Long poLineId,
            @NotNull @Min(1) Integer quantityAccepted,
            @Min(0) Integer quantityRejected,
            String rejectionReason
    ) {}

    public record ReceiveGoodsRequest(
            @NotNull UUID purchaseOrderId,
            @NotNull Long destinationLocationId,
            @NotNull String receivedBy,
            String notes,
            @NotEmpty @Valid List<ReceiveItemRequest> receivedItems
    ) {}

    public record PoLineResponse(
            Long lineId,
            UUID productId,
            String productSku,
            String productName,
            int quantityOrdered,
            int quantityReceived,
            BigDecimal unitPrice
    ) {}

    public record PoResponse(
            UUID id,
            String orderNumber,
            UUID supplierId,
            UUID warehouseId,
            PurchaseOrderStatus status,
            BigDecimal totalAmount,
            List<PoLineResponse> lines
    ) {}

    public record GoodsReceiptResponse(
            UUID id,
            String grnNumber,
            UUID purchaseOrderId,
            Long destinationLocationId,
            String receivedBy,
            Instant createdAt
    ) {}
}