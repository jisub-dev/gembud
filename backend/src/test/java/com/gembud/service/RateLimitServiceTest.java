package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for RateLimitService.
 *
 * @author Gembud Team
 * @since 2026-03-06
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RateLimitService rateLimitService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(rateLimitService, "loginLockThreshold", 10);
        ReflectionTestUtils.setField(rateLimitService, "loginLockWindowMinutes", 10);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("checkLoginLimit_BelowIpLimit: IP 카운트 10 이하 정상 반환")
    void checkLoginLimit_BelowIpLimit() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        long result = rateLimitService.checkLoginLimit("1.2.3.4", "user@example.com");

        assertThat(result).isEqualTo(5L);
    }

    @Test
    @DisplayName("checkLoginLimit_IpLimitExceeded: IP 카운트 11 → RATE_LIMIT_EXCEEDED")
    void checkLoginLimit_IpLimitExceeded() {
        when(valueOperations.increment(anyString())).thenReturn(11L);

        assertThatThrownBy(() -> rateLimitService.checkLoginLimit("1.2.3.4", "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("checkLoginLimit_AccountCount_Returned: 계정 카운트 반환값 검증")
    void checkLoginLimit_AccountCount_Returned() {
        // IP key returns 1, account key returns 3
        when(valueOperations.increment(anyString()))
            .thenReturn(1L)   // IP key
            .thenReturn(3L);  // account key

        long result = rateLimitService.checkLoginLimit("1.2.3.4", "user@example.com");

        assertThat(result).isEqualTo(3L);
    }

    @Test
    @DisplayName("checkRefreshLimit_Exceeded: 카운트 21 → RATE_LIMIT_EXCEEDED")
    void checkRefreshLimit_Exceeded() {
        when(valueOperations.increment(anyString())).thenReturn(21L);

        assertThatThrownBy(() -> rateLimitService.checkRefreshLimit("1.2.3.4"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("checkWsLimit_Exceeded: 카운트 31 → RATE_LIMIT_EXCEEDED")
    void checkWsLimit_Exceeded() {
        when(valueOperations.increment(anyString())).thenReturn(31L);

        assertThatThrownBy(() -> rateLimitService.checkWsLimit("1.2.3.4"))
            .isInstanceOf(BusinessException.class)
            .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED));
    }

    @Test
    @DisplayName("resetLoginCount: Redis delete 호출 검증")
    void resetLoginCount() {
        rateLimitService.resetLoginCount("user@example.com");

        verify(redisTemplate).delete("ratelimit:login:account:user@example.com");
    }
}
