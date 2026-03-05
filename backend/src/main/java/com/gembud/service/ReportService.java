package com.gembud.service;

import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportCategory;
import com.gembud.entity.Report.ReportPriority;
import com.gembud.entity.Report.ReportStatus;
import com.gembud.entity.Room;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.ReportRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;

/**
 * Service for report management.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    /**
     * Create a new report with category (Phase 11).
     *
     * @param reporterEmail reporter email
     * @param reportedId reported user ID
     * @param roomId room ID (nullable)
     * @param category report category
     * @param reason report reason
     * @param description detailed description
     * @return created report
     */
    @Transactional
    public Report createReport(
        String reporterEmail,
        Long reportedId,
        Long roomId,
        ReportCategory category,
        String reason,
        String description
    ) {
        User reporter = userRepository.findByEmail(reporterEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        User reported = userRepository.findById(reportedId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // Cannot report yourself
        if (reporter.getId().equals(reportedId)) {
            throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
        }

        Room room = null;
        if (roomId != null) {
            room = roomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ROOM_NOT_FOUND));

            // Check if already reported in this room
            if (reportRepository.existsByReporterIdAndReportedIdAndRoomId(
                reporter.getId(), reportedId, roomId)) {
                throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
            }
        }

        // Phase 11: Determine priority based on category
        ReportPriority priority = determinePriority(category);

        Report report = Report.builder()
            .reporter(reporter)
            .reported(reported)
            .room(room)
            .category(category)
            .priority(priority)
            .reason(reason)
            .description(description)
            .status(ReportStatus.PENDING)
            .build();

        Report savedReport = reportRepository.save(report);
        log.info("Report created: {} reported {} (category: {}, priority: {})",
            reporter.getNickname(), reported.getNickname(), category, priority);

        // Phase 11: Check for auto-sanction (3+ pending reports)
        checkAutoSanction(reportedId);

        return savedReport;
    }

    /**
     * Determine report priority based on category (Phase 11).
     *
     * @param category report category
     * @return priority
     */
    private ReportPriority determinePriority(ReportCategory category) {
        return switch (category) {
            case HARASSMENT, FRAUD -> ReportPriority.CRITICAL;
            case VERBAL_ABUSE -> ReportPriority.HIGH;
            case GAME_DISRUPTION -> ReportPriority.MEDIUM;
            case FALSE_INFO -> ReportPriority.LOW;
        };
    }

    /**
     * Check and apply auto-sanction if user has 3+ pending reports (Phase 11).
     *
     * @param reportedId reported user ID
     */
    private void checkAutoSanction(Long reportedId) {
        long pendingCount = reportRepository.countPendingByReportedId(reportedId);

        if (pendingCount >= 3) {
            User reported = userRepository.findById(reportedId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

            // Skip if already suspended
            if (reported.isSuspended()) {
                log.info("[AUTO-SANCTION] User {} already suspended until {}",
                    reported.getNickname(), reported.getSuspendedUntil());
                return;
            }

            // Suspend for 7 days
            java.time.LocalDateTime suspendUntil = java.time.LocalDateTime.now().plusDays(7);
            reported.suspend(suspendUntil);
            userRepository.save(reported);

            log.warn("[AUTO-SANCTION] User {} suspended until {} ({} pending reports)",
                reported.getNickname(), suspendUntil, pendingCount);

            notificationService.notifyUserSuspended(reported, suspendUntil);
        }
    }

    /**
     * Get reports by reporter.
     *
     * @param reporterEmail reporter email
     * @return list of reports
     */
    public List<Report> getMyReports(String reporterEmail) {
        User reporter = userRepository.findByEmail(reporterEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return reportRepository.findByReporterId(reporter.getId());
    }

    /**
     * Get reports by status.
     *
     * @param status report status
     * @return list of reports
     */
    public List<Report> getReportsByStatus(ReportStatus status) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(status);
    }

    /**
     * Get reports against a user.
     *
     * @param reportedId reported user ID
     * @return list of reports
     */
    public List<Report> getReportsAgainstUser(Long reportedId) {
        if (!userRepository.existsById(reportedId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        return reportRepository.findByReportedId(reportedId);
    }

    /**
     * Mark report as reviewed.
     *
     * @param reportId report ID
     * @return updated report
     */
    @Transactional
    public Report markAsReviewed(Long reportId) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new BusinessException(ErrorCode.REPORT_NOT_PENDING);
        }

        report.markAsReviewed();
        return reportRepository.save(report);
    }

    /**
     * Resolve report.
     *
     * @param reportId report ID
     * @param adminComment admin comment
     * @return updated report
     */
    @Transactional
    public Report resolveReport(Long reportId, String adminComment) {
        Report report = reportRepository.findById(reportId)
            .orElseThrow(() -> new BusinessException(ErrorCode.REPORT_NOT_FOUND));

        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new BusinessException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }

        report.resolve(adminComment);
        Report resolvedReport = reportRepository.save(report);

        log.info("Report {} resolved with comment: {}",
            reportId, adminComment);

        return resolvedReport;
    }

    /**
     * Get pending report count for a user.
     *
     * @param reportedId reported user ID
     * @return count
     */
    public long getPendingReportCount(Long reportedId) {
        return reportRepository.countPendingByReportedId(reportedId);
    }

    /**
     * Delete report (admin only).
     *
     * @param reportId report ID
     */
    @Transactional
    public void deleteReport(Long reportId) {
        if (!reportRepository.existsById(reportId)) {
            throw new BusinessException(ErrorCode.REPORT_NOT_FOUND);
        }

        reportRepository.deleteById(reportId);
        log.info("Report {} deleted", reportId);
    }
}
