package com.gembud.repository;

import com.gembud.entity.GameOption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for GameOption entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface GameOptionRepository extends JpaRepository<GameOption, Long> {

    /**
     * Find all options for a game.
     *
     * @param gameId game ID
     * @return list of game options
     */
    List<GameOption> findByGameId(Long gameId);

    /**
     * Find common options across all games.
     *
     * @param isCommon true for common options
     * @return list of common options
     */
    List<GameOption> findByIsCommon(Boolean isCommon);
}
