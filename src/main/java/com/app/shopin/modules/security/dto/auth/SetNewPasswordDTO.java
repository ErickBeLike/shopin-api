package com.app.shopin.modules.security.dto.auth;

public record SetNewPasswordDTO(String validationToken, String newPassword) {}

