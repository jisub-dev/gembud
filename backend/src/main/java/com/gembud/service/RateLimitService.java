package com.gembud.service;

import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Rate limiting service using Redis INCR + TTL pattern.
 *
 * @author Gembud Team
 * @since 2026-03-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.login-lock-threshold:10}")
    private int loginLockThreshold;

    @Value("${app.security.login-lock-window-minutes:10}")
    private int loginLockWindowMinutes;

    /**
     * Check login rate limit by IP and account.
     * Returns the current attempt count (used by caller to decide lockout).
     *
     * @param ip    client IP address
     * @param email target account email
     * @return current attempt count for this account key
     * @throws BusinessException RATE_LIMIT_EXCEEDED if IP limit exceeded
     */
    public long checkLoginLimit(String ip, String email) {
        // IP-level: 10 attempts per minute
        String ipKey = "ratelimit:login:ip:" + ip;
        long ipCount = increment(ipKey, 60, TimeUnit.SECONDS);
        if (ipCount > 10) {
            log.warn("Login rate limit exceeded for IP {}", ip);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }

        // Account-level: loginLockThreshold attempts per 10 minutes
        String accountKey = "ratelimit:login:account:" + email;
        long accountCount = increment(accountKey, loginLockWindowMinutes, TimeUnit.MINUTES);
        return accountCount;
    }

    /**
     * Check refresh token rate limit by IP.
     *
     * @param ip client IP address
     * @throws BusinessException RATE_LIMIT_EXCEEDED if limit exceeded
     */
    public void checkRefreshLimit(String ip) {
        String key = "ratelimit:refresh:ip:" + ip;
        long count = increment(key, 60, TimeUnit.SECONDS);
        if (count > 20) {
            log.warn("Refresh rate limit exceeded for IP {}", ip);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * Check WebSocket connect rate limit by IP.
     *
     * @param ip client IP address
     * @throws BusinessException RATE_LIMIT_EXCEEDED if limit exceeded
     */
    public void checkWsLimit(String ip) {
        String key = "ratelimit:ws:ip:" + ip;
        long count = increment(key, 60, TimeUnit.SECONDS);
        if (count > 30) {
            log.warn("WebSocket connect rate limit exceeded for IP {}", ip);
            throw new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED);
        }
    }

    /**
     * Reset login attempt counter for an account (called after successful login).
     *
     * @param email account email
     */
    public void resetLoginCount(String email) {
        redisTemplate.delete("ratelimit:login:account:" + email);
    }

    /**
     * Get current failed login count in active window for an account.
     */
    public long getLoginFailedCount(String email) {
        String key = "ratelimit:login:account:" + email;
        String raw = redisTemplate.opsForValue().get(key);
        if (raw == null) {
            return 0L;
        }
        try {
            return Long.parseLong(raw);
        } catch (NumberFormatException ignored) {
            return 0L;
        }
    }

    public int getLoginWindowMinutes() {
        return loginLockWindowMinutes;
    }

    /**
     * Increment Redis counter with TTL set only on first increment.
     *
     * @param key      Redis key
     * @param ttl      time-to-live value
     * @param timeUnit time unit for TTL
     * @return new counter value
     */
    private long increment(String key, long ttl, TimeUnit timeUnit) {
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, ttl, timeUnit);
        }
        return count != null ? count : 1L;
    }
}
