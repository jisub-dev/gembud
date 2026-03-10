package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.request.AdminWarnReportRequest;
import com.gembud.dto.response.ReportResponse;
import com.gembud.entity.Report;
import com.gembud.entity.Report.ReportStatus;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only report moderation APIs.
 */
@Tag(name = "Admin Report", description = "관리자 신고 조치 API")
@Validated
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "List reports", description = "관리자 신고 목록 조회 (status/search/page)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> listReports(
        @RequestParam(defaultValue = "PENDING") ReportStatus status,
        @RequestParam(required = false) String search,
        @RequestParam(required = false) String reporterNickname,
        @RequestParam(required = false) String reportedNickname,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Report> result = reportService.searchAdminReports(
            status,
            search,
            reporterNickname,
            reportedNickname,
            pageable
        );

        Map<String, Object> data = Map.of(
            "content", result.getContent().stream().map(ReportResponse::from).toList(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

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
