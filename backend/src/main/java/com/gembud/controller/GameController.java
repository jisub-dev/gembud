package com.gembud.controller;

import com.gembud.dto.response.GameResponse;
import com.gembud.service.GameService;
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
    @GetMapping
    public ResponseEntity<List<GameResponse>> getAllGames(
        @RequestParam(required = false) String genre
    ) {
        if (genre != null) {
            return ResponseEntity.ok(gameService.getGamesByGenre(genre));
        }
        return ResponseEntity.ok(gameService.getAllGames());
    }

    /**
     * Get game by ID with options.
     *
     * @param gameId game ID
     * @return game with options
     */
    @GetMapping("/{gameId}")
    public ResponseEntity<GameResponse> getGameById(@PathVariable Long gameId) {
        return ResponseEntity.ok(gameService.getGameById(gameId));
    }
}
