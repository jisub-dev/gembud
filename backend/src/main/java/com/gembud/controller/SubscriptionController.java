package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.SubscriptionStatusResponse;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.SubscriptionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Subscription management controller.
 *
 * @author Gembud Team
 * @since 2026-03-03
 */
@Tag(name = "Subscription", description = "프리미엄 구독 API")
@RestController
@RequestMapping("/subscriptions")
@RequiredArgsConstructor
@ConditionalOnProperty(
    prefix = "app.feature.premium",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    @Operation(summary = "Get subscription status", description = "현재 구독 상태 조회")
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> getStatus(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionStatusResponse status = subscriptionService.getStatus(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @Operation(summary = "Activate premium", description = "프리미엄 활성화 (관리자 전용)")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/activate")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> activate(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @RequestBody(required = false) ActivateRequest request
    ) {
        int months = (request != null && request.getMonths() > 0) ? request.getMonths() : 1;
        SubscriptionStatusResponse status = subscriptionService.activatePremium(
            userDetails.getUserId(), months);
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @Operation(summary = "Cancel premium", description = "프리미엄 구독 취소")
    @PostMapping("/cancel")
    public ResponseEntity<ApiResponse<SubscriptionStatusResponse>> cancel(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        SubscriptionStatusResponse status = subscriptionService.cancelPremium(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(status));
    }

    @Data
    public static class ActivateRequest {
        private int months = 1;
    }
}
