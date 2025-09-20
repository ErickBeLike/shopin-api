package com.app.shopin.modules.user.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.blacklist.TokenBlacklist;
import com.app.shopin.modules.security.dto.twofactor.CodeConfirmationDTO;
import com.app.shopin.modules.security.dto.twofactor.PasswordConfirmationDTO;
import com.app.shopin.modules.security.dto.twofactor.SetupTwoFactorDTO;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.SocialLink;
import com.app.shopin.modules.security.enums.TwoFactorMethod;
import com.app.shopin.modules.security.service.TwoFactorService;
import com.app.shopin.modules.user.dto.NewUserDTO;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.security.jwt.JwtProvider;
import com.app.shopin.modules.security.service.RolService;
import com.app.shopin.modules.user.dto.UpdateEmailDTO;
import com.app.shopin.modules.user.dto.UpdateUserDataDTO;
import com.app.shopin.modules.user.dto.UpdateUsernameDTO;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import com.app.shopin.services.cloudinary.StorageService;
import com.app.shopin.services.email.EmailService;
import com.app.shopin.util.UserResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class UserService {

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
    StorageService storageService;

    @Autowired
    private EmailService emailService;

    @Autowired
    private TwoFactorService twoFactorService;

    private void checkOwnershipOrAdmin(Long targetUserId, UserDetails currentUser) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_SUPERADMIN"));

        PrincipalUser principal = (PrincipalUser) currentUser;
        User authenticatedUser = principal.getUser();

        if (!targetUserId.equals(authenticatedUser.getUserId()) && !isAdmin) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción sobre otro usuario.");
        }
    }

    private void checkOwnership(Long targetUserId, UserDetails currentUser) {
        PrincipalUser principal = (PrincipalUser) currentUser;
        if (!targetUserId.equals(principal.getUser().getUserId())) {
            throw new CustomException(HttpStatus.FORBIDDEN, "No tienes permiso para realizar esta acción.");
        }
    }

    @Transactional(readOnly = true)
    public List<User> getAllTheUsers() {
        return userRepository.findAll();
    }

    @Transactional(readOnly = true)
    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró un usuario para el ID: " + id));
    }

    private User findUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró un usuario para el ID: " + id));
    }

    // METHOD FOR GENERATE A AVATAR IMAGE
    private String generateAvatarUrl(String firstName, String lastName) {
        // 1. Obtenemos solo el primer nombre y el primer apellido, en caso de que haya más de uno.
        // Usamos un bloque try-catch por si los campos vienen nulos o vacíos.
        String primerNombre = "";
        if (firstName != null && !firstName.isEmpty()) {
            primerNombre = firstName.split(" ")[0];
        }

        String primerApellido = "";
        if (lastName != null && !lastName.isEmpty()) {
            primerApellido = lastName.split(" ")[0];
        }

        // 2. Creamos la cadena específica que queremos para las iniciales
        String nombreParaAvatar = (primerNombre + " " + primerApellido).trim(); // "Raúl Ocasio"

        // 3. Codificamos esa cadena específica para la URL
        String encodedName = URLEncoder.encode(nombreParaAvatar, StandardCharsets.UTF_8);

        return "https://ui-avatars.com/api/?name=" + encodedName + "&background=random&color=fff";
    }

    private Map<String, String> generateAndUploadAvatar(String firstName, String lastName) {
        try {
            String avatarUrl = generateAvatarUrl(firstName, lastName);
            return storageService.uploadFromUrl(avatarUrl, "profileimages");
        } catch (Exception e) {
            return Collections.emptyMap(); // Devuelve un mapa vacío en caso de error
        }
    }

    @Transactional
    public UserResponse save(NewUserDTO dto,
                             MultipartFile profileImage) {
        // 1) Validaciones de existencia
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "ese correo ya está en uso");
        }

        // 2) Validación contraseña
        String rawPassword = dto.getPassword().trim();
        if (rawPassword.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "contraseña inválida");
        }

        // 3) Crear entidad User
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setHasSetLocalPassword(true);
        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setHasSetLocalPassword(true);

        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            user.setPhone(dto.getPhone());
        }

        String displayName = (dto.getUsername() == null || dto.getUsername().trim().isEmpty())
                ? dto.getFirstName().trim() // Default al primer nombre si el username viene vacío
                : dto.getUsername().trim();

        user.setUsername(displayName);
        user.setDiscriminator(findNextAvailableDiscriminator(displayName)); // Asignamos su tag único

        // 4) Asignar roles
        Set<Rol> roles = new HashSet<>();
        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            roles.add(rolService.getByRolName(RolName.ROLE_USER).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol USER no encontrado")));
        } else {
            dto.getRoles().forEach(rol -> {
                switch (rol) {
                    case "superadmin":
                        roles.add(rolService.getByRolName(RolName.ROLE_SUPERADMIN).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol SUPERADMIN no encontrado")));
                        break;
                    case "admin":
                        roles.add(rolService.getByRolName(RolName.ROLE_ADMIN).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol ADMIN no encontrado")));
                        break;
                    case "employee":
                        roles.add(rolService.getByRolName(RolName.ROLE_EMPLOYEE).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol EMPLOYEE no encontrado")));
                        break;
                }
            });
        }
        user.setRoles(roles);

        // 5) Procesar imagen de perfil
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // Intenta subir la imagen proporcionada
                Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
                user.setProfilePictureUrl(fileInfo.get("url"));
                user.setProfilePicturePublicId(fileInfo.get("publicId"));
            } catch (Exception e) {
                // Fallback: Si la subida falla, genera y sube un avatar
                Map<String, String> fileInfo = generateAndUploadAvatar(dto.getFirstName(), dto.getLastName());
                user.setProfilePictureUrl(fileInfo.get("url"));
                user.setProfilePicturePublicId(fileInfo.get("publicId"));
            }
        } else {
            // Si no se proporciona imagen, genera y sube un avatar
            Map<String, String> fileInfo = generateAndUploadAvatar(dto.getFirstName(), dto.getLastName());
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId"));
        }

        userRepository.save(user);
        return new UserResponse("Usuario " + user.getFullTag() + " ha sido creado");
    }

    @Transactional
    public UserResponse updateUser(Long id,
                                   NewUserDTO dto,
                                   MultipartFile profileImage) {

        // 1) Traer usuario existente
        User user = findUserById(id);

        // 2) Validar y actualizar email
        String newEmail = dto.getEmail().trim();
        if (newEmail.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "email inválido");
        }
        if (!newEmail.equalsIgnoreCase(user.getEmail())) {
            if (userRepository.existsByEmail(newEmail)) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "ese correo ya está en uso por otro usuario"
                );
            }
            user.setEmail(newEmail);
        }

        // 3) Validar y actualizar contraseña
        String rawPassword = dto.getPassword().trim();
        if (rawPassword.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "contraseña inválida");
        }
        user.setPassword(passwordEncoder.encode(rawPassword));

        // 4) Actualizar userName
        if (dto.getUsername() != null && !dto.getUsername().trim().isEmpty() && !dto.getUsername().trim().equalsIgnoreCase(user.getUsername())) {
            String newUsername = dto.getUsername().trim();
            user.setUsername(newUsername);
            user.setDiscriminator(findNextAvailableDiscriminator(newUsername));
        }

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());
        user.setPhone(dto.getPhone());

        // 5) Asignar roles (igual que antes)
        Set<Rol> roles = new HashSet<>();
        if (dto.getRoles() == null || dto.getRoles().isEmpty()) {
            roles.add(rolService.getByRolName(RolName.ROLE_USER).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol USER no encontrado")));
        } else {
            dto.getRoles().forEach(rol -> {
                switch (rol) {
                    case "superadmin":
                        roles.add(rolService.getByRolName(RolName.ROLE_SUPERADMIN).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol SUPERADMIN no encontrado")));
                        break;
                    case "admin":
                        roles.add(rolService.getByRolName(RolName.ROLE_ADMIN).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol ADMIN no encontrado")));
                        break;
                    case "employee":
                        roles.add(rolService.getByRolName(RolName.ROLE_EMPLOYEE).orElseThrow(() -> new CustomException(HttpStatus.INTERNAL_SERVER_ERROR, "rol EMPLOYEE no encontrado")));
                        break;
                }
            });
        }
        user.setRoles(roles);

        // 6) Handle profile image (this is the key part)
        if (profileImage != null && !profileImage.isEmpty()) {
            try {
                // Si se sube una nueva imagen, borra la anterior de Cloudinary (si existe)
                if (user.getProfilePicturePublicId() != null) {
                    storageService.deleteFile(user.getProfilePicturePublicId(), null);
                }
                // Sube la nueva imagen
                Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
                user.setProfilePictureUrl(fileInfo.get("url"));
                user.setProfilePicturePublicId(fileInfo.get("publicId"));
            } catch (Exception e) {
                // Fallback: Si la subida falla, genera un avatar (no borra el anterior en este caso)
                Map<String, String> fileInfo = generateAndUploadAvatar(dto.getFirstName(), dto.getLastName());
                user.setProfilePictureUrl(fileInfo.get("url"));
                user.setProfilePicturePublicId(fileInfo.get("publicId"));
            }
        } else {
            // Si no se sube imagen Y la actual es un avatar temporal (publicId es nulo),
            // se "actualiza" a un avatar permanente en Cloudinary.
            if (user.getProfilePicturePublicId() == null) {
                Map<String, String> fileInfo = generateAndUploadAvatar(dto.getFirstName(), dto.getLastName());
                user.setProfilePictureUrl(fileInfo.get("url"));
                user.setProfilePicturePublicId(fileInfo.get("publicId"));
            }
        }
        // Si no se sube imagen y la que tiene ya es de Cloudinary, no se hace nada.

        user.incrementTokenVersion();
        userRepository.save(user);
        return new UserResponse(
                user.getUsername() +
                        " ha sido actualizado correctamente. Por seguridad, su sesión actual se invalidará; por favor, inicie sesión de nuevo.");
    }

    @Transactional
    public Map<String, Boolean> softDeleteUser(Long id, UserDetails currentUser) {
        // 1. Verifica los permisos antes de continuar
        checkOwnershipOrAdmin(id, currentUser);

        // 2. Obtiene el usuario
        User user = findUserById(id);

        // 3. Realiza el "soft delete"
        user.setDeletedAt(LocalDateTime.now());

        // 4. Invalida el token para que ya no pueda usarlo
        user.incrementTokenVersion();

        userRepository.save(user);

        // 5. Envía la notificación
        emailService.sendDeletionNoticeEmail(user);

        Map<String, Boolean> response = new HashMap<>();
        response.put("eliminado", Boolean.TRUE);
        return response;
    }

    public String findNextAvailableDiscriminator(String username) {
        List<String> usedDiscriminators = userRepository.findDiscriminatorsByUsername(username);
        Set<String> usedSet = new HashSet<>(usedDiscriminators);
        for (int i = 1; i <= 9999; i++) {
            String newDiscriminator = String.format("%04d", i);
            if (!usedSet.contains(newDiscriminator)) {
                return newDiscriminator;
            }
        }
        throw new CustomException(HttpStatus.CONFLICT, "No hay más discriminadores disponibles para este nombre de usuario.");
    }

    //  METHOD FOT UPDATE USER DATA
    @Transactional
    public UserResponse updateUserProfile(Long id, UpdateUserDataDTO dto) {
        // 1. Buscar al usuario
        User user = findUserById(id);

        // 2. Actualizar los campos del perfil
        user.setFirstName(dto.firstName());
        user.setLastName(dto.lastName());
        user.setPhone(dto.phone());

        // 3. Opcional pero recomendado: Invalidar tokens para refrescar la info del perfil
        user.incrementTokenVersion();

        // 4. Guardar los cambios (el @PreUpdate actualizará el `updatedAt`)
        userRepository.save(user);

        return new UserResponse("El perfil de " + user.getUsername() + " ha sido actualizado.");
    }

    @Transactional
    public UserResponse updateProfileImage(Long id, MultipartFile profileImage) {
        if (profileImage == null || profileImage.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No se ha proporcionado una imagen.");
        }
        User user = findUserById(id);

        // Si la imagen anterior era de Cloudinary, la borramos
        if (user.getProfilePicturePublicId() != null) {
            storageService.deleteFile(user.getProfilePicturePublicId(), null);
        }

        try {
            // Guardamos la nueva imagen y actualizamos la entidad
            Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId"));
        } catch (Exception e) {
            // Fallback: Si la subida falla, genera y sube un avatar
            Map<String, String> fileInfo = generateAndUploadAvatar(user.getFirstName(), user.getLastName());
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId"));
        }

        userRepository.save(user);
        return new UserResponse("Imagen de perfil de " + user.getUsername() + " actualizada.");
    }

    @Transactional
    public UserResponse deleteProfileImage(Long id) {
        User user = findUserById(id);

        // Si hay una imagen en Cloudinary, la borramos
        if (user.getProfilePicturePublicId() != null) {
            storageService.deleteFile(user.getProfilePicturePublicId(), null);
        }

        // Siempre se restaura a un avatar por defecto subido a Cloudinary
        Map<String, String> fileInfo = generateAndUploadAvatar(user.getFirstName(), user.getLastName());
        user.setProfilePictureUrl(fileInfo.get("url"));
        user.setProfilePicturePublicId(fileInfo.get("publicId"));

        userRepository.save(user);
        return new UserResponse("Imagen de perfil restaurada al avatar por defecto.");
    }

    // METHOD FOR UPDATE USER USERNAME
    @Transactional
    public UserResponse updateUsername(Long id, UpdateUsernameDTO dto) {
        User user = findUserById(id);
        String newUsername = dto.userName().trim();

        if (newUsername.equalsIgnoreCase(user.getUsername())) {
            return new UserResponse("El nuevo nombre de usuario es el mismo que el actual.");
        }

        // Al cambiar el nombre, le asignamos un nuevo #XXXX para ese nuevo nombre
        user.setUsername(newUsername);
        user.setDiscriminator(findNextAvailableDiscriminator(newUsername));

        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("Nombre de usuario actualizado a: " + user.getFullTag() + ". Por seguridad, por favor inicie sesión de nuevo.");
    }

    // METHOD FOR UPDATE USER EMAIL
    @Transactional
    public UserResponse updateEmail(Long id, UpdateEmailDTO dto) {
        // 1. Buscar al usuario
        User user = findUserById(id);
        String newEmail = dto.email().trim();

        // 2. Validar si hay cambios
        if (newEmail.equalsIgnoreCase(user.getEmail())) {
            return new UserResponse("El nuevo correo es el mismo que el actual.");
        }

        // 3. Validar si ya existe
        if (userRepository.existsByEmail(newEmail)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Ese correo ya está en uso");
        }

        // 4. Actualizar
        user.setEmail(newEmail);

        // 5. Invalidar sesión por seguridad
        user.incrementTokenVersion();
        // 6. Guardar
        userRepository.save(user);

        return new UserResponse("Correo electrónico actualizado, por seguridad favor de volver a autenticarse.");
    }

    @Transactional
    public UserResponse changePassword(Long userId, String oldPassword, String newPassword, UserDetails currentUser) {
        checkOwnershipOrAdmin(userId, currentUser);

        User user = findUserById(userId);

        // 1. Verificar que la contraseña antigua sea correcta
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La contraseña actual es incorrecta.");
        }

        // 2. Validar la nueva contraseña
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede estar vacía.");
        }

        // 3. Actualizar la contraseña
        user.setPassword(passwordEncoder.encode(newPassword));

        // 4. Invalidar todas las sesiones anteriores
        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("Contraseña actualizada correctamente, por seguridad favor de volver a autenticarse.");
    }

    @Transactional
    public UserResponse setPasswordForOAuthUser(Long userId, String newPassword, UserDetails currentUser) {
        checkOwnership(userId, currentUser);
        User user = findUserById(userId);

        // 1. Verificamos que el usuario NO tenga ya una contraseña establecida.
        // Este método es solo para la primera vez.
        if (user.isHasSetLocalPassword()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Este usuario ya tiene una contraseña. Use la función 'Cambiar Contraseña'.");
        }

        // 2. Validamos y hasheamos la nueva contraseña.
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "La nueva contraseña no puede estar vacía.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));

        // 3. ¡Cambiamos la bandera!
        user.setHasSetLocalPassword(true);

        // 4. Invalidamos otras sesiones por seguridad.
        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("Contraseña creada exitosamente. Ahora puedes usarla para iniciar sesión y para acciones de seguridad, por seguridad favor de volver a autenticarse.");
    }

    // 2FA SECTION --------------
    private void verifyUserPassword(User user, String password) {
        if (password == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new CustomException(HttpStatus.UNAUTHORIZED, "La contraseña proporcionada es incorrecta.");
        }
    }

    private String generateRandomCode() {
        return String.valueOf(new Random().nextInt(900000) + 100000);
    }

    // 2FA APP SECTION
    @Transactional
    public SetupTwoFactorDTO setupAppTwoFactor(Long userId, PasswordConfirmationDTO dto) {
        User user = findUserById(userId);
        // 1. PRIMERO, verificar la identidad del usuario con su contraseña
        verifyUserPassword(user, dto.password());

        // 2. Si la contraseña es correcta, AHORA SÍ generamos el secreto y el QR
        final String secret = twoFactorService.generateNewSecret();
        String qrCodeImageUri = twoFactorService.generateQrCodeImageUri(secret, user.getEmail());

        user.setTwoFactorSecret(secret);
        user.setTwoFactorAppEnabled(false);
        userRepository.save(user);

        return new SetupTwoFactorDTO(secret, qrCodeImageUri);
    }

    @Transactional
    public UserResponse enableAppTwoFactor(Long userId, CodeConfirmationDTO dto) {
        User user = findUserById(userId);
        // Para este paso, ya no es necesaria la contraseña, porque ya la pidió el 'setup'.
        // El "secreto" de que el usuario ya validó su contraseña es que el campo 'twoFactorSecret' no es nulo.
        if (user.getTwoFactorSecret() == null) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Debes completar el paso de configuración inicial primero.");
        }
        if (dto.code() == null || !twoFactorService.isCodeValid(user.getTwoFactorSecret(), dto.code())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de verificación es incorrecto.");
        }

        user.setTwoFactorAppEnabled(true);
        if (user.getPreferredTwoFactorMethod() == TwoFactorMethod.NONE) {
            user.setPreferredTwoFactorMethod(TwoFactorMethod.APP);
        }

        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("La autenticación de dos factores con la aplicación ha sido habilitada exitosamente, por seguridad favor de volver a autenticarse.");
    }

    public UserResponse preDisableAppTwoFactor(Long userId, PasswordConfirmationDTO dto) {
        User user = findUserById(userId);
        verifyUserPassword(user, dto.password());
        return new UserResponse("Contraseña verificada. Por favor, ingrese su código de la aplicación para confirmar la desactivación.");
    }

    @Transactional
    public UserResponse confirmDisableAppTwoFactor(Long userId, CodeConfirmationDTO dto) { // <- DTO Actualizado
        User user = findUserById(userId);
        if (dto.code() == null || !twoFactorService.isCodeValid(user.getTwoFactorSecret(), dto.code())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de verificación de la aplicación es incorrecto.");
        }

        user.setTwoFactorAppEnabled(false);
        user.setTwoFactorSecret(null);
        if (user.getPreferredTwoFactorMethod() == TwoFactorMethod.APP) {
            user.setPreferredTwoFactorMethod(user.isTwoFactorEmailEnabled() ? TwoFactorMethod.EMAIL : TwoFactorMethod.NONE);
        }

        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("La autenticación de dos factores con la aplicación ha sido deshabilitada, por seguridad favor de volver a autenticarse.");
    }

    // 2FA EMAIL SECTION
    @Transactional
    public UserResponse setupEmailTwoFactor(Long userId, PasswordConfirmationDTO dto) {
        User user = findUserById(userId);
        verifyUserPassword(user, dto.password());

        String code = generateRandomCode();
        user.setTwoFactorEmailCode(code);
        user.setTwoFactorCodeExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendEnableConfirmationCode(user, code);

        return new UserResponse("Contraseña verificada. Hemos enviado un código a tu correo para activar el servicio.");
    }

    @Transactional
    public UserResponse enableEmailTwoFactor(Long userId, CodeConfirmationDTO  dto) {
        User user = findUserById(userId);
        if (dto.code() == null ||
                !dto.code().equals(user.getTwoFactorEmailCode()) ||
                user.getTwoFactorCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de activación es incorrecto o ha expirado.");
        }

        // Limpiamos el código temporal
        user.setTwoFactorEmailCode(null);
        user.setTwoFactorCodeExpiration(null);

        // Ahora sí, activamos el método
        user.setTwoFactorEmailEnabled(true);
        if (user.getPreferredTwoFactorMethod() == TwoFactorMethod.NONE) {
            user.setPreferredTwoFactorMethod(TwoFactorMethod.EMAIL);
        }

        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("La autenticación de dos factores por correo ha sido habilitada exitosamente, por seguridad favor de volver a autenticarse.");
    }

    @Transactional
    public UserResponse preDisableEmailTwoFactor(Long userId, PasswordConfirmationDTO dto) {
        User user = findUserById(userId);
        verifyUserPassword(user, dto.password());

        String code = generateRandomCode();
        user.setTwoFactorEmailCode(code);
        user.setTwoFactorCodeExpiration(LocalDateTime.now().plusMinutes(10));
        userRepository.save(user);
        emailService.sendDisableConfirmationCode(user, code);

        return new UserResponse("Contraseña verificada. Hemos enviado un código a tu correo para confirmar la desactivación.");
    }

    @Transactional
    public UserResponse confirmDisableEmailTwoFactor(Long userId, CodeConfirmationDTO dto) { // <- DTO Actualizado
        User user = findUserById(userId);
        if (dto.code() == null ||
                !dto.code().equals(user.getTwoFactorEmailCode()) ||
                user.getTwoFactorCodeExpiration().isBefore(LocalDateTime.now())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de confirmación es incorrecto o ha expirado.");
        }

        user.setTwoFactorEmailEnabled(false);
        user.setTwoFactorEmailCode(null);
        user.setTwoFactorCodeExpiration(null);
        if (user.getPreferredTwoFactorMethod() == TwoFactorMethod.EMAIL) {
            user.setPreferredTwoFactorMethod(user.isTwoFactorAppEnabled() ? TwoFactorMethod.APP : TwoFactorMethod.NONE);
        }

        user.incrementTokenVersion();
        userRepository.save(user);

        return new UserResponse("La autenticación de dos factores por correo ha sido deshabilitada, por seguridad favor de volver a autenticarse.");
    }

    // 2FA PREFERRED METHOD SECTION
    @Transactional
    public UserResponse setPreferredTwoFactorMethod(Long userId, TwoFactorMethod method) {
        User user = findUserById(userId);
        if (method != TwoFactorMethod.NONE &&
                (method == TwoFactorMethod.APP && !user.isTwoFactorAppEnabled()) ||
                (method == TwoFactorMethod.EMAIL && !user.isTwoFactorEmailEnabled())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "No puedes establecer como preferido un método de 2FA que no has habilitado.");
        }
        user.setPreferredTwoFactorMethod(method);
        userRepository.save(user);
        return new UserResponse("Se ha actualizado tu método de autenticación preferido a: " + method);
    }

    // UN-LINk ACCOUNT
    @Transactional
    public UserResponse unlinkSocialAccount(Long userId, String provider, PasswordConfirmationDTO dto, UserDetails currentUser) {
        // 1. Verificamos que el usuario tenga permiso para esta acción.
        checkOwnershipOrAdmin(userId, currentUser);

        User user = findUserById(userId);

        // 2. Le informamos al usuario que para administrar sus vinculaciones es necesario una contraseña otorgada por el
        if (!user.isHasSetLocalPassword()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Para gestionar tus vinculaciones, primero debes crear una contraseña para tu cuenta.");
        }

        // 3. Verificamos la contraseña del usuario para confirmar la acción.
        verifyUserPassword(user, dto.password());

        // 4. Buscamos y eliminamos la vinculación.
        SocialLink linkToRemove = user.getSocialLinks().stream()
                .filter(link -> link.getProvider().equalsIgnoreCase(provider))
                .findFirst()
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró una vinculación con " + provider + " para este usuario."));

        // Eliminamos el link de la colección y dejamos que JPA se encargue del borrado
        user.getSocialLinks().remove(linkToRemove);

        user.incrementTokenVersion();
        userRepository.save(user); // Guardamos el usuario para que la relación se actualice

        return new UserResponse("La cuenta de " + provider + " ha sido desvinculada exitosamente, por seguridad favor de volver a autenticarse.");
    }

}
