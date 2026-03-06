package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Security audit event entity.
 *
 * @author Gembud Team
 * @since 2026-03-05
 */
@Entity
@Table(name = "security_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SecurityEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "user_id")
    private Long userId;

    @Column(length = 45)
    private String ip;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(length = 200)
    private String endpoint;

    @Column(length = 20)
    private String result;

    @Column(name = "risk_score", length = 10)
    private String riskScore;

    /** JSON detail blob (stored as text; use JSONB column in Postgres). */
    @Column(columnDefinition = "jsonb")
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @Builder
    public SecurityEvent(EventType eventType, Long userId, String ip, String userAgent,
                         String endpoint, String result, String riskScore, String detail) {
        this.eventType = eventType;
        this.userId = userId;
        this.ip = ip;
        this.userAgent = userAgent;
        this.endpoint = endpoint;
        this.result = result;
        this.riskScore = riskScore;
        this.detail = detail;
    }

    public enum EventType {
        LOGIN_SUCCESS,
        LOGIN_FAIL,
        LOGIN_FAIL_BURST,
        LOGIN_LOCKED,
        REFRESH_SUCCESS,
        REFRESH_FAIL,
        REFRESH_REUSE_DETECTED,
        SESSION_REVOKED,
        WS_CONNECT_DENIED,
        RATE_LIMIT_HIT,
        ADMIN_UNLOCK_LOGIN,
        REPORT_WARNED
    }
}
