package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.AdminUserSecurityStatusResponse;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.service.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Admin-only user security APIs.
 */
@Tag(name = "Admin User", description = "관리자 사용자 보안 API")
@RestController
@RequestMapping("/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "Unlock login lock", description = "잠긴 계정 해제 (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{userId}/login-lock")
    public ResponseEntity<Void> unlockLoginLock(@PathVariable Long userId, Authentication authentication) {
        requireAdmin(authentication);
        adminUserService.unlockLoginLock(userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Get user security status", description = "사용자 보안 상태 조회 (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/{userId}/security-status")
    public ResponseEntity<ApiResponse<AdminUserSecurityStatusResponse>> getSecurityStatus(
        @PathVariable Long userId,
        Authentication authentication
    ) {
        requireAdmin(authentication);
        AdminUserSecurityStatusResponse data = adminUserService.getSecurityStatus(userId);
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    private void requireAdmin(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities().stream()
            .noneMatch(auth -> "ROLE_ADMIN".equals(auth.getAuthority()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
    }
}
