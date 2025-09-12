package com.app.shopin.modules.user.repository;

import com.app.shopin.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUserName(String userName);
    boolean existsByUserName(String userName);

    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    Optional<User> findByUserNameOrEmail(String userName, String email);
    Optional<User> findByPasswordResetCode(String code);
}

