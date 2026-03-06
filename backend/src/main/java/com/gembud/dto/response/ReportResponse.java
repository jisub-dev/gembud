package com.gembud.dto.response;

import com.gembud.entity.Report;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for report.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponse {

    /**
     * Report ID.
     */
    private Long id;

    /**
     * Reporter user summary.
     */
    private UserSummary reporter;

    /**
     * Reported user summary.
     */
    private UserSummary reported;

    /**
     * Room ID (nullable).
     */
    private Long roomId;

    /**
     * Room title (nullable).
     */
    private String roomTitle;

    /**
     * Report reason.
     */
    private String reason;

    /**
     * Detailed description.
     */
    private String description;

    /**
     * Report status.
     */
    private String status;

    /**
     * Report category (Phase 11).
     */
    private String category;

    /**
     * Report priority (Phase 11).
     */
    private String priority;

    /**
     * Created at.
     */
    private LocalDateTime createdAt;

    /**
     * Reviewed at (nullable).
     */
    private LocalDateTime reviewedAt;

    /**
     * Resolved at (nullable).
     */
    private LocalDateTime resolvedAt;

    /**
     * Admin comment (nullable).
     */
    private String adminComment;

    /**
     * True only when response is from admin warning action.
     */
    private Boolean warningIssued;

    /**
     * User summary DTO.
     *
     * Phase 12: email 제거 (타인 개인정보 노출 차단)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserSummary {
        private Long id;
        private String nickname;
        // email 제거: Phase 12 개인정보 보호
    }

    /**
     * Create from Report entity.
     *
     * @param report report entity
     * @return report response
     */
    public static ReportResponse from(Report report) {
        return from(report, null);
    }

    public static ReportResponse from(Report report, Boolean warningIssued) {
        return ReportResponse.builder()
            .id(report.getId())
            .reporter(UserSummary.builder()
                .id(report.getReporter().getId())
                .nickname(report.getReporter().getNickname())
                // Phase 12: email 제거 (타인 개인정보 노출 차단)
                .build())
            .reported(UserSummary.builder()
                .id(report.getReported().getId())
                .nickname(report.getReported().getNickname())
                // Phase 12: email 제거 (타인 개인정보 노출 차단)
                .build())
            .roomId(report.getRoom() != null ? report.getRoom().getId() : null)
            .roomTitle(report.getRoom() != null ? report.getRoom().getTitle() : null)
            .reason(report.getReason())
            .description(report.getDescription())
            .status(report.getStatus().name())
            .category(report.getCategory().name())
            .priority(report.getPriority().name())
            .createdAt(report.getCreatedAt())
            .reviewedAt(report.getReviewedAt())
            .resolvedAt(report.getResolvedAt())
            .adminComment(report.getAdminComment())
            .warningIssued(warningIssued)
            .build();
    }
}
