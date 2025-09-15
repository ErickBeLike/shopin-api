package com.app.shopin.modules.security.dto;

import jakarta.validation.constraints.NotBlank;

public record CodeConfirmationDTO(@NotBlank String code) {
}
