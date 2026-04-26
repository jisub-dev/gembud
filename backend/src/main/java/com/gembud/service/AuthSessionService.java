package com.gembud.service;

import com.gembud.config.JwtConfig;
import com.gembud.dto.response.AuthResponse;
import com.gembud.entity.User;
import com.gembud.security.JwtTokenProvider;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Issues access/refresh tokens while persisting the single-session contract in Redis.
 * Shared by email login/signup, refresh rotation, and OAuth2 success handling.
 *
 * @author Gembud Team
 * @since 2026-03-25
 */
@Service
@RequiredArgsConstructor
public class AuthSessionService {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtConfig jwtConfig;

    /**
     * Issue a new access/refresh token pair for a user and overwrite the stored session.
     *
     * @param user authenticated user
     * @return auth response containing the new tokens
     */
    public AuthResponse issueTokens(User user) {
        String sessionId = UUID.randomUUID().toString();
        String accessToken = jwtTokenProvider.generateAccessToken(
            user.getEmail(),
            user.getRole().name(),
            sessionId
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            user.getEmail(),
            user.getRole().name()
        );

        refreshTokenStore.save(user.getEmail(), refreshToken, jwtConfig.getRefreshTokenExpiration());
        refreshTokenStore.saveSession(user.getEmail(), sessionId, jwtConfig.getAccessTokenExpiration());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .email(user.getEmail())
            .nickname(user.getNickname())
            .build();
    }
}
