package com.sorascm.backend.inventory.service;

import com.sorascm.backend.common.exception.BusinessException;
import com.sorascm.backend.common.exception.ResourceNotFoundException;
import com.sorascm.backend.inventory.dto.InventoryDto;
import com.sorascm.backend.inventory.entity.Inventory;
import com.sorascm.backend.inventory.entity.InventoryMovement;
import com.sorascm.backend.inventory.entity.MovementType;
import com.sorascm.backend.inventory.repository.InventoryMovementRepository;
import com.sorascm.backend.inventory.repository.InventoryRepository;
import com.sorascm.backend.outbox.service.OutboxService;
import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.product.repository.ProductRepository;
import com.sorascm.backend.warehouse.entity.WarehouseLocation;
import com.sorascm.backend.warehouse.repository.WarehouseLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

/**
 * Core engine managing stock levels, physical transfers, allocations,
 * and double-entry immutable inventory movement ledgers.
 *
 * Concurrency is guaranteed via pessimistic write locks (SELECT ... FOR UPDATE)
 * at the row level to prevent overselling and race conditions.
 */
@Service
@Transactional(readOnly = true)
public class InventoryService {

    private static final Logger log = LoggerFactory.getLogger(InventoryService.class);

    private final InventoryRepository inventoryRepository;
    private final InventoryMovementRepository movementRepository;
    private final ProductRepository productRepository;
    private final WarehouseLocationRepository locationRepository;
    private final OutboxService outboxService;

    public InventoryService(
            InventoryRepository inventoryRepository,
            InventoryMovementRepository movementRepository,
            ProductRepository productRepository,
            WarehouseLocationRepository locationRepository,
            OutboxService outboxService
    ) {
        this.inventoryRepository = inventoryRepository;
        this.movementRepository = movementRepository;
        this.productRepository = productRepository;
        this.locationRepository = locationRepository;
        this.outboxService = outboxService;
    }

