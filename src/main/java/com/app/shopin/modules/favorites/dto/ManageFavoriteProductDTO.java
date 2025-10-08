package com.app.shopin.modules.favorites.dto;

import jakarta.validation.constraints.NotNull;

public record ManageFavoriteProductDTO(
        @NotNull Long productId
) {}