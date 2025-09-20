package com.app.shopin.modules.security.dto.twofactor;

import com.app.shopin.modules.security.enums.TwoFactorMethod;
import jakarta.validation.constraints.NotNull;

public record SetPreferredTwoFactorDTO(@NotNull TwoFactorMethod method) {
}
