package com.sorascm.backend.outbox.repository;

import com.sorascm.backend.outbox.entity.OutboxEvent;
import com.sorascm.backend.outbox.entity.OutboxStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT e FROM OutboxEvent e
            Where e.status = :status AND e.nextRetryAt <= :now
            ORDER BY e.createdAt ASC 
            LIMIT :limit
            """)
    List<OutboxEvent> findPendingEventsForProcessing(

            @Param("status") OutboxStatus status,
            @Param("now") Instant now,
            @Param("limit") int limit
    );
}
