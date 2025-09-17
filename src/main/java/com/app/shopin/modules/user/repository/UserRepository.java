package com.app.shopin.modules.user.repository;

import com.app.shopin.modules.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUsername(String username);
    Optional<User> findByEmail(String email);

    @Query("SELECT u.discriminator FROM User u WHERE u.username = :username")
    List<String> findDiscriminatorsByUsername(@Param("username") String username);
    Optional<User> findByUsernameAndDiscriminator(String username, String discriminator);


    boolean existsByEmail(String email);

    Optional<User> findByPasswordResetCode(String code);

    @Modifying
    @Query("DELETE FROM User u WHERE u.userId = :id")
    void hardDeleteById(@Param("id") Long id);
    @Query(value = "SELECT * FROM users WHERE deleted_at <= :date", nativeQuery = true)
    List<User> findUsersForPermanentDeletion(@Param("date") LocalDateTime date);

    @Query(value = "SELECT * FROM users WHERE (user_name = :input OR email = :input) AND deleted_at IS NOT NULL", nativeQuery = true)
    Optional<User> findInactiveByEmail(@Param("input") String input);
    @Modifying
    @Query(value = "UPDATE users SET deleted_at = NULL WHERE user_id = :id", nativeQuery = true)
    void reactivateUserById(@Param("id") Long id);
}

