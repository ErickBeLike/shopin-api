package com.app.shopin.modules.product.dto;

import jakarta.validation.constraints.NotBlank;

public record CategoryDTO(
        Long id,

        @NotBlank(message = "El nombre de la categoría es obligatorio")
        String name,

        String description,

        // Se usa para anidar esta categoría debajo de otra. Si es null, es una categoría principal.
        Long parentId
) {}
