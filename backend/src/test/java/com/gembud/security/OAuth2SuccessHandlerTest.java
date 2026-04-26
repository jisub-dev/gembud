package com.gembud.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.config.JwtConfig;
import com.gembud.entity.User;
import com.gembud.repository.UserRepository;
import com.gembud.service.AuthSessionService;
import com.gembud.service.RefreshTokenStore;
import com.gembud.websocket.WebSocketSessionRegistry;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private JwtConfig jwtConfig;

    @Mock
    private RefreshTokenStore refreshTokenStore;

    @Mock
    private WebSocketSessionRegistry webSocketSessionRegistry;

    private AuthSessionService authSessionService;
    private OAuth2SuccessHandler oAuth2SuccessHandler;
    private User existingOAuthUser;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionService(jwtTokenProvider, refreshTokenStore, jwtConfig);
        oAuth2SuccessHandler = new OAuth2SuccessHandler(
            userRepository,
            authSessionService,
            jwtConfig,
            webSocketSessionRegistry
        );

        ReflectionTestUtils.setField(oAuth2SuccessHandler, "redirectUri", "http://localhost:5173/oauth2/callback");
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "cookieSecure", false);
        ReflectionTestUtils.setField(oAuth2SuccessHandler, "cookieSameSite", "Lax");

        existingOAuthUser = User.builder()
            .email("oauth@test.com")
            .nickname("oauth-user")
            .oauthProvider(User.OAuthProvider.GOOGLE)
            .oauthId("google-sub-123")
            .role(User.UserRole.USER)
            .build();
        ReflectionTestUtils.setField(existingOAuthUser, "id", 1L);

        when(jwtConfig.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtTokenProvider.generateAccessToken(eq("oauth@test.com"), eq("USER"), anyString()))
            .thenReturn("oauth-access-token");
        when(jwtTokenProvider.generateRefreshToken("oauth@test.com", "USER"))
            .thenReturn("oauth-refresh-token");
    }

    @Test
    @DisplayName("onAuthenticationSuccess - should issue single-session tokens, close existing WS sessions, and redirect")
    void onAuthenticationSuccess_ShouldIssueTokensAndRedirect() throws Exception {
        OAuth2User oAuth2User = new DefaultOAuth2User(
            List.of(new SimpleGrantedAuthority("ROLE_USER")),
            Map.of(
                "sub", "google-sub-123",
                "email", "oauth@test.com",
                "name", "OAuth User"
            ),
            "sub"
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(userRepository.findByOauthProviderAndOauthId(User.OAuthProvider.GOOGLE, "google-sub-123"))
            .thenReturn(Optional.of(existingOAuthUser));

        oAuth2SuccessHandler.onAuthenticationSuccess(
            request,
            response,
            new UsernamePasswordAuthenticationToken(oAuth2User, null, oAuth2User.getAuthorities())
        );

        verify(refreshTokenStore).save("oauth@test.com", "oauth-refresh-token", 604800000L);
        verify(refreshTokenStore).saveSession(eq("oauth@test.com"), anyString(), eq(3600000L));
        verify(webSocketSessionRegistry).closeUserSessions("oauth@test.com");

        assertThat(response.getRedirectedUrl()).isEqualTo("http://localhost:5173/oauth2/callback?success=true");
        assertThat(response.getHeaders("Set-Cookie"))
            .anySatisfy(header -> assertThat(header).contains("accessToken=oauth-access-token"))
            .anySatisfy(header -> assertThat(header).contains("refreshToken=oauth-refresh-token"));
    }
}
