package com.app.shopin.modules.security.controller;


import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.dto.*;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.service.AuthService;
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
    public ResponseEntity<LoginResponseDTO> login(
            @Valid @RequestBody LoginDTO loginDTO,
            HttpServletResponse response) { // Inyectamos HttpServletResponse

        AuthService.TokenPair tokenPair = authService.login(loginDTO);

        // 1. Creamos la cookie para el Refresh Token
        Cookie refreshTokenCookie = new Cookie("refreshToken", tokenPair.refreshToken());
        refreshTokenCookie.setHttpOnly(true); // ¡Importante! Inaccesible desde JavaScript
        refreshTokenCookie.setSecure(true); // Solo en HTTPS (en producción)
        refreshTokenCookie.setPath("/api/auth/refresh"); // Solo se envía a este endpoint
        refreshTokenCookie.setMaxAge(30 * 24 * 60 * 60); // 30 días

        // 2. Añadimos la cookie a la respuesta HTTP
        response.addCookie(refreshTokenCookie);

        // 3. Devolvemos el Access Token y datos del usuario en el cuerpo JSON
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
        List<String> roles = principal.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList());
        LoginResponseDTO loginResponse = new LoginResponseDTO(tokenPair.accessToken(), principal.getUsername(), roles);

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
