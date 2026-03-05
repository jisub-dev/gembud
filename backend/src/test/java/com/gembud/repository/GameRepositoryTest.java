package com.gembud.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gembud.config.TestcontainersConfig;
import com.gembud.entity.Game;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for GameRepository.
 *
 * Phase 4: Repository layer testing with @DataJpaTest + Testcontainers
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
@DataJpaTest
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
@AutoConfigureTestDatabase(replace = Replace.NONE)
class GameRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private GameRepository gameRepository;

    private Game lol;
    private Game pubg;
    private Game valorant;

    @BeforeEach
    void setUp() {
        // Use names that don't conflict with Flyway seed data (V4)
        lol = Game.builder()
            .name("TestGame_MOBA_01")
            .imageUrl("https://example.com/lol.png")
            .genre("MOBA")
            .description("5v5 team strategy game")
            .build();

        pubg = Game.builder()
            .name("TestGame_BattleRoyale_01")
            .imageUrl("https://example.com/pubg.png")
            .genre("Battle Royale")
            .description("Battle royale survival shooting game")
            .build();

        valorant = Game.builder()
            .name("TestGame_FPS_01")
            .imageUrl("https://example.com/valorant.png")
            .genre("FPS")
            .description("5v5 tactical shooting game")
            .build();
    }

    @Test
    @DisplayName("save - should persist game entity")
    void save_ShouldPersistGame() {
        // When
        Game saved = gameRepository.save(lol);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getName()).isEqualTo("TestGame_MOBA_01");
        assertThat(saved.getGenre()).isEqualTo("MOBA");
    }

    @Test
    @DisplayName("findById - should return game when exists")
    void findById_WhenExists_ShouldReturnGame() {
        // Given
        Game saved = entityManager.persist(lol);
        entityManager.flush();

        // When
        Optional<Game> found = gameRepository.findById(saved.getId());

        // Then
        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("TestGame_MOBA_01");
    }

    @Test
    @DisplayName("findById - should return empty when not exists")
    void findById_WhenNotExists_ShouldReturnEmpty() {
        // When
        Optional<Game> found = gameRepository.findById(999L);

        // Then
        assertThat(found).isEmpty();
    }

    @Test
    @DisplayName("findAll - should return all games (including Flyway seed)")
    void findAll_ShouldReturnAllGames() {
        // Given
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.persist(valorant);
        entityManager.flush();

        // When
        List<Game> games = gameRepository.findAll();

        // Then - at least our 3 test games should be present (Flyway also seeded 3 games)
        assertThat(games.size()).isGreaterThanOrEqualTo(3);
        assertThat(games).extracting(Game::getName)
            .contains("TestGame_MOBA_01", "TestGame_BattleRoyale_01", "TestGame_FPS_01");
    }

    @Test
    @DisplayName("findByGenre - should return games with matching genre")
    void findByGenre_ShouldReturnMatchingGames() {
        // Given
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.persist(valorant);
        entityManager.flush();

        // When - use unique genre name to avoid interference from Flyway seed data
        List<Game> mobaGames = gameRepository.findByGenre("MOBA");

        // Then - Flyway seeds "League of Legends" as MOBA, plus our TestGame_MOBA_01
        assertThat(mobaGames).isNotEmpty();
        assertThat(mobaGames).extracting(Game::getGenre).containsOnly("MOBA");
    }

    @Test
    @DisplayName("findByGenre - should return multiple games for same genre")
    void findByGenre_MultipleGames_ShouldReturnAll() {
        // Given — use a genre not seeded by Flyway
        Game game1 = Game.builder()
            .name("TestGame_Strategy_01")
            .genre("Strategy")
            .description("Strategy game 1")
            .build();
        Game game2 = Game.builder()
            .name("TestGame_Strategy_02")
            .genre("Strategy")
            .description("Strategy game 2")
            .build();

        entityManager.persist(game1);
        entityManager.persist(game2);
        entityManager.flush();

        // When
        List<Game> stratGames = gameRepository.findByGenre("Strategy");

        // Then
        assertThat(stratGames).hasSize(2);
        assertThat(stratGames).extracting(Game::getGenre)
            .containsOnly("Strategy");
    }

    @Test
    @DisplayName("findByGenre - should return empty list when no games match")
    void findByGenre_NoMatches_ShouldReturnEmptyList() {
        // When — use a genre that doesn't exist in seed data or test data
        List<Game> rpgGames = gameRepository.findByGenre("RPG_NONEXISTENT_UNIQUE_GENRE");

        // Then
        assertThat(rpgGames).isEmpty();
    }

    @Test
    @DisplayName("delete - should remove game from database")
    void delete_ShouldRemoveGame() {
        // Given
        Game saved = entityManager.persist(lol);
        entityManager.flush();
        Long gameId = saved.getId();

        // When
        gameRepository.delete(saved);
        entityManager.flush();

        // Then
        Optional<Game> deleted = gameRepository.findById(gameId);
        assertThat(deleted).isEmpty();
    }

    @Test
    @DisplayName("count - should increase by 3 after saving 3 new games")
    void count_ShouldReturnTotalGames() {
        // Given
        long countBefore = gameRepository.count();
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.persist(valorant);
        entityManager.flush();

        // When
        long count = gameRepository.count();

        // Then - 3 new games added
        assertThat(count).isEqualTo(countBefore + 3);
    }

    @Test
    @DisplayName("Game entity should have auto-generated id")
    void gameEntity_ShouldHaveAutoGeneratedId() {
        // Given
        Game game = Game.builder()
            .name("Test Game")
            .genre("Test Genre")
            .build();

        // When
        Game saved = gameRepository.save(game);
        entityManager.flush();

        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isGreaterThan(0L);
    }
}
