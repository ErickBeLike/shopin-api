package com.app.shopin.modules.security.dto;

import java.util.List;

public record LoginStepOneResponseDTO(
        boolean twoFactorRequired,
        String accessToken, // Será nulo si twoFactorRequired es true
        String username,    // Será nulo si twoFactorRequired es true
        List<String> roles  // Será nulo si twoFactorRequired es true
) {}