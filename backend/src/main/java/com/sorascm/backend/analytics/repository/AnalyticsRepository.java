package com.sorascm.backend.analytics.repository;

import com.sorascm.backend.inventory.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Repository
public interface AnalyticsRepository extends JpaRepository<Inventory, Long> {

    interface LowStockProjection {
        UUID getProductId();
        String getProductSku();
        String getProductName();
        UUID getWarehouseId();
        String getWarehouseName();
        Integer getTotalOnHand();
        Integer getTotalReserved();
        Integer getReorderThreshold();
    }

    interface WarehouseValuationProjection {
        UUID getWarehouseId();
        String getWarehouseCode();
        String getWarehouseName();
        Long getTotalUnits();
        BigDecimal getTotalValuation();
    }

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.sku AS productSku,
            p.name AS productName,
            w.id AS warehouseId,
            w.name AS warehouseName,
            COALESCE(SUM(i.quantity_on_hand), 0) AS totalOnHand,
            COALESCE(SUM(i.quantity_reserved), 0) AS totalReserved,
            p.reorder_threshold AS reorderThreshold
        FROM products p
        CROSS JOIN warehouses w
        LEFT JOIN warehouse_locations wl ON wl.warehouse_id = w.id
        LEFT JOIN inventory i ON i.product_id = p.id AND i.location_id = wl.id
        WHERE p.is_active = true AND w.is_active = true
        GROUP BY p.id, p.sku, p.name, w.id, w.name, p.reorder_threshold
        HAVING COALESCE(SUM(i.quantity_on_hand), 0) <= p.reorder_threshold
        """, nativeQuery = true)
    List<LowStockProjection> findProductsBelowReorderThreshold();

    @Query(value = """
        SELECT 
            w.id AS warehouseId,
            w.code AS warehouseCode,
            w.name AS warehouseName,
            COALESCE(SUM(i.quantity_on_hand), 0) AS totalUnits,
            COALESCE(SUM(i.quantity_on_hand * p.unit_price), 0.00) AS totalValuation
        FROM warehouses w
        LEFT JOIN warehouse_locations wl ON wl.warehouse_id = w.id
        LEFT JOIN inventory i ON i.location_id = wl.id
        LEFT JOIN products p ON p.id = i.product_id
        WHERE w.is_active = true
        GROUP BY w.id, w.code, w.name
        """, nativeQuery = true)
    List<WarehouseValuationProjection> calculateWarehouseValuations();
}