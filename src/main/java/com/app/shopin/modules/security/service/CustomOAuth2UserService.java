package com.app.shopin.modules.security.service;

import com.app.shopin.modules.exception.CustomOAuth2AuthenticationException;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.Rol;
import com.app.shopin.modules.security.entity.SocialLink;
import com.app.shopin.modules.security.enums.RolName;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.SocialLinkRepository;
import com.app.shopin.modules.user.repository.UserRepository;
import com.app.shopin.services.cloudinary.StorageService;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialLinkRepository socialLinkRepository;

    @Autowired
    private RolService rolService;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private StorageService storageService;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        Map<String, Object> attributes = oauth2User.getAttributes();
        String providerUserId = attributes.get("id").toString();
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

        User user = processOAuth2User(provider, providerUserId, email, firstName, lastName, pictureUrl);
        return PrincipalUser.build(user, oauth2User);
    }

    public User processOAuth2User(String provider, String providerUserId, String email, String firstName, String lastName, String pictureUrl) {

        // Flujo 1: ¿El usuario ya está logueado? (Quiere VINCULAR una cuenta)
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
            User currentUser = principal.getUser();

            if (socialLinkRepository.findByProviderAndProviderUserId(provider, providerUserId).isPresent()) {
                throw new CustomOAuth2AuthenticationException("Esta cuenta de " + provider + " ya está vinculada a otro usuario.");
            }

            SocialLink newSocialLink = new SocialLink();
            newSocialLink.setUser(currentUser);
            newSocialLink.setProvider(provider);
            newSocialLink.setProviderUserId(providerUserId);
            socialLinkRepository.save(newSocialLink);

            return currentUser;
        }

        // Flujo 2: El usuario NO está logueado (Quiere INICIAR SESIÓN o REGISTRARSE)
        Optional<SocialLink> socialLinkOptional = socialLinkRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (socialLinkOptional.isPresent()) {
            // El usuario ya existe a través de esta vinculación social. Lo logueamos.
            return socialLinkOptional.get().getUser();
        }

        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            // El email existe, pero no está vinculado a esta cuenta social. Lanzamos error.
            throw new CustomOAuth2AuthenticationException("Ya existe una cuenta con el correo " + email + ". Por favor, inicie sesión con su método original para vincular su cuenta de " + provider + ".");
        }

        // Flujo 3: Es un usuario completamente nuevo. Creamos tod0.
        User newUser = new User();
        newUser.setEmail(email);
        newUser.setFirstName(firstName);
        newUser.setLastName(lastName);
        newUser.setUserName(email); // O tu lógica de username generado
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));

        try {
            if (pictureUrl != null && !pictureUrl.isEmpty()) {
                Map<String, String> fileInfo = storageService.uploadFromUrl(pictureUrl, "profileimages");
                newUser.setProfilePictureUrl(fileInfo.get("url"));
                newUser.setProfilePicturePublicId(fileInfo.get("publicId"));
            }
        } catch (Exception e) { /* Manejar error */ }

        Set<Rol> roles = new HashSet<>();
        roles.add(rolService.getByRolName(RolName.ROLE_USER).orElseThrow());
        newUser.setRoles(roles);

        SocialLink newSocialLink = new SocialLink();
        newSocialLink.setUser(newUser);
        newSocialLink.setProvider(provider);
        newSocialLink.setProviderUserId(providerUserId);
        newUser.getSocialLinks().add(newSocialLink);

        return userRepository.save(newUser);
    }
}