package com.app.shopin.modules.security.dto;

public record OAuth2TempInfo(String provider,
                             String providerUserId,
                             String email,
                             String firstName,
                             String lastName,
                             String pictureUrl) {}

