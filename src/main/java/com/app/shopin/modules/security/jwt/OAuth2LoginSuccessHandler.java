package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.security.dto.OAuth2TempInfo;
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
import org.springframework.web.util.UriComponentsBuilder;

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

        // 1. Revisamos la sesión para ver si es un registro incompleto
        OAuth2TempInfo tempInfo = (OAuth2TempInfo) request.getSession().getAttribute("OAUTH2_TEMP_INFO");

        if (tempInfo != null) {
            // 2. Si SÍ es incompleto, lo redirigimos al formulario de finalización en el frontend.
            // Limpiamos el atributo para que no se quede en la sesión.
            request.getSession().removeAttribute("OAUTH2_TEMP_INFO");

            String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/complete-registration") // Nueva ruta en tu frontend
                    .queryParam("provider", tempInfo.provider()) // Le pasamos info útil
                    .queryParam("name", tempInfo.firstName())
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return; // Detenemos el proceso aquí.
        }

        // 3. Si NO es incompleto, es un login normal. Generamos el JWT.
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);

        // TODO CHANGE TO TRUE IN PRODUCTION
        refreshTokenCookie.setSecure(true);

        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);

        String redirectUrl = frontendRedirectUrl + "?token=" + accessToken;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}