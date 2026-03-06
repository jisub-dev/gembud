package com.gembud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.GameResponse;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.GameService;
import com.gembud.service.RefreshTokenStore;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for GameController.
 *
 * Phase 4: Controller layer testing with ApiResponse<T> wrapper
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
@WebMvcTest(GameController.class)
@AutoConfigureMockMvc(addFilters = false)
class GameControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private GameService gameService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("GET /games - should return all games with ApiResponse wrapper")
    @WithMockUser
    void getAllGames_Success() throws Exception {
        // Given
        GameResponse lol = GameResponse.builder()
            .id(1L)
            .name("League of Legends")
            .genre("MOBA")
            .description("5v5 team strategy game")
            .build();

        GameResponse pubg = GameResponse.builder()
            .id(2L)
            .name("PUBG")
            .genre("Battle Royale")
            .description("Battle royale survival shooting game")
            .build();

        List<GameResponse> games = Arrays.asList(lol, pubg);
        when(gameService.getAllGames()).thenReturn(games);

        // When & Then
        mockMvc.perform(get("/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[0].name").value("League of Legends"))
            .andExpect(jsonPath("$.data[0].genre").value("MOBA"))
            .andExpect(jsonPath("$.data[1].id").value(2))
            .andExpect(jsonPath("$.data[1].name").value("PUBG"))
            .andExpect(jsonPath("$.data[1].genre").value("Battle Royale"));
    }

    @Test
    @DisplayName("GET /games - should return empty array when no games exist")
    @WithMockUser
    void getAllGames_EmptyList() throws Exception {
        // Given
        when(gameService.getAllGames()).thenReturn(Collections.emptyList());

        // When & Then
        mockMvc.perform(get("/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("GET /games?genre=FPS - should return filtered games by genre")
    @WithMockUser
    void getAllGames_WithGenreFilter() throws Exception {
        // Given
        GameResponse valorant = GameResponse.builder()
            .id(3L)
            .name("Valorant")
            .genre("FPS")
            .description("5v5 tactical shooting game")
            .build();

        when(gameService.getGamesByGenre("FPS"))
            .thenReturn(Collections.singletonList(valorant));

        // When & Then
        mockMvc.perform(get("/games").param("genre", "FPS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].name").value("Valorant"))
            .andExpect(jsonPath("$.data[0].genre").value("FPS"));
    }

    @Test
    @DisplayName("GET /games/{id} - should return game by id with ApiResponse wrapper")
    @WithMockUser
    void getGameById_Success() throws Exception {
        // Given
        GameResponse lol = GameResponse.builder()
            .id(1L)
            .name("League of Legends")
            .genre("MOBA")
            .description("5v5 team strategy game")
            .imageUrl("https://example.com/lol.png")
            .build();

        when(gameService.getGameById(1L)).thenReturn(lol);

        // When & Then
        mockMvc.perform(get("/games/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.message").value("Success"))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.name").value("League of Legends"))
            .andExpect(jsonPath("$.data.genre").value("MOBA"))
            .andExpect(jsonPath("$.data.imageUrl").value("https://example.com/lol.png"));
    }

    @Test
    @DisplayName("GET /games/{id} - should return 404 when game not found")
    @WithMockUser
    void getGameById_NotFound() throws Exception {
        // Given
        when(gameService.getGameById(999L))
            .thenThrow(new BusinessException(ErrorCode.GAME_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/games/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.code").value("GAME001"))
            .andExpect(jsonPath("$.message").value("Game not found"));
    }

    @Test
    @DisplayName("GET /games - should work without authentication (public endpoint)")
    void getAllGames_PublicAccess() throws Exception {
        // Given
        GameResponse lol = GameResponse.builder()
            .id(1L)
            .name("League of Legends")
            .build();

        when(gameService.getAllGames()).thenReturn(Collections.singletonList(lol));

        // When & Then - No @WithMockUser annotation
        mockMvc.perform(get("/games"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].name").value("League of Legends"));
    }

    @Test
    @DisplayName("GET /games/{id} - should work without authentication (public endpoint)")
    void getGameById_PublicAccess() throws Exception {
        // Given
        GameResponse lol = GameResponse.builder()
            .id(1L)
            .name("League of Legends")
            .build();

        when(gameService.getGameById(1L)).thenReturn(lol);

        // When & Then - No @WithMockUser annotation
        mockMvc.perform(get("/games/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.name").value("League of Legends"));
    }
}
