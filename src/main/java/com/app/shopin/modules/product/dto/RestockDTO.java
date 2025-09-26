package com.app.shopin.modules.product.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record RestockDTO(
        @NotNull(message = "La cantidad no puede ser nula.")
        @Positive(message = "La cantidad a reabastecer debe ser mayor que cero.")
        Integer quantityToAdd
) {}
