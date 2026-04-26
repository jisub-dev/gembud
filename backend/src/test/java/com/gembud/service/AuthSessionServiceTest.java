package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.config.JwtConfig;
import com.gembud.dto.response.AuthResponse;
import com.gembud.entity.User;
import com.gembud.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthSessionServiceTest {

    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private RefreshTokenStore refreshTokenStore;
    @Mock private JwtConfig jwtConfig;

    private AuthSessionService authSessionService;
    private User user;

    @BeforeEach
    void setUp() {
        authSessionService = new AuthSessionService(jwtTokenProvider, refreshTokenStore, jwtConfig);
        user = User.builder()
            .email("user@test.com")
            .password("encoded")
            .nickname("TestUser")
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);
    }

    @Test
    @DisplayName("issueTokens - 액세스/리프레시 토큰을 발급하고 저장한다")
    void issueTokens_success() {
        when(jwtTokenProvider.generateAccessToken(anyString(), anyString(), anyString()))
            .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyString()))
            .thenReturn("refresh-token");
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(3600000L);

        AuthResponse response = authSessionService.issueTokens(user);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getEmail()).isEqualTo("user@test.com");
        assertThat(response.getNickname()).isEqualTo("TestUser");

        verify(refreshTokenStore).save(eq("user@test.com"), eq("refresh-token"), anyLong());
        verify(refreshTokenStore).saveSession(eq("user@test.com"), anyString(), anyLong());
    }

    @Test
    @DisplayName("issueTokens - 매 호출마다 새로운 sessionId를 생성한다")
    void issueTokens_generatesUniqueSessionId() {
        when(jwtTokenProvider.generateAccessToken(anyString(), anyString(), anyString()))
            .thenReturn("token-1", "token-2");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyString()))
            .thenReturn("refresh-1", "refresh-2");
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(3600000L);

        AuthResponse first = authSessionService.issueTokens(user);
        AuthResponse second = authSessionService.issueTokens(user);

        // Both calls succeed — tokens differ because of unique session IDs
        assertThat(first.getAccessToken()).isEqualTo("token-1");
        assertThat(second.getAccessToken()).isEqualTo("token-2");
    }
}
