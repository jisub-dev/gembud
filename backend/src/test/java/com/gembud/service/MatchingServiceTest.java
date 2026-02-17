package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gembud.dto.response.RecommendedRoomResponse;
import com.gembud.entity.Evaluation;
import com.gembud.entity.Game;
import com.gembud.entity.Room;
import com.gembud.entity.RoomFilter;
import com.gembud.entity.RoomParticipant;
import com.gembud.entity.User;
import com.gembud.repository.EvaluationRepository;
import com.gembud.repository.RoomFilterRepository;
import com.gembud.repository.RoomParticipantRepository;
import com.gembud.repository.RoomRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
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
 * Tests for MatchingService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomFilterRepository filterRepository;

    @Mock
    private RoomParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private MatchingService matchingService;

    private User testUser;
    private Game testGame;
    private Room highTempRoom;
    private Room lowTempRoom;
    private Room normalRoom;
    private User highTempHost;
    private User lowTempHost;
    private User normalHost;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("TestUser")
            .temperature(new BigDecimal("36.5"))
            .ageRange("20대")
            .build();

        testGame = Game.builder()
            .id(1L)
            .name("리그 오브 레전드")
            .build();

        highTempHost = User.builder()
            .id(2L)
            .email("hightemp@example.com")
            .nickname("HighTempUser")
            .temperature(new BigDecimal("45.0"))
            .ageRange("20대")
            .build();

        lowTempHost = User.builder()
            .id(3L)
            .email("lowtemp@example.com")
            .nickname("LowTempUser")
            .temperature(new BigDecimal("32.0"))
            .ageRange("30대")
            .build();

        normalHost = User.builder()
            .id(4L)
            .email("normal@example.com")
            .nickname("NormalUser")
            .temperature(new BigDecimal("36.5"))
            .ageRange("20대")
            .build();

        highTempRoom = Room.builder()
            .id(1L)
            .game(testGame)
            .title("High Temp Room")
            .status(Room.RoomStatus.OPEN)
            .createdBy(highTempHost)
            .build();

        lowTempRoom = Room.builder()
            .id(2L)
            .game(testGame)
            .title("Low Temp Room")
            .status(Room.RoomStatus.OPEN)
            .createdBy(lowTempHost)
            .build();

        normalRoom = Room.builder()
            .id(3L)
            .game(testGame)
            .title("Normal Room")
            .status(Room.RoomStatus.OPEN)
            .createdBy(normalHost)
            .build();
    }

    @Test
    @DisplayName("getRecommendedRooms - should return recommended rooms sorted by score")
    void getRecommendedRooms_ShouldReturnSortedRecommendations() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(roomRepository.findByGameIdAndStatus(1L, Room.RoomStatus.OPEN))
            .thenReturn(Arrays.asList(highTempRoom, lowTempRoom, normalRoom));

        // Mock filters (no filters for simplicity)
        when(filterRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(3L)).thenReturn(Collections.emptyList());

        // Mock participants
        when(participantRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(3L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomIdAndUserId(1L, 1L))
            .thenReturn(Optional.empty());
        when(participantRepository.findByRoomIdAndUserId(2L, 1L))
            .thenReturn(Optional.empty());
        when(participantRepository.findByRoomIdAndUserId(3L, 1L))
            .thenReturn(Optional.empty());

        // Mock evaluations (no past evaluations)
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 2L))
            .thenReturn(Collections.emptyList());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 3L))
            .thenReturn(Collections.emptyList());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 4L))
            .thenReturn(Collections.emptyList());

        // When
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            "test@example.com",
            1L,
            10
        );

        // Then
        assertThat(recommendations).hasSize(3);

        // High temp room should be first (highest host temperature bonus)
        assertThat(recommendations.get(0).getRoom().getId()).isEqualTo(1L);
        assertThat(recommendations.get(0).getHostTemperature())
            .isEqualByComparingTo("45.0");
        assertThat(recommendations.get(0).getMatchingScore()).isGreaterThan(0);
    }

    @Test
    @DisplayName("getRecommendedRooms - should throw exception when user not found")
    void getRecommendedRooms_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            matchingService.getRecommendedRooms("unknown@example.com", 1L, 10))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("getRecommendedRooms - should exclude rooms user is already in")
    void getRecommendedRooms_ShouldExcludeUserRooms() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(roomRepository.findByGameIdAndStatus(1L, Room.RoomStatus.OPEN))
            .thenReturn(Arrays.asList(highTempRoom, lowTempRoom, normalRoom));

        // User is already in highTempRoom
        RoomParticipant participant = RoomParticipant.builder()
            .room(highTempRoom)
            .user(testUser)
            .build();
        when(participantRepository.findByRoomIdAndUserId(1L, 1L))
            .thenReturn(Optional.of(participant));
        when(participantRepository.findByRoomIdAndUserId(2L, 1L))
            .thenReturn(Optional.empty());
        when(participantRepository.findByRoomIdAndUserId(3L, 1L))
            .thenReturn(Optional.empty());

        // Mock other dependencies
        when(filterRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(3L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(3L)).thenReturn(Collections.emptyList());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 3L))
            .thenReturn(Collections.emptyList());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 4L))
            .thenReturn(Collections.emptyList());

        // When
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            "test@example.com",
            1L,
            10
        );

        // Then
        assertThat(recommendations).hasSize(2);
        assertThat(recommendations).noneMatch(r -> r.getRoom().getId().equals(1L));
    }

    @Test
    @DisplayName("getRecommendedRooms - should limit results")
    void getRecommendedRooms_ShouldLimitResults() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(roomRepository.findByGameIdAndStatus(1L, Room.RoomStatus.OPEN))
            .thenReturn(Arrays.asList(highTempRoom, lowTempRoom, normalRoom));

        // Mock dependencies
        when(filterRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());
        when(filterRepository.findByRoomId(3L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(2L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(3L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomIdAndUserId(1L, 1L))
            .thenReturn(Optional.empty());
        when(participantRepository.findByRoomIdAndUserId(2L, 1L))
            .thenReturn(Optional.empty());
        when(participantRepository.findByRoomIdAndUserId(3L, 1L))
            .thenReturn(Optional.empty());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 2L))
            .thenReturn(Collections.emptyList());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 3L))
            .thenReturn(Collections.emptyList());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 4L))
            .thenReturn(Collections.emptyList());

        // When
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            "test@example.com",
            1L,
            2 // Limit to 2
        );

        // Then
        assertThat(recommendations).hasSize(2);
    }

    @Test
    @DisplayName("getRecommendedRooms - should return empty list when no open rooms")
    void getRecommendedRooms_NoRooms_ShouldReturnEmptyList() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(roomRepository.findByGameIdAndStatus(1L, Room.RoomStatus.OPEN))
            .thenReturn(Collections.emptyList());

        // When
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            "test@example.com",
            1L,
            10
        );

        // Then
        assertThat(recommendations).isEmpty();
    }

    @Test
    @DisplayName("getRecommendedRooms - should include reason in response")
    void getRecommendedRooms_ShouldIncludeReason() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(roomRepository.findByGameIdAndStatus(1L, Room.RoomStatus.OPEN))
            .thenReturn(Collections.singletonList(highTempRoom));

        when(filterRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomId(1L)).thenReturn(Collections.emptyList());
        when(participantRepository.findByRoomIdAndUserId(1L, 1L))
            .thenReturn(Optional.empty());
        when(evaluationRepository.findByEvaluatorIdAndEvaluatedId(1L, 2L))
            .thenReturn(Collections.emptyList());

        // When
        List<RecommendedRoomResponse> recommendations = matchingService.getRecommendedRooms(
            "test@example.com",
            1L,
            10
        );

        // Then
        assertThat(recommendations).hasSize(1);
        assertThat(recommendations.get(0).getReason()).isNotNull();
        assertThat(recommendations.get(0).getReason()).isNotEmpty();
    }
}
