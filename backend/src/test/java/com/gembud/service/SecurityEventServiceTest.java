package com.gembud.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.SecurityEvent;
import com.gembud.entity.SecurityEvent.EventType;
import com.gembud.repository.SecurityEventRepository;
import java.lang.reflect.Method;
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
import org.springframework.scheduling.annotation.Async;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for SecurityEventService.
 *
 * @author Gembud Team
 * @since 2026-03-06
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SecurityEventServiceTest {

    @Mock private SecurityEventRepository securityEventRepository;
    @Mock private SlackAlertService slackAlertService;
    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private SecurityEventService securityEventService;

    @Test
    @DisplayName("record_SavesEvent: securityEventRepository.save() 호출 검증")
    void record_SavesEvent() {
        ReflectionTestUtils.setField(securityEventService, "retentionDays", 90);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        securityEventService.record(EventType.LOGIN_SUCCESS, 1L, "127.0.0.1",
            null, "/auth/login", "SUCCESS", "LOW");

        verify(securityEventRepository).save(any(SecurityEvent.class));
    }

    @Test
    @DisplayName("record_HighRiskScore_CallsSlack: HIGH 이벤트 → slackAlertService.sendAlert() 호출")
    void record_HighRiskScore_CallsSlack() {
        ReflectionTestUtils.setField(securityEventService, "retentionDays", 90);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        securityEventService.record(EventType.LOGIN_LOCKED, 1L, "127.0.0.1",
            null, "/auth/login", "BLOCKED", "HIGH");

        verify(slackAlertService).sendAlert(anyString(), anyString(), any(), anyString(),
            anyString(), anyString());
    }

    @Test
    @DisplayName("record_LowRiskScore_NoSlack: LOW 이벤트 → Slack 미호출")
    void record_LowRiskScore_NoSlack() {
        ReflectionTestUtils.setField(securityEventService, "retentionDays", 90);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        securityEventService.record(EventType.LOGIN_SUCCESS, 1L, "127.0.0.1",
            null, "/auth/login", "SUCCESS", "LOW");

        verify(slackAlertService, never()).sendAlert(anyString(), anyString(), any(), anyString(),
            anyString(), anyString());
    }

    @Test
    @DisplayName("purgeOldEvents_CallsDeleteBefore: 스케줄러 메서드 직접 호출 검증")
    void purgeOldEvents_CallsDeleteBefore() {
        ReflectionTestUtils.setField(securityEventService, "retentionDays", 90);

        securityEventService.purgeOldEvents();

        verify(securityEventRepository).deleteByCreatedAtBefore(any());
    }

    @Test
    @DisplayName("record_HasAsyncAnnotation: record 메서드는 @Async로 선언된다")
    void record_HasAsyncAnnotation() throws Exception {
        Method method = SecurityEventService.class.getMethod(
            "record",
            EventType.class, Long.class, String.class, String.class, String.class, String.class, String.class
        );

        org.assertj.core.api.Assertions.assertThat(method.isAnnotationPresent(Async.class)).isTrue();
    }

    @Test
    @DisplayName("record_LoginFailBurstHigh_CallsSlackWithBurstEvent")
    void record_LoginFailBurstHigh_CallsSlackWithBurstEvent() {
        ReflectionTestUtils.setField(securityEventService, "loginFailBurstHighThreshold", 2);
        ReflectionTestUtils.setField(securityEventService, "loginFailBurstCriticalThreshold", 30);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.increment(anyString())).thenReturn(2L);

        securityEventService.record(
            EventType.LOGIN_FAIL,
            1L,
            "127.0.0.1",
            null,
            "/auth/login",
            "FAIL",
            "LOW"
        );

        verify(slackAlertService).sendAlert(
            eq("LOGIN_FAIL_BURST"),
            eq("HIGH"),
            eq(1L),
            eq("127.0.0.1"),
            eq("/auth/login"),
            anyString()
        );
    }
}
