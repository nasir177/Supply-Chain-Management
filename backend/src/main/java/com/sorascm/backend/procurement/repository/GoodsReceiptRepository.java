package com.sorascm.backend.procurement.repository;

import com.sorascm.backend.procurement.entity.GoodsReceipt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface GoodsReceiptRepository  extends JpaRepository<GoodsReceipt, UUID> {
}
