package com.app.shopin.modules.security.controller;


import com.app.shopin.modules.security.dto.*;
import com.app.shopin.modules.security.service.AuthService;
import com.app.shopin.util.UserResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.util.Map;

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
    public ResponseEntity<JwtDTO> login(@Valid @RequestBody LoginDTO loginDTO){
        return ResponseEntity.ok(authService.login(loginDTO));
    }

    @PostMapping("/refresh")
    public ResponseEntity<JwtDTO> refresh(@RequestBody JwtDTO jwtDTO) throws ParseException {
        return ResponseEntity.ok(authService.refresh(jwtDTO));
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
