package com.app.shopin.modules.security.dto;

public record LoginTwoFactorRequestDTO(String usernameOrEmail, String code) {}