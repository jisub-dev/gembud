package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.dto.response.ChatRoomResponse;
import com.gembud.entity.ChatRoom;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Chat", description = "채팅 API")
@RestController
@RequestMapping("/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    /**
     * Get chat rooms the current user is a member of.
     *
     * @param userDetails authenticated user
     * @return list of chat rooms
     */
    @Operation(summary = "Get my chat rooms", description = "내가 참여 중인 채팅방 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/rooms/my")
    public ResponseEntity<ApiResponse<List<ChatRoomResponse>>> getMyChatRooms(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<ChatRoomResponse> rooms = chatService.getMyChatRooms(userDetails.getUserId()).stream()
            .map(ChatRoomResponse::from)
            .collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    /**
     * Get recent messages from a chat room.
     *
     * @param userDetails authenticated user
     * @param chatRoomPublicId chat room public ID
     * @param limit maximum number of messages (default: 50)
     * @return list of messages
     */
    @Operation(summary = "Get recent messages", description = "채팅방의 최근 메시지 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "메시지 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 접근 권한 없음")
    })
    @GetMapping("/rooms/{chatRoomPublicId}/messages")
    public ResponseEntity<ApiResponse<List<ChatMessageResponse>>> getRecentMessages(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable String chatRoomPublicId,
        @RequestParam(defaultValue = "50") int limit
    ) {
        List<ChatMessageResponse> messages = chatService.getRecentMessages(
            chatRoomPublicId,
            userDetails.getUserId(),
            limit
        );

        return ResponseEntity.ok(ApiResponse.success(messages));
    }

    /**
     * Get chat room ID for a game room.
     *
     * @param roomId game room ID
     * @return chat room public ID
     */
    @Operation(summary = "Get chat room by game room", description = "게임 방 ID로 채팅방 ID 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "채팅방 ID 반환"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방을 찾을 수 없음")
    })
    @GetMapping("/rooms/by-game-room/{roomId}")
    public ResponseEntity<ApiResponse<String>> getChatRoomByGameRoomId(
        @PathVariable Long roomId
    ) {
        String chatRoomId = chatService.getChatRoomByGameRoomId(roomId);
        return ResponseEntity.ok(ApiResponse.success(chatRoomId));
    }

    /**
     * Create a direct chat room with another user.
     *
     * @param userDetails authenticated user
     * @param request create direct chat request
     * @return created chat room ID
     */
    @Operation(summary = "Create direct chat", description = "1:1 채팅방 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    })
    @PostMapping("/rooms/direct")
    public ResponseEntity<ApiResponse<ChatRoomIdResponse>> createDirectChatRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CreateDirectChatRequest request
    ) {
        Long chatRoomId = chatService.createDirectChatRoom(
            userDetails.getUserId(),
            request.getFriendId()
        );
        String publicId = chatService.getPublicIdByChatRoomId(chatRoomId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(new ChatRoomIdResponse(publicId)));
    }

    /**
     * Create a group chat room.
     *
     * @param userDetails authenticated user
     * @param request create group chat request
     * @return created chat room ID
     */
    @Operation(summary = "Create group chat", description = "그룹 채팅방 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "채팅방 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PostMapping("/rooms/group")
    public ResponseEntity<ApiResponse<ChatRoomIdResponse>> createGroupChatRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody CreateGroupChatRequest request
    ) {
        Long chatRoomId = chatService.createGroupChatRoom(
            request.getName(),
            userDetails.getUserId()
        );
        String publicId = chatService.getPublicIdByChatRoomId(chatRoomId);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.created(new ChatRoomIdResponse(publicId)));
    }

    /**
     * Add a member to a group chat room.
     *
     * @param userDetails authenticated user
     * @param chatRoomId chat room ID
     * @param request add member request
     * @return no content
     */
    @Operation(summary = "Add member to chat", description = "그룹 채팅방에 멤버 추가")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "멤버 추가 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "채팅방 관리 권한 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "채팅방 또는 사용자를 찾을 수 없음")
    })
    @PostMapping("/rooms/{chatRoomId}/members")
    public ResponseEntity<ApiResponse<Void>> addMemberToChatRoom(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long chatRoomId,
        @RequestBody AddMemberRequest request
    ) {
        chatService.addMemberToChatRoom(chatRoomId, request.getUserId(), userDetails.getUserId());
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.created(null));
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
        private String chatRoomId;
    }
}
