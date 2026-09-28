package com.sorascm.backend.catalog.service;

import com.sorascm.backend.catalog.dto.CategoryDto;
import com.sorascm.backend.catalog.dto.SupplierDto;
import com.sorascm.backend.catalog.entity.Category;
import com.sorascm.backend.catalog.entity.Supplier;
import com.sorascm.backend.catalog.repository.CategoryRepository;
import com.sorascm.backend.catalog.repository.SupplierRepository;
import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final SupplierRepository supplierRepository;

    public CatalogService(CategoryRepository categoryRepository, SupplierRepository supplierRepository) {
        this.categoryRepository = categoryRepository;
        this.supplierRepository = supplierRepository;
    }

    @Transactional
    public CategoryDto.Response createCategory(CategoryDto.CreateRequest req) {
        if (categoryRepository.existsByCode(req.code())) {
            throw new BusinessException("DUPLICATE_CATEGORY", "Category code already exists: " + req.code(), HttpStatus.CONFLICT);
        }
        Category category = new Category();
        category.setName(req.name());
        category.setCode(req.code().toUpperCase());
        category.setDescription(req.description());

        if (req.parentId() != null) {
            Category parent = categoryRepository.findById(req.parentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", req.parentId()));
            category.setParent(parent);
        }

        Category saved = categoryRepository.save(category);
        return mapToCategoryResponse(saved);
    }

    public List<CategoryDto.Response> getAllCategories() {
        return categoryRepository.findAll().stream().map(this::mapToCategoryResponse).toList();
    }

    @Transactional
    public SupplierDto.Response createSupplier(SupplierDto.CreateRequest req) {
        if (supplierRepository.existsByCode(req.code())) {
            throw new BusinessException("DUPLICATE_SUPPLIER", "Supplier code already exists: " + req.code(), HttpStatus.CONFLICT);
        }
        Supplier supplier = new Supplier();
        supplier.setCode(req.code().toUpperCase());
        supplier.setName(req.name());
        supplier.setContactName(req.contactName());
        supplier.setEmail(req.email());
        supplier.setPhone(req.phone());
        supplier.setAddressLine1(req.addressLine1());
        supplier.setAddressLine2(req.addressLine2());
        supplier.setCity(req.city());
        supplier.setCountryCode(req.countryCode().toUpperCase());

        Supplier saved = supplierRepository.save(supplier);
        return mapToSupplierResponse(saved);
    }

    public List<SupplierDto.Response> getAllSuppliers() {
        return supplierRepository.findAll().stream().map(this::mapToSupplierResponse).toList();
    }

    public SupplierDto.Response getSupplierById(UUID id) {
        return supplierRepository.findById(id)
                .map(this::mapToSupplierResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", id));
    }

    private CategoryDto.Response mapToCategoryResponse(Category entity) {
        return new CategoryDto.Response(
                entity.getId(),
                entity.getName(),
                entity.getCode(),
                entity.getDescription(),
                entity.getParent() != null ? entity.getParent().getId() : null,
                entity.isActive()
        );
    }

    private SupplierDto.Response mapToSupplierResponse(Supplier entity) {
        return new SupplierDto.Response(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getContactName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getCity(),
                entity.getCountryCode(),
                entity.isActive()
        );
    }
}