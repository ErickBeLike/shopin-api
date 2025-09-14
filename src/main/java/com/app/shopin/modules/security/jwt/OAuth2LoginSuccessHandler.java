package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.security.service.UserDetailsServiceImpl;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${oauth2.redirect.url}")
    private String frontendRedirectUrl;

    @Autowired
    private JwtProvider jwtProvider;

    // YA NO NECESITAMOS UserDetailsServiceImpl aquí

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        // Obtenemos el usuario de OAuth2 que nos da Spring.
        OAuth2User oauth2User = (OAuth2User) authentication.getPrincipal();

        // Llamamos a los nuevos métodos del JwtProvider que saben cómo manejar un OAuth2User.
        String accessToken = jwtProvider.generateAccessToken(oauth2User);
        String refreshToken = jwtProvider.generateRefreshToken(oauth2User);

        // El resto del código para la cookie y la redirección es el mismo.
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(false);
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);

        String redirectUrl = frontendRedirectUrl + "?token=" + accessToken;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}