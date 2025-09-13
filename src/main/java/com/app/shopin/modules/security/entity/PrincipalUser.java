package com.app.shopin.modules.security.entity;

import com.app.shopin.modules.user.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PrincipalUser implements UserDetails, OAuth2User {
    private String userName;
    private String password;
    private Integer tokenVersion;
    private Collection<? extends GrantedAuthority> authorities;

    private User user;

    private Map<String, Object> attributes;

    public PrincipalUser(String userName, String password, Integer tokenVersion, Collection<? extends GrantedAuthority> authorities, User user) {
        this.userName = userName;
        this.password = password;
        this.tokenVersion = tokenVersion;
        this.authorities = authorities;
        this.user = user;
    }

    public static PrincipalUser build(User user){
        List<GrantedAuthority> authorities =
                user.getRoles().stream().map(rol -> new SimpleGrantedAuthority(rol
                        .getRolName().name())).collect(Collectors.toList());
        return new PrincipalUser(user.getUserName(), user.getPassword(), user.getTokenVersion(), authorities, user);
    }

    public static PrincipalUser build(User user, Map<String, Object> attributes) {
        PrincipalUser principalUser = build(user);
        principalUser.setAttributes(attributes);
        return principalUser;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public String getName() {
        return user.getEmail();
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

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
