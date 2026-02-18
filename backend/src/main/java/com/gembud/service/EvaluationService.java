package com.gembud.service;

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
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for evaluation operations.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Service
@RequiredArgsConstructor
public class EvaluationService {

    private final EvaluationRepository evaluationRepository;
    private final RoomRepository roomRepository;
    private final RoomParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final TemperatureService temperatureService;

    /**
     * Create an evaluation for a room participant.
     *
     * @param roomId room ID
     * @param request evaluation request
     * @param evaluatorEmail evaluator email
     * @return created evaluation response
     */
    @Transactional
    public EvaluationResponse createEvaluation(
        Long roomId,
        EvaluationRequest request,
        String evaluatorEmail
    ) {
        User evaluator = userRepository.findByEmail(evaluatorEmail)
            .orElseThrow(() -> new IllegalArgumentException("Evaluator not found"));

        User evaluated = userRepository.findById(request.getEvaluatedId())
            .orElseThrow(() -> new IllegalArgumentException("Evaluated user not found"));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Check if room is closed
        if (room.getStatus() != Room.RoomStatus.CLOSED) {
            throw new IllegalStateException("Can only evaluate after room is closed");
        }

        // Check if evaluator was in the room
        RoomParticipant evaluatorParticipant = participantRepository
            .findByRoomIdAndUserId(roomId, evaluator.getId())
            .orElseThrow(() -> new IllegalArgumentException("Evaluator was not in this room"));

        // Check if evaluated was in the room
        RoomParticipant evaluatedParticipant = participantRepository
            .findByRoomIdAndUserId(roomId, evaluated.getId())
            .orElseThrow(() -> new IllegalArgumentException("Evaluated user was not in this room"));

        // Check if already evaluated
        if (evaluationRepository.findByRoomIdAndEvaluatorIdAndEvaluatedId(
            roomId, evaluator.getId(), evaluated.getId()
        ).isPresent()) {
            throw new IllegalStateException("Already evaluated this user for this room");
        }

        // Cannot evaluate self
        if (evaluator.getId().equals(evaluated.getId())) {
            throw new IllegalArgumentException("Cannot evaluate yourself");
        }

        // Check monthly evaluation limit (Phase 11: Anti-manipulation)
        checkMonthlyEvaluationLimit(evaluator.getId(), evaluated.getId());

        // Create evaluation
        Evaluation evaluation = Evaluation.builder()
            .room(room)
            .evaluator(evaluator)
            .evaluated(evaluated)
            .mannerScore(request.getMannerScore())
            .skillScore(request.getSkillScore())
            .communicationScore(request.getCommunicationScore())
            .comment(request.getComment())
            .build();

        evaluationRepository.save(evaluation);

        // Update temperature with evaluator credibility weight (Phase 11: Anti-manipulation)
        updateTemperatureWithWeight(evaluated.getId(), evaluation, evaluator.getId());

        return EvaluationResponse.from(evaluation);
    }

    /**
     * Get all evaluations for a room.
     *
     * @param roomId room ID
     * @return list of evaluations
     */
    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluationsByRoom(Long roomId) {
        return evaluationRepository.findByRoomId(roomId).stream()
            .map(EvaluationResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get all evaluations received by a user.
     *
     * @param userId user ID
     * @return list of evaluations
     */
    @Transactional(readOnly = true)
    public List<EvaluationResponse> getEvaluationsReceived(Long userId) {
        return evaluationRepository.findByEvaluatedId(userId).stream()
            .map(EvaluationResponse::from)
            .collect(Collectors.toList());
    }

    /**
     * Get participants that can be evaluated in a room.
     *
     * @param roomId room ID
     * @param userEmail current user email
     * @return list of user IDs that can be evaluated
     */
    @Transactional(readOnly = true)
    public List<Long> getEvaluatableParticipants(Long roomId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Room room = roomRepository.findById(roomId)
            .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        // Check if user was in the room
        participantRepository.findByRoomIdAndUserId(roomId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("User was not in this room"));

        // Get all participants except self
        List<RoomParticipant> participants = participantRepository.findByRoomId(roomId);

        return participants.stream()
            .map(p -> p.getUser().getId())
            .filter(id -> !id.equals(user.getId()))
            .collect(Collectors.toList());
    }

    /**
     * Check monthly evaluation limit to prevent manipulation.
     * Same evaluator can evaluate same user maximum 3 times per month.
     *
     * @param evaluatorId evaluator ID
     * @param evaluatedId evaluated ID
     * @throws IllegalStateException if monthly limit exceeded
     */
    private void checkMonthlyEvaluationLimit(Long evaluatorId, Long evaluatedId) {
        java.time.LocalDateTime startOfMonth = java.time.LocalDateTime.now()
            .withDayOfMonth(1)
            .withHour(0)
            .withMinute(0)
            .withSecond(0)
            .withNano(0);

        long count = evaluationRepository.countByEvaluatorAndEvaluatedInCurrentMonth(
            evaluatorId, evaluatedId, startOfMonth
        );

        if (count >= 3) {
            throw new IllegalStateException(
                "이번 달에 이미 이 사용자를 3회 평가했습니다. 다음 달에 다시 평가할 수 있습니다."
            );
        }
    }

    /**
     * Update temperature with evaluator credibility weight.
     * Higher temperature evaluators have more influence.
     *
     * @param evaluatedId evaluated user ID
     * @param evaluation evaluation
     * @param evaluatorId evaluator ID
     */
    private void updateTemperatureWithWeight(Long evaluatedId, Evaluation evaluation, Long evaluatorId) {
        User user = userRepository.findById(evaluatedId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        double averageScore = evaluation.getAverageScore();
        java.math.BigDecimal weightedDelta = temperatureService.calculateWeightedTemperatureDelta(
            averageScore, evaluatorId
        );

        user.updateTemperature(weightedDelta);
        userRepository.save(user);
    }
}
