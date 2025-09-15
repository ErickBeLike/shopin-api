package com.app.shopin.modules.security.dto;

import com.app.shopin.modules.security.enums.TwoFactorMethod;
import jakarta.validation.constraints.NotNull;

public record SetPreferredTwoFactorDTO(@NotNull TwoFactorMethod method) {
}
