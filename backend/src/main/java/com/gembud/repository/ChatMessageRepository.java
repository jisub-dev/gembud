package com.gembud.repository;

import com.gembud.entity.ChatMessage;
import java.util.List;
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
}
