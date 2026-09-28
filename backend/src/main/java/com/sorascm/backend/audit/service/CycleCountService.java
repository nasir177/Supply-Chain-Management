package com.sorascm.backend.audit.service;

import com.sorascm.backend.audit.dto.CycleCountDto;
import com.sorascm.backend.audit.entity.CycleCount;
import com.sorascm.backend.audit.entity.CycleCountLine;
import com.sorascm.backend.audit.entity.CycleCountStatus;
import com.sorascm.backend.audit.repository.CycleCountRepository;
import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import com.sorascm.backend.inventory.dto.InventoryDto;
import com.sorascm.backend.inventory.entity.Inventory;
import com.sorascm.backend.inventory.entity.MovementType;
import com.sorascm.backend.inventory.repository.InventoryRepository;
import com.sorascm.backend.inventory.service.InventoryService;
import com.sorascm.backend.warehouse.entity.Warehouse;
import com.sorascm.backend.warehouse.repository.WarehouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CycleCountService {

    private final CycleCountRepository cycleCountRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryRepository inventoryRepository;
    private final InventoryService inventoryService;

    public CycleCountService(
            CycleCountRepository cycleCountRepository,
            WarehouseRepository warehouseRepository,
            InventoryRepository inventoryRepository,
            InventoryService inventoryService
    ) {
        this.cycleCountRepository = cycleCountRepository;
        this.warehouseRepository = warehouseRepository;
        this.inventoryRepository = inventoryRepository;
        this.inventoryService = inventoryService;
    }

    /**
     * Initializes a snapshot of book stock for audit verification across designated bins.
     */
    @Transactional
    public CycleCountDto.CountResponse createCycleCount(CycleCountDto.CreateCountRequest req) {
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", req.warehouseId()));

        CycleCount cc = new CycleCount();
        cc.setCountNumber("CC-" + System.currentTimeMillis());
        cc.setWarehouse(warehouse);
        cc.setInitiatedBy(req.initiatedBy());
        cc.setNotes(req.notes());
        cc.setStatus(CycleCountStatus.PLANNED);

        for (Long locId : req.locationIds()) {
            List<Inventory> inventories = inventoryRepository.findByLocationId(locId);
            for (Inventory inv : inventories) {
                CycleCountLine line = new CycleCountLine();
                line.setProduct(inv.getProduct());
                line.setLocation(inv.getLocation());
                line.setSystemQuantity(inv.getQuantityOnHand());
                cc.addLine(line);
            }
        }

        if (cc.getLines().isEmpty()) {
            throw new BusinessException("EMPTY_AUDIT_SCOPE", "No inventory found at the specified locations to audit", HttpStatus.BAD_REQUEST);
        }

        CycleCount saved = cycleCountRepository.save(cc);
        return mapToResponse(saved);
    }

    /**
     * Records floor counts submitted by warehouse operators.
     */
    @Transactional
    public CycleCountDto.CountResponse recordCounts(UUID countId, CycleCountDto.SubmitCountsRequest req) {
        CycleCount cc = cycleCountRepository.findByIdWithLock(countId)
                .orElseThrow(() -> new ResourceNotFoundException("CycleCount", countId));

        if (cc.getStatus() == CycleCountStatus.RECONCILED || cc.getStatus() == CycleCountStatus.CANCELLED) {
            throw new BusinessException("INVALID_STATE", "Cannot submit counts against " + cc.getStatus(), HttpStatus.CONFLICT);
        }

        Map<Long, CycleCountLine> lineMap = cc.getLines().stream()
                .collect(Collectors.toMap(CycleCountLine::getId, l -> l));

        for (CycleCountDto.RecordCountItem item : req.counts()) {
            CycleCountLine line = lineMap.get(item.lineId());
            if (line == null) {
                throw new BusinessException("INVALID_LINE", "Line " + item.lineId() + " does not belong to this count session", HttpStatus.BAD_REQUEST);
            }

            line.setCountedQuantity(item.countedQuantity());
            line.setCountedBy(item.countedBy());
            line.setCountedAt(Instant.now());

            int diff = item.countedQuantity() - line.getSystemQuantity();
            BigDecimal unitPrice = line.getProduct().getUnitPrice() != null ? line.getProduct().getUnitPrice() : BigDecimal.ZERO;
            line.setVarianceValue(unitPrice.multiply(BigDecimal.valueOf(diff)));
        }

        cc.setStatus(CycleCountStatus.IN_PROGRESS);
        return mapToResponse(cycleCountRepository.save(cc));
    }

    /**
     * Approves variances and applies ledger adjustments to synchronize real stock.
     */
    @Transactional
    public CycleCountDto.CountResponse reconcile(UUID countId, CycleCountDto.ReconcileRequest req) {
        CycleCount cc = cycleCountRepository.findByIdWithLock(countId)
                .orElseThrow(() -> new ResourceNotFoundException("CycleCount", countId));

        if (cc.getStatus() != CycleCountStatus.IN_PROGRESS) {
            throw new BusinessException("INVALID_STATE", "Counts must be recorded before reconciliation", HttpStatus.CONFLICT);
        }

        for (CycleCountLine line : cc.getLines()) {
            if (line.getCountedQuantity() == null) {
                throw new BusinessException("INCOMPLETE_COUNT", "Line " + line.getId() + " has not been counted yet", HttpStatus.BAD_REQUEST);
            }

            int diff = line.getCountedQuantity() - line.getSystemQuantity();
            if (diff != 0) {
                MovementType moveType = diff > 0 ? MovementType.ADJUSTMENT_ADD : MovementType.ADJUSTMENT_SUB;
                inventoryService.adjustStock(new InventoryDto.AdjustStockRequest(
                        line.getProduct().getId(),
                        line.getLocation().getId(),
                        Math.abs(diff),
                        moveType,
                        "CYCLE_COUNT_RECONCILIATION",
                        cc.getId(),
                        "Reconciliation from %s approved by %s".formatted(cc.getCountNumber(), req.approvedBy())
                ));
            }
        }

        cc.setStatus(CycleCountStatus.RECONCILED);
        cc.setApprovedBy(req.approvedBy());
        cc.setCompletedAt(Instant.now());
        if (req.reconciliationNotes() != null) {
            cc.setNotes((cc.getNotes() != null ? cc.getNotes() + "\n" : "") + "Approval Notes: " + req.reconciliationNotes());
        }

        return mapToResponse(cycleCountRepository.save(cc));
    }

    public CycleCountDto.CountResponse getCount(UUID id) {
        CycleCount cc = cycleCountRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("CycleCount", id));
        return mapToResponse(cc);
    }

    private CycleCountDto.CountResponse mapToResponse(CycleCount cc) {
        return new CycleCountDto.CountResponse(
                cc.getId(),
                cc.getCountNumber(),
                cc.getWarehouse().getId(),
                cc.getStatus(),
                cc.getInitiatedBy(),
                cc.getApprovedBy(),
                cc.getCreatedAt(),
                cc.getCompletedAt(),
                cc.getLines().stream().map(l -> new CycleCountDto.LineResponse(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getLocation().getId(),
                        l.getLocation().getCode(),
                        l.getSystemQuantity(),
                        l.getCountedQuantity(),
                        l.getCountedQuantity() != null ? (l.getCountedQuantity() - l.getSystemQuantity()) : null,
                        l.getVarianceValue(),
                        l.getCountedBy()
                )).toList()
        );
    }
}