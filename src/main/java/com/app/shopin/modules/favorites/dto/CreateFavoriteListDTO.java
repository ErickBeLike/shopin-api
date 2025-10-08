package com.app.shopin.modules.favorites.dto;

import com.app.shopin.modules.favorites.entity.FavoriteListIcon;
import jakarta.validation.constraints.NotBlank;

public record CreateFavoriteListDTO(@NotBlank String name, FavoriteListIcon icon) {}