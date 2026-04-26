package com.gembud.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SlackAlertServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOps;

    private SlackAlertService slackAlertService;

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        slackAlertService = new SlackAlertService(redisTemplate);
        ReflectionTestUtils.setField(slackAlertService, "webhookUrl", "");
    }

    @Test
    @DisplayName("sendAlert - webhookUrl이 비어 있으면 Redis 조회 없이 조용히 종료한다")
    void sendAlert_noWebhookUrl_skips() {
        slackAlertService.sendAlert("LOGIN_FAIL", "HIGH", 1L, "127.0.0.1", "/api/auth/login", "dedupe-key");

        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    @DisplayName("sendAlert - 이미 같은 dedupeKey가 있으면 전송하지 않는다")
    void sendAlert_duplicate_skips() {
        ReflectionTestUtils.setField(slackAlertService, "webhookUrl", "https://hooks.slack.com/test");

        // setIfAbsent returns false → duplicate
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
            .thenReturn(false);

        slackAlertService.sendAlert("LOGIN_FAIL", "HIGH", 1L, "127.0.0.1", "/api/auth/login", "dedupe-key");

        // Only checked dedup — no HTTP call attempted beyond what mocks allow
        verify(valueOps).setIfAbsent(eq("ratelimit:slack:dedupe-key"), eq("1"), eq(10L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("processRetries - 빈 큐에서는 아무 작업도 하지 않는다")
    void processRetries_emptyQueue_noOp() {
        slackAlertService.processRetries();
        // No exception, no interaction
        verify(redisTemplate, never()).opsForValue();
    }
}
