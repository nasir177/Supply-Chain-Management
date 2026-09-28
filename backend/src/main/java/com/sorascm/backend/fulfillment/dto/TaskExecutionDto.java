package com.sorascm.backend.fulfillment.dto;

import com.sorascm.backend.fulfillment.entity.PackTaskStatus;
import com.sorascm.backend.fulfillment.entity.PickTaskStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class TaskExecutionDto {

    public record ConfirmPickRequest(
            @NotNull @Min(1) Integer quantityPicked,
            @NotBlank String pickerName
    ) {}

    public record PackLineItem(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity
    ) {}

    public record CreatePackRequest(
            @NotBlank String containerType,
            @NotNull BigDecimal weightKg,
            @NotBlank String packerName,
            @NotNull List<PackLineItem> items
    ) {}

    public record PickTaskResponse(
            UUID id,
            String taskNumber,
            UUID salesOrderId,
            UUID productId,
            String productSku,
            String productName,
            Long locationId,
            String locationCode,
            int quantityToPick,
            int quantityPicked,
            PickTaskStatus status,
            String assignedPicker,
            Instant pickedAt
    ) {}

    public record PackLineResponse(
            Long id,
            UUID productId,
            String productSku,
            int quantityPacked
    ) {}

    public record PackTaskResponse(
            UUID id,
            String packNumber,
            UUID salesOrderId,
            String containerType,
            BigDecimal weightKg,
            PackTaskStatus status,
            String packerName,
            Instant packedAt,
            List<PackLineResponse> lines
    ) {}
}