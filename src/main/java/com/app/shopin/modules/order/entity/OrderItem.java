package com.app.shopin.modules.order.entity;

import com.app.shopin.modules.product.entity.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "order_items")
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false, precision = 10, scale = 2) // BasePriceProduct
    private BigDecimal originalPriceAtPurchase;
    @Column(nullable = false, precision = 10, scale = 2) // FinalPriceProduct
    private BigDecimal finalPriceAtPurchase;
    @Column(precision = 10, scale = 2) // DiscountProduct
    private BigDecimal discountAmountAtPurchase;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public int getQuantity() {
        return quantity;
    }

    public void setQuantity(int quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getOriginalPriceAtPurchase() {
        return originalPriceAtPurchase;
    }

    public void setOriginalPriceAtPurchase(BigDecimal originalPriceAtPurchase) {
        this.originalPriceAtPurchase = originalPriceAtPurchase;
    }

    public BigDecimal getFinalPriceAtPurchase() {
        return finalPriceAtPurchase;
    }

    public void setFinalPriceAtPurchase(BigDecimal finalPriceAtPurchase) {
        this.finalPriceAtPurchase = finalPriceAtPurchase;
    }

    public BigDecimal getDiscountAmountAtPurchase() {
        return discountAmountAtPurchase;
    }

    public void setDiscountAmountAtPurchase(BigDecimal discountAmountAtPurchase) {
        this.discountAmountAtPurchase = discountAmountAtPurchase;
    }
}
