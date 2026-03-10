package com.gembud.repository;

import com.gembud.entity.ChatRoom;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for ChatRoom entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, Long> {

    /**
     * Find chat room by related room ID.
     *
     * @param roomId related room ID
     * @return optional chat room
     */
    Optional<ChatRoom> findByRelatedRoomId(Long roomId);

    Optional<ChatRoom> findByPublicId(String publicId);

    /**
     * Find chat room by type and related room.
     *
     * @param type chat room type
     * @param roomId related room ID
     * @return optional chat room
     */
    Optional<ChatRoom> findByTypeAndRelatedRoomId(ChatRoom.ChatRoomType type, Long roomId);
}
