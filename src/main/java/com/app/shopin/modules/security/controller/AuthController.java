package com.app.shopin.modules.security.controller;


import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.dto.*;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.enums.TwoFactorMethod;
import com.app.shopin.modules.security.service.AuthService;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.util.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    @Autowired
    AuthService authService;

    @PostMapping("/forgot-password")
    public ResponseEntity<UserResponse> forgotPassword(@RequestBody PasswordResetRequestDTO request) {
        UserResponse response = authService.requestPasswordReset(request.email());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/validate-reset-code")
    public ResponseEntity<ValidationResponseDTO> validateResetCode(@RequestBody ValidateCodeRequestDTO request) {
        ValidationResponseDTO response = authService.validateResetCode(request.code());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/set-new-password")
    public ResponseEntity<UserResponse> setNewPassword(@Valid @RequestBody SetNewPasswordDTO request) {
        UserResponse response = authService.setNewPassword(request.validationToken(), request.newPassword());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletResponse response) {

        User user = authService.authenticateAndGetUser(loginDTO);

        if (user.getPreferredTwoFactorMethod() != TwoFactorMethod.NONE) {
            authService.sendTwoFactorCodeIfApplicable(user);
            return ResponseEntity.ok(Map.of(
                    "twoFactorRequired", true,
                    "preferredMethod", user.getPreferredTwoFactorMethod()
            ));
        }

        AuthService.TokenPair tokenPair = authService.generateTokensForUser(user);
        return createLoginSuccessResponse(tokenPair, response);
    }

    // 2FA APP SECTION
    @PostMapping("/login/verify-app")
    public ResponseEntity<LoginResponseDTO> verifyAppCode(
            @Valid @RequestBody LoginTwoFactorRequestDTO dto,
            HttpServletResponse response) {
        AuthService.TokenPair tokenPair = authService.verifyAppCodeAndLogin(dto);
        return createLoginSuccessResponse(tokenPair, response);
    }

    // 2FA EMAIL SECTION
    @PostMapping("/login/verify-email")
    public ResponseEntity<LoginResponseDTO> verifyEmailCode(
            @Valid @RequestBody LoginTwoFactorRequestDTO dto,
            HttpServletResponse response) {
        AuthService.TokenPair tokenPair = authService.verifyEmailCodeAndLogin(dto);
        return createLoginSuccessResponse(tokenPair, response);
    }

    private ResponseEntity<LoginResponseDTO> createLoginSuccessResponse(AuthService.TokenPair tokenPair, HttpServletResponse response) {
        // 1. Creamos y configuramos la cookie para el Refresh Token.
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenPair.refreshToken());
        refreshTokenCookie.setHttpOnly(true);
        refreshTokenCookie.setSecure(true); // Siempre 'true' en producción (solo HTTPS).
        refreshTokenCookie.setPath("/api/auth/refresh");
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30 días.
        response.addCookie(refreshTokenCookie);

        // 2. Obtenemos detalles del usuario autenticado para la respuesta.
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
        User user = principal.getUser();
        List<String> roles = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        // 3. Construimos el cuerpo de la respuesta JSON.
        LoginResponseDTO loginResponse = new LoginResponseDTO(tokenPair.accessToken(), user.getFullTag(), roles);

        return ResponseEntity.ok(loginResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDTO> refresh(
            @CookieValue(name = "refreshToken", required = false) String refreshToken) throws ParseException {

        if (refreshToken == null) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "No se encontró el refresh token.");
        }
        return ResponseEntity.ok(authService.refresh(refreshToken));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(@RequestHeader("Authorization") String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.replace("Bearer ", "");
            authService.logout(token);
            return ResponseEntity.ok(Map.of("message", "Sesión cerrada correctamente"));
        }
        return ResponseEntity.badRequest().body(Map.of("error", "Token no proporcionado"));
    }

}
