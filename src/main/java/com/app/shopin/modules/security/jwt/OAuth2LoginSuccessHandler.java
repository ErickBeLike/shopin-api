package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.security.dto.OAuth2TempInfo;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
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

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {

        PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
        User user = principal.getUser();

        if ("PENDING_REGISTRATION".equals(user.getEmail())) {
            OAuth2TempInfo tempInfo = (OAuth2TempInfo) request.getSession().getAttribute("OAUTH2_TEMP_INFO");
            request.getSession().removeAttribute("OAUTH2_TEMP_INFO");

            if (tempInfo != null) {
                String registrationToken = jwtProvider.generateRegistrationToken(tempInfo);
                String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                        .path("/complete-registration")
                        .queryParam("token", registrationToken)
                        .build().toUriString();

                getRedirectStrategy().sendRedirect(request, response, targetUrl);
                return;
            }
        }

        String flowType = (String) request.getSession().getAttribute("OAUTH2_FLOW_TYPE");
        if ("LINKING".equals(flowType)) {
            request.getSession().removeAttribute("OAUTH2_FLOW_TYPE"); // Limpiamos la nota

            // Redirigimos a una URL de éxito de vinculación, por ejemplo, al perfil.
            String targetUrl = UriComponentsBuilder.fromUriString(frontendRedirectUrl)
                    .path("/profile") // O a la ruta de configuración que prefieras
                    .queryParam("link_success", "true") // Parámetro para que el frontend muestre un mensaje
                    .build().toUriString();

            getRedirectStrategy().sendRedirect(request, response, targetUrl);
            return;
        }

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