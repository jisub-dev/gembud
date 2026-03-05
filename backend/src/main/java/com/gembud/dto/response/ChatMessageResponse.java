package com.gembud.dto.response;

import com.gembud.entity.ChatMessage;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for chat message.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageResponse {

    /**
     * Message ID.
     */
    private Long id;

    /**
     * Chat room ID.
     */
    private Long chatRoomId;

    /**
     * User ID who sent the message.
     */
    private Long userId;

    /**
     * Username (nickname) of the sender.
     */
    private String username;

    /**
     * Message content.
     */
    private String message;

    /**
     * Timestamp when message was created.
     */
    private LocalDateTime createdAt;

    /**
     * Message type: "CHAT" for normal messages, "ROOM_UPDATE" for room state changes.
     */
    private String type;

    /**
     * Convert ChatMessage entity to response DTO.
     *
     * @param chatMessage chat message entity
     * @return response DTO
     */
    public static ChatMessageResponse from(ChatMessage chatMessage) {
        return ChatMessageResponse.builder()
            .id(chatMessage.getId())
            .chatRoomId(chatMessage.getChatRoom().getId())
            .userId(chatMessage.getUser().getId())
            .username(chatMessage.getUser().getNickname())
            .message(chatMessage.getMessage())
            .createdAt(chatMessage.getCreatedAt())
            .type("CHAT")
            .build();
    }
}
