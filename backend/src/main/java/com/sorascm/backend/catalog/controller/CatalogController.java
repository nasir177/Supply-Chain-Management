package com.sorascm.backend.catalog.controller;

import com.sorascm.backend.catalog.dto.CategoryDto;
import com.sorascm.backend.catalog.dto.SupplierDto;
import com.sorascm.backend.catalog.service.CatalogService;
import com.sorascm.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping("/categories")
    public ResponseEntity<ApiResponse<CategoryDto.Response>> createCategory(@Valid @RequestBody CategoryDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(catalogService.createCategory(request), "Category created successfully"));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getAllCategories()));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> createSupplier(@Valid @RequestBody SupplierDto.CreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(catalogService.createSupplier(request), "Supplier created successfully"));
    }

    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<List<SupplierDto.Response>>> getSuppliers() {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getAllSuppliers()));
    }

    @GetMapping("/suppliers/{id}")
    public ResponseEntity<ApiResponse<SupplierDto.Response>> getSupplierById(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok(catalogService.getSupplierById(id)));
    }
}