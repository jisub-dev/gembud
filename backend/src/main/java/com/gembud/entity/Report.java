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
 * Report entity.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Entity
@Table(name = "reports")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Report {

    /**
     * Report status enum.
     */
    public enum ReportStatus {
        PENDING,    // 대기 중
        REVIEWED,   // 검토 중
        RESOLVED    // 처리 완료
    }

    /**
     * Report category enum (Phase 11).
     */
    public enum ReportCategory {
        VERBAL_ABUSE("욕설/비방"),
        GAME_DISRUPTION("게임 방해"),
        HARASSMENT("성희롱"),
        FRAUD("사기/계정거래"),
        FALSE_INFO("허위 정보");

        private final String displayName;

        ReportCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Report priority enum (Phase 11).
     */
    public enum ReportPriority {
        LOW,        // 낮음
        MEDIUM,     // 중간
        HIGH,       // 높음
        CRITICAL    // 최고 (성희롱, 사기 등)
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @ManyToOne
    @JoinColumn(name = "reported_id", nullable = false)
    private User reported;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @Column(nullable = false, length = 50)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ReportCategory category = ReportCategory.VERBAL_ABUSE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportPriority priority = ReportPriority.MEDIUM;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime reviewedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column(columnDefinition = "TEXT")
    private String adminComment;

    /**
     * Mark report as reviewed.
     */
    public void markAsReviewed() {
        this.status = ReportStatus.REVIEWED;
        this.reviewedAt = LocalDateTime.now();
    }

    /**
     * Resolve report with comment.
     *
     * @param adminComment admin comment
     */
    public void resolve(String adminComment) {
        this.status = ReportStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.adminComment = adminComment;
    }

    /**
     * Add admin warning comment and move to REVIEWED.
     *
     * @param warningMessage warning message
     */
    public void warn(String warningMessage) {
        this.status = ReportStatus.REVIEWED;
        if (this.reviewedAt == null) {
            this.reviewedAt = LocalDateTime.now();
        }
        this.adminComment = warningMessage;
    }
}
