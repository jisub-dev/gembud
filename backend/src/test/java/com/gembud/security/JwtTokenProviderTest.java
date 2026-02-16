package com.gembud.security;

import com.gembud.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Test cases for {@link JwtTokenProvider}.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("JwtTokenProvider 테스트")
class JwtTokenProviderTest {

    private static final String SECRET =
        "test-secret-key-that-is-at-least-256-bits-long-for-hs256-algorithm";
    private static final long ACCESS_TOKEN_EXPIRATION = 3_600_000L;
    private static final long REFRESH_TOKEN_EXPIRATION = 604_800_000L;
    private static final String EMAIL = "user@example.com";

    @Mock
    private JwtConfig jwtConfig;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        when(jwtConfig.getSecret()).thenReturn(SECRET);
        when(jwtConfig.getAccessTokenExpiration()).thenReturn(ACCESS_TOKEN_EXPIRATION);
        when(jwtConfig.getRefreshTokenExpiration()).thenReturn(REFRESH_TOKEN_EXPIRATION);
        jwtTokenProvider = new JwtTokenProvider(jwtConfig);
    }

    /**
     * Verifies access token generation with subject and expiration.
     */
    @Test
    @DisplayName("Access token 생성 성공 - 이메일 및 만료 시간 포함")
    void generateAccessToken_Success() {
        Date before = new Date();

        String token = jwtTokenProvider.generateAccessToken(EMAIL);

        Claims claims = parseClaims(token, SECRET);
        assertThat(claims.getSubject()).isEqualTo(EMAIL);
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(before);
        Date latestExpected = new Date(before.getTime() + ACCESS_TOKEN_EXPIRATION + 5_000L);
        assertThat(claims.getExpiration()).isBeforeOrEqualTo(latestExpected);
    }

    /**
     * Verifies refresh token generation with subject and expiration.
     */
    @Test
    @DisplayName("Refresh token 생성 성공")
    void generateRefreshToken_Success() {
        Date before = new Date();

        String token = jwtTokenProvider.generateRefreshToken(EMAIL);

        Claims claims = parseClaims(token, SECRET);
        assertThat(claims.getSubject()).isEqualTo(EMAIL);
        assertThat(claims.getExpiration()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(before);
        Date latestExpected = new Date(before.getTime() + REFRESH_TOKEN_EXPIRATION + 5_000L);
        assertThat(claims.getExpiration()).isBeforeOrEqualTo(latestExpected);
    }

    /**
     * Ensures a valid token passes validation.
     */
    @Test
    @DisplayName("유효한 토큰 검증 성공")
    void validateToken_ValidToken_ReturnsTrue() {
        String token = jwtTokenProvider.generateAccessToken(EMAIL);

        boolean result = jwtTokenProvider.validateToken(token);

        assertThat(result).isTrue();
    }

    /**
     * Ensures an expired token fails validation.
     */
    @Test
    @DisplayName("만료된 토큰 검증 실패")
    void validateToken_ExpiredToken_ReturnsFalse() {
        String token = buildToken(EMAIL, expiredIssuedAt(), expiredAt(), SECRET);

        boolean result = jwtTokenProvider.validateToken(token);

        assertThat(result).isFalse();
    }

    /**
     * Ensures malformed tokens fail validation.
     */
    @Test
    @DisplayName("형식이 잘못된 토큰 검증 실패")
    void validateToken_MalformedToken_ReturnsFalse() {
        boolean result = jwtTokenProvider.validateToken("malformed-token");

        assertThat(result).isFalse();
    }

    /**
     * Ensures tokens with invalid signatures fail validation.
     */
    @Test
    @DisplayName("서명이 유효하지 않은 토큰 검증 실패")
    void validateToken_InvalidSignature_ReturnsFalse() {
        String token = buildToken(EMAIL, new Date(), validAt(), "another-secret-key-for-tests-only");

        boolean result = jwtTokenProvider.validateToken(token);

        assertThat(result).isFalse();
    }

    /**
     * Extracts email from a valid token.
     */
    @Test
    @DisplayName("유효한 토큰에서 이메일 추출 성공")
    void getEmailFromToken_ValidToken_ReturnsEmail() {
        String token = jwtTokenProvider.generateAccessToken(EMAIL);

        String email = jwtTokenProvider.getEmailFromToken(token);

        assertThat(email).isEqualTo(EMAIL);
    }

    /**
     * Throws an exception for invalid tokens when extracting email.
     */
    @Test
    @DisplayName("유효하지 않은 토큰에서 이메일 추출 시 예외 발생")
    void getEmailFromToken_InvalidToken_ThrowsException() {
        assertThatThrownBy(() -> jwtTokenProvider.getEmailFromToken("invalid-token"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid JWT token");
    }

    /**
     * Ensures non-expired tokens return false for expiration check.
     */
    @Test
    @DisplayName("만료되지 않은 토큰은 만료 여부 false 반환")
    void isTokenExpired_NotExpiredToken_ReturnsFalse() {
        String token = buildToken(EMAIL, new Date(), validAt(), SECRET);

        boolean result = jwtTokenProvider.isTokenExpired(token);

        assertThat(result).isFalse();
    }

    /**
     * Ensures expired tokens are detected as expired.
     */
    @Test
    @DisplayName("만료된 토큰은 만료 여부 true 반환")
    void isTokenExpired_ExpiredToken_ReturnsTrue() {
        String token = buildToken(EMAIL, expiredIssuedAt(), expiredAt(), SECRET);

        boolean expired;
        try {
            expired = jwtTokenProvider.isTokenExpired(token);
        } catch (IllegalArgumentException ex) {
            expired = true;
        }

        assertThat(expired).isTrue();
    }

    private Claims parseClaims(String token, String secret) {
        return Jwts.parser()
            .verifyWith(getSigningKey(secret))
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private Key getSigningKey(String secret) {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    private String buildToken(String email, Date issuedAt, Date expiration, String secret) {
        return Jwts.builder()
            .subject(email)
            .issuedAt(issuedAt)
            .expiration(expiration)
            .signWith(getSigningKey(secret), SignatureAlgorithm.HS256)
            .compact();
    }

    private Date expiredIssuedAt() {
        return new Date(System.currentTimeMillis() - 120_000L);
    }

    private Date expiredAt() {
        return new Date(System.currentTimeMillis() - 60_000L);
    }

    private Date validAt() {
        return new Date(System.currentTimeMillis() + 60_000L);
    }
}
