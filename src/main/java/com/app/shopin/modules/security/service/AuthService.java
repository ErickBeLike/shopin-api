package com.app.shopin.modules.security.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.dto.*;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.services.mailtrap.EmailService;
import com.app.shopin.util.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.app.shopin.modules.security.blacklist.TokenBlacklist;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.security.jwt.JwtProvider;
import com.app.shopin.modules.user.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

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

    @Autowired
    private TwoFactorService twoFactorService;

    public record TokenPair(String accessToken, String refreshToken) {}
    private final long VALIDATION_TOKEN_DURATION = 5 * 60 * 1000;

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
        emailService.sendPasswordResetCode(user, code);

        return new UserResponse("Se ha enviado un código de restablecimiento a tu correo.");
    }

    public ValidationResponseDTO validateResetCode(String code) {
        // 1. Buscar al usuario por el código
        User user = userRepository.findByPasswordResetCode(code)
                .orElseThrow(() -> new CustomException(HttpStatus.BAD_REQUEST, "El código de restablecimiento es inválido."));

        // 2. Verificar que el código no haya expirado
        if (user.getResetCodeExpiration().isBefore(LocalDateTime.now())) {
            user.setPasswordResetCode(null);
            user.setResetCodeExpiration(null);
            userRepository.save(user);
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de restablecimiento ha expirado.");
        }

        // 3. Generar el token de validación temporal
        String validationToken = jwtProvider.generateTokenWithExpiration(user.getUserName(), VALIDATION_TOKEN_DURATION);

        return new ValidationResponseDTO(validationToken, "Código validado correctamente.");
    }

    public UserResponse setNewPassword(String validationToken, String newPassword) {
        // 1. Validar el token temporal
        if (!jwtProvider.validateToken(validationToken)) {
            // Aquí podríamos diferenciar si está malformado o si solo expiró
            throw new CustomException(HttpStatus.UNAUTHORIZED, "El pase de validación es inválido o ha expirado.");
        }

        // 2. Extraer el username del token y buscar al usuario
        String username = jwtProvider.getNombreUsuarioFromToken(validationToken);
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "Usuario no encontrado."));

        // 3. Validar y actualizar la nueva contraseña
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede estar vacía.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));

        // 4. Limpiar los campos de reseteo para que el código no se reutilice
        user.setPasswordResetCode(null);
        user.setResetCodeExpiration(null);

        // 5. Invalidar todas las sesiones anteriores
        user.incrementTokenVersion();

        // 6. Guardar el usuario
        userRepository.save(user);

        return new UserResponse("Tu contraseña ha sido actualizada correctamente. Por favor, inicia sesión de nuevo.");
    }

    public User authenticateAndGetUser(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getUsernameOrEmail(), loginDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
        return principal.getUser();
    }

    public TokenPair generateTokensForUser(User user) {
        // Creamos el PrincipalUser y la Autenticación para el contexto de Spring Security
        PrincipalUser principalUser = PrincipalUser.build(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                principalUser, null, principalUser.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        try {
            emailService.sendLoginNotification(user);
        } catch (Exception e) {
            log.error("Error al enviar el correo de notificación de login para el usuario: " + user.getEmail(), e);
        }
        String accessToken = jwtProvider.generateAccessToken(authentication);
        String refreshToken = jwtProvider.generateRefreshToken(authentication);
        return new TokenPair(accessToken, refreshToken);
    }

    public TokenPair verifyTwoFactorCodeAndLogin(LoginTwoFactorRequestDTO login2FaRequestDTO) {
        String identifier = login2FaRequestDTO.usernameOrEmail();

        // 1. Buscamos al usuario
        User user = userRepository.findByUserNameOrEmail(identifier, identifier)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."));

        // 2. Verificamos que 2FA esté habilitado y que el código sea válido
        if (!user.isTwoFactorEnabled() || !twoFactorService.isCodeValid(user.getTwoFactorSecret(), login2FaRequestDTO.code())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Código 2FA inválido.");
        }

        // 3. Si t0do es correcto, generamos los tokens llamando a nuestro método reutilizable
        return generateTokensForUser(user);
    }

    public JwtDTO refresh(String refreshToken) throws ParseException {
        String newAccessToken = jwtProvider.refreshAccessToken(refreshToken);
        return new JwtDTO(newAccessToken);
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