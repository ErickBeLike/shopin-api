package com.app.shopin.modules.security.entity;

import com.app.shopin.modules.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrincipalUser implements UserDetails, OidcUser {
    private String userName;
    private String password;
    private Integer tokenVersion;
    private Collection<? extends GrantedAuthority> authorities;

    private User user;

    private OAuth2User oauth2User;

    public PrincipalUser(User user, Collection<? extends GrantedAuthority> authorities, OAuth2User oauth2User) {
        this.user = user;
        this.userName = user.getUserName();
        this.password = user.getPassword();
        this.tokenVersion = user.getTokenVersion();
        this.authorities = authorities;
        this.oauth2User = oauth2User;
    }

    public static PrincipalUser build(User user){
        List<GrantedAuthority> authorities =
                user.getRoles().stream().map(rol -> new SimpleGrantedAuthority(rol
                        .getRolName().name())).collect(Collectors.toList());
        return new PrincipalUser(user, authorities, null);
    }

    public static PrincipalUser build(User user, OAuth2User oauth2User) {
        // Reutiliza el primer método 'build' para obtener las authorities
        PrincipalUser principalUser = build(user);
        // Y solo le asigna el oauth2User
        principalUser.oauth2User = oauth2User;
        return principalUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return oauth2User != null ? oauth2User.getAttributes() : Collections.emptyMap();
    }

    @Override
    public Map<String, Object> getClaims() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getClaims();
        }
        return getAttributes();
    }

    @Override
    public OidcUserInfo getUserInfo() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getUserInfo();
        }
        return null;
    }

    @Override
    public OidcIdToken getIdToken() {
        if (oauth2User instanceof OidcUser) {
            return ((OidcUser) oauth2User).getIdToken();
        }
        return null;
    }

    @Override
    public String getName() { return user.getEmail(); }

    public Integer getTokenVersion() {
        return tokenVersion;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return userName;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }
}
