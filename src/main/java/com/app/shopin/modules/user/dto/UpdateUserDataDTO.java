package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UpdateUserDataDTO(
        @NotBlank String userName,
        @Email @NotBlank String email
        // More data soon...
) {}
