package com.sorascm.backend.fulfillment.repository;

import com.sorascm.backend.fulfillment.entity.PackTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PackTaskRepository extends JpaRepository<PackTask, UUID> {
    List<PackTask> findBySalesOrderId(UUID salesOrderId);
}