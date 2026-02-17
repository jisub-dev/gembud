package com.gembud.service;

import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportStatus;
import com.gembud.entity.Room;
import com.gembud.entity.User;
import com.gembud.repository.ReportRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Create a new report.
     *
     * @param reporterEmail reporter email
     * @param reportedId reported user ID
     * @param roomId room ID (nullable)
     * @param reason report reason
     * @param description detailed description
     * @return created report
     */
    @Transactional
    public Report createReport(
        String reporterEmail,
        Long reportedId,
        Long roomId,
        String reason,
        String description
    ) {
        User reporter = userRepository.findByEmail(reporterEmail)
            .orElseThrow(() -> new IllegalArgumentException("Reporter not found"));

        User reported = userRepository.findById(reportedId)
            .orElseThrow(() -> new IllegalArgumentException("Reported user not found"));

        // Cannot report yourself
        if (reporter.getId().equals(reportedId)) {
            throw new IllegalArgumentException("Cannot report yourself");
        }

        Room room = null;
        if (roomId != null) {
            room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

            // Check if already reported in this room
            if (reportRepository.existsByReporterIdAndReportedIdAndRoomId(
                reporter.getId(), reportedId, roomId)) {
                throw new IllegalArgumentException("Already reported this user in this room");
            }
        }

        Report report = Report.builder()
            .reporter(reporter)
            .reported(reported)
            .room(room)
            .reason(reason)
            .description(description)
            .status(ReportStatus.PENDING)
            .build();

        Report savedReport = reportRepository.save(report);
        log.info("Report created: {} reported {} (reason: {})",
            reporter.getNickname(), reported.getNickname(), reason);

        return savedReport;
    }

    /**
     * Get reports by reporter.
     *
     * @param reporterEmail reporter email
     * @return list of reports
     */
    public List<Report> getMyReports(String reporterEmail) {
        User reporter = userRepository.findByEmail(reporterEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

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
            throw new IllegalArgumentException("User not found");
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
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (report.getStatus() != ReportStatus.PENDING) {
            throw new IllegalStateException("Report is not in PENDING status");
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
            .orElseThrow(() -> new IllegalArgumentException("Report not found"));

        if (report.getStatus() == ReportStatus.RESOLVED) {
            throw new IllegalStateException("Report is already resolved");
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
            throw new IllegalArgumentException("Report not found");
        }

        reportRepository.deleteById(reportId);
        log.info("Report {} deleted", reportId);
    }
}
