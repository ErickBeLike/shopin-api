package com.app.shopin.modules.security.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.dto.*;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.entity.SocialLink;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.security.enums.TwoFactorMethod;
import com.app.shopin.modules.user.service.UserService;
import com.app.shopin.services.cloudinary.StorageService;
import com.app.shopin.services.email.EmailService;
import com.app.shopin.util.UserResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.app.shopin.modules.security.blacklist.TokenBlacklist;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.security.jwt.JwtProvider;
import com.app.shopin.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.ParseException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
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

    @Autowired
    private UserService userService;
    @Autowired
    private StorageService storageService;

    public record TokenPair(String accessToken, String refreshToken) {}
    private final long VALIDATION_TOKEN_DURATION = 5 * 60 * 1000;

    @Transactional
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

    @Transactional
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
        String validationToken = jwtProvider.generateTokenWithExpiration(user.getEmail(), VALIDATION_TOKEN_DURATION);

        return new ValidationResponseDTO(validationToken, "Código validado correctamente.");
    }

    @Transactional
    public UserResponse setNewPassword(String validationToken, String newPassword) {
        // 1. Validar el token temporal
        if (!jwtProvider.validateToken(validationToken)) {
            // Aquí podríamos diferenciar si está malformado o si solo expiró
            throw new CustomException(HttpStatus.UNAUTHORIZED, "El pase de validación es inválido o ha expirado.");
        }

        // 2. Extraer el username del token y buscar al usuario
        String email = jwtProvider.getEmailFromToken(validationToken);
        User user = userRepository.findByEmail(email)
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

    @Transactional(readOnly = true)
    public User authenticateAndGetUser(LoginDTO loginDTO) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDTO.getEmail(), loginDTO.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
        return principal.getUser();
    }

    @Transactional
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

    // 2FA EMAIL SECTION
    @Transactional
    public void sendTwoFactorCodeIfApplicable(User user) {
        if (user.getPreferredTwoFactorMethod() == TwoFactorMethod.EMAIL && user.isTwoFactorEmailEnabled()) {
            String code = generateRandomCode();
            user.setTwoFactorEmailCode(code);
            user.setTwoFactorCodeExpiration(LocalDateTime.now().plusMinutes(5)); // Código válido por 5 min
            userRepository.save(user);
            // IMPORTANTE: Debes crear este método en tu EmailService
            emailService.sendTwoFactorCode(user, code);
        }
    }

    @Transactional
    public TokenPair verifyEmailCodeAndLogin(LoginTwoFactorRequestDTO dto) {
        String email = dto.email();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."));

        if (!user.isTwoFactorEmailEnabled() ||
                !dto.code().equals(user.getTwoFactorEmailCode()) ||
                user.getTwoFactorCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Código de correo inválido o expirado.");
        }

        user.setTwoFactorEmailCode(null);
        user.setTwoFactorCodeExpiration(null);
        userRepository.save(user);

        return generateTokensForUser(user);
    }

    // 2FA APP SECTION
    @Transactional
    public TokenPair verifyAppCodeAndLogin(LoginTwoFactorRequestDTO dto) {
        String email = dto.email();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas."));

        if (!user.isTwoFactorAppEnabled() || !twoFactorService.isCodeValid(user.getTwoFactorSecret(), dto.code())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "Código de autenticación inválido.");
        }
        return generateTokensForUser(user);
    }

    @Transactional
    public TokenPair createOAuth2UserAndGetTokens(String registrationToken, String username) {
        // 1. Validamos el token y extraemos la información del usuario
        OAuth2TempInfo tempInfo = jwtProvider.getRegistrationInfoFromToken(registrationToken);

        // 2. Creamos la entidad User
        User newUser = new User();
        newUser.setEmail(tempInfo.email());
        newUser.setFirstName(tempInfo.firstName());
        newUser.setLastName(tempInfo.lastName());
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        // 3. Asignamos username y discriminador
        newUser.setUsername(username);
        newUser.setDiscriminator(userService.findNextAvailableDiscriminator(username));

        // 4. Creamos la vinculación social
        SocialLink newSocialLink = new SocialLink();
        newSocialLink.setUser(newUser);
        newSocialLink.setProvider(tempInfo.provider());
        newSocialLink.setProviderUserId(tempInfo.providerUserId());
        newUser.getSocialLinks().add(newSocialLink);

        // 5. Asignamos roles
        Set<Rol> roles = new HashSet<>();
        roles.add(rolService.getByRolName(RolName.ROLE_USER).orElseThrow());
        newUser.setRoles(roles);

        // 6. Subimos la foto de perfil
        try {
            if (tempInfo.pictureUrl() != null && !tempInfo.pictureUrl().isEmpty()) {
                Map<String, String> fileInfo = storageService.uploadFromUrl(tempInfo.pictureUrl(), "profileimages");
                newUser.setProfilePictureUrl(fileInfo.get("url"));
                newUser.setProfilePicturePublicId(fileInfo.get("publicId"));
            }
        } catch (Exception e) {
            log.error("Error al subir la imagen de perfil de OAuth2 para el usuario: {}", tempInfo.email(), e);
        }

        // 7. Guardamos el usuario nuevo y completo
        User savedUser = userRepository.save(newUser);

        // 8. Creamos la autenticación para el nuevo usuario
        PrincipalUser principal = PrincipalUser.build(savedUser);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 9. Generamos los tokens de sesión finales
        return generateTokensForUser(savedUser);
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