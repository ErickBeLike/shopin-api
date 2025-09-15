package com.app.shopin.modules.security.dto;

import jakarta.validation.constraints.NotBlank;

public record PasswordConfirmationDTO(@NotBlank String password) {
}
