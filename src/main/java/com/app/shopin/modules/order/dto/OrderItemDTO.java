package com.app.shopin.modules.order.dto;

import java.math.BigDecimal;

public record OrderItemDTO(
        Long productId,
        String productName,
        int quantity,
        BigDecimal originalPriceAtPurchase,
        BigDecimal finalPriceAtPurchase
) {}
