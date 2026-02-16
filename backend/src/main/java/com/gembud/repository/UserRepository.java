package com.gembud.repository;

import com.gembud.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for User entity
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find user by email
     *
     * @param email user email
     * @return Optional User
     */
    Optional<User> findByEmail(String email);

    /**
     * Find user by OAuth provider and OAuth ID
     *
     * @param oauthProvider OAuth provider (GOOGLE, DISCORD)
     * @param oauthId OAuth user ID
     * @return Optional User
     */
    Optional<User> findByOauthProviderAndOauthId(
            User.OAuthProvider oauthProvider,
            String oauthId
    );

    /**
     * Check if email already exists
     *
     * @param email user email
     * @return true if exists, false otherwise
     */
    boolean existsByEmail(String email);

    /**
     * Check if nickname already exists
     *
     * @param nickname user nickname
     * @return true if exists, false otherwise
     */
    boolean existsByNickname(String nickname);

    /**
     * Check if OAuth account already exists
     *
     * @param oauthProvider OAuth provider
     * @param oauthId OAuth user ID
     * @return true if exists, false otherwise
     */
    boolean existsByOauthProviderAndOauthId(
            User.OAuthProvider oauthProvider,
            String oauthId
    );
}
