package com.app.shopin.modules.order.dto;

import jakarta.validation.constraints.NotNull;

public record CreateOrderDTO(
        @NotNull Long cartId,
        @NotNull Long addressId,
        @NotNull Long paymentMethodId
) {}
