package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.AdminWarnReportRequest;
import com.gembud.dto.response.ReportResponse;
import com.gembud.entity.Report;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only report moderation APIs.
 */
@Tag(name = "Admin Report", description = "관리자 신고 조치 API")
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "Warn report target", description = "신고 대상 사용자 경고 (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{reportId}/warn")
    public ResponseEntity<ApiResponse<ReportResponse>> warnReport(
        @PathVariable Long reportId,
        @Valid @RequestBody AdminWarnReportRequest request,
        Authentication authentication
    ) {
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails principal)) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }

        Report report = reportService.warnReport(
            reportId,
            principal.getUserId(),
            request.getWarningMessage()
        );
        return ResponseEntity.ok(ApiResponse.success(ReportResponse.from(report, true)));
    }
}
