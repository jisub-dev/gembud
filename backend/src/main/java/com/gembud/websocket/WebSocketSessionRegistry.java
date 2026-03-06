package com.gembud.websocket;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;

/**
 * Registry for managing WebSocket sessions.
 * Enables forced disconnection of existing sessions on new login.
 *
 * @author Gembud Team
 * @since 2026-03-06
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketSessionRegistry {

    private final SimpUserRegistry simpUserRegistry;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Notify and close all active WebSocket sessions for the given user (email).
     * Sends a session-expired message so the frontend can show the modal.
     *
     * @param email user email (principal name)
     */
    public void closeUserSessions(String email) {
        boolean hasSession = simpUserRegistry.getUsers().stream()
            .anyMatch(user -> email.equals(user.getName()));

        if (hasSession) {
            log.info("Sending session-expired to existing WS sessions for {}", email);
            messagingTemplate.convertAndSendToUser(email, "/queue/session-expired", "SESSION_REPLACED");
        }
    }
}
