package com.app.shopin.modules.security.jwt;

import com.app.shopin.modules.security.dto.OAuth2TempInfo;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.SocialLink;
import com.app.shopin.modules.security.service.UserDetailsServiceImpl;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.SocialLinkRepository;
import com.app.shopin.modules.user.repository.UserRepository;
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
import java.util.Map;
import java.util.Optional;

@Component
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    @Value("${oauth2.redirect.url}")
    private String frontendRedirectUrl;

    @Autowired
    private JwtProvider jwtProvider;

    @Autowired
    private SocialLinkRepository socialLinkRepository;
    @Autowired
    private UserRepository userRepository;

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

        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);

        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // ¡True en producción!
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60);
        response.addCookie(refreshTokenCookie);

        String redirectUrl = frontendRedirectUrl + "?token=" + accessToken;
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    // EN ESTE VEO QUE EN LO QUE ME DISTE HAY OTRO MÉTODO ACÁ PERO ESE NO LO TENGO
}