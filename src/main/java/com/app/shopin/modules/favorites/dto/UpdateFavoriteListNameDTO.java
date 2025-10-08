package com.app.shopin.modules.favorites.dto;

import jakarta.validation.constraints.NotBlank;

public record UpdateFavoriteListNameDTO(@NotBlank String name) {}
