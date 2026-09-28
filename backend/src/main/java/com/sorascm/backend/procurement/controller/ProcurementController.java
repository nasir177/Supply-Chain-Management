package com.sorascm.backend.procurement.controller;

import com.sorascm.backend.common.dto.ApiResponse;
import com.sorascm.backend.procurement.dto.ProcurementDto;
import com.sorascm.backend.procurement.service.ProcurementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/procurement")
public class ProcurementController {

    private final ProcurementService procurementService;

    public ProcurementController(ProcurementService procurementService) {
        this.procurementService = procurementService;
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<ProcurementDto.PoResponse>> createPurchaseOrder(
            @Valid @RequestBody ProcurementDto.CreatePoRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(procurementService.createPurchaseOrder(request), "Purchase Order created"));
    }

    @PostMapping("/receipts")
    public ResponseEntity<ApiResponse<ProcurementDto.GoodsReceiptResponse>> receiveGoods(
            @Valid @RequestBody ProcurementDto.ReceiveGoodsRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(procurementService.receiveGoods(request), "Goods received and inventory ledger updated"));
    }
}