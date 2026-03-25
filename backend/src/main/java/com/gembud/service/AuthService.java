package com.gembud.service;

import com.gembud.dto.request.LoginRequest;
import com.gembud.dto.request.RefreshTokenRequest;
import com.gembud.dto.request.SignupRequest;
import com.gembud.dto.response.AuthResponse;
import com.gembud.entity.SecurityEvent.EventType;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import com.gembud.security.JwtTokenProvider;
import com.gembud.websocket.WebSocketSessionRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for handling user authentication.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenStore refreshTokenStore;
    private final AuthSessionService authSessionService;
    private final RateLimitService rateLimitService;
    private final SecurityEventService securityEventService;
    private final WebSocketSessionRegistry webSocketSessionRegistry;

    @org.springframework.beans.factory.annotation.Value("${app.security.login-lock-threshold:10}")
    private int loginLockThreshold;

    @org.springframework.beans.factory.annotation.Value("${app.security.login-lock-duration-minutes:10}")
    private int loginLockDurationMinutes;

    /**
     * Registers a new user.
     *
     * @param request signup request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthResponse signup(SignupRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        if (userRepository.existsByNickname(request.getNickname())) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }

        User user = User.builder()
            .email(request.getEmail())
            .password(passwordEncoder.encode(request.getPassword()))
            .nickname(request.getNickname())
            .ageRange(request.getAgeRange())
            .build();

        userRepository.save(user);

        return issueTokens(user);
    }

    /**
     * Authenticates user and returns tokens.
     *
     * @param request login request
     * @param ip      client IP (for rate limiting)
     * @return authentication response with tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ip) {
        // Check account lock first (no rate-limit increment needed)
        User preCheckUser = userRepository.findByEmail(request.getEmail()).orElse(null);
        if (preCheckUser != null && preCheckUser.isLoginLocked()) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED);
        }

        // Rate limit check (may throw RATE_LIMIT_EXCEEDED)
        long attemptCount = rateLimitService.checkLoginLimit(ip, request.getEmail());

        try {
            Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                    request.getEmail(),
                    request.getPassword()
                )
            );

            User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // Reset failed attempt counter on success
            rateLimitService.resetLoginCount(request.getEmail());

            AuthResponse authResponse = issueTokens(user);

            // Force-close any existing WebSocket connections for this user
            webSocketSessionRegistry.closeUserSessions(user.getEmail());

            securityEventService.record(EventType.LOGIN_SUCCESS, user.getId(), ip,
                null, "/auth/login", "SUCCESS", "LOW");

            return authResponse;

        } catch (org.springframework.security.core.AuthenticationException e) {
            // Find user for audit (may not exist)
            User failedUser = userRepository.findByEmail(request.getEmail()).orElse(null);
            Long failedUserId = failedUser != null ? failedUser.getId() : null;

            // Lock account if threshold reached
            if (attemptCount >= loginLockThreshold) {
                if (failedUser != null && !failedUser.isLoginLocked()) {
                    failedUser.lock(loginLockDurationMinutes);
                    userRepository.save(failedUser);
                    log.warn("Account locked for {} after {} failed attempts",
                        maskEmail(request.getEmail()), attemptCount);
                    securityEventService.record(EventType.LOGIN_LOCKED, failedUserId, ip,
                        null, "/auth/login", "BLOCKED", "HIGH");
                }
            } else {
                securityEventService.record(EventType.LOGIN_FAIL, failedUserId, ip,
                    null, "/auth/login", "FAIL", "MEDIUM");
            }
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    /**
     * Authenticates user (legacy overload without IP for backward compat).
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        return login(request, "unknown");
    }

    /**
     * Refreshes access token using refresh token with rotation.
     *
     * @param request refresh token request
     * @return authentication response with new tokens
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        return refreshToken(request, "unknown");
    }

    /**
     * Refreshes access token using refresh token with rotation.
     *
     * @param request refresh token request
     * @param ip      client IP for audit logging
     * @return authentication response with new tokens
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request, String ip) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            securityEventService.record(EventType.REFRESH_FAIL, null, ip,
                null, "/auth/refresh", "FAIL", "MEDIUM");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // Validate against Redis — reject if token was already rotated or user logged out
        String stored = refreshTokenStore.get(email);
        if (stored == null || !stored.equals(refreshToken)) {
            // Token reuse detected (stored is different — another session already rotated it)
            User reuseUser = userRepository.findByEmail(email).orElse(null);
            Long reuseUserId = reuseUser != null ? reuseUser.getId() : null;
            securityEventService.record(EventType.REFRESH_REUSE_DETECTED, reuseUserId, ip,
                null, "/auth/refresh", "BLOCKED", "HIGH");
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        AuthResponse authResponse = issueTokens(user);

        securityEventService.record(EventType.REFRESH_SUCCESS, user.getId(), ip,
            null, "/auth/refresh", "SUCCESS", "LOW");

        return authResponse;
    }

    /**
     * Invalidate the stored refresh token for a user (logout).
     *
     * @param email user email
     */
    public void invalidateRefreshToken(String email) {
        refreshTokenStore.delete(email);
        refreshTokenStore.deleteSession(email);
    }

    /**
     * Issues access/refresh tokens and stores the single-session contract in Redis.
     * Shared by email login/signup, refresh rotation, and OAuth2 success handling.
     *
     * @param user authenticated user
     * @return auth response containing the newly issued tokens
     */
    public AuthResponse issueTokens(User user) {
        return authSessionService.issueTokens(user);
    }

    private String maskEmail(String email) {
        if (email == null || email.isBlank()) {
            return "unknown";
        }
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) {
            return "***";
        }
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
