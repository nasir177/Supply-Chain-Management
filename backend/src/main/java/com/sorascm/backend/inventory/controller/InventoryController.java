package com.sorascm.backend.inventory.controller;

import com.sorascm.backend.common.dto.ApiResponse;
import com.sorascm.backend.inventory.dto.InventoryDto;
import com.sorascm.backend.inventory.service.InventoryService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

    private final InventoryService inventoryService;

    public InventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @PostMapping("/adjust")
    public ResponseEntity<ApiResponse<InventoryDto.StockResponse>> adjustStock(
            @Valid @RequestBody InventoryDto.AdjustStockRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.adjustStock(request), "Stock adjusted successfully"));
    }

    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Void>> transferStock(
            @Valid @RequestBody InventoryDto.TransferStockRequest request
    ) {
        inventoryService.transferStock(request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Stock transferred successfully"));
    }

    @GetMapping("/stock")
    public ResponseEntity<ApiResponse<InventoryDto.StockResponse>> getStock(
            @RequestParam UUID productId,
            @RequestParam Long locationId
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getStock(productId, locationId)));
    }

    @GetMapping("/{inventoryId}/movements")
    public ResponseEntity<ApiResponse<Page<InventoryDto.MovementResponse>>> getMovementLedger(
            @PathVariable Long inventoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getMovementLedger(inventoryId, pageable)));
    }
}