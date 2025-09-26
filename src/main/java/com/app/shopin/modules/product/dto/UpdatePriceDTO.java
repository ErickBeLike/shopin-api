package com.app.shopin.modules.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdatePriceDTO(
        @NotNull(message = "El nuevo precio no puede ser nulo.")
        @Positive(message = "El precio debe ser un valor positivo.")
        BigDecimal newPrice
) {}
