package com.sorascm.backend.audit.dto;

import com.sorascm.backend.audit.entity.CycleCountStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class CycleCountDto {

    public record CreateCountRequest(
            @NotNull UUID warehouseId,
            @NotBlank String initiatedBy,
            String notes,
            @NotEmpty List<Long> locationIds
    ) {}

    public record RecordCountItem(
            @NotNull Long lineId,
            @NotNull @Min(0) Integer countedQuantity,
            @NotBlank String countedBy
    ) {}

    public record SubmitCountsRequest(
            @NotEmpty @Valid List<RecordCountItem> counts
    ) {}

    public record ReconcileRequest(
            @NotBlank String approvedBy,
            String reconciliationNotes
    ) {}

    public record LineResponse(
            Long lineId,
            UUID productId,
            String productSku,
            String productName,
            Long locationId,
            String locationCode,
            int systemQuantity,
            Integer countedQuantity,
            Integer variance,
            BigDecimal varianceValue,
            String countedBy
    ) {}

    public record CountResponse(
            UUID id,
            String countNumber,
            UUID warehouseId,
            CycleCountStatus status,
            String initiatedBy,
            String approvedBy,
            Instant createdAt,
            Instant completedAt,
            List<LineResponse> lines
    ) {}
}