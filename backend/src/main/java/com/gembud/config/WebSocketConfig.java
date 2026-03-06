package com.gembud.config;

import com.gembud.entity.SecurityEvent.EventType;
import com.gembud.exception.BusinessException;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.RateLimitService;
import com.gembud.service.SecurityEventService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket configuration for STOMP messaging.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Value("${app.websocket.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;

    private final JwtTokenProvider jwtTokenProvider;
    private final RateLimitService rateLimitService;
    private final SecurityEventService securityEventService;

    /**
     * Configure message broker for pub/sub messaging.
     *
     * @param registry message broker registry
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // Enable simple broker for topics and queues
        registry.enableSimpleBroker("/topic", "/queue");

        // Set application destination prefix
        registry.setApplicationDestinationPrefixes("/app");

        // Set user destination prefix
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Register STOMP endpoints.
     *
     * @param registry STOMP endpoint registry
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns(allowedOrigins.split(","))
            .withSockJS();
    }

    /**
     * Configure inbound channel interceptor for CONNECT JWT validation and rate limiting.
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                    message, StompHeaderAccessor.class);

                if (accessor == null || !StompCommand.CONNECT.equals(accessor.getCommand())) {
                    return message;
                }

                // Extract IP from STOMP native headers (set by SockJS transport)
                String ip = "unknown";
                List<String> ipHeaders = accessor.getNativeHeader("X-Forwarded-For");
                if (ipHeaders != null && !ipHeaders.isEmpty()) {
                    ip = ipHeaders.get(0).split(",")[0].trim();
                }

                // Rate limit check
                final String finalIp = ip;
                try {
                    rateLimitService.checkWsLimit(finalIp);
                } catch (BusinessException e) {
                    log.warn("WebSocket CONNECT rate limit exceeded for IP {}", finalIp);
                    securityEventService.record(EventType.WS_CONNECT_DENIED, null, finalIp,
                        null, "/ws", "BLOCKED", "HIGH");
                    throw new org.springframework.messaging.MessageDeliveryException(
                        message, "Rate limit exceeded");
                }

                // JWT validation
                String authHeader = accessor.getFirstNativeHeader("Authorization");
                if (authHeader != null && authHeader.startsWith("Bearer ")) {
                    String token = authHeader.substring(7);
                    if (jwtTokenProvider.validateToken(token)) {
                        String email = jwtTokenProvider.getEmailFromToken(token);
                        String role = jwtTokenProvider.getRoleFromToken(token);
                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            email, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + role))
                        );
                        accessor.setUser(auth);
                    } else {
                        log.warn("WebSocket CONNECT rejected: invalid JWT from IP {}", finalIp);
                        securityEventService.record(EventType.WS_CONNECT_DENIED, null, finalIp,
                            null, "/ws", "BLOCKED", "MEDIUM");
                        throw new org.springframework.messaging.MessageDeliveryException(
                            message, "Invalid JWT token");
                    }
                }
                // If no auth header, allow connection (auth is validated per-message via Principal)

                return message;
            }
        });
    }
}
