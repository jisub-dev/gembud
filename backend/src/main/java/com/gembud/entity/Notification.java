package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Notification entity.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Entity
@Table(name = "notifications")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    /**
     * Notification type enum.
     */
    public enum NotificationType {
        FRIEND_REQUEST,         // 친구 요청
        FRIEND_ACCEPTED,        // 친구 요청 수락
        ROOM_INVITE,            // 방 초대
        ROOM_JOIN,              // 방 참가
        EVALUATION_RECEIVED,    // 평가 받음
        REPORT_RESOLVED,        // 신고 처리 완료
        REPORT_WARNED,          // 신고 경고
        ACCOUNT_SUSPENDED       // 계정 정지
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_id")
    private Long relatedId;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Mark notification as read.
     */
    public void markAsRead() {
        this.isRead = true;
    }
}
