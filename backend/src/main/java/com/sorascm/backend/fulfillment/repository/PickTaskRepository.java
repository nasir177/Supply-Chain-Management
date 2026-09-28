package com.sorascm.backend.fulfillment.repository;

import com.sorascm.backend.fulfillment.entity.PickTask;
import com.sorascm.backend.fulfillment.entity.PickTaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PickTaskRepository extends JpaRepository<PickTask, UUID> {
    List<PickTask> findBySalesOrderId(UUID salesOrderId);
    List<PickTask> findByStatus(PickTaskStatus status);
}