package com.app.shopin.modules.favorites.dto;

import jakarta.validation.constraints.NotNull;
import java.util.List;

public record AssignProductToListsDTO(
        @NotNull Long productId,
        List<Long> listIds
) {}
