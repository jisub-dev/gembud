package com.gembud.security;

import com.gembud.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT token provider for access and refresh tokens.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Component
public class JwtTokenProvider {

    private final JwtConfig jwtConfig;

    /**
     * Creates a JwtTokenProvider with configuration values.
     *
     * @param jwtConfig JWT configuration properties
     */
    public JwtTokenProvider(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Generates an access token for the given email and role (Phase 12).
     *
     * @param email user email to store as subject
     * @param role user role to store in claims
     * @return signed access token
     */
    public String generateAccessToken(String email, String role) {
        return generateToken(email, role, jwtConfig.getAccessTokenExpiration());
    }

    /**
     * Generates a refresh token for the given email and role (Phase 12).
     *
     * @param email user email to store as subject
     * @param role user role to store in claims
     * @return signed refresh token
     */
    public String generateRefreshToken(String email, String role) {
        return generateToken(email, role, jwtConfig.getRefreshTokenExpiration());
    }

    /**
     * Validates the given token.
     *
     * @param token JWT to validate
     * @return true when valid, false otherwise
     */
    public boolean validateToken(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (ExpiredJwtException
                 | UnsupportedJwtException
                 | MalformedJwtException
                 | SecurityException
                 | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * Extracts the email (subject) from the token.
     *
     * @param token JWT to parse
     * @return email stored in the token subject
     */
    public String getEmailFromToken(String token) {
        try {
            return parseClaims(token).getSubject();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    /**
     * Extracts the role from the token (Phase 12).
     *
     * @param token JWT to parse
     * @return role stored in the token claims
     */
    public String getRoleFromToken(String token) {
        try {
            return parseClaims(token).get("role", String.class);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    /**
     * Checks whether the token has expired.
     *
     * @param token JWT to check
     * @return true if expired, false otherwise
     */
    public boolean isTokenExpired(String token) {
        try {
            Date expiration = parseClaims(token).getExpiration();
            return expiration.before(new Date());
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid JWT token", ex);
        }
    }

    private String generateToken(String email, String role, Long expirationMillis) {
        Date now = new Date();
        Date expiration = new Date(now.getTime() + expirationMillis);
        return Jwts.builder()
            .subject(email)
            .claim("role", role)
            .issuedAt(now)
            .expiration(expiration)
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
            .verifyWith(getSigningKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
