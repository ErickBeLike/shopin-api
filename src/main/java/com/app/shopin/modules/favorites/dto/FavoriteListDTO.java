package com.app.shopin.modules.favorites.dto;

import com.app.shopin.modules.favorites.entity.FavoriteListIcon;
import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record FavoriteListDTO(
        Long id,
        @NotBlank String name,
        FavoriteListIcon icon,
        List<Long> productIds
) {}