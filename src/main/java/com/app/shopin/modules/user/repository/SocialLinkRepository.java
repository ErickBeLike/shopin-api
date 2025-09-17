package com.app.shopin.modules.user.repository;

import com.app.shopin.modules.security.entity.SocialLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SocialLinkRepository extends JpaRepository<SocialLink, Long> {
    Optional<SocialLink> findByProviderAndProviderUserId(String provider, String providerUserId);
}
