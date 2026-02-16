package com.gembud.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * JWT configuration properties.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Configuration
@Getter
public class JwtConfig {

    /**
     * Secret key for signing JWTs.
     */
    @Value("${jwt.secret}")
    private String secret;

    /**
     * Access token expiration time in milliseconds.
     */
    @Value("${jwt.access-token-expiration}")
    private Long accessTokenExpiration;

    /**
     * Refresh token expiration time in milliseconds.
     */
    @Value("${jwt.refresh-token-expiration}")
    private Long refreshTokenExpiration;
}
