package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.exception.CustomOAuth2AuthenticationException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class CustomOAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${oauth2.redirect.url}")
    private String redirectUrl;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception) throws IOException, ServletException {
        String targetUrl = redirectUrl;

        // Comprobamos si el fallo fue por nuestra excepción personalizada de "email ya existe"
        if (exception instanceof CustomOAuth2AuthenticationException) {
            // Si es así, construimos una URL de redirección con un error específico
            targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("error", "email_exists")
                    .build().toUriString();
        } else {
            // Para cualquier otro error de OAuth2, usamos un mensaje genérico
            targetUrl = UriComponentsBuilder.fromUriString(redirectUrl)
                    .queryParam("error", "oauth2_error")
                    .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}