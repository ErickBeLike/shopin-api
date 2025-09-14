package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import com.app.shopin.services.cloudinary.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends OidcUserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RolService rolService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StorageService storageService;

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {

        OidcUser oidcUser = super.loadUser(userRequest);
        String email = oidcUser.getAttribute("email");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
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

            try {
                String googlePictureUrl = oidcUser.getAttribute("picture");
                if (googlePictureUrl != null && !googlePictureUrl.isEmpty()) {
                    Map<String, String> fileInfo = storageService.uploadFromUrl(googlePictureUrl, "profileimages");
                    newUser.setProfilePictureUrl(fileInfo.get("url"));
                    newUser.setProfilePicturePublicId(fileInfo.get("publicId"));
                }
            } catch (Exception e) {
                // Opcional: podrías generar un avatar aquí como fallback
            }

            Set<Rol> roles = new HashSet<>();
            Rol userRole = rolService.getByRolName(RolName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("FATAL: Rol 'ROLE_USER' no encontrado."));
            roles.add(userRole);
            newUser.setRoles(roles);

            return userRepository.save(newUser);
        });

        return PrincipalUser.build(user, oidcUser);
    }
}