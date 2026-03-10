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
    private String publicId;
    private String type;
    private String name;
    private Long relatedRoomId;
    private String relatedRoomTitle;
    private String lastMessage;
    private String lastMessageAt;
    private int unreadCount;

    public static ChatRoomResponse from(ChatRoom chatRoom) {
        return from(chatRoom, null, null, 0);
    }

    public static ChatRoomResponse from(
        ChatRoom chatRoom,
        String lastMessage,
        String lastMessageAt,
        int unreadCount
    ) {
        return ChatRoomResponse.builder()
            .id(chatRoom.getId())
            .publicId(chatRoom.getPublicId())
            .type(chatRoom.getType().name())
            .name(chatRoom.getName())
            .relatedRoomId(chatRoom.getRelatedRoom() != null ? chatRoom.getRelatedRoom().getId() : null)
            .relatedRoomTitle(chatRoom.getRelatedRoom() != null ? chatRoom.getRelatedRoom().getTitle() : null)
            .lastMessage(lastMessage)
            .lastMessageAt(lastMessageAt)
            .unreadCount(unreadCount)
            .build();
    }
}
