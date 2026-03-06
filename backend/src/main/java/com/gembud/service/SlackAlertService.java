package com.gembud.service;

import java.time.LocalDateTime;
import java.util.PriorityQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Slack webhook alert service with Redis-based deduplication.
 *
 * @author Gembud Team
 * @since 2026-03-05
 */
@Service
@Slf4j
public class SlackAlertService {

    @Value("${app.security.slack-webhook-url:}")
    private String webhookUrl;

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;
    private final PriorityQueue<RetryTask> retryQueue = new PriorityQueue<>();
    private final AtomicLong sentCount = new AtomicLong(0);
    private final AtomicLong failedCount = new AtomicLong(0);
    private final AtomicLong scheduledRetryCount = new AtomicLong(0);

    public SlackAlertService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
    }

    /**
     * Send a Slack alert for a security event with 10-minute deduplication.
     *
     * @param eventType  event type name
     * @param riskScore  risk level
     * @param userId     affected user ID (nullable)
     * @param ip         client IP
     * @param endpoint   request endpoint
     * @param dedupeKey  unique key for deduplication
     */
    public void sendAlert(String eventType, String riskScore, Long userId,
                          String ip, String endpoint, String dedupeKey) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String redisKey = "ratelimit:slack:" + dedupeKey;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isNew)) {
            // Duplicate within 10-minute window — skip
            log.debug("slack_alert_skipped dedupeKey={}", dedupeKey);
            return;
        }

        if (!trySend(eventType, riskScore, userId, ip, endpoint)) {
            enqueueRetry(eventType, riskScore, userId, ip, endpoint, 1);
        }
    }

    @Scheduled(fixedDelay = 5000)
    public void processRetries() {
        long now = System.currentTimeMillis();
        while (true) {
            RetryTask task;
            synchronized (retryQueue) {
                task = retryQueue.peek();
                if (task == null || task.runAtEpochMs > now) {
                    return;
                }
                retryQueue.poll();
            }

            if (!trySend(task.eventType, task.riskScore, task.userId, task.ip, task.endpoint)
                && task.attempt < 3) {
                enqueueRetry(task.eventType, task.riskScore, task.userId, task.ip, task.endpoint, task.attempt + 1);
            }
        }
    }

    private boolean trySend(String eventType, String riskScore, Long userId, String ip, String endpoint) {
        String text = String.format(
            "*[%s]* Security event: `%s`%nRisk: *%s*%nUser: %s | IP: %s%nEndpoint: %s",
            riskScore, eventType,
            riskScore,
            userId != null ? userId.toString() : "anonymous",
            ip != null ? ip : "unknown",
            endpoint != null ? endpoint : "-"
        );
        String payload = "{\"text\": " + jsonEscape(text) + "}";

        try {
            restTemplate.postForObject(webhookUrl, payload, String.class);
            long c = sentCount.incrementAndGet();
            log.info("slack_alert_sent eventType={} risk={} sentCount={} endpoint={}",
                eventType, riskScore, c, endpoint);
            return true;
        } catch (Exception e) {
            long c = failedCount.incrementAndGet();
            log.warn("slack_alert_failed eventType={} risk={} failedCount={} reason={}",
                eventType, riskScore, c, e.getMessage());
            return false;
        }
    }

    private void enqueueRetry(
        String eventType,
        String riskScore,
        Long userId,
        String ip,
        String endpoint,
        int attempt
    ) {
        long delayMs = switch (attempt) {
            case 1 -> 60_000L;
            case 2 -> 300_000L;
            default -> 900_000L;
        };
        long runAt = System.currentTimeMillis() + delayMs;
        RetryTask task = new RetryTask(eventType, riskScore, userId, ip, endpoint, attempt, runAt);
        synchronized (retryQueue) {
            retryQueue.offer(task);
        }
        long c = scheduledRetryCount.incrementAndGet();
        log.info("slack_alert_retry_scheduled eventType={} attempt={} scheduledCount={} runAt={}",
            eventType, attempt, c, LocalDateTime.now().plusNanos(delayMs * 1_000_000));
    }

    private String jsonEscape(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            + "\"";
    }

    private record RetryTask(
        String eventType,
        String riskScore,
        Long userId,
        String ip,
        String endpoint,
        int attempt,
        long runAtEpochMs
    ) implements Comparable<RetryTask> {
        @Override
        public int compareTo(RetryTask other) {
            return Long.compare(this.runAtEpochMs, other.runAtEpochMs);
        }
    }
}
