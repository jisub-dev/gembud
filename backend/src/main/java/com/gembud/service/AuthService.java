package com.gembud.service;

import com.gembud.config.JwtConfig;
import com.gembud.dto.request.LoginRequest;
import com.gembud.dto.request.RefreshTokenRequest;
import com.gembud.dto.request.SignupRequest;
import com.gembud.dto.response.AuthResponse;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.UserRepository;
import com.gembud.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
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
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenStore refreshTokenStore;
    private final JwtConfig jwtConfig;

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

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name());

        // Store refresh token — invalidates any previous session
        refreshTokenStore.save(user.getEmail(), refreshToken, jwtConfig.getRefreshTokenExpiration());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .email(user.getEmail())
            .nickname(user.getNickname())
            .build();
    }

    /**
     * Authenticates user and returns tokens.
     *
     * @param request login request
     * @return authentication response with tokens
     */
    @Transactional
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                request.getEmail(),
                request.getPassword()
            )
        );

        User user = userRepository.findByEmail(request.getEmail())
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String accessToken = jwtTokenProvider.generateAccessToken(user.getEmail(), user.getRole().name());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getEmail(), user.getRole().name());

        // Store refresh token — invalidates any previous session (single-session enforcement)
        refreshTokenStore.save(user.getEmail(), refreshToken, jwtConfig.getRefreshTokenExpiration());

        return AuthResponse.builder()
            .accessToken(accessToken)
            .refreshToken(refreshToken)
            .email(user.getEmail())
            .nickname(user.getNickname())
            .build();
    }

    /**
     * Refreshes access token using refresh token with rotation.
     *
     * @param request refresh token request
     * @return authentication response with new tokens
     */
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        String email = jwtTokenProvider.getEmailFromToken(refreshToken);

        // Validate against Redis — reject if token was already rotated or user logged out
        String stored = refreshTokenStore.get(email);
        if (stored == null || !stored.equals(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        String newAccessToken = jwtTokenProvider.generateAccessToken(email, user.getRole().name());
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(email, user.getRole().name());

        // Rotate: replace old refresh token with new one
        refreshTokenStore.save(email, newRefreshToken, jwtConfig.getRefreshTokenExpiration());

        return AuthResponse.builder()
            .accessToken(newAccessToken)
            .refreshToken(newRefreshToken)
            .email(user.getEmail())
            .nickname(user.getNickname())
            .build();
    }

    /**
     * Invalidate the stored refresh token for a user (logout).
     *
     * @param email user email
     */
    public void invalidateRefreshToken(String email) {
        refreshTokenStore.delete(email);
    }
}
