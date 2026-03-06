package com.gembud.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Disabled;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.RoomResponse;
import com.gembud.entity.Room;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.security.JwtTokenProvider;
import com.gembud.service.RefreshTokenStore;
import com.gembud.service.RoomService;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests for RoomController.
 *
 * Phase 4: Controller layer testing with authentication
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
@WebMvcTest(RoomController.class)
@AutoConfigureMockMvc(addFilters = false)
class RoomControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RoomService roomService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserDetailsService userDetailsService;

    @MockBean
    private RefreshTokenStore refreshTokenStore;

    @Test
    @DisplayName("POST /rooms - should create room and return 201")
    @WithMockUser(username = "test@example.com")
    void createRoom_Success() throws Exception {
        // Given
        CreateRoomRequest request = CreateRoomRequest.builder()
            .gameId(1L)
            .title("LOL 랭크 같이 하실 분")
            .maxParticipants(5)
            .isPrivate(false)
            .build();

        RoomResponse response = RoomResponse.builder()
            .id(1L)
            .gameId(1L)
            .title("LOL 랭크 같이 하실 분")
            .createdBy("테스트유저")
            .currentParticipants(1)
            .maxParticipants(5)
            .status("OPEN")
            .isPrivate(false)
            .build();

        when(roomService.createRoom(any(CreateRoomRequest.class), eq("test@example.com")))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value(201))
            .andExpect(jsonPath("$.message").value("Created"))
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").value("LOL 랭크 같이 하실 분"))
            .andExpect(jsonPath("$.data.currentParticipants").value(1))
            .andExpect(jsonPath("$.data.maxParticipants").value(5))
            .andExpect(jsonPath("$.data.status").value("OPEN"));
    }

    @Test
    @DisplayName("POST /rooms - should return 403 when temperature too low")
    @WithMockUser(username = "test@example.com")
    void createRoom_LowTemperature() throws Exception {
        // Given
        CreateRoomRequest request = CreateRoomRequest.builder()
            .gameId(1L)
            .title("Test Room")
            .maxParticipants(5)
            .build();

        when(roomService.createRoom(any(CreateRoomRequest.class), eq("test@example.com")))
            .thenThrow(new BusinessException(ErrorCode.LOW_TEMPERATURE));

        // When & Then
        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("USER002"))
            .andExpect(jsonPath("$.message").value("Temperature too low to create room"));
    }

    @Test
    @DisplayName("POST /rooms - should return 409 when already in another active room")
    @WithMockUser(username = "test@example.com")
    void createRoom_AlreadyInOtherRoom() throws Exception {
        CreateRoomRequest request = CreateRoomRequest.builder()
            .gameId(1L)
            .title("중복 생성 시도")
            .maxParticipants(5)
            .build();

        when(roomService.createRoom(any(CreateRoomRequest.class), eq("test@example.com")))
            .thenThrow(new BusinessException(ErrorCode.ALREADY_IN_OTHER_ROOM));

        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ROOM008"))
            .andExpect(jsonPath("$.message").value("이미 다른 대기방에 참가 중입니다."));
    }

    @Test
    @DisplayName("GET /rooms?gameId=1 - should return rooms for game")
    @WithMockUser
    void getRoomsByGame_Success() throws Exception {
        // Given
        RoomResponse room1 = RoomResponse.builder()
            .id(1L)
            .gameId(1L)
            .title("Room 1")
            .currentParticipants(2)
            .maxParticipants(5)
            .build();

        RoomResponse room2 = RoomResponse.builder()
            .id(2L)
            .gameId(1L)
            .title("Room 2")
            .currentParticipants(3)
            .maxParticipants(5)
            .build();

        List<RoomResponse> rooms = Arrays.asList(room1, room2);
        when(roomService.getRoomsByGame(1L)).thenReturn(rooms);

        // When & Then
        mockMvc.perform(get("/rooms").param("gameId", "1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data").isArray())
            .andExpect(jsonPath("$.data.length()").value(2))
            .andExpect(jsonPath("$.data[0].id").value(1))
            .andExpect(jsonPath("$.data[1].id").value(2));
    }

    @Test
    @DisplayName("GET /rooms/{roomId} - should return room details")
    @WithMockUser
    void getRoomById_Success() throws Exception {
        // Given
        RoomResponse response = RoomResponse.builder()
            .id(1L)
            .gameId(1L)
            .title("LOL 랭크 방")
            .createdBy("호스트")
            .currentParticipants(3)
            .maxParticipants(5)
            .status("OPEN")
            .build();

        when(roomService.getRoomById(1L)).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/rooms/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(1))
            .andExpect(jsonPath("$.data.title").value("LOL 랭크 방"))
            .andExpect(jsonPath("$.data.createdBy").value("호스트"))
            .andExpect(jsonPath("$.data.currentParticipants").value(3));
    }

    @Test
    @DisplayName("GET /rooms/{roomId} - should return 404 when room not found")
    @WithMockUser
    void getRoomById_NotFound() throws Exception {
        // Given
        when(roomService.getRoomById(999L))
            .thenThrow(new BusinessException(ErrorCode.ROOM_NOT_FOUND));

        // When & Then
        mockMvc.perform(get("/rooms/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code").value("ROOM001"))
            .andExpect(jsonPath("$.message").value("Room not found"));
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/join - should join room successfully")
    @WithMockUser(username = "test@example.com")
    void joinRoom_Success() throws Exception {
        // Given
        JoinRoomRequest request = JoinRoomRequest.builder()
            .password(null)
            .build();

        RoomResponse response = RoomResponse.builder()
            .id(1L)
            .gameId(1L)
            .title("Test Room")
            .currentParticipants(2)
            .maxParticipants(5)
            .build();

        when(roomService.joinRoom(eq(1L), any(JoinRoomRequest.class), eq("test@example.com")))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/rooms/1/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(200))
            .andExpect(jsonPath("$.data.currentParticipants").value(2));
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/join - should return 409 when room is full")
    @WithMockUser(username = "test@example.com")
    void joinRoom_RoomFull() throws Exception {
        // Given
        JoinRoomRequest request = new JoinRoomRequest();

        when(roomService.joinRoom(eq(1L), any(JoinRoomRequest.class), eq("test@example.com")))
            .thenThrow(new BusinessException(ErrorCode.ROOM_FULL));

        // When & Then
        mockMvc.perform(post("/rooms/1/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ROOM002"))
            .andExpect(jsonPath("$.message").value("Room is full"));
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/join - should return 409 when already in another room")
    @WithMockUser(username = "test@example.com")
    void joinRoom_AlreadyInOtherRoom() throws Exception {
        // Given
        JoinRoomRequest request = new JoinRoomRequest();

        when(roomService.joinRoom(eq(1L), any(JoinRoomRequest.class), eq("test@example.com")))
            .thenThrow(new BusinessException(ErrorCode.ALREADY_IN_OTHER_ROOM));

        // When & Then
        mockMvc.perform(post("/rooms/1/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code").value("ROOM008"))
            .andExpect(jsonPath("$.message").value("이미 다른 대기방에 참가 중입니다."));
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/join - should return 401 when invalid password")
    @WithMockUser(username = "test@example.com")
    void joinRoom_InvalidPassword() throws Exception {
        // Given
        JoinRoomRequest request = JoinRoomRequest.builder()
            .password("wrongpassword")
            .build();

        when(roomService.joinRoom(eq(1L), any(JoinRoomRequest.class), eq("test@example.com")))
            .thenThrow(new BusinessException(ErrorCode.INVALID_ROOM_PASSWORD));

        // When & Then
        mockMvc.perform(post("/rooms/1/join")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.code").value("ROOM006"))
            .andExpect(jsonPath("$.message").value("Invalid room password"));
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/leave - should leave room successfully")
    @WithMockUser(username = "test@example.com")
    void leaveRoom_Success() throws Exception {
        // Given
        doNothing().when(roomService).leaveRoom(1L, "test@example.com");

        // When & Then
        mockMvc.perform(post("/rooms/1/leave"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value(204));
    }

    @Test
    @DisplayName("POST /rooms/{roomId}/kick/{userId} - should return 403 when requester is not host")
    @WithMockUser(username = "member@example.com")
    void kickParticipant_NotHost() throws Exception {
        doThrow(new BusinessException(ErrorCode.NOT_HOST))
            .when(roomService).kickParticipant(1L, 2L, "member@example.com");

        mockMvc.perform(post("/rooms/1/kick/2"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.code").value("ROOM007"))
            .andExpect(jsonPath("$.message").value("Only host can perform this action"));
    }

    @Test
    @Disabled("Security filter disabled in @WebMvcTest slice; authentication test requires @SpringBootTest")
    @DisplayName("POST /rooms - should return 401 when not authenticated")
    void createRoom_Unauthorized() throws Exception {
        // Given
        CreateRoomRequest request = CreateRoomRequest.builder()
            .gameId(1L)
            .title("Test Room")
            .maxParticipants(5)
            .build();

        // When & Then - No @WithMockUser annotation
        mockMvc.perform(post("/rooms")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
    }
}
