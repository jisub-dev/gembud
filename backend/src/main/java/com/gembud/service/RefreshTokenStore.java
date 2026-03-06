package com.gembud.service;

import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis-backed store for refresh tokens.
 * Supports single-session enforcement and token rotation.
 *
 * Key format: "refresh:{email}"
 *
 * @author Gembud Team
 * @since 2026-03-05
 */
@Component
@RequiredArgsConstructor
public class RefreshTokenStore {

    private static final String KEY_PREFIX = "refresh:";
    private static final String SESSION_KEY_PREFIX = "session:";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * Save (or overwrite) refresh token for a user, invalidating any previous session.
     *
     * @param email user email
     * @param token refresh token value
     * @param ttlMillis token TTL in milliseconds
     */
    public void save(String email, String token, long ttlMillis) {
        String key = KEY_PREFIX + email;
        stringRedisTemplate.opsForValue().set(key, token, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Retrieve the stored refresh token for a user.
     *
     * @param email user email
     * @return stored token, or null if not present
     */
    public String get(String email) {
        return stringRedisTemplate.opsForValue().get(KEY_PREFIX + email);
    }

    /**
     * Delete the stored refresh token (e.g., on logout).
     *
     * @param email user email
     */
    public void delete(String email) {
        stringRedisTemplate.delete(KEY_PREFIX + email);
    }

    /**
     * Save sessionId for an active user session.
     *
     * @param email      user email
     * @param sessionId  UUID session identifier embedded in the access token
     * @param ttlMillis  TTL matching the access token expiry
     */
    public void saveSession(String email, String sessionId, long ttlMillis) {
        stringRedisTemplate.opsForValue().set(SESSION_KEY_PREFIX + email, sessionId, ttlMillis, TimeUnit.MILLISECONDS);
    }

    /**
     * Retrieve the current valid sessionId for a user.
     *
     * @param email user email
     * @return stored sessionId, or null if not present
     */
    public String getSession(String email) {
        return stringRedisTemplate.opsForValue().get(SESSION_KEY_PREFIX + email);
    }

    /**
     * Delete the session (e.g., on logout).
     *
     * @param email user email
     */
    public void deleteSession(String email) {
        stringRedisTemplate.delete(SESSION_KEY_PREFIX + email);
    }
}
