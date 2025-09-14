package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
// --- IMPORTANTE: Añadir estos imports ---
import org.springframework.security.crypto.password.PasswordEncoder;
import java.util.UUID;
// ---
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RolService rolService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        // ====================================================================================
        // ======================= LA PRUEBA DEFINITIVA =======================================
        // ====================================================================================
        System.out.println("\n\n\n\n***************************************************");
        System.out.println("****** MÉTODO loadUser SÍ SE ESTÁ EJECUTANDO ******");
        System.out.println("***************************************************\n\n\n\n");
        // ====================================================================================

        OAuth2User oauth2User = super.loadUser(userRequest);
        String email = oauth2User.getAttribute("email");
        logger.info("Iniciando proceso de loadUser para el email: {}", email); // Log inicial

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            logger.info("Usuario con email {} no encontrado en la BD. Procediendo a crear uno nuevo.", email);
            System.out.println("****** CREANDO NUEVO USUARIO PARA: " + email + " ******"); // <-- Otro println
            try {
                String firstName = oauth2User.getAttribute("given_name");
                String lastName = oauth2User.getAttribute("family_name");
                String pictureUrl = oauth2User.getAttribute("picture");

                User newUser = new User();
                newUser.setEmail(email);
                newUser.setFirstName(firstName);
                newUser.setLastName(lastName);
                newUser.setUserName(email);
                newUser.setProfilePictureUrl(pictureUrl);
                newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

                Set<Rol> roles = new HashSet<>();
                Rol userRole = rolService.getByRolName(RolName.ROLE_USER)
                        .orElseThrow(() -> new RuntimeException("FATAL: El rol 'ROLE_USER' no se encuentra en la base de datos."));
                roles.add(userRole);
                newUser.setRoles(roles);

                logger.info("Guardando nuevo usuario en la BD con email: {}", email);
                User savedUser = userRepository.save(newUser);
                logger.info("Nuevo usuario guardado exitosamente con ID: {}", savedUser.getUserId());
                return savedUser;

            } catch (Exception e) {
                logger.error("¡ERROR! No se pudo crear el usuario para el email {}: {}", email, e.getMessage(), e);
                // Imprimimos el error directamente a la consola también
                System.out.println("!!!!!! ERROR AL CREAR USUARIO: " + e.getMessage());
                e.printStackTrace(); // Esto imprimirá el stack trace completo
                throw new OAuth2AuthenticationException("Error al procesar el nuevo usuario de OAuth2.");
            }
        });

        logger.info("Usuario procesado correctamente para email: {}. ID: {}", user.getEmail(), user.getUserId());
        return PrincipalUser.build(user, oauth2User.getAttributes());
    }
}