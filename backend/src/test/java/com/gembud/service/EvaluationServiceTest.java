package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.request.EvaluationRequest;
import com.gembud.dto.response.EvaluationResponse;
import com.gembud.entity.Evaluation;
import com.gembud.entity.Room;
import com.gembud.entity.RoomParticipant;
import com.gembud.entity.User;
import com.gembud.repository.EvaluationRepository;
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
 * Tests for EvaluationService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class EvaluationServiceTest {

    @Mock
    private EvaluationRepository evaluationRepository;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomParticipantRepository participantRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TemperatureService temperatureService;

    @InjectMocks
    private EvaluationService evaluationService;

    private User evaluator;
    private User evaluated;
    private Room closedRoom;
    private Room openRoom;
    private RoomParticipant evaluatorParticipant;
    private RoomParticipant evaluatedParticipant;
    private EvaluationRequest validRequest;
    private Evaluation evaluation;

    @BeforeEach
    void setUp() {
        evaluator = User.builder()
            .id(1L)
            .email("evaluator@example.com")
            .nickname("Evaluator")
            .temperature(new BigDecimal("36.5"))
            .build();

        evaluated = User.builder()
            .id(2L)
            .email("evaluated@example.com")
            .nickname("Evaluated")
            .temperature(new BigDecimal("36.5"))
            .build();

        closedRoom = Room.builder()
            .id(100L)
            .title("Test Room")
            .status(Room.RoomStatus.CLOSED)
            .build();

        openRoom = Room.builder()
            .id(101L)
            .title("Open Room")
            .status(Room.RoomStatus.OPEN)
            .build();

        evaluatorParticipant = RoomParticipant.builder()
            .id(1L)
            .room(closedRoom)
            .user(evaluator)
            .isHost(true)
            .build();

        evaluatedParticipant = RoomParticipant.builder()
            .id(2L)
            .room(closedRoom)
            .user(evaluated)
            .isHost(false)
            .build();

        validRequest = EvaluationRequest.builder()
            .evaluatedId(2L)
            .mannerScore(5)
            .skillScore(4)
            .communicationScore(5)
            .comment("Great player!")
            .build();

        evaluation = Evaluation.builder()
            .id(1L)
            .room(closedRoom)
            .evaluator(evaluator)
            .evaluated(evaluated)
            .mannerScore(5)
            .skillScore(4)
            .communicationScore(5)
            .comment("Great player!")
            .build();
    }

    @Test
    @DisplayName("createEvaluation - should create evaluation successfully")
    void createEvaluation_ValidRequest_ShouldCreateEvaluation() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(evaluated));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.of(evaluatorParticipant));
        when(participantRepository.findByRoomIdAndUserId(100L, 2L))
            .thenReturn(Optional.of(evaluatedParticipant));
        when(evaluationRepository.findByRoomIdAndEvaluatorIdAndEvaluatedId(100L, 1L, 2L))
            .thenReturn(Optional.empty());
        when(evaluationRepository.save(any(Evaluation.class))).thenReturn(evaluation);

        // When
        EvaluationResponse response = evaluationService.createEvaluation(
            100L, validRequest, "evaluator@example.com"
        );

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getMannerScore()).isEqualTo(5);
        assertThat(response.getSkillScore()).isEqualTo(4);
        assertThat(response.getCommunicationScore()).isEqualTo(5);
        verify(evaluationRepository, times(1)).save(any(Evaluation.class));
        verify(temperatureService, times(1))
            .updateTemperatureFromEvaluation(eq(2L), any(Evaluation.class));
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when evaluator not found")
    void createEvaluation_EvaluatorNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(100L, validRequest, "unknown@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Evaluator not found");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when evaluated user not found")
    void createEvaluation_EvaluatedNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        EvaluationRequest invalidRequest = EvaluationRequest.builder()
            .evaluatedId(999L)
            .mannerScore(5)
            .skillScore(4)
            .communicationScore(5)
            .build();

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(100L, invalidRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Evaluated user not found");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when room not found")
    void createEvaluation_RoomNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(evaluated));
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(999L, validRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Room not found");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when room is not closed")
    void createEvaluation_RoomNotClosed_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(evaluated));
        when(roomRepository.findById(101L)).thenReturn(Optional.of(openRoom));

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(101L, validRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Can only evaluate after room is closed");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when evaluator was not in room")
    void createEvaluation_EvaluatorNotInRoom_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(evaluated));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(100L, validRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Evaluator was not in this room");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when evaluated user was not in room")
    void createEvaluation_EvaluatedNotInRoom_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(evaluated));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.of(evaluatorParticipant));
        when(participantRepository.findByRoomIdAndUserId(100L, 2L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(100L, validRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Evaluated user was not in this room");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when already evaluated")
    void createEvaluation_AlreadyEvaluated_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(2L)).thenReturn(Optional.of(evaluated));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.of(evaluatorParticipant));
        when(participantRepository.findByRoomIdAndUserId(100L, 2L))
            .thenReturn(Optional.of(evaluatedParticipant));
        when(evaluationRepository.findByRoomIdAndEvaluatorIdAndEvaluatedId(100L, 1L, 2L))
            .thenReturn(Optional.of(evaluation));

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(100L, validRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Already evaluated this user for this room");
    }

    @Test
    @DisplayName("createEvaluation - should throw exception when evaluating self")
    void createEvaluation_EvaluatingSelf_ShouldThrowException() {
        // Given
        EvaluationRequest selfRequest = EvaluationRequest.builder()
            .evaluatedId(1L) // Same as evaluator
            .mannerScore(5)
            .skillScore(5)
            .communicationScore(5)
            .build();

        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(userRepository.findById(1L)).thenReturn(Optional.of(evaluator));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.of(evaluatorParticipant));
        when(evaluationRepository.findByRoomIdAndEvaluatorIdAndEvaluatedId(100L, 1L, 1L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.createEvaluation(100L, selfRequest, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Cannot evaluate yourself");
    }

    @Test
    @DisplayName("getEvaluationsByRoom - should return all evaluations for room")
    void getEvaluationsByRoom_ShouldReturnEvaluations() {
        // Given
        Evaluation evaluation2 = Evaluation.builder()
            .id(2L)
            .room(closedRoom)
            .evaluator(evaluated)
            .evaluated(evaluator)
            .mannerScore(4)
            .skillScore(4)
            .communicationScore(4)
            .build();

        when(evaluationRepository.findByRoomId(100L))
            .thenReturn(Arrays.asList(evaluation, evaluation2));

        // When
        List<EvaluationResponse> evaluations = evaluationService.getEvaluationsByRoom(100L);

        // Then
        assertThat(evaluations).hasSize(2);
        assertThat(evaluations.get(0).getMannerScore()).isEqualTo(5);
        assertThat(evaluations.get(1).getMannerScore()).isEqualTo(4);
    }

    @Test
    @DisplayName("getEvaluationsByRoom - should return empty list when no evaluations")
    void getEvaluationsByRoom_NoEvaluations_ShouldReturnEmptyList() {
        // Given
        when(evaluationRepository.findByRoomId(100L))
            .thenReturn(Collections.emptyList());

        // When
        List<EvaluationResponse> evaluations = evaluationService.getEvaluationsByRoom(100L);

        // Then
        assertThat(evaluations).isEmpty();
    }

    @Test
    @DisplayName("getEvaluationsReceived - should return all evaluations received by user")
    void getEvaluationsReceived_ShouldReturnEvaluations() {
        // Given
        when(evaluationRepository.findByEvaluatedId(2L))
            .thenReturn(Collections.singletonList(evaluation));

        // When
        List<EvaluationResponse> evaluations = evaluationService.getEvaluationsReceived(2L);

        // Then
        assertThat(evaluations).hasSize(1);
        assertThat(evaluations.get(0).getMannerScore()).isEqualTo(5);
    }

    @Test
    @DisplayName("getEvaluationsReceived - should return empty list when no evaluations received")
    void getEvaluationsReceived_NoEvaluations_ShouldReturnEmptyList() {
        // Given
        when(evaluationRepository.findByEvaluatedId(2L))
            .thenReturn(Collections.emptyList());

        // When
        List<EvaluationResponse> evaluations = evaluationService.getEvaluationsReceived(2L);

        // Then
        assertThat(evaluations).isEmpty();
    }

    @Test
    @DisplayName("getEvaluatableParticipants - should return other participants")
    void getEvaluatableParticipants_ShouldReturnOtherParticipants() {
        // Given
        User thirdUser = User.builder()
            .id(3L)
            .email("third@example.com")
            .build();

        RoomParticipant thirdParticipant = RoomParticipant.builder()
            .id(3L)
            .room(closedRoom)
            .user(thirdUser)
            .build();

        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.of(evaluatorParticipant));
        when(participantRepository.findByRoomId(100L))
            .thenReturn(Arrays.asList(evaluatorParticipant, evaluatedParticipant, thirdParticipant));

        // When
        List<Long> evaluatableIds = evaluationService.getEvaluatableParticipants(
            100L, "evaluator@example.com"
        );

        // Then
        assertThat(evaluatableIds).hasSize(2);
        assertThat(evaluatableIds).containsExactlyInAnyOrder(2L, 3L);
        assertThat(evaluatableIds).doesNotContain(1L); // Excludes self
    }

    @Test
    @DisplayName("getEvaluatableParticipants - should throw exception when user not found")
    void getEvaluatableParticipants_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.getEvaluatableParticipants(100L, "unknown@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("getEvaluatableParticipants - should throw exception when room not found")
    void getEvaluatableParticipants_RoomNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(roomRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.getEvaluatableParticipants(999L, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Room not found");
    }

    @Test
    @DisplayName("getEvaluatableParticipants - should throw exception when user was not in room")
    void getEvaluatableParticipants_UserNotInRoom_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("evaluator@example.com"))
            .thenReturn(Optional.of(evaluator));
        when(roomRepository.findById(100L)).thenReturn(Optional.of(closedRoom));
        when(participantRepository.findByRoomIdAndUserId(100L, 1L))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            evaluationService.getEvaluatableParticipants(100L, "evaluator@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User was not in this room");
    }
}
