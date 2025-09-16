package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.UserRepository;
import com.app.shopin.services.cloudinary.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private RolService rolService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StorageService storageService;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oauth2User = super.loadUser(userRequest);

        Map<String, Object> attributes = oauth2User.getAttributes();
        String email = attributes.get("email").toString();
        String name = attributes.get("name").toString();
        String[] names = name.split(" ");
        String firstName = names[0];
        String lastName = names.length > 1 ? names[names.length - 1] : "";

        String pictureUrl = "";
        if (attributes.containsKey("picture")) {
            Map<String, Object> pictureObj = (Map<String, Object>) attributes.get("picture");
            if (pictureObj.containsKey("data")) {
                Map<String, Object> dataObj = (Map<String, Object>) pictureObj.get("data");
                if (dataObj.containsKey("url")) {
                    pictureUrl = dataObj.get("url").toString();
                }
            }
        }

        User user = processOAuth2User(email, firstName, lastName, pictureUrl);
        return PrincipalUser.build(user, oauth2User);
    }

    public User processOAuth2User(String email, String firstName, String lastName, String pictureUrl) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstName(firstName);
            newUser.setLastName(lastName);
            // Usamos el email como username por defecto, ya que es único
            newUser.setUserName(email);
            // Creamos una contraseña aleatoria, ya que este usuario no la usará
            newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

            // Subir la imagen de perfil a Cloudinary si existe
            try {
                if (pictureUrl != null && !pictureUrl.isEmpty()) {
                    Map<String, String> fileInfo = storageService.uploadFromUrl(pictureUrl, "profileimages");
                    newUser.setProfilePictureUrl(fileInfo.get("url"));
                    newUser.setProfilePicturePublicId(fileInfo.get("publicId"));
                }
            } catch (Exception e) {
                // Manejar la excepción, por ejemplo, logueándola.
            }

            Set<Rol> roles = new HashSet<>();
            Rol userRole = rolService.getByRolName(RolName.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("FATAL: Rol 'ROLE_USER' no encontrado."));
            roles.add(userRole);
            newUser.setRoles(roles);

            return userRepository.save(newUser);
        });
    }
}