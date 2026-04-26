package com.gembud.websocket;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionRegistryTest {

    @Mock private SimpUserRegistry simpUserRegistry;
    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private SimpUser simpUser;

    private WebSocketSessionRegistry registry;

    @BeforeEach
    void setUp() {
        registry = new WebSocketSessionRegistry(simpUserRegistry, messagingTemplate);
    }

    @Test
    @DisplayName("closeUserSessions - 활성 세션이 있으면 session-expired 메시지를 전송한다")
    void closeUserSessions_hasSession_sendsMessage() {
        when(simpUser.getName()).thenReturn("user@test.com");
        when(simpUserRegistry.getUsers()).thenReturn(Set.of(simpUser));

        registry.closeUserSessions("user@test.com");

        verify(messagingTemplate).convertAndSendToUser(
            eq("user@test.com"),
            eq("/queue/session-expired"),
            eq("SESSION_REPLACED")
        );
    }

    @Test
    @DisplayName("closeUserSessions - 해당 유저의 세션이 없으면 메시지를 전송하지 않는다")
    void closeUserSessions_noSession_doesNotSend() {
        when(simpUser.getName()).thenReturn("other@test.com");
        when(simpUserRegistry.getUsers()).thenReturn(Set.of(simpUser));

        registry.closeUserSessions("user@test.com");

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("closeUserSessions - 세션이 없으면 메시지를 전송하지 않는다")
    void closeUserSessions_emptyRegistry_doesNotSend() {
        when(simpUserRegistry.getUsers()).thenReturn(Set.of());

        registry.closeUserSessions("user@test.com");

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), anyString());
    }
}
