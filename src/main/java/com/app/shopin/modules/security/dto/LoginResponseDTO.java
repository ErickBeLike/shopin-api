package com.app.shopin.modules.security.dto;

import java.util.List;

public record LoginResponseDTO(String accessToken, String username, List<String> roles) {}

