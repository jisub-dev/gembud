package com.gembud.service;

import com.gembud.dto.response.GameOptionResponse;
import com.gembud.dto.response.GameResponse;
import com.gembud.entity.Game;
import com.gembud.entity.GameOption;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.GameOptionRepository;
import com.gembud.repository.GameRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for game operations.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Service
@RequiredArgsConstructor
public class GameService {

    private final GameRepository gameRepository;
    private final GameOptionRepository gameOptionRepository;

    /**
     * Get all games.
     *
     * @return list of game responses
     */
    @Cacheable(value = "games", key = "'all'")
    @Transactional(readOnly = true)
    public List<GameResponse> getAllGames() {
        return gameRepository.findAll().stream()
            .map(GameResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get game by ID with options.
     *
     * @param gameId game ID
     * @return game response with options
     */
    @Cacheable(value = "games", key = "#gameId")
    @Transactional(readOnly = true)
    public GameResponse getGameById(Long gameId) {
        Game game = gameRepository.findById(gameId)
            .orElseThrow(() -> new BusinessException(ErrorCode.GAME_NOT_FOUND));

        List<GameOption> options = gameOptionRepository.findByGameId(gameId);
        List<GameOptionResponse> optionResponses = options.stream()
            .map(GameOptionResponse::from)
            .collect(Collectors.toList());

        GameResponse response = GameResponse.from(game);
        return GameResponse.builder()
            .id(response.getId())
            .name(response.getName())
            .imageUrl(response.getImageUrl())
            .genre(response.getGenre())
            .description(response.getDescription())
            .options(optionResponses)
            .build();
    }

    /**
     * Get games by genre.
     *
     * @param genre game genre
     * @return list of game responses
     */
    @Transactional(readOnly = true)
    public List<GameResponse> getGamesByGenre(String genre) {
        return gameRepository.findByGenre(genre).stream()
            .map(GameResponse::from)
            .collect(Collectors.toList());
    }
}
