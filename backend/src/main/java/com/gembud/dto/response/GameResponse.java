package com.gembud.dto.response;

import com.gembud.entity.Game;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Game.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameResponse {

    private Long id;
    private String name;
    private String imageUrl;
    private String genre;
    private String description;
    private List<GameOptionResponse> options;

    /**
     * Convert Game entity to GameResponse.
     *
     * @param game game entity
     * @return game response
     */
    public static GameResponse from(Game game) {
        return GameResponse.builder()
            .id(game.getId())
            .name(game.getName())
            .imageUrl(game.getImageUrl())
            .genre(game.getGenre())
            .description(game.getDescription())
            .build();
    }
}
