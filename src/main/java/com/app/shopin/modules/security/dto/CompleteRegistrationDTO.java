package com.app.shopin.modules.security.dto;

import jakarta.validation.constraints.NotBlank;

public record CompleteRegistrationDTO(
        @NotBlank String username,
        @NotBlank String registrationToken
) {}
