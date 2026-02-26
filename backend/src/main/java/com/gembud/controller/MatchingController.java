package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.RecommendedRoomResponse;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.MatchingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Matching", description = "매칭 추천 API")
@RestController
@RequestMapping("/matching")
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
    @Operation(summary = "Get recommended rooms", description = "게임별 추천 방 목록 조회 (사용자 온도 및 선호도 기반)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "추천 방 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/recommendations/game/{gameId}")
    public ResponseEntity<ApiResponse<List<RecommendedRoomResponse>>> getRecommendedRooms(
        @AuthenticationPrincipal CustomUserDetails userDetails,
        @PathVariable Long gameId,
        @RequestParam(defaultValue = "10") int limit
    ) {
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            userDetails.getUsername(),
            gameId,
            limit
        );

        return ResponseEntity.ok(ApiResponse.success(recommendations));
    }
}
