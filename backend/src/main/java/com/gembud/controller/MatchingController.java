package com.gembud.controller;

import com.gembud.dto.response.RecommendedRoomResponse;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.MatchingService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for matching recommendations.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@RestController
@RequestMapping("/api/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    /**
     * Get recommended rooms for a game.
     *
     * @param userDetails authenticated user
     * @param gameId game ID
     * @param limit maximum number of recommendations (default: 10)
     * @return list of recommended rooms
     */
    @GetMapping("/recommendations/game/{gameId}")
    public ResponseEntity<List<RecommendedRoomResponse>> getRecommendedRooms(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long gameId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            userDetails.getUsername(),
            gameId,
            limit
        );

        return ResponseEntity.ok(recommendations);
    }
}
