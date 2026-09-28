package com.sorascm.backend.warehouse.repository;

import com.sorascm.backend.warehouse.entity.WarehouseLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocation, Long> {
    Optional<WarehouseLocation> findByWarehouseIdAndCode(UUID warehouseId, String code);
    List<WarehouseLocation> findByWarehouseId(UUID warehouseId);
}