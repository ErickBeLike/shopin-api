package com.app.shopin.modules.security.jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${oauth2.redirect.url}")
    private String frontendRedirectUrl;

    @Autowired
    private JwtProvider jwtProvider;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // 1. Genera tus tokens (Access y Refresh)
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        // 2. Crea y configura la cookie para el Refresh Token
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30 días
        response.addCookie(refreshTokenCookie);

        // 3. Redirige al frontend con el Access Token como parámetro
        String redirectUrl = frontendRedirectUrl + "?token=" + accessToken; // Cambia la URL a la de tu frontend
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}