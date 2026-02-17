package com.gembud.repository;

import com.gembud.entity.Game;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository for Game entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface GameRepository extends JpaRepository<Game, Long> {

    /**
     * Find game by name.
     *
     * @param name game name
     * @return optional game
     */
    Optional<Game> findByName(String name);

    /**
     * Find games by genre.
     *
     * @param genre game genre
     * @return list of games
     */
    List<Game> findByGenre(String genre);

    /**
     * Check if game exists by name.
     *
     * @param name game name
     * @return true if exists
     */
    boolean existsByName(String name);
}
