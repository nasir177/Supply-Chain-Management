package com.sorascm.backend.logistics.repository;

import com.sorascm.backend.logistics.entity.ShippingManifest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface ShippingManifestRepository extends JpaRepository<ShippingManifest, UUID> {
    Optional<ShippingManifest> findByManifestNumber(String manifestNumber);
}