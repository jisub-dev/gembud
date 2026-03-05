package com.gembud.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.gembud.entity.Game;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

/**
 * Tests for GameRepository.
 *
 * Phase 4: Repository layer testing with @DataJpaTest
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
@DataJpaTest
@ActiveProfiles("test")
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
        lol = Game.builder()
            .name("League of Legends")
            .imageUrl("https://example.com/lol.png")
            .genre("MOBA")
            .description("5v5 team strategy game")
            .build();

        pubg = Game.builder()
            .name("PUBG")
            .imageUrl("https://example.com/pubg.png")
            .genre("Battle Royale")
            .description("Battle royale survival shooting game")
            .build();

        valorant = Game.builder()
            .name("Valorant")
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
        assertThat(saved.getName()).isEqualTo("League of Legends");
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
        assertThat(found.get().getName()).isEqualTo("League of Legends");
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
    @DisplayName("findAll - should return all games")
    void findAll_ShouldReturnAllGames() {
        // Given
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.persist(valorant);
        entityManager.flush();

        // When
        List<Game> games = gameRepository.findAll();

        // Then
        assertThat(games).hasSize(3);
        assertThat(games).extracting(Game::getName)
            .containsExactlyInAnyOrder("League of Legends", "PUBG", "Valorant");
    }

    @Test
    @DisplayName("findByGenre - should return games with matching genre")
    void findByGenre_ShouldReturnMatchingGames() {
        // Given
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.persist(valorant);
        entityManager.flush();

        // When
        List<Game> fpsGames = gameRepository.findByGenre("FPS");

        // Then
        assertThat(fpsGames).hasSize(1);
        assertThat(fpsGames.get(0).getName()).isEqualTo("Valorant");
        assertThat(fpsGames.get(0).getGenre()).isEqualTo("FPS");
    }

    @Test
    @DisplayName("findByGenre - should return multiple games for same genre")
    void findByGenre_MultipleGames_ShouldReturnAll() {
        // Given
        Game csgo = Game.builder()
            .name("CS:GO")
            .genre("FPS")
            .description("Counter-Strike")
            .build();

        entityManager.persist(valorant);
        entityManager.persist(csgo);
        entityManager.flush();

        // When
        List<Game> fpsGames = gameRepository.findByGenre("FPS");

        // Then
        assertThat(fpsGames).hasSize(2);
        assertThat(fpsGames).extracting(Game::getGenre)
            .containsOnly("FPS");
    }

    @Test
    @DisplayName("findByGenre - should return empty list when no games match")
    void findByGenre_NoMatches_ShouldReturnEmptyList() {
        // Given
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.flush();

        // When
        List<Game> rpgGames = gameRepository.findByGenre("RPG");

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
    @DisplayName("count - should return total number of games")
    void count_ShouldReturnTotalGames() {
        // Given
        entityManager.persist(lol);
        entityManager.persist(pubg);
        entityManager.persist(valorant);
        entityManager.flush();

        // When
        long count = gameRepository.count();

        // Then
        assertThat(count).isEqualTo(3);
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
