package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

// --- CAMBIO 1: Extiende OidcUserService ---
@Service
public class CustomOAuth2UserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RolService rolService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private static final Logger logger = LoggerFactory.getLogger(CustomOAuth2UserService.class);

    // --- CAMBIO 2: El método ahora trabaja con OIDC ---
    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        System.out.println("\n\n****** ¡AHORA SÍ! El método OIDC loadUser se está ejecutando ******\n\n");

        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getAttribute("email");
        logger.info("Iniciando proceso de loadUser OIDC para el email: {}", email);

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            logger.info("Usuario con email {} no encontrado. Creando uno nuevo.", email);
            String firstName = oidcUser.getAttribute("given_name");
            String lastName = oidcUser.getAttribute("family_name");
            String pictureUrl = oidcUser.getAttribute("picture");

            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            newUser.setUserName(email);
            newUser.setProfilePictureUrl(pictureUrl);
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            Set<Rol> roles = new HashSet<>();
            Rol userRole = rolService.getByRolName(RolName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("FATAL: Rol 'ROLE_USER' no encontrado."));
            roles.add(userRole);
            newUser.setRoles(roles);

            logger.info("Guardando nuevo usuario OIDC con email: {}", email);
            return userRepository.save(newUser);
        });

        logger.info("Usuario OIDC procesado para email: {}", user.getEmail());

        // --- CAMBIO 3: Usamos el nuevo método build para crear nuestro PrincipalUser ---
        return PrincipalUser.build(user, oidcUser);
    }
}