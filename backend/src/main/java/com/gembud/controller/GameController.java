package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.GameResponse;
import com.gembud.service.GameService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for game endpoints.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Tag(name = "Game", description = "게임 정보 API")
@RestController
@RequestMapping("/games")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    /**
     * Get all games.
     *
     * @return list of games
     */
    @Operation(summary = "Get all games", description = "모든 게임 목록 조회 (장르 필터링 가능)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 목록 조회 성공")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<GameResponse>>> getAllGames(
        @RequestParam(required = false) String genre
    ) {
        List<GameResponse> games = genre != null
            ? gameService.getGamesByGenre(genre)
            : gameService.getAllGames();
        return ResponseEntity.ok(ApiResponse.success(games));
    }

    /**
     * Get game by ID with options.
     *
     * @param gameId game ID
     * @return game with options
     */
    @Operation(summary = "Get game by ID", description = "게임 상세 정보 조회 (게임 옵션 포함)")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "게임 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "게임을 찾을 수 없음")
    })
    @GetMapping("/{gameId}")
    public ResponseEntity<ApiResponse<GameResponse>> getGameById(@PathVariable Long gameId) {
        return ResponseEntity.ok(ApiResponse.success(gameService.getGameById(gameId)));
    }
}
