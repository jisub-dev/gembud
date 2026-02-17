package com.gembud.controller;

import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ChatService;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for chat operations.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Get recent messages from a chat room.
     *
     * @param userDetails authenticated user
     * @param chatRoomId chat room ID
     * @param limit maximum number of messages (default: 50)
     * @return list of messages
     */
    @GetMapping("/rooms/{chatRoomId}/messages")
    public ResponseEntity<List<ChatMessageResponse>> getRecentMessages(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long chatRoomId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<ChatMessageResponse> messages = chatService.getRecentMessages(
            chatRoomId,
            userDetails.getUserId(),
            limit
        );

        return ResponseEntity.ok(messages);
    }

    /**
     * Get chat room ID for a game room.
     *
     * @param roomId game room ID
     * @return chat room ID
     */
    @GetMapping("/rooms/by-game-room/{roomId}")
    public ResponseEntity<Long> getChatRoomByGameRoomId(
        @PathVariable Long roomId
    ) {
        Long chatRoomId = chatService.getChatRoomByGameRoomId(roomId);
        return ResponseEntity.ok(chatRoomId);
    }

    /**
     * Create a direct chat room with another user.
     *
     * @param userDetails authenticated user
     * @param request create direct chat request
     * @return created chat room ID
     */
    @PostMapping("/rooms/direct")
    public ResponseEntity<ChatRoomIdResponse> createDirectChatRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CreateDirectChatRequest request
    ) {
        Long chatRoomId = chatService.createDirectChatRoom(
            userDetails.getUserId(),
            request.getFriendId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ChatRoomIdResponse(chatRoomId));
    }

    /**
     * Create a group chat room.
     *
     * @param userDetails authenticated user
     * @param request create group chat request
     * @return created chat room ID
     */
    @PostMapping("/rooms/group")
    public ResponseEntity<ChatRoomIdResponse> createGroupChatRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CreateGroupChatRequest request
    ) {
        Long chatRoomId = chatService.createGroupChatRoom(
            request.getName(),
            userDetails.getUserId()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(new ChatRoomIdResponse(chatRoomId));
    }

    /**
     * Add a member to a group chat room.
     *
     * @param userDetails authenticated user
     * @param chatRoomId chat room ID
     * @param request add member request
     * @return no content
     */
    @PostMapping("/rooms/{chatRoomId}/members")
    public ResponseEntity<Void> addMemberToChatRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long chatRoomId,
        @RequestBody AddMemberRequest request
    ) {
        chatService.addMemberToChatRoom(chatRoomId, request.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Request DTOs
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateDirectChatRequest {
        private Long friendId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateGroupChatRequest {
        private String name;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddMemberRequest {
        private Long userId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatRoomIdResponse {
        private Long chatRoomId;
    }
}
