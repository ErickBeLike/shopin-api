package com.app.shopin.modules.user.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.blacklist.TokenBlacklist;
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
import com.app.shopin.util.UserResponse;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

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
        // Reemplazamos espacios para que la URL sea válida
        String formattedName = (firstName + " " + lastName).replace(" ", "+");
        // Construimos la URL con parámetros para color de fondo y texto
        return "https://ui-avatars.com/api/?name=" + formattedName + "&background=random&color=fff";
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
            // Si se sube una imagen, se guarda en Cloudinary
            Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId"));
        } else {
            // Si no, se genera el avatar y el publicId se deja nulo
            user.setProfilePictureUrl(generateAvatarUrl(dto.getFirstName(), dto.getLastName()));
            user.setProfilePicturePublicId(null); // Importante: no hay publicId para avatares generados
        }

        // 6) Persistir y devolver respuesta
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
            // Si se sube una nueva imagen, se borra la anterior de Cloudinary (si existe y no es un avatar)
            if (user.getProfilePicturePublicId() != null) {
                storageService.deleteFile(user.getProfilePicturePublicId(), null);
            }
            // Se sube la nueva imagen
            Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId"));
        } else if (user.getProfilePicturePublicId() == null) {
            // Si no se sube imagen Y la actual es un avatar (publicId es nulo),
            // se regenera por si cambiaron el nombre.
            user.setProfilePictureUrl(generateAvatarUrl(dto.getFirstName(), dto.getLastName()));
        }
        // Si no se sube imagen y la que tiene es de Cloudinary, no se hace nada.

        // 7) Increment token version
        user.incrementTokenVersion();

        // 8) Update timestamp and save
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        // 9) Response
        return new UserResponse(
                user.getUserName() +
                        " ha sido actualizado correctamente. Por seguridad, su sesión actual se invalidará; por favor, inicie sesión de nuevo."
        );
    }

    public Map<String, Boolean> deleteUser(Long id) {

        User user = findUserById(id);

        // Borra la imagen de Cloudinary antes de eliminar el usuario
        if (user.getProfilePicturePublicId() != null) {
            storageService.deleteFile(user.getProfilePicturePublicId(), null);
        }

        userRepository.delete(user);

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

        // Si la imagen anterior era de Cloudinary (tiene publicId), la borramos
        if (user.getProfilePicturePublicId() != null) {
            storageService.deleteFile(user.getProfilePicturePublicId(), null);
        }

        // Guardamos la nueva imagen y actualizamos la entidad
        Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
        user.setProfilePictureUrl(fileInfo.get("url"));
        user.setProfilePicturePublicId(fileInfo.get("publicId"));

        userRepository.save(user);
        return new UserResponse("Imagen de perfil de " + user.getUserName() + " actualizada.");
    }

    public UserResponse deleteProfileImage(Long id) {
        User user = findUserById(id);

        // Si no hay publicId, ya está usando un avatar. No hay nada que hacer.
        if (user.getProfilePicturePublicId() == null) {
            return new UserResponse("El usuario ya está usando un avatar generado.");
        }

        // Si hay publicId, significa que es una imagen de Cloudinary. La borramos.
        storageService.deleteFile(user.getProfilePicturePublicId(), null);

        // Restauramos al avatar por defecto
        user.setProfilePictureUrl(generateAvatarUrl(user.getFirstName(), user.getLastName()));
        user.setProfilePicturePublicId(null); // Limpiamos el publicId

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

}
