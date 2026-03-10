package com.gembud.repository;

import com.gembud.entity.ChatMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for ChatMessage entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

    /**
     * Find recent messages for a chat room (for GROUP_CHAT and DIRECT_CHAT).
     *
     * @param chatRoomId chat room ID
     * @param pageable pagination
     * @return list of messages
     */
    @Query("SELECT m FROM ChatMessage m WHERE m.chatRoom.id = :chatRoomId ORDER BY m.createdAt DESC")
    List<ChatMessage> findRecentMessages(@Param("chatRoomId") Long chatRoomId, Pageable pageable);

    /**
     * Delete old messages for GROUP_CHAT (keep only last 100).
     *
     * @param chatRoomId chat room ID
     * @param limit limit
     */
    @Query(value = "DELETE FROM chat_messages WHERE chat_room_id = :chatRoomId AND id NOT IN " +
           "(SELECT id FROM chat_messages WHERE chat_room_id = :chatRoomId ORDER BY created_at DESC LIMIT :limit)",
           nativeQuery = true)
    void deleteOldMessages(@Param("chatRoomId") Long chatRoomId, @Param("limit") int limit);

    /**
     * Count messages in a chat room.
     *
     * @param chatRoomId chat room ID
     * @return message count
     */
    long countByChatRoomId(Long chatRoomId);

    Optional<ChatMessage> findTopByChatRoomIdOrderByCreatedAtDesc(Long chatRoomId);

    long countByChatRoomIdAndCreatedAtAfter(Long chatRoomId, LocalDateTime createdAt);

    /**
     * Delete old ROOM_CHAT messages (Phase 11: Evidence retention).
     * Deletes ROOM_CHAT messages older than the specified date.
     *
     * @param cutoffDate cutoff date (e.g., 7 days ago)
     * @return number of deleted messages
     */
    @Query(value = "DELETE FROM chat_messages m USING chat_rooms r " +
           "WHERE m.chat_room_id = r.id " +
           "AND r.type = 'ROOM_CHAT' " +
           "AND m.created_at < :cutoffDate",
           nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    int deleteOldRoomChatMessages(@Param("cutoffDate") java.time.LocalDateTime cutoffDate);
}
