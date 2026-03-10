package com.gembud.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.ChatMessageRequest;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.entity.User;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ChatService;
import java.security.Principal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private com.gembud.repository.UserRepository userRepository;

    @InjectMocks
    private ChatWebSocketController controller;

    private Principal principal;

    @BeforeEach
    void setUp() {
        CustomUserDetails userDetails =
            new CustomUserDetails(1L, "user@example.com", "pw", User.UserRole.USER);
        principal = new UsernamePasswordAuthenticationToken(
            userDetails,
            null,
            userDetails.getAuthorities()
        );
    }

    @Test
    @DisplayName("sendMessage - destination publicId와 payload chatRoomId가 다르면 거부")
    void sendMessage_MismatchChatRoomId_ShouldReject() {
        ChatMessageRequest request = ChatMessageRequest.builder()
            .chatRoomId("chat-public-2")
            .message("hello")
            .build();

        controller.sendMessage("chat-public-1", request, principal);

        verify(chatService, never()).sendMessage(any(), any());
        verify(messagingTemplate).convertAndSendToUser(
            eq("user@example.com"),
            eq("/queue/errors"),
            eq("chatRoomId mismatch")
        );
    }

    @Test
    @DisplayName("joinChatRoom - 비멤버면 에러 큐로 전송")
    void joinChatRoom_NotMember_ShouldSendErrorQueue() {
        when(chatService.isChatRoomMemberByPublicId("chat-public-1", 1L)).thenReturn(false);

        controller.joinChatRoom("chat-public-1", principal);

        verify(messagingTemplate).convertAndSendToUser(
            eq("user@example.com"),
            eq("/queue/errors"),
            eq("Not a member of this chat room")
        );
        verify(messagingTemplate, never()).convertAndSend(
            eq("/topic/chat/chat-public-1"),
            any(ChatMessageResponse.class)
        );
    }

    @Test
    @DisplayName("joinChatRoom - 멤버면 입장 메시지 브로드캐스트")
    void joinChatRoom_Member_ShouldBroadcastJoinMessage() {
        User user = User.builder().nickname("Tester").build();
        ReflectionTestUtils.setField(user, "id", 1L);

        when(chatService.isChatRoomMemberByPublicId("chat-public-1", 1L)).thenReturn(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        controller.joinChatRoom("chat-public-1", principal);

        verify(messagingTemplate).convertAndSend(eq("/topic/chat/chat-public-1"), any(ChatMessageResponse.class));
        verify(messagingTemplate, never()).convertAndSendToUser(eq("user@example.com"), eq("/queue/errors"), any());
    }
}