    /**
     * Adjusts physical on-hand stock for receiving, cycle counts, or manual write-offs.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public InventoryDto.StockResponse adjustStock(InventoryDto.AdjustStockRequest req) {
        // 1. Acquire an exclusive lock on the row so nobody else modifies this bin simultaneously
        Inventory inventory = getOrCreateInventoryWithLock(req.productId(), req.locationId());

        // 2. Derive delta based on movement type
        int delta = switch (req.movementType()) {
            case INBOUND_RECEIPT, ADJUSTMENT_ADD -> req.quantity();
            case OUTBOUND_SHIP, ADJUSTMENT_SUB -> -req.quantity();
            default -> throw new BusinessException(
                    "INVALID_OPERATION",
                    "Unsupported manual adjustment type: " + req.movementType(),
                    HttpStatus.BAD_REQUEST
            );
        };

        // 3. Ensure we never drop below reserved orders
        int newOnHand = inventory.getQuantityOnHand() + delta;
        if (newOnHand < inventory.getQuantityReserved()) {
            throw new BusinessException(
                    "INSUFFICIENT_STOCK",
                    "Resulting on-hand stock (%d) cannot be less than reserved stock (%d)"
                            .formatted(newOnHand, inventory.getQuantityReserved()),
                    HttpStatus.CONFLICT
            );
        }

        // 4. Update state and save
        inventory.setQuantityOnHand(newOnHand);
        Inventory savedInventory = inventoryRepository.save(inventory);

        // 5. Append immutable ledger movement
        recordMovement(savedInventory, req.movementType().name(), delta, req.referenceType(), req.referenceId(), req.notes());

        // 6. Enqueue outbox event inside this exact same database transaction
        outboxService.recordEvent(
                "INVENTORY",
                savedInventory.getId().toString(),
                "INVENTORY_STOCK_CHANGED",
                Map.of(
                        "inventoryId", savedInventory.getId(),
                        "productId", savedInventory.getProduct().getId(),
                        "sku", savedInventory.getProduct().getSku(),
                        "locationId", savedInventory.getLocation().getId(),
                        "quantityOnHand", savedInventory.getQuantityOnHand(),
                        "quantityReserved", savedInventory.getQuantityReserved(),
                        "movementType", req.movementType().name()
                )
        );

        log.info("Stock adjusted for SKU: {} at Location: {}. New On-Hand: {}",
                savedInventory.getProduct().getSku(), savedInventory.getLocation().getCode(), newOnHand);

        return mapToStockResponse(savedInventory);
    }

    /**
     * Transfers inventory between two bins within or across warehouses.
     * Prevents database deadlocks by acquiring locks in deterministic ascending ID order.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void transferStock(InventoryDto.TransferStockRequest req) {
        if (req.fromLocationId().equals(req.toLocationId())) {
            throw new BusinessException(
                    "INVALID_TRANSFER",
                    "Source and destination locations cannot be identical",
                    HttpStatus.BAD_REQUEST
            );
        }

        // Deadlock prevention: always lock lower ID first
        Inventory source;
        Inventory target;

        if (req.fromLocationId() < req.toLocationId()) {
            source = getOrCreateInventoryWithLock(req.productId(), req.fromLocationId());
            target = getOrCreateInventoryWithLock(req.productId(), req.toLocationId());
        } else {
            target = getOrCreateInventoryWithLock(req.productId(), req.toLocationId());
            source = getOrCreateInventoryWithLock(req.productId(), req.fromLocationId());
        }

        int available = source.getQuantityOnHand() - source.getQuantityReserved();
        if (available < req.quantity()) {
            throw new BusinessException(
                    "INSUFFICIENT_AVAILABLE_STOCK",
                    "Cannot transfer %d units. Available: %d".formatted(req.quantity(), available),
                    HttpStatus.CONFLICT
            );
        }

        // Deduct from source
        source.setQuantityOnHand(source.getQuantityOnHand() - req.quantity());
        inventoryRepository.save(source);
        recordMovement(source, MovementType.INTERNAL_TRANSFER.name(), -req.quantity(), "TRANSFER_OUT", null, req.notes());

        // Add to destination
        target.setQuantityOnHand(target.getQuantityOnHand() + req.quantity());
        inventoryRepository.save(target);
        recordMovement(target, MovementType.INTERNAL_TRANSFER.name(), req.quantity(), "TRANSFER_IN", null, req.notes());

        // Notify downstream consumers of transfer
        outboxService.recordEvent(
                "INVENTORY",
                source.getId().toString(),
                "INVENTORY_TRANSFERRED",
                Map.of(
                        "productId", req.productId(),
                        "fromLocationId", req.fromLocationId(),
                        "toLocationId", req.toLocationId(),
                        "quantity", req.quantity()
                )
        );
    }

    /**
     * Reserves stock for an approved sales order without physically moving it yet.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void allocateStock(UUID productId, Long locationId, int quantity, UUID salesOrderId) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationIdWithLock(productId, locationId)
                .orElseThrow(() -> new BusinessException("STOCK_NOT_FOUND", "No stock found at location", HttpStatus.NOT_FOUND));

        int available = inventory.getQuantityOnHand() - inventory.getQuantityReserved();
        if (available < quantity) {
            throw new BusinessException(
                    "INSUFFICIENT_STOCK",
                    "Cannot reserve %d units. Available: %d".formatted(quantity, available),
                    HttpStatus.CONFLICT
            );
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() + quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.RESERVATION_HOLD.name(), quantity, "SALES_ORDER_ALLOCATION", salesOrderId, "Reserved for SO");

        outboxService.recordEvent(
                "SALES_ORDER",
                salesOrderId.toString(),
                "STOCK_ALLOCATED",
                Map.of("salesOrderId", salesOrderId, "productId", productId, "quantity", quantity)
        );
    }

    /**
     * Deducts reserved inventory upon physical dispatch from warehouse loading bay.
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void dispatchStock(UUID productId, Long locationId, int quantity, UUID salesOrderId) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationIdWithLock(productId, locationId)
                .orElseThrow(() -> new BusinessException("STOCK_NOT_FOUND", "No stock found at location", HttpStatus.NOT_FOUND));

        if (inventory.getQuantityReserved() < quantity || inventory.getQuantityOnHand() < quantity) {
            throw new BusinessException("ILLEGAL_DISPATCH", "Cannot dispatch unreserved stock", HttpStatus.CONFLICT);
        }

        inventory.setQuantityReserved(inventory.getQuantityReserved() - quantity);
        inventory.setQuantityOnHand(inventory.getQuantityOnHand() - quantity);
        inventoryRepository.save(inventory);

        recordMovement(inventory, MovementType.OUTBOUND_SHIP.name(), -quantity, "SALES_ORDER_SHIPMENT", salesOrderId, "Shipped to customer");

        outboxService.recordEvent(
                "SALES_ORDER",
                salesOrderId.toString(),
                "ORDER_DISPATCHED",
                Map.of("salesOrderId", salesOrderId, "productId", productId, "quantityShipped", quantity)
        );
    }

    /**
     * Read-only stock lookup.
     */
    public InventoryDto.StockResponse getStock(UUID productId, Long locationId) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationId(productId, locationId)
                .orElseThrow(() -> new BusinessException(
                        "INVENTORY_NOT_FOUND",
                        "No inventory record exists for product in specified location",
                        HttpStatus.NOT_FOUND
                ));
        return mapToStockResponse(inventory);
    }

    /**
     * Returns an audit trail of stock movements for a specific inventory bucket.
     */
    public Page<InventoryDto.MovementResponse> getMovementLedger(Long inventoryId, Pageable pageable) {
        return movementRepository.findByInventoryIdOrderByCreatedAtDesc(inventoryId, pageable)
                .map(m -> new InventoryDto.MovementResponse(
                        m.getId(),
                        m.getInventory().getId(),
                        m.getMovementType(),
                        m.getDeltaQuantity(),
                        m.getReferenceType(),
                        m.getReferenceId(),
                        m.getNotes(),
                        m.getCreatedAt()
                ));
    }

    // ==========================================
    // Internal Helper Methods
    // ==========================================

    private Inventory getOrCreateInventoryWithLock(UUID productId, Long locationId) {
        return inventoryRepository.findByProductIdAndLocationIdWithLock(productId, locationId)
                .orElseGet(() -> {
                    Product product = productRepository.findById(productId)
                            .orElseThrow(() -> new ResourceNotFoundException("Product", productId));
                    WarehouseLocation location = locationRepository.findById(locationId)
                            .orElseThrow(() -> new ResourceNotFoundException("WarehouseLocation", locationId));

                    Inventory newInv = new Inventory();
                    newInv.setProduct(product);
                    newInv.setLocation(location);
                    newInv.setQuantityOnHand(0);
                    newInv.setQuantityReserved(0);
                    return inventoryRepository.saveAndFlush(newInv);
                });
    }

    private void recordMovement(Inventory inventory, String type, int delta, String refType, UUID refId, String notes) {
        InventoryMovement movement = new InventoryMovement();
        movement.setInventory(inventory);
        movement.setMovementType(type);
        movement.setDeltaQuantity(delta);
        movement.setReferenceType(refType);
        movement.setReferenceId(refId);
        movement.setNotes(notes);
        movementRepository.save(movement);
    }

    private InventoryDto.StockResponse mapToStockResponse(Inventory inv) {
        return new InventoryDto.StockResponse(
                inv.getId(),
                inv.getProduct().getId(),
                inv.getProduct().getSku(),
                inv.getProduct().getName(),
                inv.getLocation().getId(),
                inv.getLocation().getCode(),
                inv.getQuantityOnHand(),
                inv.getQuantityReserved(),
                inv.getQuantityOnHand() - inv.getQuantityReserved()
        );
    }
}