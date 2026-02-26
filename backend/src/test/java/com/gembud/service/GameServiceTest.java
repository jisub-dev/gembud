package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gembud.dto.response.GameResponse;
import com.gembud.entity.Game;
import com.gembud.entity.GameOption;
import com.gembud.repository.GameOptionRepository;
import com.gembud.repository.GameRepository;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Tests for GameService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class GameServiceTest {

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GameOptionRepository gameOptionRepository;

    @InjectMocks
    private GameService gameService;

    private Game lol;
    private Game pubg;
    private Game valorant;
    private GameOption lolOption1;
    private GameOption lolOption2;

    @BeforeEach
    void setUp() {
        lol = Game.builder()
            .id(1L)
            .name("리그 오브 레전드")
            .imageUrl("https://example.com/lol.png")
            .genre("MOBA")
            .description("5v5 팀 전략 게임")
            .build();

        pubg = Game.builder()
            .id(2L)
            .name("배틀그라운드")
            .imageUrl("https://example.com/pubg.png")
            .genre("Battle Royale")
            .description("100명이 싸우는 배틀로얄")
            .build();

        valorant = Game.builder()
            .id(3L)
            .name("발로란트")
            .imageUrl("https://example.com/valorant.png")
            .genre("FPS")
            .description("5v5 전술 FPS")
            .build();

        lolOption1 = GameOption.builder()
            .id(1L)
            .game(lol)
            .optionKey("position")
            .optionType(GameOption.OptionType.SELECT)
            .optionValues("[\"탑\",\"정글\",\"미드\",\"원딜\",\"서폿\"]")
            .isCommon(false)
            .build();

        lolOption2 = GameOption.builder()
            .id(2L)
            .game(lol)
            .optionKey("tier")
            .optionType(GameOption.OptionType.SELECT)
            .optionValues("[\"아이언\",\"브론즈\",\"실버\",\"골드\",\"플래티넘\",\"다이아몬드\",\"마스터\",\"그랜드마스터\",\"챌린저\"]")
            .isCommon(false)
            .build();
    }

    @Test
    @DisplayName("getAllGames - should return all games")
    void getAllGames_ShouldReturnAllGames() {
        // Given
        when(gameRepository.findAll()).thenReturn(Arrays.asList(lol, pubg, valorant));

        // When
        List<GameResponse> games = gameService.getAllGames();

        // Then
        assertThat(games).hasSize(3);
        assertThat(games.get(0).getName()).isEqualTo("리그 오브 레전드");
        assertThat(games.get(1).getName()).isEqualTo("배틀그라운드");
        assertThat(games.get(2).getName()).isEqualTo("발로란트");
    }

    @Test
    @DisplayName("getAllGames - should return empty list when no games exist")
    void getAllGames_NoGames_ShouldReturnEmptyList() {
        // Given
        when(gameRepository.findAll()).thenReturn(Collections.emptyList());

        // When
        List<GameResponse> games = gameService.getAllGames();

        // Then
        assertThat(games).isEmpty();
    }

    @Test
    @DisplayName("getGameById - should return game with options")
    void getGameById_ShouldReturnGameWithOptions() {
        // Given
        when(gameRepository.findById(1L)).thenReturn(Optional.of(lol));
        when(gameOptionRepository.findByGameId(1L))
            .thenReturn(Arrays.asList(lolOption1, lolOption2));

        // When
        GameResponse response = gameService.getGameById(1L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("리그 오브 레전드");
        assertThat(response.getGenre()).isEqualTo("MOBA");
        assertThat(response.getOptions()).hasSize(2);
        assertThat(response.getOptions().get(0).getOptionKey()).isEqualTo("position");
        assertThat(response.getOptions().get(1).getOptionKey()).isEqualTo("tier");
    }

    @Test
    @DisplayName("getGameById - should return game without options when no options exist")
    void getGameById_NoOptions_ShouldReturnGameWithoutOptions() {
        // Given
        when(gameRepository.findById(2L)).thenReturn(Optional.of(pubg));
        when(gameOptionRepository.findByGameId(2L)).thenReturn(Collections.emptyList());

        // When
        GameResponse response = gameService.getGameById(2L);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(2L);
        assertThat(response.getName()).isEqualTo("배틀그라운드");
        assertThat(response.getOptions()).isEmpty();
    }

    @Test
    @DisplayName("getGameById - should throw exception when game not found")
    void getGameById_NotFound_ShouldThrowException() {
        // Given
        when(gameRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> gameService.getGameById(999L))
            .isInstanceOf(com.gembud.exception.BusinessException.class)
            .hasMessageContaining("Game not found");
    }

    @Test
    @DisplayName("getGamesByGenre - should return games by genre")
    void getGamesByGenre_ShouldReturnGamesByGenre() {
        // Given
        when(gameRepository.findByGenre("FPS")).thenReturn(Collections.singletonList(valorant));

        // When
        List<GameResponse> games = gameService.getGamesByGenre("FPS");

        // Then
        assertThat(games).hasSize(1);
        assertThat(games.get(0).getName()).isEqualTo("발로란트");
        assertThat(games.get(0).getGenre()).isEqualTo("FPS");
    }

    @Test
    @DisplayName("getGamesByGenre - should return multiple games for same genre")
    void getGamesByGenre_MultipleGames_ShouldReturnAll() {
        // Given
        Game anotherFps = Game.builder()
            .id(4L)
            .name("카운터 스트라이크")
            .genre("FPS")
            .build();

        when(gameRepository.findByGenre("FPS"))
            .thenReturn(Arrays.asList(valorant, anotherFps));

        // When
        List<GameResponse> games = gameService.getGamesByGenre("FPS");

        // Then
        assertThat(games).hasSize(2);
        assertThat(games.get(0).getGenre()).isEqualTo("FPS");
        assertThat(games.get(1).getGenre()).isEqualTo("FPS");
    }

    @Test
    @DisplayName("getGamesByGenre - should return empty list when no games found for genre")
    void getGamesByGenre_NoGames_ShouldReturnEmptyList() {
        // Given
        when(gameRepository.findByGenre("RPG")).thenReturn(Collections.emptyList());

        // When
        List<GameResponse> games = gameService.getGamesByGenre("RPG");

        // Then
        assertThat(games).isEmpty();
    }
}
