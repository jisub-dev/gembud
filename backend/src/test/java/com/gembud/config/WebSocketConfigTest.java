package com.gembud.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.User;
import com.gembud.repository.UserRepository;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.ChatService;
import com.gembud.service.RateLimitService;
import com.gembud.service.SecurityEventService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WebSocketConfigTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private SecurityEventService securityEventService;

    @Mock
    private ChatService chatService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChannelRegistration channelRegistration;

    @Mock
    private MessageChannel messageChannel;

    private WebSocketConfig webSocketConfig;

    @BeforeEach
    void setUp() {
        webSocketConfig = new WebSocketConfig(
            jwtTokenProvider,
            rateLimitService,
            securityEventService,
            chatService,
            userRepository
        );
        ReflectionTestUtils.setField(webSocketConfig, "allowedOrigins", "http://localhost:5173");
        when(channelRegistration.interceptors(any(ChannelInterceptor[].class))).thenReturn(channelRegistration);
    }

    @Test
    @DisplayName("SUBSCRIBE /topic/chat/{publicId} - publicId 멤버 검증을 통과하면 구독된다")
    void subscribe_PublicChatRoomMember_AllowsSubscription() {
        ChannelInterceptor interceptor = captureInterceptor();
        Message<byte[]> message = buildSubscribeMessage("/topic/chat/chat-public-123", "member@test.com");

        User member = User.builder()
            .email("member@test.com")
            .nickname("member")
            .password("pw")
            .build();
        ReflectionTestUtils.setField(member, "id", 7L);

        when(userRepository.findByEmail("member@test.com")).thenReturn(Optional.of(member));
        when(chatService.isChatRoomMemberByPublicId("chat-public-123", 7L)).thenReturn(true);

        Message<?> result = interceptor.preSend(message, messageChannel);

        assertThat(result).isSameAs(message);
        verify(chatService).isChatRoomMemberByPublicId("chat-public-123", 7L);
    }

    @Test
    @DisplayName("SUBSCRIBE /topic/chat/{publicId} - 멤버가 아니면 구독이 거부된다")
    void subscribe_NonMember_ThrowsAccessDenied() {
        ChannelInterceptor interceptor = captureInterceptor();
        Message<byte[]> message = buildSubscribeMessage("/topic/chat/chat-public-999", "intruder@test.com");

        User intruder = User.builder()
            .email("intruder@test.com")
            .nickname("intruder")
            .password("pw")
            .build();
        ReflectionTestUtils.setField(intruder, "id", 13L);

        when(userRepository.findByEmail("intruder@test.com")).thenReturn(Optional.of(intruder));
        when(chatService.isChatRoomMemberByPublicId("chat-public-999", 13L)).thenReturn(false);

        assertThatThrownBy(() -> interceptor.preSend(message, messageChannel))
            .isInstanceOf(MessageDeliveryException.class)
            .hasMessageContaining("채팅방");
    }

    private ChannelInterceptor captureInterceptor() {
        webSocketConfig.configureClientInboundChannel(channelRegistration);
        ArgumentCaptor<ChannelInterceptor[]> captor = ArgumentCaptor.forClass(ChannelInterceptor[].class);
        verify(channelRegistration).interceptors(captor.capture());
        return captor.getValue()[0];
    }

    private Message<byte[]> buildSubscribeMessage(String destination, String email) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.SUBSCRIBE);
        accessor.setDestination(destination);
        accessor.setUser(new UsernamePasswordAuthenticationToken(email, null));
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
