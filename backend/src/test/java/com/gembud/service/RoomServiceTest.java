package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.RoomResponse;
import com.gembud.entity.Game;
import com.gembud.entity.Room;
import com.gembud.entity.RoomFilter;
import com.gembud.entity.RoomParticipant;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.GameRepository;
import com.gembud.repository.RoomFilterRepository;
import com.gembud.repository.RoomParticipantRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for RoomService.
 *
 * @author Gembud Team
 * @since 2026-03-04
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository participantRepository;

    @Mock
    private RoomFilterRepository filterRepository;

    @Mock
    private GameRepository gameRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TemperatureService temperatureService;

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private RoomService roomService;

    private User user;
    private Game game;
    private Room room;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("user@example.com")
            .nickname("TestUser")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        game = Game.builder()
            .name("League of Legends")
            .build();
        ReflectionTestUtils.setField(game, "id", 1L);

        room = Room.builder()
            .game(game)
            .title("Test Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(false)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(room, "id", 1L);
    }

    // ──────────────────────────────────────────────
    // createRoom
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("createRoom - should create room and return response")
    void createRoom_Valid_ShouldCreate() {
        // Given
        CreateRoomRequest request = CreateRoomRequest.builder()
            .gameId(1L)
            .title("Test Room")
            .maxParticipants(5)
            .isPrivate(false)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(temperatureService.canCreateRoom(1L)).thenReturn(true);
        when(participantRepository.existsActiveParticipationByUserId(1L)).thenReturn(false);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 1L);
            return r;
        });
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatService.createChatRoomForGameRoom(anyLong())).thenReturn(10L);
        when(participantRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        // When
        RoomResponse response = roomService.createRoom(request, "user@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Room");
        verify(roomRepository).save(any(Room.class));
        verify(chatService).createChatRoomForGameRoom(anyLong());
    }

    @Test
    @DisplayName("createRoom - should throw when user not found")
    void createRoom_UserNotFound_ShouldThrow() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(
            CreateRoomRequest.builder().gameId(1L).title("T").build(), "unknown@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.USER_NOT_FOUND);
    }

    @Test
    @DisplayName("createRoom - should throw when temperature too low")
    void createRoom_LowTemperature_ShouldThrow() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(temperatureService.canCreateRoom(1L)).thenReturn(false);

        assertThatThrownBy(() -> roomService.createRoom(
            CreateRoomRequest.builder().gameId(1L).title("T").build(), "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.LOW_TEMPERATURE);
    }

    @Test
    @DisplayName("createRoom - should throw when game not found")
    void createRoom_GameNotFound_ShouldThrow() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(temperatureService.canCreateRoom(1L)).thenReturn(true);
        when(gameRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.createRoom(
            CreateRoomRequest.builder().gameId(99L).title("T").build(), "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.GAME_NOT_FOUND);
    }

    // ──────────────────────────────────────────────
    // joinRoom
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("joinRoom - should join open room successfully")
    void joinRoom_OpenRoom_ShouldJoin() {
        // Given
        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.countByRoomId(1L)).thenReturn(1L);
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(chatService.getChatRoomByGameRoomId(1L)).thenReturn(10L);
        when(participantRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        // When
        RoomResponse response = roomService.joinRoom(1L, new JoinRoomRequest(), "joiner@example.com");

        // Then
        assertThat(response).isNotNull();
        verify(participantRepository).save(any(RoomParticipant.class));
        verify(chatService).addMemberToChatRoomInternal(10L, 2L);
    }

    @Test
    @DisplayName("joinRoom - should throw when already in another active room")
    void joinRoom_AlreadyInOtherRoom_ShouldThrow() {
        // Given
        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> roomService.joinRoom(1L, new JoinRoomRequest(), "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ALREADY_IN_OTHER_ROOM);
    }

    @Test
    @DisplayName("joinRoom - should throw when already in this room")
    void joinRoom_AlreadyInSameRoom_ShouldThrow() {
        // Given
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.existsActiveParticipationByUserId(1L)).thenReturn(false);
        RoomParticipant existing = RoomParticipant.builder().room(room).user(user).isHost(true).joinOrder(1).build();
        when(participantRepository.findByRoomIdAndUserId(1L, 1L)).thenReturn(Optional.of(existing));

        // When / Then
        assertThatThrownBy(() -> roomService.joinRoom(1L, new JoinRoomRequest(), "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ALREADY_IN_ROOM);
    }

    @Test
    @DisplayName("joinRoom - should throw when room is full")
    void joinRoom_RoomFull_ShouldThrow() {
        // Given
        Room fullRoom = Room.builder()
            .game(game).title("Full Room").maxParticipants(5).currentParticipants(5)
            .isPrivate(false).createdBy(user).build();
        ReflectionTestUtils.setField(fullRoom, "id", 2L);
        // Simulate FULL status
        for (int i = 0; i < 5; i++) {
            fullRoom.incrementParticipants();
        }

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(2L)).thenReturn(Optional.of(fullRoom));

        assertThatThrownBy(() -> roomService.joinRoom(2L, new JoinRoomRequest(), "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_FULL);
    }

    // ──────────────────────────────────────────────
    // leaveRoom
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("leaveRoom - host leaving last should close room")
    void leaveRoom_LastParticipant_ShouldCloseRoom() {
        // Given
        Room singleRoom = Room.builder()
            .game(game).title("Single Room").maxParticipants(5).currentParticipants(1)
            .isPrivate(false).createdBy(user).build();
        ReflectionTestUtils.setField(singleRoom, "id", 3L);

        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(singleRoom).user(user).isHost(true).joinOrder(1).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(roomRepository.findById(3L)).thenReturn(Optional.of(singleRoom));
        when(participantRepository.findByRoomIdAndUserId(3L, 1L)).thenReturn(Optional.of(hostParticipant));
        when(chatService.getChatRoomByGameRoomId(3L)).thenReturn(10L);
        when(roomRepository.save(any(Room.class))).thenReturn(singleRoom);

        // When
        roomService.leaveRoom(3L, "user@example.com");

        // Then: room should be saved (closed)
        verify(roomRepository).save(singleRoom);
        assertThat(singleRoom.getStatus()).isEqualTo(Room.RoomStatus.CLOSED);
    }

    @Test
    @DisplayName("leaveRoom - should throw when user not in room")
    void leaveRoom_NotInRoom_ShouldThrow() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdAndUserId(1L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.leaveRoom(1L, "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_IN_ROOM);
    }

    // ──────────────────────────────────────────────
    // closeRoom
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("closeRoom - host should close room successfully")
    void closeRoom_Host_ShouldClose() {
        // Given
        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(room).user(user).isHost(true).joinOrder(1).build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdAndUserId(1L, 1L)).thenReturn(Optional.of(hostParticipant));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(chatService.getChatRoomByGameRoomId(1L)).thenReturn(10L);

        // When
        roomService.closeRoom(1L, "user@example.com");

        // Then
        assertThat(room.getStatus()).isEqualTo(Room.RoomStatus.CLOSED);
        verify(roomRepository).save(room);
    }

    @Test
    @DisplayName("closeRoom - non-host should throw")
    void closeRoom_NotHost_ShouldThrow() {
        // Given
        User nonHost = User.builder()
            .email("member@example.com")
            .nickname("Member")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(nonHost, "id", 3L);

        RoomParticipant memberParticipant = RoomParticipant.builder()
            .room(room).user(nonHost).isHost(false).joinOrder(2).build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(nonHost));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdAndUserId(1L, 3L)).thenReturn(Optional.of(memberParticipant));

        assertThatThrownBy(() -> roomService.closeRoom(1L, "member@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_HOST);
    }

    // ──────────────────────────────────────────────
    // getRoomById
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getRoomById - should return room when found")
    void getRoomById_Found_ShouldReturn() {
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());

        RoomResponse response = roomService.getRoomById(1L);

        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Room");
    }

    @Test
    @DisplayName("getRoomById - should throw when not found")
    void getRoomById_NotFound_ShouldThrow() {
        when(roomRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> roomService.getRoomById(99L))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }
}
