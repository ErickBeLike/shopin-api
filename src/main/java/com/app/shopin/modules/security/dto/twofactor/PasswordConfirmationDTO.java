package com.app.shopin.modules.security.dto.twofactor;

import jakarta.validation.constraints.NotBlank;

public record PasswordConfirmationDTO(@NotBlank String password) {
}
