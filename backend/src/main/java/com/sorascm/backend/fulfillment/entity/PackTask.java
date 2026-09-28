package com.sorascm.backend.fulfillment.entity;

import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "pack_tasks")
@EntityListeners(AuditingEntityListener.class)
public class PackTask {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "pack_number", nullable = false, unique = true, length = 64)
    private String packNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sales_order_id", nullable = false)
    private SalesOrder salesOrder;

    @Column(name = "container_type", nullable = false, length = 64)
    private String containerType = "STANDARD_BOX";

    @Column(name = "weight_kg", precision = 8, scale = 2)
    private BigDecimal weightKg;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private PackTaskStatus status = PackTaskStatus.OPEN;

    @Column(name = "packer_name", length = 128)
    private String packerName;

    @Column(name = "packed_at")
    private Instant packedAt;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @OneToMany(mappedBy = "packTask", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PackTaskLine> lines = new ArrayList<>();

    public void addLine(PackTaskLine line) {
        lines.add(line);
        line.setPackTask(this);
    }

    public UUID getId() { return id; }
    public String getPackNumber() { return packNumber; }
    public void setPackNumber(String packNumber) { this.packNumber = packNumber; }
    public SalesOrder getSalesOrder() { return salesOrder; }
    public void setSalesOrder(SalesOrder salesOrder) { this.salesOrder = salesOrder; }
    public String getContainerType() { return containerType; }
    public void setContainerType(String containerType) { this.containerType = containerType; }
    public BigDecimal getWeightKg() { return weightKg; }
    public void setWeightKg(BigDecimal weightKg) { this.weightKg = weightKg; }
    public PackTaskStatus getStatus() { return status; }
    public void setStatus(PackTaskStatus status) { this.status = status; }
    public String getPackerName() { return packerName; }
    public void setPackerName(String packerName) { this.packerName = packerName; }
    public Instant getPackedAt() { return packedAt; }
    public void setPackedAt(Instant packedAt) { this.packedAt = packedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public List<PackTaskLine> getLines() { return lines; }
}