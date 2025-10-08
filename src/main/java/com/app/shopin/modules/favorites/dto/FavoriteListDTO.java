package com.app.shopin.modules.favorites.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FavoriteListDTO(
        Long id,
        @NotBlank String name,
        List<Long> productIds
) {}