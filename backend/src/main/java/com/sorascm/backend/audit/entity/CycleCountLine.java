package com.sorascm.backend.audit.entity;

import com.sorascm.backend.product.entity.Product;
import com.sorascm.backend.warehouse.entity.WarehouseLocation;
import jakarta.persistence.*;
import org.hibernate.annotations.Generated;
import org.hibernate.generator.EventType;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "cycle_count_lines")
public class CycleCountLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cycle_count_id", nullable = false)
    private CycleCount cycleCount;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "location_id", nullable = false)
    private WarehouseLocation location;

    @Column(name = "system_quantity", nullable = false)
    private int systemQuantity;

    @Column(name = "counted_quantity")
    private Integer countedQuantity;

    @Generated(event = EventType.INSERT)
    @Column(name = "variance", insertable = false, updatable = false)
    private Integer variance;

    @Column(name = "variance_value", precision = 12, scale = 2)
    private BigDecimal varianceValue;

    @Column(name = "counted_by", length = 128)
    private String countedBy;

    @Column(name = "counted_at")
    private Instant countedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public CycleCount getCycleCount() { return cycleCount; }
    public void setCycleCount(CycleCount cycleCount) { this.cycleCount = cycleCount; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public WarehouseLocation getLocation() { return location; }
    public void setLocation(WarehouseLocation location) { this.location = location; }
    public int getSystemQuantity() { return systemQuantity; }
    public void setSystemQuantity(int systemQuantity) { this.systemQuantity = systemQuantity; }
    public Integer getCountedQuantity() { return countedQuantity; }
    public void setCountedQuantity(Integer countedQuantity) { this.countedQuantity = countedQuantity; }
    public Integer getVariance() { return variance; }
    public BigDecimal getVarianceValue() { return varianceValue; }
    public void setVarianceValue(BigDecimal varianceValue) { this.varianceValue = varianceValue; }
    public String getCountedBy() { return countedBy; }
    public void setCountedBy(String countedBy) { this.countedBy = countedBy; }
    public Instant getCountedAt() { return countedAt; }
    public void setCountedAt(Instant countedAt) { this.countedAt = countedAt; }
    public Instant getCreatedAt() { return createdAt; }
}