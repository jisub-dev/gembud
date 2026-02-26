package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.AdResponse;
import com.gembud.entity.Advertisement;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.AdService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for advertisements (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Tag(name = "Advertisement", description = "광고 관리 API")
@RestController
@RequestMapping("/ads")
@RequiredArgsConstructor
public class AdController {

    private final AdService adService;

    /**
     * Get ads for current user (Phase 11: 1-day 3x limit, premium excluded).
     * Intended to be shown at the bottom of recommended rooms.
     *
     * @param userDetails authenticated user
     * @return list of ads
     */
    @Operation(summary = "Get ads", description = "현재 사용자에게 표시할 광고 목록 조회 (1일 3회 제한, 프리미엄 제외)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "광고 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<AdResponse>>> getAds(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Advertisement> ads = adService.getAdsForUser(userDetails.getUserId());

        List<AdResponse> responses = ads.stream()
            .map(AdResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Record ad view (Phase 11).
     * Called when user actually sees the ad.
     *
     * @param adId ad ID
     * @param userDetails authenticated user
     * @return no content
     */
    @Operation(summary = "Record ad view", description = "광고 조회 기록 (사용자가 실제로 광고를 봤을 때 호출)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "광고 조회 기록 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "광고를 찾을 수 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 오늘 조회한 광고")
    })
    @PostMapping("/{adId}/view")
    public ResponseEntity<ApiResponse<Void>> recordView(
        @PathVariable Long adId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adService.recordAdView(adId, userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    /**
     * Get remaining ad views for today (Phase 11).
     *
     * @param userDetails authenticated user
     * @return remaining views count
     */
    @Operation(summary = "Get remaining ad views", description = "오늘 남은 광고 조회 가능 횟수 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "남은 조회 횟수 반환"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/remaining")
    public ResponseEntity<ApiResponse<Integer>> getRemainingViews(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int remaining = adService.getRemainingAdViews(userDetails.getUserId());
        return ResponseEntity.ok(ApiResponse.success(remaining));
    }
}
