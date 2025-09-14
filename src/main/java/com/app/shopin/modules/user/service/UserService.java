package com.app.shopin.modules.user.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.blacklist.TokenBlacklist;
import com.app.shopin.modules.security.dto.SetupTwoFactorDTO;
import com.app.shopin.modules.security.entity.PrincipalUser;
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
import com.app.shopin.services.mailtrap.EmailService;
import com.app.shopin.util.UserResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional
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

    public List<User> getAllTheUsers() {
        return userRepository.findAll();
    }

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

    public UserResponse save(NewUserDTO dto,
                             MultipartFile profileImage) {
        // 1) Validaciones de existencia
        if (userRepository.existsByUserName(dto.getUserName())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "ese nombre de usuario ya existe");
        }
        if (userRepository.existsByEmail(dto.getEmail())) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "ese correo ya está en uso");
        }

        // 2) Validación contraseña
        String rawPassword = dto.getPassword().trim();
        if (rawPassword.isEmpty()) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "contraseña inválida");
        }

        // 3) Crear entidad User
        User user = new User(
                dto.getUserName(),
                dto.getEmail(),
                passwordEncoder.encode(rawPassword)
        );

        user.setFirstName(dto.getFirstName());
        user.setLastName(dto.getLastName());

        if (dto.getPhone() != null && !dto.getPhone().trim().isEmpty()) {
            user.setPhone(dto.getPhone());
        }

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
        return new UserResponse(user.getUserName() + " ha sido creado");
    }

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
        if (!dto.getUserName().equals(user.getUserName())) {
            if (userRepository.existsByUserName(dto.getUserName())) {
                throw new CustomException(
                        HttpStatus.BAD_REQUEST,
                        "ese nombre de usuario ya existe"
                );
            }
            user.setUserName(dto.getUserName());
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
                user.getUserName() +
                        " ha sido actualizado correctamente. Por seguridad, su sesión actual se invalidará; por favor, inicie sesión de nuevo.");
    }

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

    //  METHOD FOT UPDATE USER DATA
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

        return new UserResponse("El perfil de " + user.getUserName() + " ha sido actualizado.");
    }

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
        return new UserResponse("Imagen de perfil de " + user.getUserName() + " actualizada.");
    }

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
    public UserResponse updateUsername(Long id, UpdateUsernameDTO dto) {
        // 1. Buscar al usuario
        User user = findUserById(id);
        String newUsername = dto.userName().trim();

        // 2. Validar si hay cambios
        if (newUsername.equalsIgnoreCase(user.getUserName())) {
            return new UserResponse("El nuevo nombre de usuario es el mismo que el actual.");
        }

        // 3. Validar si ya existe
        if (userRepository.existsByUserName(newUsername)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "Ese nombre de usuario ya existe");
        }

        // 4. Actualizar
        user.setUserName(newUsername);

        // 5. Invalidar sesión por seguridad
        user.incrementTokenVersion();

        // 6. Guardar
        userRepository.save(user);

        return new UserResponse("Nombre de usuario actualizado. Por seguridad, por favor inicie sesión de nuevo.");
    }

    // METHOD FOR UPDATE USER EMAIL
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

        return new UserResponse("Correo electrónico actualizado. Por seguridad, por favor inicie sesión de nuevo.");
    }

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

        return new UserResponse("Contraseña actualizada correctamente. Las demás sesiones han sido cerradas.");
    }

    // 2FA SECTION
    public SetupTwoFactorDTO setupTwoFactorAuthentication(Long userId) {
        User user = findUserById(userId);
        // Generamos un nuevo secreto
        final String secret = twoFactorService.generateNewSecret();
        // Generamos el QR
        String qrCodeImageUri = twoFactorService.generateQrCodeImageUri(secret, user.getEmail());

        // Guardamos el secreto en el usuario PERO aún no lo activamos
        user.setTwoFactorSecret(secret);
        user.setTwoFactorEnabled(false); // Asegurarse de que esté desactivado hasta la verificación
        userRepository.save(user);

        return new SetupTwoFactorDTO(secret, qrCodeImageUri);
    }

    public UserResponse enableTwoFactorAuthentication(Long userId, String code) {
        User user = findUserById(userId);

        // Verificamos que el código sea válido para el secreto guardado
        if (!twoFactorService.isCodeValid(user.getTwoFactorSecret(), code)) {
            throw new CustomException(HttpStatus.BAD_REQUEST, "El código de verificación es incorrecto.");
        }

        // Si es válido, activamos la 2FA
        user.setTwoFactorEnabled(true);
        userRepository.save(user);
        return new UserResponse("La autenticación de dos factores ha sido habilitada exitosamente.");
    }

    public UserResponse disableTwoFactorAuthentication(Long userId) {
        User user = findUserById(userId);
        user.setTwoFactorEnabled(false);
        user.setTwoFactorSecret(null); // Opcional: limpiar el secreto
        userRepository.save(user);
        return new UserResponse("La autenticación de dos factores ha sido deshabilitada.");
    }

}
