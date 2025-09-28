package com.app.shopin.modules.promotion.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.List;

public record PromotionDTO(
        Long id,

        @NotBlank
        String name,

        String description,

        @NotNull @Min(1) @Max(100)
        Integer discountPercent,

        LocalDateTime startDate,

        LocalDateTime endDate,

        boolean isActive,

        List<Long> productIds,
        List<Long> categoryIds

) {}
