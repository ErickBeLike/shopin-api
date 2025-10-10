package com.app.shopin.modules.security.service;

import com.app.shopin.modules.exception.CustomOAuth2AuthenticationException;
import com.app.shopin.modules.security.dto.oauth2.OAuth2TempInfo;
import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.security.entity.SocialLink;
import com.app.shopin.modules.user.entity.User;
import com.app.shopin.modules.user.repository.SocialLinkRepository;
import com.app.shopin.modules.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private SocialLinkRepository socialLinkRepository;

    @Autowired
    private HttpServletRequest request;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();

        // Extraemos los datos de Facebook
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

        // Creamos y devolvemos nuestro PrincipalUser, que sabe cómo manejar un User real o uno temporal
        return PrincipalUser.build(user, oauth2User);
    }

    public User processOAuth2User(String provider, String providerUserId, String email, String firstName, String lastName, String pictureUrl) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            PrincipalUser principal = (PrincipalUser) authentication.getPrincipal();
            Long currentUserId = principal.getUser().getId();
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new IllegalStateException("Usuario logueado no encontrado."));

            if (socialLinkRepository.findByProviderAndProviderUserId(provider, providerUserId).isPresent()) {
                throw new CustomOAuth2AuthenticationException("Esta cuenta de " + provider + " ya está vinculada a otro usuario.");
            }
            SocialLink newSocialLink = new SocialLink();
            newSocialLink.setUser(currentUser);
            newSocialLink.setProvider(provider);
            newSocialLink.setProviderUserId(providerUserId);
            socialLinkRepository.save(newSocialLink);

            request.getSession().setAttribute("OAUTH2_FLOW_TYPE", "LINKING");

            return currentUser;
        }

        Optional<SocialLink> socialLinkOptional = socialLinkRepository.findByProviderAndProviderUserId(provider, providerUserId);
        if (socialLinkOptional.isPresent()) {
            return socialLinkOptional.get().getUser();
        }
        Optional<User> userOptional = userRepository.findByEmail(email);
        if (userOptional.isPresent()) {
            throw new CustomOAuth2AuthenticationException("Ya existe una cuenta con el correo " + email + ".");
        }

        OAuth2TempInfo tempInfo = new OAuth2TempInfo(provider, providerUserId, email, firstName, lastName, pictureUrl);
        request.getSession().setAttribute("OAUTH2_TEMP_INFO", tempInfo);
        User incompleteUser = new User();
        incompleteUser.setEmail("PENDING_REGISTRATION");
        return incompleteUser;
    }
}