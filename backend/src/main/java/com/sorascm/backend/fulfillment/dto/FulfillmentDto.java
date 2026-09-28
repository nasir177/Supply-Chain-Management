package com.sorascm.backend.fulfillment.dto;

import com.sorascm.backend.fulfillment.entity.SalesOrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public final class FulfillmentDto {

    public record CreateCustomerRequest(
            @NotBlank String customerCode,
            @NotBlank String name,
            @NotBlank String email,
            String phone,
            @NotBlank String addressLine1,
            @NotBlank String city,
            @NotBlank String countryCode
    ) {}

    public record CreateSoLineItem(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity,
            @NotNull BigDecimal unitPrice
    ) {}

    public record CreateSoRequest(
            @NotNull UUID customerId,
            @NotNull UUID warehouseId,
            String notes,
            @NotEmpty @Valid List<CreateSoLineItem> items
    ) {}

    public record AllocateRequest(
            @NotNull Long sourceLocationId
    ) {}

    public record ShipRequest(
            @NotBlank String carrier,
            @NotBlank String trackingNumber,
            @NotNull Long sourceLocationId
    ) {}

    public record SoLineResponse(
            Long lineId,
            UUID productId,
            String sku,
            String productName,
            int quantityOrdered,
            int quantityAllocated,
            int quantityShipped,
            BigDecimal unitPrice
    ) {}

    public record SoResponse(
            UUID id,
            String orderNumber,
            UUID customerId,
            UUID warehouseId,
            SalesOrderStatus status,
            BigDecimal totalAmount,
            String shippingCarrier,
            String trackingNumber,
            List<SoLineResponse> lines
    ) {}
}