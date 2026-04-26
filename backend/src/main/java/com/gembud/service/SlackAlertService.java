package com.gembud.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
@Slf4j
public class SlackAlertService {

    private static final String CIRCUIT_BREAKER_NAME = "slack";

    @Value("${app.security.slack-webhook-url:}")
    private String webhookUrl;

    private final StringRedisTemplate redisTemplate;
    private final RestTemplate restTemplate;

    public SlackAlertService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
    }

    public void sendAlert(String eventType, String riskScore, Long userId,
                          String ip, String endpoint, String dedupeKey) {
        if (webhookUrl == null || webhookUrl.isBlank()) {
            return;
        }

        String redisKey = "ratelimit:slack:" + dedupeKey;
        Boolean isNew = redisTemplate.opsForValue().setIfAbsent(redisKey, "1", 10, TimeUnit.MINUTES);
        if (Boolean.FALSE.equals(isNew)) {
            log.debug("slack_alert_skipped dedupeKey={}", dedupeKey);
            return;
        }

        doSend(eventType, riskScore, userId, ip, endpoint);
    }

    @CircuitBreaker(name = CIRCUIT_BREAKER_NAME, fallbackMethod = "sendFallback")
    @Retry(name = CIRCUIT_BREAKER_NAME)
    void doSend(String eventType, String riskScore, Long userId, String ip, String endpoint) {
        String text = String.format(
            "*[%s]* Security event: `%s`%nRisk: *%s*%nUser: %s | IP: %s%nEndpoint: %s",
            riskScore, eventType,
            riskScore,
            userId != null ? userId.toString() : "anonymous",
            ip != null ? ip : "unknown",
            endpoint != null ? endpoint : "-"
        );
        String payload = "{\"text\": " + jsonEscape(text) + "}";
        restTemplate.postForObject(webhookUrl, payload, String.class);
        log.info("slack_alert_sent eventType={} risk={} endpoint={}", eventType, riskScore, endpoint);
    }

    @SuppressWarnings("unused")
    void sendFallback(String eventType, String riskScore, Long userId,
                      String ip, String endpoint, Throwable t) {
        log.error("slack_alert_circuit_open eventType={} risk={} reason={}",
            eventType, riskScore, t.getMessage());
    }

    private String jsonEscape(String text) {
        if (text == null) return "\"\"";
        return "\"" + text
            .replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            + "\"";
    }
}
