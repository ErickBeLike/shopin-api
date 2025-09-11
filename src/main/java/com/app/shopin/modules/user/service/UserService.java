package com.app.shopin.modules.user.service;

import com.app.shopin.modules.exception.CustomException;
import com.app.shopin.modules.security.blacklist.TokenBlacklist;
import com.app.shopin.modules.user.dto.NewUserDTO;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.security.jwt.JwtProvider;
import com.app.shopin.modules.security.service.RolService;
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


    public UserResponse save(NewUserDTO dto,
                             MultipartFile profileImage,
                             String baseUrl) {
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
            // El servicio ahora devuelve un mapa con la URL y el publicId
            Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId")); // ¡Guardamos el publicId!
        }

        // 6) Persistir y devolver respuesta
        userRepository.save(user);
        return new UserResponse(user.getUserName() + " ha sido creado");
    }

    public List<User> getAllTheUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró un usuario para el ID: " + id));
    }

    public UserResponse updateUser(Long id,
                                   NewUserDTO dto,
                                   MultipartFile profileImage,
                                   String baseUrl) {

        // 1) Traer usuario existente
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(
                        HttpStatus.NOT_FOUND,
                        "No se encontró un usuario para el ID: " + id
                ));

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
        // Lógica de actualización de imagen:
        if (profileImage != null && !profileImage.isEmpty() && storageService.isImageFile(profileImage)) {
            // Escenario 1: Se ha proporcionado una nueva imagen.
            // Borra la imagen anterior si existe.
            if (user.getProfilePicturePublicId() != null) {
                storageService.deleteFile(user.getProfilePicturePublicId(), null);
            }
            // Sube la nueva imagen y actualiza los campos del usuario.
            Map<String, String> fileInfo = storageService.saveFile(profileImage, profileImage.getOriginalFilename(), "profileimages");
            user.setProfilePictureUrl(fileInfo.get("url"));
            user.setProfilePicturePublicId(fileInfo.get("publicId"));
        }

        // Escenario 2: Si no se proporciona una nueva imagen,
        // no se hace nada y la imagen anterior se mantiene.
        // La lógica para esto no necesita un 'else' aquí.

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
        User user = userRepository.findById(id)
                .orElseThrow(() -> new CustomException(HttpStatus.NOT_FOUND, "No se encontró un usuario para el ID: " + id));

        // Borra la imagen de Cloudinary antes de eliminar el usuario
        if (user.getProfilePicturePublicId() != null) {
            storageService.deleteFile(user.getProfilePicturePublicId(), null);
        }

        userRepository.delete(user);

        Map<String, Boolean> response = new HashMap<>();
        response.put("eliminado", Boolean.TRUE);
        return response;
    }
}
