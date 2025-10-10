package com.app.shopin.modules.cart.entity;

import com.app.shopin.modules.product.entity.Product;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "cart_items")
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false)
    private int quantity;

    // --- MÉTODO AUXILIAR PARA CALCULAR EL SUBTOTAL ---
    @Transient
    public BigDecimal getSubtotal() {
        if (product == null || product.getEffectivePrice() == null) {
            return BigDecimal.ZERO;
        }
        return product.getEffectivePrice().multiply(BigDecimal.valueOf(quantity));
    }

    // Getters y Setters...
}