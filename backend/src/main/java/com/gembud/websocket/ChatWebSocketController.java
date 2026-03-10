package com.gembud.websocket;

import com.gembud.dto.request.ChatMessageRequest;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ChatService;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;

/**
 * WebSocket controller for real-time chat messaging.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Controller
@RequiredArgsConstructor
@Slf4j
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final com.gembud.repository.UserRepository userRepository;

    /**
     * Handle incoming chat messages.
     * Client sends message to: /app/chat.send
     * Server broadcasts to: /topic/chat/{chatRoomPublicId}
     *
     * @param chatRoomPublicId chat room public ID
     * @param request message request
     * @param principal authenticated user
     */
    private Long extractUserId(Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth
                && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails.getUserId();
        }
        throw new IllegalStateException("Cannot extract user ID from principal");
    }

    private String resolvePrincipalName(Principal principal) {
        return principal != null && principal.getName() != null
            ? principal.getName()
            : "anonymous";
    }

    private void sendUserError(Principal principal, String message) {
        messagingTemplate.convertAndSendToUser(
            resolvePrincipalName(principal),
            "/queue/errors",
            message
        );
    }

    @MessageMapping("/chat.send/{chatRoomPublicId}")
    public void sendMessage(
        @DestinationVariable String chatRoomPublicId,
        @Payload ChatMessageRequest request,
        Principal principal
    ) {
        try {
            Long userId = extractUserId(principal);

            if (!chatRoomPublicId.equals(request.getChatRoomId())) {
                sendUserError(principal, "chatRoomId mismatch");
                return;
            }

            log.debug("Received message from user {} to chat room {}, len={}",
                userId, chatRoomPublicId, request.getMessage().length());

            // Process message through ChatService
            ChatMessageResponse response = chatService.sendMessage(userId, request);

            // Broadcast message to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomPublicId,
                response
            );

            log.debug("Broadcasted message to /topic/chat/{}: {}",
                chatRoomPublicId, response.getId());

        } catch (NumberFormatException e) {
            log.error("Invalid user ID in principal: {}", principal.getName(), e);
            sendUserError(principal, "Invalid user authentication");
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
            sendUserError(principal, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error processing chat message", e);
            sendUserError(principal, "An unexpected error occurred while sending your message");
        }
    }

    /**
     * Handle user joining a chat room.
     * Client sends to: /app/chat.join/{chatRoomPublicId}
     * Server broadcasts join notification to: /topic/chat/{chatRoomPublicId}
     *
     * @param chatRoomPublicId chat room public ID
     * @param principal authenticated user
     */
    @MessageMapping("/chat.join/{chatRoomPublicId}")
    public void joinChatRoom(
        @DestinationVariable String chatRoomPublicId,
        Principal principal
    ) {
        try {
            Long userId = extractUserId(principal);

            if (!chatService.isChatRoomMemberByPublicId(chatRoomPublicId, userId)) {
                sendUserError(principal, "Not a member of this chat room");
                return;
            }

            String nickname = userRepository.findById(userId)
                .map(u -> u.getNickname())
                .orElse("사용자 " + userId);

            log.info("User {} joined chat room {}", userId, chatRoomPublicId);

            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomPublicId,
                ChatMessageResponse.builder()
                    .chatRoomId(chatRoomPublicId)
                    .userId(userId)
                    .username(nickname)
                    .message("User joined the chat")
                    .build()
            );

        } catch (Exception e) {
            log.error("Error handling chat room join", e);
            sendUserError(principal, "Failed to process chat room join");
        }
    }

    /**
     * Handle user leaving a chat room.
     * Client sends to: /app/chat.leave/{chatRoomPublicId}
     * Server broadcasts leave notification to: /topic/chat/{chatRoomPublicId}
     *
     * @param chatRoomPublicId chat room public ID
     * @param principal authenticated user
     */
    @MessageMapping("/chat.leave/{chatRoomPublicId}")
    public void leaveChatRoom(
        @DestinationVariable String chatRoomPublicId,
        Principal principal
    ) {
        try {
            Long userId = extractUserId(principal);

            if (!chatService.isChatRoomMemberByPublicId(chatRoomPublicId, userId)) {
                sendUserError(principal, "Not a member of this chat room");
                return;
            }

            String nickname = userRepository.findById(userId)
                .map(u -> u.getNickname())
                .orElse("사용자 " + userId);

            log.info("User {} left chat room {}", userId, chatRoomPublicId);

            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomPublicId,
                ChatMessageResponse.builder()
                    .chatRoomId(chatRoomPublicId)
                    .userId(userId)
                    .username(nickname)
                    .message("User left the chat")
                    .build()
            );

        } catch (Exception e) {
            log.error("Error handling chat room leave", e);
            sendUserError(principal, "Failed to process chat room leave");
        }
    }
}
