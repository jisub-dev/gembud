package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.CreateRoomRequest;
import com.gembud.dto.request.JoinRoomRequest;
import com.gembud.dto.response.ChatMessageResponse;
import com.gembud.dto.response.RoomResponse;
import com.gembud.entity.Game;
import com.gembud.entity.Notification.NotificationType;
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
import java.util.Collection;
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
    private RateLimitService rateLimitService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private NotificationService notificationService;

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

        when(userRepository.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(temperatureService.canCreateRoom(1L)).thenReturn(true);
        when(participantRepository.existsActiveParticipationByUserId(1L)).thenReturn(false);
        when(gameRepository.findById(1L)).thenReturn(Optional.of(game));
        when(roomRepository.save(any(Room.class))).thenAnswer(inv -> {
            Room r = inv.getArgument(0);
            ReflectionTestUtils.setField(r, "id", 1L);
            return r;
        });
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(chatService.getChatRoomIdByGameRoomId(anyLong()))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        when(chatService.createChatRoomForGameRoom(anyLong())).thenReturn(10L);
        when(chatService.getPublicIdByChatRoomId(10L)).thenReturn("chat-public-10");
        when(participantRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        // When
        RoomResponse response = roomService.createRoom(request, "user@example.com");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getTitle()).isEqualTo("Test Room");
        verify(userRepository).findByEmailForUpdate("user@example.com");
        verify(roomRepository).save(any(Room.class));
        verify(chatService).addMemberToChatRoomInternal(10L, 1L);
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
    @DisplayName("createRoom - should throw when user already in active room")
    void createRoom_AlreadyInOtherRoom_ShouldThrow() {
        when(userRepository.findByEmailForUpdate("user@example.com")).thenReturn(Optional.of(user));
        when(temperatureService.canCreateRoom(1L)).thenReturn(true);
        when(participantRepository.existsActiveParticipationByUserId(1L)).thenReturn(true);

        assertThatThrownBy(() -> roomService.createRoom(
            CreateRoomRequest.builder().gameId(1L).title("T").build(), "user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ALREADY_IN_OTHER_ROOM);

        verify(userRepository).findByEmailForUpdate("user@example.com");
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

        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(room)
            .user(user)
            .isHost(true)
            .joinOrder(1)
            .build();

        when(userRepository.findByEmailForUpdate("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.countByRoomId(1L)).thenReturn(1L);
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(chatService.getChatRoomIdByGameRoomId(1L)).thenReturn(10L);
        when(chatService.getChatRoomByGameRoomId(1L)).thenReturn("chat-public-10");
        when(participantRepository.findByRoomId(1L)).thenReturn(List.of(hostParticipant));
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        // When
        RoomResponse response = roomService.joinRoom(1L, new JoinRoomRequest(), "joiner@example.com");

        // Then
        assertThat(response).isNotNull();
        verify(userRepository).findByEmailForUpdate("joiner@example.com");
        verify(roomRepository).findByIdForUpdate(1L);
        verify(participantRepository).save(any(RoomParticipant.class));
        verify(chatService).addMemberToChatRoomInternal(10L, 2L);
        verify(notificationService).createNotification(
            eq(1L),
            eq(NotificationType.ROOM_JOIN),
            eq("Joiner님이 대기방에 참가했습니다"),
            eq(1L)
        );
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

        when(userRepository.findByEmailForUpdate("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(room));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(true);

        // When / Then
        assertThatThrownBy(() -> roomService.joinRoom(1L, new JoinRoomRequest(), "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ALREADY_IN_OTHER_ROOM);

        verify(userRepository).findByEmailForUpdate("joiner@example.com");
        verify(roomRepository).findByIdForUpdate(1L);
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

        when(userRepository.findByEmailForUpdate("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByIdForUpdate(2L)).thenReturn(Optional.of(fullRoom));

        assertThatThrownBy(() -> roomService.joinRoom(2L, new JoinRoomRequest(), "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_FULL);

        verify(userRepository).findByEmailForUpdate("joiner@example.com");
        verify(roomRepository).findByIdForUpdate(2L);
    }

    @Test
    @DisplayName("joinRoom - should throw when room is already in progress")
    void joinRoom_InProgress_ShouldThrow() {
        Room inProgressRoom = Room.builder()
            .game(game)
            .title("Running Room")
            .maxParticipants(5)
            .currentParticipants(3)
            .isPrivate(false)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(inProgressRoom, "id", 15L);
        inProgressRoom.start();

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        when(userRepository.findByEmailForUpdate("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByIdForUpdate(15L)).thenReturn(Optional.of(inProgressRoom));

        assertThatThrownBy(() -> roomService.joinRoom(15L, new JoinRoomRequest(), "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_ALREADY_IN_PROGRESS);

        verify(userRepository).findByEmailForUpdate("joiner@example.com");
        verify(roomRepository).findByIdForUpdate(15L);
    }

    @Test
    @DisplayName("joinRoom - should create missing room chat and continue")
    void joinRoom_MissingRoomChat_ShouldCreateAndJoin() {
        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(room)
            .user(user)
            .isHost(true)
            .joinOrder(1)
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(1L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.countByRoomId(1L)).thenReturn(1L);
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenReturn(room);
        when(chatService.getChatRoomIdByGameRoomId(1L))
            .thenThrow(new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        when(chatService.createChatRoomForGameRoom(1L)).thenReturn(77L);
        when(chatService.getPublicIdByChatRoomId(77L)).thenReturn("chat-public-77");
        when(participantRepository.findByRoomId(1L)).thenReturn(List.of(hostParticipant));
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        RoomResponse response = roomService.joinRoom(1L, new JoinRoomRequest(), "joiner@example.com");

        assertThat(response).isNotNull();
        verify(chatService).createChatRoomForGameRoom(1L);
        verify(chatService).addMemberToChatRoomInternal(77L, 2L);
    }

    @Test
    @DisplayName("joinRoom - should throw ROOM_FULL when participant count reached max even if status is OPEN")
    void joinRoom_MaxCountReached_StatusOpen_ShouldThrowRoomFull() {
        Room staleOpenRoom = Room.builder()
            .game(game)
            .title("Stale Open Room")
            .maxParticipants(5)
            .currentParticipants(5)
            .isPrivate(false)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(staleOpenRoom, "id", 4L);

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(4L)).thenReturn(Optional.of(staleOpenRoom));

        assertThatThrownBy(() -> roomService.joinRoom(4L, new JoinRoomRequest(), "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_FULL);
    }

    @Test
    @DisplayName("joinRoomByPublicId - old invite should be invalid after regenerate")
    void joinRoomByPublicId_OldInviteAfterRegenerate_ShouldThrow() {
        User host = User.builder()
            .email("host@example.com")
            .nickname("Host")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(host, "id", 1L);

        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .createdBy(host)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 55L);
        ReflectionTestUtils.setField(privateRoom, "publicId", "public-55");
        ReflectionTestUtils.setField(privateRoom, "inviteCode", "old-code");
        ReflectionTestUtils.setField(privateRoom, "inviteCodeExpiresAt", java.time.LocalDateTime.now().plusHours(1));

        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(privateRoom)
            .user(host)
            .isHost(true)
            .joinOrder(1)
            .build();

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByPublicId("public-55")).thenReturn(Optional.of(privateRoom));
        when(roomRepository.findById(55L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.findByRoomIdAndUserId(55L, 1L)).thenReturn(Optional.of(hostParticipant));
        when(participantRepository.findByRoomIdAndUserId(55L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(roomRepository.save(any(Room.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(participantRepository.findByRoomId(55L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(55L)).thenReturn(Collections.emptyList());

        String previousInvite = privateRoom.getInviteCode();
        RoomResponse regenerated = roomService.regenerateInviteCode("public-55", "host@example.com");
        assertThat(regenerated.getInviteCode()).isNotNull();
        assertThat(regenerated.getInviteCode()).isNotEqualTo(previousInvite);

        JoinRoomRequest oldInviteRequest = JoinRoomRequest.builder()
            .inviteCode(previousInvite)
            .build();

        assertThatThrownBy(
            () -> roomService.joinRoomByPublicId("public-55", oldInviteRequest, "joiner@example.com", "127.0.0.1")
        )
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INVITE_CODE);

        verify(rateLimitService).checkJoinLimit("127.0.0.1", "public-55");
    }

    @Test
    @DisplayName("joinRoomByPublicId - should throw INVALID_INVITE_CODE when invite code expired")
    void joinRoomByPublicId_ExpiredInviteCode_ShouldThrow() {
        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 5L);
        ReflectionTestUtils.setField(privateRoom, "publicId", "public-5");
        ReflectionTestUtils.setField(privateRoom, "inviteCode", "expired-code");
        ReflectionTestUtils.setField(privateRoom, "inviteCodeExpiresAt", java.time.LocalDateTime.now().minusMinutes(1));

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        JoinRoomRequest request = JoinRoomRequest.builder()
            .inviteCode("expired-code")
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByPublicId("public-5")).thenReturn(Optional.of(privateRoom));
        when(roomRepository.findById(5L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(5L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> roomService.joinRoomByPublicId("public-5", request, "joiner@example.com", "127.0.0.1")
        )
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_INVITE_CODE);
        verify(rateLimitService).checkJoinLimit("127.0.0.1", "public-5");
    }

    @Test
    @DisplayName("joinRoomByPublicId - should join successfully with valid invite code")
    void joinRoomByPublicId_ValidInviteCode_ShouldJoin() {
        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 6L);
        ReflectionTestUtils.setField(privateRoom, "publicId", "public-6");
        ReflectionTestUtils.setField(privateRoom, "inviteCode", "valid-code");
        ReflectionTestUtils.setField(privateRoom, "inviteCodeExpiresAt", java.time.LocalDateTime.now().plusMinutes(10));

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        JoinRoomRequest request = JoinRoomRequest.builder()
            .inviteCode("valid-code")
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByPublicId("public-6")).thenReturn(Optional.of(privateRoom));
        when(roomRepository.findById(6L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(6L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.countByRoomId(6L)).thenReturn(1L);
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenReturn(privateRoom);
        when(chatService.getChatRoomIdByGameRoomId(6L)).thenReturn(60L);
        when(chatService.getChatRoomByGameRoomId(6L)).thenReturn("chat-public-60");
        when(participantRepository.findByRoomId(6L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(6L)).thenReturn(Collections.emptyList());

        RoomService.JoinRoomResult result = roomService.joinRoomByPublicId(
            "public-6", request, "joiner@example.com", "127.0.0.1"
        );

        assertThat(result).isNotNull();
        assertThat(result.chatRoomId()).isEqualTo("chat-public-60");
        assertThat(result.room().getId()).isEqualTo(6L);
        verify(chatService).addMemberToChatRoomInternal(60L, 2L);
        verify(rateLimitService).resetJoinLimit("127.0.0.1", "public-6");
        verify(passwordEncoder, never()).matches(any(String.class), any(String.class));
    }

    @Test
    @DisplayName("joinRoomByPublicId - should throw RATE_LIMIT_EXCEEDED when invalid attempts exceed threshold")
    void joinRoomByPublicId_InvalidPassword_RateLimitExceeded_ShouldThrow() {
        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .passwordHash("$2a$10$hashed")
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 7L);
        ReflectionTestUtils.setField(privateRoom, "publicId", "public-7");

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        JoinRoomRequest request = JoinRoomRequest.builder()
            .password("wrong")
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByPublicId("public-7")).thenReturn(Optional.of(privateRoom));
        when(roomRepository.findById(7L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(7L, 2L)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);
        doThrow(new BusinessException(ErrorCode.RATE_LIMIT_EXCEEDED))
            .when(rateLimitService).checkJoinLimit("127.0.0.1", "public-7");

        assertThatThrownBy(
            () -> roomService.joinRoomByPublicId("public-7", request, "joiner@example.com", "127.0.0.1")
        )
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.RATE_LIMIT_EXCEEDED);
    }

    @Test
    @DisplayName("joinRoomByPublicId - should increase failure count on invalid password")
    void joinRoomByPublicId_InvalidPassword_ShouldIncreaseFailureCount() {
        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .passwordHash("$2a$10$hashed")
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 8L);
        ReflectionTestUtils.setField(privateRoom, "publicId", "public-8");

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        JoinRoomRequest request = JoinRoomRequest.builder()
            .password("wrong")
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findByPublicId("public-8")).thenReturn(Optional.of(privateRoom));
        when(roomRepository.findById(8L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(8L, 2L)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong", "$2a$10$hashed")).thenReturn(false);

        assertThatThrownBy(
            () -> roomService.joinRoomByPublicId("public-8", request, "joiner@example.com", "127.0.0.1")
        )
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_ROOM_PASSWORD);

        verify(rateLimitService).checkJoinLimit("127.0.0.1", "public-8");
        verify(rateLimitService, never()).resetJoinLimit(eq("127.0.0.1"), eq("public-8"));
    }

    @Test
    @DisplayName("joinRoom - should join private room when password matches")
    void joinRoom_PrivateRoomPasswordMatch_ShouldJoin() {
        // Given
        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .passwordHash("$2a$10$hashed")
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 4L);

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        JoinRoomRequest request = JoinRoomRequest.builder()
            .password("correct-password")
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(4L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(4L, 2L)).thenReturn(Optional.empty());
        when(participantRepository.countByRoomId(4L)).thenReturn(1L);
        when(passwordEncoder.matches("correct-password", "$2a$10$hashed")).thenReturn(true);
        when(participantRepository.save(any(RoomParticipant.class))).thenAnswer(inv -> inv.getArgument(0));
        when(roomRepository.save(any(Room.class))).thenReturn(privateRoom);
        when(chatService.getChatRoomIdByGameRoomId(4L)).thenReturn(10L);
        when(chatService.getChatRoomByGameRoomId(4L)).thenReturn("chat-public-10");
        when(participantRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        // When
        RoomResponse response = roomService.joinRoom(4L, request, "joiner@example.com");

        // Then
        assertThat(response).isNotNull();
        verify(passwordEncoder).matches("correct-password", "$2a$10$hashed");
        verify(participantRepository).save(any(RoomParticipant.class));
    }

    @Test
    @DisplayName("joinRoom - should throw when private room password is invalid")
    void joinRoom_PrivateRoomPasswordMismatch_ShouldThrow() {
        // Given
        Room privateRoom = Room.builder()
            .game(game)
            .title("Private Room")
            .maxParticipants(5)
            .currentParticipants(1)
            .isPrivate(true)
            .passwordHash("$2a$10$hashed")
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(privateRoom, "id", 5L);

        User joiner = User.builder()
            .email("joiner@example.com")
            .nickname("Joiner")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(joiner, "id", 2L);

        JoinRoomRequest request = JoinRoomRequest.builder()
            .password("wrong-password")
            .build();

        when(userRepository.findByEmail("joiner@example.com")).thenReturn(Optional.of(joiner));
        when(roomRepository.findById(5L)).thenReturn(Optional.of(privateRoom));
        when(participantRepository.existsActiveParticipationByUserId(2L)).thenReturn(false);
        when(participantRepository.findByRoomIdAndUserId(5L, 2L)).thenReturn(Optional.empty());
        when(passwordEncoder.matches("wrong-password", "$2a$10$hashed")).thenReturn(false);

        // When / Then
        assertThatThrownBy(() -> roomService.joinRoom(5L, request, "joiner@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.INVALID_ROOM_PASSWORD);
    }

    // ──────────────────────────────────────────────
    // leaveRoom
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("leaveRoom - last participant leaving should soft-delete room")
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
        when(chatService.getChatRoomIdByGameRoomId(3L)).thenReturn(10L);
        when(chatService.getChatRoomByGameRoomId(3L)).thenReturn("chat-public-10");
        when(roomRepository.save(any(Room.class))).thenReturn(singleRoom);

        // When
        roomService.leaveRoom(3L, "user@example.com");

        // Then: room should be saved (closed)
        verify(roomRepository).save(singleRoom);
        assertThat(singleRoom.getStatus()).isEqualTo(Room.RoomStatus.CLOSED);
        assertThat(singleRoom.getDeletedAt()).isNotNull();
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
    // host-only actions
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("kickParticipant - should throw when requester is not host")
    void kickParticipant_NotHost_ShouldThrow() {
        User member = User.builder()
            .email("member@example.com")
            .nickname("Member")
            .build();
        ReflectionTestUtils.setField(member, "id", 2L);

        RoomParticipant memberParticipant = RoomParticipant.builder()
            .room(room)
            .user(member)
            .isHost(false)
            .joinOrder(2)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdAndUserId(1L, 2L)).thenReturn(Optional.of(memberParticipant));

        assertThatThrownBy(() -> roomService.kickParticipant(1L, 3L, "member@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_HOST);
    }

    @Test
    @DisplayName("transferHost - should throw when requester is not host")
    void transferHost_NotHost_ShouldThrow() {
        User member = User.builder()
            .email("member@example.com")
            .nickname("Member")
            .build();
        ReflectionTestUtils.setField(member, "id", 2L);

        RoomParticipant memberParticipant = RoomParticipant.builder()
            .room(room)
            .user(member)
            .isHost(false)
            .joinOrder(2)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdAndUserId(1L, 2L)).thenReturn(Optional.of(memberParticipant));

        assertThatThrownBy(() -> roomService.transferHost(1L, 3L, "member@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_HOST);
    }

    @Test
    @DisplayName("startRoom - should throw when requester is not host")
    void startRoom_NotHost_ShouldThrow() {
        User member = User.builder()
            .email("member@example.com")
            .nickname("Member")
            .build();
        ReflectionTestUtils.setField(member, "id", 2L);

        RoomParticipant memberParticipant = RoomParticipant.builder()
            .room(room)
            .user(member)
            .isHost(false)
            .joinOrder(2)
            .build();

        when(userRepository.findByEmail("member@example.com")).thenReturn(Optional.of(member));
        when(roomRepository.findById(1L)).thenReturn(Optional.of(room));
        when(participantRepository.findByRoomIdAndUserId(1L, 2L)).thenReturn(Optional.of(memberParticipant));

        assertThatThrownBy(() -> roomService.startRoom(1L, "member@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.NOT_HOST);
    }

    @Test
    @DisplayName("resetRoom - should reset IN_PROGRESS room to OPEN when requester is host")
    void resetRoom_HostAndInProgress_ShouldResetToOpen() {
        User host = User.builder()
            .email("host@example.com")
            .nickname("Host")
            .build();
        ReflectionTestUtils.setField(host, "id", 1L);

        Room inProgressRoom = Room.builder()
            .game(game)
            .title("In Progress Room")
            .maxParticipants(5)
            .currentParticipants(3)
            .isPrivate(false)
            .createdBy(host)
            .build();
        ReflectionTestUtils.setField(inProgressRoom, "id", 11L);
        inProgressRoom.start();

        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(inProgressRoom)
            .user(host)
            .isHost(true)
            .joinOrder(1)
            .build();

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(roomRepository.findById(11L)).thenReturn(Optional.of(inProgressRoom));
        when(participantRepository.findByRoomIdAndUserId(11L, 1L)).thenReturn(Optional.of(hostParticipant));
        when(chatService.getChatRoomIdByGameRoomId(11L)).thenReturn(88L);
        when(chatService.getChatRoomByGameRoomId(11L)).thenReturn("chat-public-88");

        roomService.resetRoom(11L, "host@example.com");

        assertThat(inProgressRoom.getStatus()).isEqualTo(Room.RoomStatus.OPEN);
        verify(roomRepository).save(inProgressRoom);
        verify(messagingTemplate).convertAndSend(eq("/topic/chat/chat-public-88"), any(ChatMessageResponse.class));
    }

    @Test
    @DisplayName("resetRoom - should throw when room status is not IN_PROGRESS")
    void resetRoom_NotInProgress_ShouldThrow() {
        User host = User.builder()
            .email("host@example.com")
            .nickname("Host")
            .build();
        ReflectionTestUtils.setField(host, "id", 1L);

        Room openRoom = Room.builder()
            .game(game)
            .title("Open Room")
            .maxParticipants(5)
            .currentParticipants(2)
            .isPrivate(false)
            .createdBy(host)
            .build();
        ReflectionTestUtils.setField(openRoom, "id", 12L);

        RoomParticipant hostParticipant = RoomParticipant.builder()
            .room(openRoom)
            .user(host)
            .isHost(true)
            .joinOrder(1)
            .build();

        when(userRepository.findByEmail("host@example.com")).thenReturn(Optional.of(host));
        when(roomRepository.findById(12L)).thenReturn(Optional.of(openRoom));
        when(participantRepository.findByRoomIdAndUserId(12L, 1L)).thenReturn(Optional.of(hostParticipant));

        assertThatThrownBy(() -> roomService.resetRoom(12L, "host@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_NOT_IN_PROGRESS);
    }

    @Test
    @DisplayName("getMyActiveRoom - should return the most recently joined active room")
    void getMyActiveRoom_Found_ShouldReturnFirstActiveRoom() {
        Room recentRoom = Room.builder()
            .game(game)
            .title("Recent Room")
            .maxParticipants(5)
            .currentParticipants(2)
            .isPrivate(false)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(recentRoom, "id", 21L);
        ReflectionTestUtils.setField(recentRoom, "status", Room.RoomStatus.OPEN);

        Room olderRoom = Room.builder()
            .game(game)
            .title("Older Room")
            .maxParticipants(5)
            .currentParticipants(3)
            .isPrivate(false)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(olderRoom, "id", 22L);
        ReflectionTestUtils.setField(olderRoom, "status", Room.RoomStatus.FULL);

        RoomParticipant recentParticipant = RoomParticipant.builder()
            .room(recentRoom)
            .user(user)
            .isHost(true)
            .joinOrder(1)
            .build();
        RoomParticipant olderParticipant = RoomParticipant.builder()
            .room(olderRoom)
            .user(user)
            .isHost(false)
            .joinOrder(2)
            .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(participantRepository.findActiveRoomsByUserId(1L)).thenReturn(List.of(recentParticipant, olderParticipant));
        when(participantRepository.findByRoomId(21L)).thenReturn(List.of(recentParticipant));
        when(filterRepository.findByRoomId(21L)).thenReturn(Collections.emptyList());

        RoomResponse response = roomService.getMyActiveRoom("user@example.com");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo(21L);
        assertThat(response.getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("getMyActiveRoom - should throw when user has no active rooms")
    void getMyActiveRoom_NotFound_ShouldThrow() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(participantRepository.findActiveRoomsByUserId(1L)).thenReturn(Collections.emptyList());

        assertThatThrownBy(() -> roomService.getMyActiveRoom("user@example.com"))
            .isInstanceOf(BusinessException.class)
            .extracting("errorCode")
            .isEqualTo(ErrorCode.ROOM_NOT_FOUND);
    }

    @Test
    @DisplayName("getRoomsByGame - should request OPEN/FULL/IN_PROGRESS rooms only")
    void getRoomsByGame_ShouldUseExpectedStatuses() {
        Room openRoom = createRoomWithStatus(31L, "Open Room", 1, Room.RoomStatus.OPEN);
        Room fullRoom = createRoomWithStatus(32L, "Full Room", 5, Room.RoomStatus.FULL);
        Room inProgressRoom = createRoomWithStatus(33L, "In Progress Room", 3, Room.RoomStatus.IN_PROGRESS);

        when(roomRepository.findByGameIdAndStatusInAndDeletedAtIsNull(eq(1L), any())).thenAnswer(invocation -> {
            Collection<Room.RoomStatus> statuses = invocation.getArgument(1);
            assertThat(statuses).containsExactlyInAnyOrder(
                Room.RoomStatus.OPEN,
                Room.RoomStatus.FULL,
                Room.RoomStatus.IN_PROGRESS
            );
            assertThat(statuses).doesNotContain(Room.RoomStatus.CLOSED);
            return List.of(openRoom, fullRoom, inProgressRoom);
        });
        when(participantRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(anyLong())).thenReturn(Collections.emptyList());

        List<RoomResponse> responses = roomService.getRoomsByGame(1L);

        assertThat(responses).hasSize(3);
        assertThat(responses).extracting(RoomResponse::getStatus)
            .containsExactly("OPEN", "FULL", "IN_PROGRESS");
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

    private Room createRoomWithStatus(Long id, String title, int currentParticipants, Room.RoomStatus status) {
        Room testRoom = Room.builder()
            .game(game)
            .title(title)
            .maxParticipants(5)
            .currentParticipants(currentParticipants)
            .isPrivate(false)
            .createdBy(user)
            .build();
        ReflectionTestUtils.setField(testRoom, "id", id);
        ReflectionTestUtils.setField(testRoom, "status", status);
        return testRoom;
    }
}
