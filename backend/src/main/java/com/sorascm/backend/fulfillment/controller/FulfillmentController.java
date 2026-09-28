package com.sorascm.backend.fulfillment.controller;

import com.sorascm.backend.common.dto.ApiResponse;
import com.sorascm.backend.fulfillment.dto.FulfillmentDto;
import com.sorascm.backend.fulfillment.entity.Customer;
import com.sorascm.backend.fulfillment.service.FulfillmentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment")
public class FulfillmentController {

    private final FulfillmentService fulfillmentService;

    public FulfillmentController(FulfillmentService fulfillmentService) {
        this.fulfillmentService = fulfillmentService;
    }

    @PostMapping("/customers")
    public ResponseEntity<ApiResponse<Customer>> createCustomer(@Valid @RequestBody FulfillmentDto.CreateCustomerRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fulfillmentService.createCustomer(request), "Customer registered"));
    }

    @PostMapping("/orders")
    public ResponseEntity<ApiResponse<FulfillmentDto.SoResponse>> createSalesOrder(@Valid @RequestBody FulfillmentDto.CreateSoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(fulfillmentService.createSalesOrder(request), "Sales order created"));
    }

    @PostMapping("/orders/{id}/allocate")
    public ResponseEntity<ApiResponse<FulfillmentDto.SoResponse>> allocateOrder(
            @PathVariable UUID id,
            @Valid @RequestBody FulfillmentDto.AllocateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(fulfillmentService.allocateSalesOrder(id, request.sourceLocationId()), "Stock successfully allocated"));
    }

    @PostMapping("/orders/{id}/ship")
    public ResponseEntity<ApiResponse<FulfillmentDto.SoResponse>> shipOrder(
            @PathVariable UUID id,
            @Valid @RequestBody FulfillmentDto.ShipRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.ok(fulfillmentService.shipSalesOrder(id, request), "Order successfully dispatched and shipped"));
    }
}