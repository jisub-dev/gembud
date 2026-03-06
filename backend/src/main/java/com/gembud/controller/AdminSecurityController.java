package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.SecurityEventResponse;
import com.gembud.dto.response.SecurityEventSummaryResponse;
import com.gembud.entity.SecurityEvent;
import com.gembud.entity.SecurityEvent.EventType;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.service.SecurityEventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only security monitoring APIs.
 */
@Tag(name = "Admin Security", description = "관리자 보안 이벤트 API")
@Validated
@RestController
@RequestMapping("/admin/security-events")
@RequiredArgsConstructor
public class AdminSecurityController {

    private final SecurityEventService securityEventService;

    @Operation(summary = "Search security events", description = "보안 이벤트 목록 조회 (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> search(
        @RequestParam(required = false) EventType eventType,
        @RequestParam(required = false) String riskScore,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime from,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
        LocalDateTime to,
        @RequestParam(defaultValue = "0") @Min(0) int page,
        @RequestParam(defaultValue = "20") @Max(100) int size
    ) {
        if (size > 100) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "`size` must be <= 100");
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "`from` must be before `to`");
        }

        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<SecurityEvent> result = securityEventService.search(eventType, riskScore, from, to, pageable);

        Map<String, Object> data = Map.of(
            "content", result.getContent().stream().map(SecurityEventResponse::from).toList(),
            "page", result.getNumber(),
            "size", result.getSize(),
            "totalElements", result.getTotalElements(),
            "totalPages", result.getTotalPages()
        );
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @Operation(summary = "Security event summary", description = "보안 이벤트 요약 조회 (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<SecurityEventSummaryResponse>> summary(
        @RequestParam(defaultValue = "60") @Min(1) @Max(1440) int windowMinutes
    ) {
        SecurityEventSummaryResponse data = securityEventService.summary(windowMinutes);
        return ResponseEntity.ok(ApiResponse.success(data));
    }
}
