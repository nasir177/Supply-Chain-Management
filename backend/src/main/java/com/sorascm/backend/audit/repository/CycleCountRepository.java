package com.sorascm.backend.audit.repository;

import com.sorascm.backend.audit.entity.CycleCount;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CycleCountRepository extends JpaRepository<CycleCount, UUID> {
    Optional<CycleCount> findByCountNumber(String countNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT cc FROM CycleCount cc WHERE cc.id = :id")
    Optional<CycleCount> findByIdWithLock(@Param("id") UUID id);
}