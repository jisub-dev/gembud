package com.gembud.repository;

import com.gembud.entity.ChatRoomMember;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ChatRoomMember entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, Long> {

    /**
     * Find all members in a chat room.
     *
     * @param chatRoomId chat room ID
     * @return list of members
     */
    List<ChatRoomMember> findByChatRoomId(Long chatRoomId);

    /**
     * Find member by chat room and user.
     *
     * @param chatRoomId chat room ID
     * @param userId user ID
     * @return optional member
     */
    @Query("SELECT m FROM ChatRoomMember m WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId")
    Optional<ChatRoomMember> findByChatRoomIdAndUserId(
        @Param("chatRoomId") Long chatRoomId,
        @Param("userId") Long userId
    );

    /**
     * Check if user is member of chat room.
     *
     * @param chatRoomId chat room ID
     * @param userId user ID
     * @return true if member
     */
    @Query("SELECT COUNT(m) > 0 FROM ChatRoomMember m WHERE m.chatRoom.id = :chatRoomId AND m.user.id = :userId")
    boolean existsByChatRoomIdAndUserId(
        @Param("chatRoomId") Long chatRoomId,
        @Param("userId") Long userId
    );

    /**
     * Find all chat rooms a user is a member of.
     *
     * @param userId user ID
     * @return list of chat room members with chat room info
     */
    @Query("SELECT m FROM ChatRoomMember m JOIN FETCH m.chatRoom c WHERE m.user.id = :userId ORDER BY c.createdAt DESC")
    List<ChatRoomMember> findChatRoomsByUserId(@Param("userId") Long userId);
}
