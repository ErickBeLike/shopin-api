package com.app.shopin.modules.security.service;

import com.app.shopin.modules.security.entity.PrincipalUser;
import com.app.shopin.modules.user.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

@Service
public class CustomOidcUserService extends OidcUserService {

    @Autowired
    private CustomOAuth2UserService customOAuth2UserService; // Inyectamos el otro servicio para reusar su lógica

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest);

        // Extraemos los datos estandarizados de OIDC
        String email = oidcUser.getAttribute("email");
        String firstName = oidcUser.getAttribute("given_name");
        String lastName = oidcUser.getAttribute("family_name");
        String pictureUrl = oidcUser.getAttribute("picture");

        // Reutilizamos la misma lógica de negocio del otro servicio
        User user = customOAuth2UserService.processOAuth2User(email, firstName, lastName, pictureUrl);

        return PrincipalUser.build(user, oidcUser);
    }
}