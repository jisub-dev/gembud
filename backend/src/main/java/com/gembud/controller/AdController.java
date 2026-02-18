package com.gembud.controller;

import com.gembud.dto.response.AdResponse;
import com.gembud.entity.Advertisement;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.AdService;
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
@RestController
@RequestMapping("/api/ads")
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
    @GetMapping
    public ResponseEntity<List<AdResponse>> getAds(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Advertisement> ads = adService.getAdsForUser(userDetails.getUserId());

        List<AdResponse> responses = ads.stream()
            .map(AdResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Record ad view (Phase 11).
     * Called when user actually sees the ad.
     *
     * @param adId ad ID
     * @param userDetails authenticated user
     * @return no content
     */
    @PostMapping("/{adId}/view")
    public ResponseEntity<Void> recordView(
        @PathVariable Long adId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        adService.recordAdView(adId, userDetails.getUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * Get remaining ad views for today (Phase 11).
     *
     * @param userDetails authenticated user
     * @return remaining views count
     */
    @GetMapping("/remaining")
    public ResponseEntity<Integer> getRemainingViews(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int remaining = adService.getRemainingAdViews(userDetails.getUserId());
        return ResponseEntity.ok(remaining);
    }
}
