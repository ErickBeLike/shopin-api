package com.app.shopin.modules.security.entity;

import com.app.shopin.modules.user.entity.User;
import jakarta.persistence.*;

@Entity
@Table(name = "social_links", uniqueConstraints = {
        // Asegura que no puedas vincular la misma cuenta de Google/Facebook a dos usuarios diferentes
        @UniqueConstraint(columnNames = {"provider", "provider_user_id"})
})
public class SocialLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider; // "google", "facebook", etc.

    @Column(name = "provider_user_id", nullable = false)
    private String providerUserId; // El ID único que nos da Google/Facebook

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderUserId() {
        return providerUserId;
    }

    public void setProviderUserId(String providerUserId) {
        this.providerUserId = providerUserId;
    }
}
