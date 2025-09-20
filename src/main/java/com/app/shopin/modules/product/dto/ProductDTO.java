package com.app.shopin.modules.product.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

public record ProductDTO(
        Long id,

        @NotBlank(message = "El SKU es obligatorio")
        String sku,

        @NotBlank(message = "El nombre del producto es obligatorio")
        String name,

        String description,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        BigDecimal price,

        @NotNull(message = "La cantidad en stock es obligatoria")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stockQuantity,

        @NotNull(message = "El ID de la categoría es obligatorio")
        Long categoryId
) {}