package com.app.shopin.modules.cart.dto;

import java.math.BigDecimal;

public record CartItemDTO(
        Long itemId,
        Long productId,
        String productName,
        int quantity,
        BigDecimal price,
        BigDecimal subtotal,
        String imageUrl // La primera imagen del producto
) {}
