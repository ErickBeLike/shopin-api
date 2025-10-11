package com.app.shopin.modules.order.dto;

import com.app.shopin.modules.order.entity.OrderStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDTO(
        Long id,
        LocalDateTime createdAt,
        OrderStatus status,
        BigDecimal totalAmount,
        List<OrderItemDTO> items
) {}
