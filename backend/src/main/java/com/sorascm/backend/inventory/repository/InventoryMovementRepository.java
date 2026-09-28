package com.sorascm.backend.inventory.repository;

import com.sorascm.backend.inventory.entity.InventoryMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryMovementRepository extends JpaRepository<InventoryMovement, Long> {
    Page<InventoryMovement> findByInventoryIdOrderByCreatedAtDesc(Long inventoryId, Pageable pageable);
}