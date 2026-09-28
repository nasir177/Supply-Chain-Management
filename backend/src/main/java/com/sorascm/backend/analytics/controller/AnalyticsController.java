package com.sorascm.backend.analytics.controller;

import com.sorascm.backend.analytics.dto.AnalyticsDto;
import com.sorascm.backend.analytics.service.AnalyticsService;
import com.sorascm.backend.common.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.LowStockItem>>> getLowStockItems() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getLowStockInventory()));
    }

    @GetMapping("/valuations")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.WarehouseValuation>>> getWarehouseValuations() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getWarehouseValuations()));
    }

    @GetMapping("/alerts")
    public ResponseEntity<ApiResponse<List<AnalyticsDto.StockAlertResponse>>> getActiveAlerts() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getActiveAlerts()));
    }

    @PostMapping("/alerts/scan")
    public ResponseEntity<ApiResponse<Void>> triggerAlertScan() {
        analyticsService.scanAndGenerateAlerts();
        return ResponseEntity.ok(ApiResponse.ok(null, "Low-stock scan completed"));
    }
}
