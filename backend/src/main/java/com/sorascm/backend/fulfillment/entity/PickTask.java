package com.sorascm.backend.fulfillment.entity;

import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.warehouse.entity.WarehouseLocation;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pick_tasks")
@EntityListeners(AuditingEntityListener.class)
public class PickTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "task_number", nullable = false, unique = true, length = 64)
    private String taskNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_line_id", nullable = false)
    private SalesOrderLine salesOrderLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_location_id", nullable = false)
    private WarehouseLocation sourceLocation;

    @Column(name = "quantity_to_pick", nullable = false)
    private int quantityToPick;

    @Column(name = "quantity_picked", nullable = false)
    private int quantityPicked = 0;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PickTaskStatus status = PickTaskStatus.PENDING;

    @Column(name = "assigned_picker", length = 128)
    private String assignedPicker;

    @Column(name = "picked_at")
    private Instant pickedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getId() { return id; }
    public String getTaskNumber() { return taskNumber; }
    public void setTaskNumber(String taskNumber) { this.taskNumber = taskNumber; }
    public SalesOrder getSalesOrder() { return salesOrder; }
    public void setSalesOrder(SalesOrder salesOrder) { this.salesOrder = salesOrder; }
    public SalesOrderLine getSalesOrderLine() { return salesOrderLine; }
    public void setSalesOrderLine(SalesOrderLine salesOrderLine) { this.salesOrderLine = salesOrderLine; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public WarehouseLocation getSourceLocation() { return sourceLocation; }
    public void setSourceLocation(WarehouseLocation sourceLocation) { this.sourceLocation = sourceLocation; }
    public int getQuantityToPick() { return quantityToPick; }
    public void setQuantityToPick(int quantityToPick) { this.quantityToPick = quantityToPick; }
    public int getQuantityPicked() { return quantityPicked; }
    public void setQuantityPicked(int quantityPicked) { this.quantityPicked = quantityPicked; }
    public PickTaskStatus getStatus() { return status; }
    public void setStatus(PickTaskStatus status) { this.status = status; }
    public String getAssignedPicker() { return assignedPicker; }
    public void setAssignedPicker(String assignedPicker) { this.assignedPicker = assignedPicker; }
    public Instant getPickedAt() { return pickedAt; }
    public void setPickedAt(Instant pickedAt) { this.pickedAt = pickedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}