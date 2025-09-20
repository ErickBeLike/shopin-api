package com.app.shopin.modules.security.dto.twofactor;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginTwoFactorRequestDTO(
        @NotBlank @Email String email,
        @NotBlank String code
) {}