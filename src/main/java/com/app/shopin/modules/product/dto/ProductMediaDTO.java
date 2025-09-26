package com.app.shopin.modules.product.dto;

public record ProductMediaDTO(
        Long id,
        String url,
        String mediaType // "IMAGE" o "VIDEO"
) {}
