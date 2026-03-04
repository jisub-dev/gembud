package com.gembud.dto.response;

import com.gembud.entity.ChatRoom;
import lombok.Builder;
import lombok.Getter;

/**
 * Response DTO for chat room info.
 *
 * @author Gembud Team
 * @since 2026-03-03
 */
@Getter
@Builder
public class ChatRoomResponse {

    private Long id;
    private String type;
    private String name;
    private Long relatedRoomId;
    private String relatedRoomTitle;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return ChatRoomResponse.builder()
            .id(chatRoom.getId())
            .type(chatRoom.getType().name())
            .name(chatRoom.getName())
            .relatedRoomId(chatRoom.getRelatedRoom() != null ? chatRoom.getRelatedRoom().getId() : null)
            .relatedRoomTitle(chatRoom.getRelatedRoom() != null ? chatRoom.getRelatedRoom().getTitle() : null)
            .build();
    }
}
