package com.app.shopin.modules.favorites.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateFavoriteListDTO(@NotBlank String name) {}