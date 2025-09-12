package com.app.shopin.modules.security.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.services.mailtrap.EmailService;
import com.app.shopin.util.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.app.shopin.modules.security.blacklist.TokenBlacklist;
import com.app.shopin.modules.security.dto.JwtDTO;
import com.app.shopin.modules.security.dto.LoginDTO;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.security.jwt.JwtProvider;
import com.app.shopin.modules.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@Transactional
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    UserRepository userRepository;

    @Autowired
    PasswordEncoder passwordEncoder;

    @Autowired
    AuthenticationManager authenticationManager;

    @Autowired
    RolService rolService;

    @Autowired
    JwtProvider jwtProvider;

    @Autowired
    TokenBlacklist tokenBlacklist;

    @Autowired
    private EmailService emailService;


    public Optional<User> getByUserName(String userName) {
        return userRepository.findByUserName(userName);
    }

    public boolean existsByUserName(String userName) {
        return userRepository.existsByUserName(userName);
    }

    public UserResponse requestPasswordReset(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No existe un usuario con ese correo electrónico."));

        // 1. Generar código aleatorio de 6 dígitos
        String code = generateRandomCode();

        // 2. Establecer el código y la fecha de expiración (10 minutos desde ahora)
        user.setPasswordResetCode(code);
        user.setResetCodeExpiration(LocalDateTime.now().plusMinutes(10));

        // 3. Guardar los cambios en la base de datos
        userRepository.save(user);

        // 4. Enviar el correo
        emailService.sendPasswordResetCode(user.getEmail(), code);

        return new UserResponse("Se ha enviado un código de restablecimiento a tu correo.");
    }

    public UserResponse resetPassword(String code, String newPassword) {
        // 1. Buscar al usuario por el código de reseteo
        User user = userRepository.findByPasswordResetCode(code)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "El código de restablecimiento es inválido."));

        // 2. Verificar que el código no haya expirado
        if (user.getResetCodeExpiration().isBefore(LocalDateTime.now())) {
            // Limpiamos el código expirado para que no se pueda reintentar
            user.setPasswordResetCode(null);
            user.setResetCodeExpiration(null);
            userRepository.save(user);
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de restablecimiento ha expirado.");
        }

        // 3. Validar y actualizar la nueva contraseña
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede estar vacía.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));

        // 4. Limpiar los campos de reseteo para que el código no se reutilice
        user.setPasswordResetCode(null);
        user.setResetCodeExpiration(null);

        // 5. Invalidar todas las sesiones anteriores por seguridad
        user.incrementTokenVersion();

        // 6. Guardar el usuario con la nueva contraseña
        userRepository.save(user);

        return new UserResponse("Tu contraseña ha sido actualizada correctamente. Por favor, inicia sesión de nuevo.");
    }

    public JwtDTO login(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsernameOrEmail(), loginDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtProvider.generateToken(authentication);

        return new JwtDTO(jwt);
    }

    public JwtDTO refresh(JwtDTO jwtDTO) throws ParseException {
        String token = jwtProvider.refreshToken(jwtDTO);
        return new JwtDTO(token);
    }

    public void logout(String token) {
        if (jwtProvider.validateToken(token)) {
            long exp = jwtProvider.getExpirationFromToken(token);
            tokenBlacklist.add(token, exp);
        }
    }

    private String generateRandomCode() {
        // Genera un número aleatorio entre 100000 y 999999
        int randomNum = ThreadLocalRandom.current().nextInt(100000, 1_000_000);
        return String.valueOf(randomNum);
    }

}