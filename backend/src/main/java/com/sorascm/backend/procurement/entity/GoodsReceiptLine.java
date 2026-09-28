package com.sorascm.backend.procurement.entity;

import com.sorascm.backend.product.entity.Product;
import jakarta.persistence.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Entity
@Table(name = "goods_receipt_lines")
@EntityListeners(AuditingEntityListener.class)
public class GoodsReceiptLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "goods_receipt_id", nullable = false)
    private GoodsReceipt goodsReceipt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "po_line_id", nullable = false)
    private PurchaseOrderLine poLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "quantity_accepted", nullable = false)
    private int quantityAccepted;

    @Column(name = "quantity_rejected", nullable = false)
    private int quantityRejected = 0;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public Long getId() { return id; }
    public GoodsReceipt getGoodsReceipt() { return goodsReceipt; }
    public void setGoodsReceipt(GoodsReceipt goodsReceipt) { this.goodsReceipt = goodsReceipt; }
    public PurchaseOrderLine getPoLine() { return poLine; }
    public void setPoLine(PurchaseOrderLine poLine) { this.poLine = poLine; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public int getQuantityAccepted() { return quantityAccepted; }
    public void setQuantityAccepted(int quantityAccepted) { this.quantityAccepted = quantityAccepted; }
    public int getQuantityRejected() { return quantityRejected; }
    public void setQuantityRejected(int quantityRejected) { this.quantityRejected = quantityRejected; }
    public String getRejectionReason() { return rejectionReason; }
    public void setRejectionReason(String rejectionReason) { this.rejectionReason = rejectionReason; }
    public Instant getCreatedAt() { return createdAt; }
}