package com.sorascm.backend.logistics.controller;

import com.sorascm.backend.common.dto.ApiResponse;
import com.sorascm.backend.logistics.dto.LogisticsDto;
import com.sorascm.backend.logistics.service.LogisticsService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/logistics")
public class LogisticsController {

    private final LogisticsService logisticsService;

    public LogisticsController(LogisticsService logisticsService) {
        this.logisticsService = logisticsService;
    }

    @PostMapping("/manifests")
    public ResponseEntity<ApiResponse<LogisticsDto.ManifestResponse>> createManifest(
            @Valid @RequestBody LogisticsDto.CreateManifestRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(logisticsService.createManifest(req), "Shipping manifest created"));
    }

    @PostMapping("/shipments")
    public ResponseEntity<ApiResponse<LogisticsDto.ShipmentResponse>> createShipment(
            @Valid @RequestBody LogisticsDto.CreateShipmentRequest req
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(logisticsService.createShipment(req), "Shipment and tracking created"));
    }

    @GetMapping("/shipments/track/{trackingNumber}")
    public ResponseEntity<ApiResponse<LogisticsDto.ShipmentResponse>> trackShipment(
            @PathVariable String trackingNumber
    ) {
        return ResponseEntity.ok(ApiResponse.ok(logisticsService.getShipmentByTracking(trackingNumber)));
    }

    @PostMapping("/webhooks/carrier-events")
    public ResponseEntity<ApiResponse<LogisticsDto.ShipmentResponse>> carrierWebhook(
            @Valid @RequestBody LogisticsDto.TrackingWebhookEvent event
    ) {
        return ResponseEntity.ok(ApiResponse.ok(logisticsService.processCarrierWebhook(event), "Carrier milestone recorded"));
    }
}