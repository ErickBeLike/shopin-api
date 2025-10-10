package com.app.shopin.modules.cart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartDTO(
        Long cartId,
        List<CartItemDTO> items,
        BigDecimal grandTotal
) {}
