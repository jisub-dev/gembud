package com.gembud.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for sending a chat message.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessageRequest {

    /**
     * Chat room ID.
     */
    @NotNull(message = "Chat room ID is required")
    private Long chatRoomId;

    /**
     * Message content.
     */
    @NotBlank(message = "Message cannot be empty")
    private String message;
}
