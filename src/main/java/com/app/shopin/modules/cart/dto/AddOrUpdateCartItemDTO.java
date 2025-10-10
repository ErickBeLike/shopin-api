package com.app.shopin.modules.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record AddOrUpdateCartItemDTO(
        @NotNull Long productId,
        @NotNull @Min(1) int quantity
) {}