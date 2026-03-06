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
     * Server broadcasts to: /topic/chat/{chatRoomId}
     *
     * @param chatRoomId chat room ID
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

    @MessageMapping("/chat.send/{chatRoomId}")
    public void sendMessage(
        @DestinationVariable Long chatRoomId,
        @Payload ChatMessageRequest request,
        Principal principal
    ) {
        try {
            Long userId = extractUserId(principal);

            if (!chatRoomId.equals(request.getChatRoomId())) {
                messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", "chatRoomId mismatch");
                return;
            }

            log.debug("Received message from user {} to chat room {}, len={}",
                userId, chatRoomId, request.getMessage().length());

            // Process message through ChatService
            ChatMessageResponse response = chatService.sendMessage(userId, request);

            // Broadcast message to all subscribers of this chat room
            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId,
                response
            );

            log.debug("Broadcasted message to /topic/chat/{}: {}",
                chatRoomId, response.getId());

        } catch (NumberFormatException e) {
            log.error("Invalid user ID in principal: {}", principal.getName(), e);
            // Send error to user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                "Invalid user authentication"
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            log.error("Error processing chat message: {}", e.getMessage(), e);
            // Send error to user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                e.getMessage()
            );
        } catch (Exception e) {
            log.error("Unexpected error processing chat message", e);
            // Send generic error to user
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                "An unexpected error occurred while sending your message"
            );
        }
    }

    /**
     * Handle user joining a chat room.
     * Client sends to: /app/chat.join/{chatRoomId}
     * Server broadcasts join notification to: /topic/chat/{chatRoomId}
     *
     * @param chatRoomId chat room ID
     * @param principal authenticated user
     */
    @MessageMapping("/chat.join/{chatRoomId}")
    public void joinChatRoom(
        @DestinationVariable Long chatRoomId,
        Principal principal
    ) {
        try {
            Long userId = extractUserId(principal);

            if (!chatService.isChatRoomMember(chatRoomId, userId)) {
                messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", "Not a member of this chat room");
                return;
            }

            String nickname = userRepository.findById(userId)
                .map(u -> u.getNickname())
                .orElse("사용자 " + userId);

            log.info("User {} joined chat room {}", userId, chatRoomId);

            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId,
                ChatMessageResponse.builder()
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .username(nickname)
                    .message("User joined the chat")
                    .build()
            );

        } catch (Exception e) {
            log.error("Error handling chat room join", e);
        }
    }

    /**
     * Handle user leaving a chat room.
     * Client sends to: /app/chat.leave/{chatRoomId}
     * Server broadcasts leave notification to: /topic/chat/{chatRoomId}
     *
     * @param chatRoomId chat room ID
     * @param principal authenticated user
     */
    @MessageMapping("/chat.leave/{chatRoomId}")
    public void leaveChatRoom(
        @DestinationVariable Long chatRoomId,
        Principal principal
    ) {
        try {
            Long userId = extractUserId(principal);

            if (!chatService.isChatRoomMember(chatRoomId, userId)) {
                messagingTemplate.convertAndSendToUser(
                    principal.getName(), "/queue/errors", "Not a member of this chat room");
                return;
            }

            String nickname = userRepository.findById(userId)
                .map(u -> u.getNickname())
                .orElse("사용자 " + userId);

            log.info("User {} left chat room {}", userId, chatRoomId);

            messagingTemplate.convertAndSend(
                "/topic/chat/" + chatRoomId,
                ChatMessageResponse.builder()
                    .chatRoomId(chatRoomId)
                    .userId(userId)
                    .username(nickname)
                    .message("User left the chat")
                    .build()
            );

        } catch (Exception e) {
            log.error("Error handling chat room leave", e);
        }
    }
}
