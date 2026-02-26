package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.CreateReportRequest;
import com.gembud.dto.response.ReportResponse;
import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportStatus;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
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
@Tag(name = "Report", description = "신고 관리 API")
@RestController
@RequestMapping("/reports")
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
    @Operation(summary = "Create report", description = "사용자 신고 생성")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "신고 생성 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 입력 또는 자기 자신 신고"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 신고한 사용자")
    })
    @PostMapping
    public ResponseEntity<ApiResponse<ReportResponse>> createReport(
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
            .body(ApiResponse.created(ReportResponse.from(report)));
    }

    /**
     * Get my reports.
     *
     * @param userDetails authenticated user
     * @return list of my reports
     */
    @Operation(summary = "Get my reports", description = "내가 생성한 신고 목록 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/my")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getMyReports(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Report> reports = reportService.getMyReports(userDetails.getUsername());

        List<ReportResponse> responses = reports.stream()
            .map(ReportResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get reports by status (admin only).
     *
     * Phase 12: ADMIN 권한 필요
     *
     * @param status report status
     * @return list of reports
     */
    @Operation(summary = "Get reports by status (Admin)", description = "상태별 신고 목록 조회 (관리자 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/status/{status}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsByStatus(
        @PathVariable ReportStatus status
    ) {
        List<Report> reports = reportService.getReportsByStatus(status);

        List<ReportResponse> responses = reports.stream()
            .map(ReportResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get reports against a user (admin only).
     *
     * Phase 12: ADMIN 권한 필요
     *
     * @param userId user ID
     * @return list of reports
     */
    @Operation(summary = "Get reports against user (Admin)", description = "특정 사용자에 대한 신고 목록 조회 (관리자 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<ReportResponse>>> getReportsAgainstUser(
        @PathVariable Long userId
    ) {
        List<Report> reports = reportService.getReportsAgainstUser(userId);

        List<ReportResponse> responses = reports.stream()
            .map(ReportResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get pending report count for a user (admin only).
     *
     * Phase 12: ADMIN 권한 필요
     *
     * @param userId user ID
     * @return count
     */
    @Operation(summary = "Get pending report count (Admin)", description = "특정 사용자의 대기 중인 신고 개수 조회 (관리자 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 개수 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/user/{userId}/count")
    public ResponseEntity<ApiResponse<Long>> getPendingReportCount(@PathVariable Long userId) {
        long count = reportService.getPendingReportCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Mark report as reviewed (admin only).
     *
     * Phase 12: ADMIN 권한 필요
     *
     * @param reportId report ID
     * @return updated report
     */
    @Operation(summary = "Mark as reviewed (Admin)", description = "신고를 검토 완료로 표시 (관리자 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 검토 완료 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{reportId}/review")
    public ResponseEntity<ApiResponse<ReportResponse>> markAsReviewed(@PathVariable Long reportId) {
        Report report = reportService.markAsReviewed(reportId);
        return ResponseEntity.ok(ApiResponse.success(ReportResponse.from(report)));
    }

    /**
     * Resolve report (admin only).
     *
     * Phase 12: ADMIN 권한 필요
     *
     * @param reportId report ID
     * @param adminComment admin comment
     * @return updated report
     */
    @Operation(summary = "Resolve report (Admin)", description = "신고 처리 완료 (관리자 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "신고 처리 완료 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{reportId}/resolve")
    public ResponseEntity<ApiResponse<ReportResponse>> resolveReport(
        @PathVariable Long reportId,
        @RequestParam String adminComment
    ) {
        Report report = reportService.resolveReport(reportId, adminComment);
        return ResponseEntity.ok(ApiResponse.success(ReportResponse.from(report)));
    }

    /**
     * Delete report (admin only).
     *
     * Phase 12: ADMIN 권한 필요
     *
     * @param reportId report ID
     * @return no content
     */
    @Operation(summary = "Delete report (Admin)", description = "신고 삭제 (관리자 전용)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "신고 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "관리자 권한 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "신고를 찾을 수 없음")
    })
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{reportId}")
    public ResponseEntity<ApiResponse<Void>> deleteReport(@PathVariable Long reportId) {
        reportService.deleteReport(reportId);
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
