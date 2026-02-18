package com.gembud.controller;

import com.gembud.dto.request.CreateReportRequest;
import com.gembud.dto.response.ReportResponse;
import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportStatus;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ReportService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for report management.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    /**
     * Create a new report.
     *
     * @param userDetails authenticated user
     * @param request report request
     * @return created report
     */
    @PostMapping
    public ResponseEntity<ReportResponse> createReport(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @Valid @RequestBody CreateReportRequest request
    ) {
        Report report = reportService.createReport(
            userDetails.getUsername(),
            request.getReportedId(),
            request.getRoomId(),
            request.getCategory(),
            request.getReason(),
            request.getDescription()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ReportResponse.from(report));
    }

    /**
     * Get my reports.
     *
     * @param userDetails authenticated user
     * @return list of my reports
     */
    @GetMapping("/my")
    public ResponseEntity<List<ReportResponse>> getMyReports(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Report> reports = reportService.getMyReports(userDetails.getUsername());

        List<ReportResponse> responses = reports.stream()
            .map(ReportResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get reports by status (admin only).
     *
     * @param status report status
     * @return list of reports
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<ReportResponse>> getReportsByStatus(
        @PathVariable ReportStatus status
    ) {
        List<Report> reports = reportService.getReportsByStatus(status);

        List<ReportResponse> responses = reports.stream()
            .map(ReportResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get reports against a user (admin only).
     *
     * @param userId user ID
     * @return list of reports
     */
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<ReportResponse>> getReportsAgainstUser(
        @PathVariable Long userId
    ) {
        List<Report> reports = reportService.getReportsAgainstUser(userId);

        List<ReportResponse> responses = reports.stream()
            .map(ReportResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get pending report count for a user (admin only).
     *
     * @param userId user ID
     * @return count
     */
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<Long> getPendingReportCount(@PathVariable Long userId) {
        long count = reportService.getPendingReportCount(userId);
        return ResponseEntity.ok(count);
    }

    /**
     * Mark report as reviewed (admin only).
     *
     * @param reportId report ID
     * @return updated report
     */
    @PutMapping("/{reportId}/review")
    public ResponseEntity<ReportResponse> markAsReviewed(@PathVariable Long reportId) {
        Report report = reportService.markAsReviewed(reportId);
        return ResponseEntity.ok(ReportResponse.from(report));
    }

    /**
     * Resolve report (admin only).
     *
     * @param reportId report ID
     * @param adminComment admin comment
     * @return updated report
     */
    @PutMapping("/{reportId}/resolve")
    public ResponseEntity<ReportResponse> resolveReport(
        @PathVariable Long reportId,
        @RequestParam String adminComment
    ) {
        Report report = reportService.resolveReport(reportId, adminComment);
        return ResponseEntity.ok(ReportResponse.from(report));
    }

    /**
     * Delete report (admin only).
     *
     * @param reportId report ID
     * @return no content
     */
    @DeleteMapping("/{reportId}")
    public ResponseEntity<Void> deleteReport(@PathVariable Long reportId) {
        reportService.deleteReport(reportId);
        return ResponseEntity.noContent().build();
    }
}
