package com.app.shopin.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

public record SetPasswordDTO(@NotBlank String newPassword) {}
