package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.config.JwtConfig;
import com.gembud.dto.request.LoginRequest;
import com.gembud.dto.request.RefreshTokenRequest;
import com.gembud.dto.response.AuthResponse;
import com.gembud.entity.SecurityEvent.EventType;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import com.gembud.security.JwtTokenProvider;
import com.gembud.websocket.WebSocketSessionRegistry;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for AuthService.
 *
 * @author Gembud Team
 * @since 2026-03-06
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtTokenProvider jwtTokenProvider;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private RefreshTokenStore refreshTokenStore;
    @Mock private JwtConfig jwtConfig;
    @Mock private RateLimitService rateLimitService;
    @Mock private SecurityEventService securityEventService;
    @Mock private WebSocketSessionRegistry webSocketSessionRegistry;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .email("test@example.com")
            .password("encodedPw")
            .nickname("tester")
            .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        ReflectionTestUtils.setField(authService, "loginLockThreshold", 10);
        ReflectionTestUtils.setField(authService, "loginLockDurationMinutes", 10);

        when(jwtConfig.getAccessTokenExpiration()).thenReturn(3600000L);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(604800000L);
        when(jwtTokenProvider.generateAccessToken(anyString(), anyString(), anyString()))
            .thenReturn("access-token");
        when(jwtTokenProvider.generateRefreshToken(anyString(), anyString()))
            .thenReturn("refresh-token");
    }

    @Test
    @DisplayName("login_Success: 정상 로그인 시 토큰 반환 및 이벤트 기록")
    void login_Success() {
        LoginRequest req = new LoginRequest("test@example.com", "password");
        when(rateLimitService.checkLoginLimit(anyString(), anyString())).thenReturn(1L);
        when(authenticationManager.authenticate(any()))
            .thenReturn(new UsernamePasswordAuthenticationToken("test@example.com", null));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse result = authService.login(req, "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        verify(rateLimitService).resetLoginCount("test@example.com");
        verify(securityEventService).record(eq(EventType.LOGIN_SUCCESS), eq(1L),
            anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("login_AccountLocked: 잠긴 계정 로그인 시 ACCOUNT_LOCKED 예외")
    void login_AccountLocked() {
        LoginRequest req = new LoginRequest("test@example.com", "password");
        User lockedUser = User.builder()
            .email("test@example.com")
            .password("encodedPw")
            .nickname("tester")
            .build();
        ReflectionTestUtils.setField(lockedUser, "id", 1L);
        lockedUser.lock(10);  // lock the account

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(lockedUser));

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("locked");
    }

    @Test
    @DisplayName("login_RateLimitExceeded: IP 레이트리밋 초과 시 예외 전파")
    void login_RateLimitExceeded() {
        LoginRequest req = new LoginRequest("test@example.com", "password");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
        when(rateLimitService.checkLoginLimit(anyString(), anyString()))
            .thenThrow(new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED));

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("login_WrongPassword_ThresholdReached: 10회 실패 시 계정 잠금 + LOGIN_LOCKED 이벤트")
    void login_WrongPassword_ThresholdReached() {
        LoginRequest req = new LoginRequest("test@example.com", "wrong");
        when(rateLimitService.checkLoginLimit(anyString(), anyString())).thenReturn(10L);
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad credentials"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
            .isInstanceOf(BusinessException.class);

        verify(securityEventService).record(eq(EventType.LOGIN_LOCKED), eq(1L),
            anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("login_WrongPassword_BelowThreshold: 임계치 미만 실패 시 lock 미호출, LOGIN_FAIL 이벤트")
    void login_WrongPassword_BelowThreshold() {
        LoginRequest req = new LoginRequest("test@example.com", "wrong");
        when(rateLimitService.checkLoginLimit(anyString(), anyString())).thenReturn(3L);
        when(authenticationManager.authenticate(any()))
            .thenThrow(new BadCredentialsException("bad credentials"));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.login(req, "127.0.0.1"))
            .isInstanceOf(BusinessException.class);

        verify(securityEventService).record(eq(EventType.LOGIN_FAIL), eq(1L),
            anyString(), any(), anyString(), anyString(), anyString());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("refreshToken_Success: 정상 토큰 갱신")
    void refreshToken_Success() {
        String oldToken = "old-refresh";
        RefreshTokenRequest req = new RefreshTokenRequest(oldToken);

        when(jwtTokenProvider.validateToken(oldToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(oldToken)).thenReturn("test@example.com");
        when(refreshTokenStore.get("test@example.com")).thenReturn(oldToken);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        AuthResponse result = authService.refreshToken(req, "127.0.0.1");

        assertThat(result.getAccessToken()).isEqualTo("access-token");
        verify(refreshTokenStore).save(eq("test@example.com"), anyString(), anyLong());
        verify(refreshTokenStore).saveSession(eq("test@example.com"), anyString(), anyLong());
    }

    @Test
    @DisplayName("refreshToken_InvalidToken: 유효하지 않은 토큰 → INVALID_REFRESH_TOKEN 예외")
    void refreshToken_InvalidToken() {
        RefreshTokenRequest req = new RefreshTokenRequest("invalid-token");
        when(jwtTokenProvider.validateToken("invalid-token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1"))
            .isInstanceOf(BusinessException.class);

        verify(securityEventService).record(eq(EventType.REFRESH_FAIL), any(),
            anyString(), any(), anyString(), anyString(), anyString());
    }

    @Test
    @DisplayName("refreshToken_ReuseDetected: stored != token → REFRESH_REUSE_DETECTED 이벤트")
    void refreshToken_ReuseDetected() {
        String staleToken = "stale-token";
        RefreshTokenRequest req = new RefreshTokenRequest(staleToken);

        when(jwtTokenProvider.validateToken(staleToken)).thenReturn(true);
        when(jwtTokenProvider.getEmailFromToken(staleToken)).thenReturn("test@example.com");
        when(refreshTokenStore.get("test@example.com")).thenReturn("different-token");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThatThrownBy(() -> authService.refreshToken(req, "127.0.0.1"))
            .isInstanceOf(BusinessException.class);

        verify(securityEventService).record(eq(EventType.REFRESH_REUSE_DETECTED), any(),
            anyString(), any(), anyString(), anyString(), anyString());
    }
}
