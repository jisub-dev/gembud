package com.gembud.repository;

import com.gembud.entity.Friend;
import com.gembud.entity.Friend.FriendStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Friend entity.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Repository
public interface FriendRepository extends JpaRepository<Friend, Long> {

    /**
     * Find friend relationship between two users.
     *
     * @param userId user ID
     * @param friendId friend ID
     * @return optional friend relationship
     */
    @Query("SELECT f FROM Friend f WHERE f.user.id = :userId AND f.friend.id = :friendId")
    Optional<Friend> findByUserIdAndFriendId(
        @Param("userId") Long userId,
        @Param("friendId") Long friendId
    );

    /**
     * Find all friend requests sent by a user.
     *
     * @param userId user ID
     * @param status friend status
     * @return list of friend relationships
     */
    @Query("SELECT f FROM Friend f WHERE f.user.id = :userId AND f.status = :status")
    List<Friend> findByUserIdAndStatus(
        @Param("userId") Long userId,
        @Param("status") FriendStatus status
    );

    /**
     * Find all friend requests received by a user.
     *
     * @param friendId friend ID (receiver)
     * @param status friend status
     * @return list of friend relationships
     */
    @Query("SELECT f FROM Friend f WHERE f.friend.id = :friendId AND f.status = :status")
    List<Friend> findByFriendIdAndStatus(
        @Param("friendId") Long friendId,
        @Param("status") FriendStatus status
    );

    /**
     * Find all accepted friends for a user (bidirectional).
     *
     * @param userId user ID
     * @return list of accepted friend relationships
     */
    @Query("SELECT f FROM Friend f WHERE (f.user.id = :userId OR f.friend.id = :userId) " +
           "AND f.status = 'ACCEPTED'")
    List<Friend> findAcceptedFriends(@Param("userId") Long userId);

    /**
     * Check if two users are friends (bidirectional).
     *
     * @param userId user ID
     * @param friendId friend ID
     * @return true if they are friends
     */
    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE " +
           "((f.user.id = :userId AND f.friend.id = :friendId) OR " +
           "(f.user.id = :friendId AND f.friend.id = :userId)) " +
           "AND f.status = 'ACCEPTED'")
    boolean areFriends(
        @Param("userId") Long userId,
        @Param("friendId") Long friendId
    );

    /**
     * Check if friend request exists (bidirectional).
     *
     * @param userId user ID
     * @param friendId friend ID
     * @return true if request exists
     */
    @Query("SELECT COUNT(f) > 0 FROM Friend f WHERE " +
           "(f.user.id = :userId AND f.friend.id = :friendId) OR " +
           "(f.user.id = :friendId AND f.friend.id = :userId)")
    boolean requestExists(
        @Param("userId") Long userId,
        @Param("friendId") Long friendId
    );

    /**
     * Delete friend relationship (for unfriend).
     *
     * @param userId user ID
     * @param friendId friend ID
     */
    @Query("DELETE FROM Friend f WHERE " +
           "(f.user.id = :userId AND f.friend.id = :friendId) OR " +
           "(f.user.id = :friendId AND f.friend.id = :userId)")
    @Modifying
    void deleteByUserIdAndFriendId(
        @Param("userId") Long userId,
        @Param("friendId") Long friendId
    );
}
