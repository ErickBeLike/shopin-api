package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RolService rolService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        String email = oauth2User.getAttribute("email");
        String firstName = oauth2User.getAttribute("given_name");
        String lastName = oauth2User.getAttribute("family_name");
        String pictureUrl = oauth2User.getAttribute("picture");

        // Busca si el usuario ya existe en tu BD por su email
        Optional<User> userOptional = userRepository.findByEmail(email);

        User user;
        if (userOptional.isPresent()) {
            // Si ya existe, lo usamos
            user = userOptional.get();
        } else {
            // Si no existe, creamos uno nuevo
            user = new User();
            user.setEmail(email);
            user.setFirstName(firstName);
            user.setLastName(lastName);
            user.setUserName(email); // O genera un username único
            user.setPassword(null); // No tienen contraseña local
            user.setProfilePictureUrl(pictureUrl);

            // Asignar rol por defecto
            Set<Rol> roles = new HashSet<>();
            roles.add(rolService.getByRolName(RolName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error: Rol no encontrado.")));
            user.setRoles(roles);

            userRepository.save(user);
        }

        // Construimos y devolvemos el PrincipalUser, que Spring Security usará
        return PrincipalUser.build(user, oauth2User.getAttributes());
    }
}
