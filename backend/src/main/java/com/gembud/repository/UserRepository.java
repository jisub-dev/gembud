package com.gembud.repository;

import com.gembud.entity.User;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
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

    /**
     * Search users by nickname (preferred) or email.
     *
     * @param nicknameQuery query for nickname
     * @param emailQuery query for email
     * @param currentUserId current user ID (excluded)
     * @param pageable max rows
     * @return matched users
     */
    @Query("""
        select u
        from User u
        where u.id <> :currentUserId
          and (
            lower(u.nickname) like concat('%', :query, '%')
            or lower(u.email) like concat('%', :query, '%')
          )
        order by u.nickname asc
        """)
    List<User> searchFriendCandidates(
        Long currentUserId,
        String query,
        Pageable pageable
    );

    /**
     * Find users whose premium flag is still true but expiry time has passed.
     */
    List<User> findByPremiumTrueAndPremiumExpiresAtBefore(LocalDateTime now);
}
