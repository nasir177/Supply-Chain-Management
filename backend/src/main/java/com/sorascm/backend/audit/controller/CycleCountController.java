package com.sorascm.backend.audit.controller;

import com.sorascm.backend.audit.dto.CycleCountDto;
import com.sorascm.backend.audit.service.CycleCountService;
import com.sorascm.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/audit/cycle-counts")
public class CycleCountController {

    private final CycleCountService cycleCountService;

    public CycleCountController(CycleCountService cycleCountService) {
        this.cycleCountService = cycleCountService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CycleCountDto.CountResponse>> createCycleCount(
            @Valid @RequestBody CycleCountDto.CreateCountRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(cycleCountService.createCycleCount(req), "Cycle count session created"));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CycleCountDto.CountResponse>> getCycleCount(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(cycleCountService.getCount(id)));
    }

    @PostMapping("/{id}/counts")
    public ResponseEntity<ApiResponse<CycleCountDto.CountResponse>> recordCounts(
            @PathVariable UUID id,
            @Valid @RequestBody CycleCountDto.SubmitCountsRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cycleCountService.recordCounts(id, req), "Physical counts recorded"));
    }

    @PostMapping("/{id}/reconcile")
    public ResponseEntity<ApiResponse<CycleCountDto.CountResponse>> reconcile(
            @PathVariable UUID id,
            @Valid @RequestBody CycleCountDto.ReconcileRequest req
    ) {
        return ResponseEntity.ok(ApiResponse.ok(cycleCountService.reconcile(id, req), "Stock reconciled and ledger updated"));
    }
}