package com.app.shopin.modules.payment.dto;

public record PaymentMethodDTO(
        Long id,
        String cardBrand,
        String lastFourDigits,
        Integer expirationMonth,
        Integer expirationYear
) {}