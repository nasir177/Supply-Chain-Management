package com.sorascm.backend.fulfillment.entity;

import com.sorascm.backend.product.entity.Product;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "pack_task_lines")
public class PackTaskLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pack_task_id", nullable = false)
    private PackTask packTask;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_packed", nullable = false)
    private int quantityPacked;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public PackTask getPackTask() { return packTask; }
    public void setPackTask(PackTask packTask) { this.packTask = packTask; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public int getQuantityPacked() { return quantityPacked; }
    public void setQuantityPacked(int quantityPacked) { this.quantityPacked = quantityPacked; }
    public Instant getCreatedAt() { return createdAt; }
}