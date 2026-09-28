package com.sorascm.backend.procurement.service;

import com.sorascm.backend.catalog.entity.Supplier;
import com.sorascm.backend.catalog.repository.SupplierRepository;
import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import com.sorascm.backend.inventory.dto.InventoryDto;
import com.sorascm.backend.inventory.entity.MovementType;
import com.sorascm.backend.inventory.service.InventoryService;
import com.sorascm.backend.procurement.dto.ProcurementDto;
import com.sorascm.backend.procurement.entity.*;
import com.sorascm.backend.procurement.repository.GoodsReceiptRepository;
import com.sorascm.backend.procurement.repository.PurchaseOrderRepository;
import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.product.repository.ProductRepository;
import com.sorascm.backend.warehouse.entity.Warehouse;
import com.sorascm.backend.warehouse.entity.WarehouseLocation;
import com.sorascm.backend.warehouse.repository.WarehouseLocationRepository;
import com.sorascm.backend.warehouse.repository.WarehouseRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProcurementService {

    private final PurchaseOrderRepository poRepository;
    private final GoodsReceiptRepository grnRepository;
    private final SupplierRepository supplierRepository;
    private final WarehouseRepository warehouseRepository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository locationRepository;
    private final InventoryService inventoryService;

    public ProcurementService(
            PurchaseOrderRepository poRepository,
            GoodsReceiptRepository grnRepository,
            SupplierRepository supplierRepository,
            WarehouseRepository warehouseRepository,
            ProductRepository productRepository,
            WarehouseLocationRepository locationRepository,
            InventoryService inventoryService
    ) {
        this.poRepository = poRepository;
        this.grnRepository = grnRepository;
        this.supplierRepository = supplierRepository;
        this.warehouseRepository = warehouseRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.inventoryService = inventoryService;
    }

    @Transactional
    public ProcurementDto.PoResponse createPurchaseOrder(ProcurementDto.CreatePoRequest req) {
        Supplier supplier = supplierRepository.findById(req.supplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier", req.supplierId()));
        Warehouse warehouse = warehouseRepository.findById(req.warehouseId())
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse", req.warehouseId()));

        PurchaseOrder po = new PurchaseOrder();
        po.setOrderNumber("PO-" + System.currentTimeMillis());
        po.setSupplier(supplier);
        po.setWarehouse(warehouse);
        po.setStatus(PurchaseOrderStatus.APPROVED);
        po.setExpectedDeliveryDate(req.expectedDeliveryDate());
        po.setNotes(req.notes());

        BigDecimal total = BigDecimal.ZERO;
        for (ProcurementDto.CreatePoLineItem item : req.lineItems()) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product", item.productId()));

            PurchaseOrderLine line = new PurchaseOrderLine();
            line.setProduct(product);
            line.setQuantityOrdered(item.quantity());
            line.setUnitPrice(item.unitPrice());
            po.addLine(line);

            total = total.add(item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())));
        }

        po.setTotalAmount(total);
        PurchaseOrder saved = poRepository.save(po);
        return mapToPoResponse(saved);
    }

    @Transactional
    public ProcurementDto.GoodsReceiptResponse receiveGoods(ProcurementDto.ReceiveGoodsRequest req) {
        PurchaseOrder po = poRepository.findByIdWithLock(req.purchaseOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("PurchaseOrder", req.purchaseOrderId()));

        if (po.getStatus() == PurchaseOrderStatus.COMPLETED || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BusinessException("INVALID_PO_STATE", "Cannot receive goods against PO status: " + po.getStatus(), HttpStatus.CONFLICT);
        }

        WarehouseLocation destLocation = locationRepository.findById(req.destinationLocationId())
                .orElseThrow(() -> new ResourceNotFoundException("WarehouseLocation", req.destinationLocationId()));

        GoodsReceipt grn = new GoodsReceipt();
        grn.setGrnNumber("GRN-" + System.currentTimeMillis());
        grn.setPurchaseOrder(po);
        grn.setDestinationLocation(destLocation);
        grn.setReceivedBy(req.receivedBy());
        grn.setNotes(req.notes());

        Map<Long, PurchaseOrderLine> poLinesMap = po.getLines().stream()
                .collect(Collectors.toMap(PurchaseOrderLine::getId, line -> line));

        for (ProcurementDto.ReceiveItemRequest item : req.receivedItems()) {
            PurchaseOrderLine poLine = poLinesMap.get(item.poLineId());
            if (poLine == null) {
                throw new BusinessException("INVALID_LINE", "PO Line " + item.poLineId() + " does not belong to this PO", HttpStatus.BAD_REQUEST);
            }

            int currentReceived = poLine.getQuantityReceived();
            int newReceived = currentReceived + item.quantityAccepted();
            if (newReceived > poLine.getQuantityOrdered()) {
                throw new BusinessException(
                        "OVER_RECEIPT_RESTRICTED",
                        "Cannot receive %d. Maximum allowed: %d".formatted(item.quantityAccepted(), poLine.getQuantityOrdered() - currentReceived),
                        HttpStatus.CONFLICT
                );
            }

            poLine.setQuantityReceived(newReceived);

            GoodsReceiptLine grnLine = new GoodsReceiptLine();
            grnLine.setPoLine(poLine);
            grnLine.setProduct(poLine.getProduct());
            grnLine.setQuantityAccepted(item.quantityAccepted());
            grnLine.setQuantityRejected(item.quantityRejected() != null ? item.quantityRejected() : 0);
            grnLine.setRejectionReason(item.rejectionReason());
            grn.addLine(grnLine);

            // Connects directly into Phase 5 inventory ledger engine
            inventoryService.adjustStock(new InventoryDto.AdjustStockRequest(
                    poLine.getProduct().getId(),
                    destLocation.getId(),
                    item.quantityAccepted(),
                    MovementType.INBOUND_RECEIPT,
                    "PURCHASE_ORDER",
                    po.getId(),
                    "GRN: " + grn.getGrnNumber()
            ));
        }

        boolean allCompleted = po.getLines().stream().allMatch(l -> l.getQuantityReceived() == l.getQuantityOrdered());
        po.setStatus(allCompleted ? PurchaseOrderStatus.COMPLETED : PurchaseOrderStatus.PARTIALLY_RECEIVED);

        GoodsReceipt savedGrn = grnRepository.save(grn);
        poRepository.save(po);

        return new ProcurementDto.GoodsReceiptResponse(
                savedGrn.getId(),
                savedGrn.getGrnNumber(),
                po.getId(),
                destLocation.getId(),
                savedGrn.getReceivedBy(),
                savedGrn.getCreatedAt()
        );
    }

    private ProcurementDto.PoResponse mapToPoResponse(PurchaseOrder po) {
        return new ProcurementDto.PoResponse(
                po.getId(),
                po.getOrderNumber(),
                po.getSupplier().getId(),
                po.getWarehouse().getId(),
                po.getStatus(),
                po.getTotalAmount(),
                po.getLines().stream().map(l -> new ProcurementDto.PoLineResponse(
                        l.getId(),
                        l.getProduct().getId(),
                        l.getProduct().getSku(),
                        l.getProduct().getName(),
                        l.getQuantityOrdered(),
                        l.getQuantityReceived(),
                        l.getUnitPrice()
                )).toList()
        );
    }
}