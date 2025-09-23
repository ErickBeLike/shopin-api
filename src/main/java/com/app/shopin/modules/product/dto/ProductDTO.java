package com.app.shopin.modules.product.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record ProductDTO(
        Long id,

        @NotBlank(message = "El SKU es obligatorio")
        String sku,

        @NotBlank(message = "El nombre del producto es obligatorio")
        String name,
        List<ProductMediaDTO> media,
        String description,

        @NotNull(message = "El precio es obligatorio")
        @Positive(message = "El precio debe ser mayor que cero")
        BigDecimal price,
        @Min(value = 0, message = "El descuento no puede ser negativo")
        @Max(value = 100, message = "El descuento no puede ser mayor a 100")
        Integer discountPercent,

        BigDecimal effectivePrice,


        @NotNull(message = "La cantidad en stock es obligatoria")
        @PositiveOrZero(message = "El stock no puede ser negativo")
        Integer stockQuantity,

        @NotNull(message = "El ID de la categoría es obligatorio")
        Long categoryId
) {}