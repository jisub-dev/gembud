package com.gembud.service;

import com.gembud.entity.SecurityEvent;
import com.gembud.entity.SecurityEvent.EventType;
import com.gembud.dto.response.SecurityEventSummaryResponse;
import com.gembud.repository.SecurityEventRepository;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for recording and managing security audit events.
 *
 * @author Gembud Team
 * @since 2026-03-05
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityEventService {

    private final SecurityEventRepository securityEventRepository;
    private final SlackAlertService slackAlertService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.security.security-event-retention-days:90}")
    private int retentionDays;

    @Value("${app.security.login-fail-burst-high-threshold:10}")
    private int loginFailBurstHighThreshold;

    @Value("${app.security.login-fail-burst-critical-threshold:30}")
    private int loginFailBurstCriticalThreshold;

    /**
     * Record a security event asynchronously.
     *
     * @param eventType  type of event
     * @param userId     affected user ID (nullable)
     * @param ip         client IP
     * @param userAgent  client user-agent
     * @param endpoint   request endpoint
     * @param result     outcome (SUCCESS / FAIL / BLOCKED)
     * @param riskScore  risk level (LOW / MEDIUM / HIGH / CRITICAL)
     */
    @Async
    @Transactional
    public void record(EventType eventType, Long userId, String ip,
                       String userAgent, String endpoint, String result, String riskScore) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                .eventType(eventType)
                .userId(userId)
                .ip(ip)
                .userAgent(userAgent != null && userAgent.length() > 500
                    ? userAgent.substring(0, 500) : userAgent)
                .endpoint(endpoint)
                .result(result)
                .riskScore(riskScore)
                .build();

            securityEventRepository.save(event);

            if (eventType == EventType.LOGIN_FAIL) {
                detectAndRecordLoginFailBurst(ip, userId, endpoint);
            }

            // Notify via Slack for HIGH/CRITICAL events
            if ("HIGH".equals(riskScore) || "CRITICAL".equals(riskScore)) {
                String key = eventType.name() + ":" + (ip != null ? ip : "unknown");
                slackAlertService.sendAlert(eventType.name(), riskScore, userId, ip, endpoint, key);
            }
        } catch (Exception e) {
            log.warn("Failed to record security event {}: {}", eventType, e.getMessage());
        }
    }

    @Transactional(readOnly = true)
    public Page<SecurityEvent> search(
        EventType eventType,
        String riskScore,
        LocalDateTime from,
        LocalDateTime to,
        Pageable pageable
    ) {
        return securityEventRepository.search(eventType, riskScore, from, to, pageable);
    }

    @Transactional(readOnly = true)
    public SecurityEventSummaryResponse summary(int windowMinutes) {
        LocalDateTime after = LocalDateTime.now().minusMinutes(windowMinutes);
        return SecurityEventSummaryResponse.builder()
            .loginFailCount(securityEventRepository.countByEventTypeAndCreatedAtAfter(
                EventType.LOGIN_FAIL, after))
            .loginLockedCount(securityEventRepository.countByEventTypeAndCreatedAtAfter(
                EventType.LOGIN_LOCKED, after))
            .refreshReuseCount(securityEventRepository.countByEventTypeAndCreatedAtAfter(
                EventType.REFRESH_REUSE_DETECTED, after))
            .rateLimitHitCount(securityEventRepository.countByEventTypeAndCreatedAtAfter(
                EventType.RATE_LIMIT_HIT, after))
            .build();
    }

    /**
     * Delete security events older than retentionDays (runs daily at 03:00).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void purgeOldEvents() {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        securityEventRepository.deleteByCreatedAtBefore(cutoff);
        log.info("Purged security events older than {} days", retentionDays);
    }

    private void detectAndRecordLoginFailBurst(String ip, Long userId, String endpoint) {
        String scope = ip != null ? ip : "unknown";
        long bucket = System.currentTimeMillis() / (5 * 60 * 1000L);
        String key = "security:burst:login_fail:" + scope + ":" + bucket;

        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1L) {
            redisTemplate.expire(key, 10, TimeUnit.MINUTES);
        }
        long current = count != null ? count : 1L;

        String burstRisk = null;
        if (current >= loginFailBurstCriticalThreshold) {
            burstRisk = "CRITICAL";
        } else if (current >= loginFailBurstHighThreshold) {
            burstRisk = "HIGH";
        }

        if (burstRisk == null) {
            return;
        }

        SecurityEvent burstEvent = SecurityEvent.builder()
            .eventType(EventType.LOGIN_FAIL_BURST)
            .userId(userId)
            .ip(ip)
            .endpoint(endpoint)
            .result("BLOCKED")
            .riskScore(burstRisk)
            .detail("{\"count\":" + current + ",\"windowMinutes\":5}")
            .build();
        securityEventRepository.save(burstEvent);

        String dedupeKey = "LOGIN_FAIL_BURST:" + scope + ":" + bucket;
        slackAlertService.sendAlert(
            EventType.LOGIN_FAIL_BURST.name(),
            burstRisk,
            userId,
            ip,
            endpoint,
            dedupeKey
        );
    }
}
