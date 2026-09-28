package com.sorascm.backend.analytics.repository;

import com.sorascm.backend.analytics.entity.StockAlert;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface StockAlertRepository extends JpaRepository<StockAlert, Long> {
    List<StockAlert> findByStatusOrderByCreatedAtDesc(String status);
    boolean existsByProductIdAndWarehouseIdAndStatus(UUID productId, UUID warehouseId, String status);
}