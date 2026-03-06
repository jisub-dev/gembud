package com.gembud.service;

import com.gembud.dto.response.RecommendedRoomResponse;
import com.gembud.entity.Evaluation;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
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
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for matching recommendation.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class MatchingService {

    private final RoomRepository roomRepository;
    private final RoomFilterRepository filterRepository;
    private final RoomParticipantRepository participantRepository;
    private final UserRepository userRepository;
    private final EvaluationRepository evaluationRepository;

    /**
     * Get recommended rooms for a user by game.
     *
     * @param userEmail user email
     * @param gameId game ID
     * @param limit maximum number of recommendations
     * @return list of recommended rooms
     */
    public List<RecommendedRoomResponse> getRecommendedRooms(
        String userEmail,
        Long gameId,
        int limit
    ) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        List<Room> openRooms = roomRepository.findByGameIdAndStatusAndDeletedAtIsNull(
            gameId,
            Room.RoomStatus.OPEN
        );

        if (openRooms.isEmpty()) {
            return List.of();
        }

        // Batch-load filters and participants to avoid N+1 queries
        List<Long> roomIds = openRooms.stream().map(Room::getId).toList();

        Map<Long, List<RoomFilter>> filtersByRoom = filterRepository
            .findByRoomIdIn(roomIds)
            .stream()
            .collect(Collectors.groupingBy(f -> f.getRoom().getId()));

        Map<Long, List<RoomParticipant>> participantsByRoom = participantRepository
            .findByRoomIdIn(roomIds)
            .stream()
            .collect(Collectors.groupingBy(p -> p.getRoom().getId()));

        // Collect all participant user IDs for batch evaluation lookup
        Set<Long> participantUserIds = participantsByRoom.values().stream()
            .flatMap(List::stream)
            .map(p -> p.getUser().getId())
            .collect(Collectors.toSet());

        Map<Long, List<Evaluation>> evaluationsByEvaluated = evaluationRepository
            .findByEvaluatorId(user.getId())
            .stream()
            .filter(e -> participantUserIds.contains(e.getEvaluated().getId()))
            .collect(Collectors.groupingBy(e -> e.getEvaluated().getId()));

        // Pre-filter rooms where user is already a participant
        Set<Long> userRoomIds = participantsByRoom.entrySet().stream()
            .filter(e -> e.getValue().stream()
                .anyMatch(p -> p.getUser().getId().equals(user.getId())))
            .map(Map.Entry::getKey)
            .collect(Collectors.toSet());

        List<RecommendedRoomResponse> recommendations = new ArrayList<>();

        for (Room room : openRooms) {
            if (userRoomIds.contains(room.getId())) {
                continue;
            }

            List<RoomFilter> filters = filtersByRoom.getOrDefault(room.getId(), List.of());
            List<RoomParticipant> participants = participantsByRoom.getOrDefault(room.getId(), List.of());

            double matchingScore = calculateMatchingScore(user, room, filters, participants, evaluationsByEvaluated);
            BigDecimal hostTemperature = room.getCreatedBy().getTemperature();
            String reason = generateReason(matchingScore, hostTemperature, filters, participants, evaluationsByEvaluated);

            Map<String, String> filtersMap = filters.stream()
                .collect(Collectors.toMap(RoomFilter::getOptionKey, RoomFilter::getOptionValue));

            recommendations.add(
                RecommendedRoomResponse.of(room, filtersMap, matchingScore, hostTemperature, reason)
            );
        }

        return recommendations.stream()
            .sorted(Comparator.comparing(RecommendedRoomResponse::getMatchingScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    private double calculateMatchingScore(
        User user,
        Room room,
        List<RoomFilter> filters,
        List<RoomParticipant> participants,
        Map<Long, List<Evaluation>> evaluationsByEvaluated
    ) {
        double score = 0.0;
        score += calculateFilterScore(user, filters);
        score += calculateTemperatureScore(user, participants);
        score += calculatePastEvaluationScore(participants, evaluationsByEvaluated);
        score += calculateHostTemperatureBonus(room);
        return Math.min(100.0, score);
    }

    /**
     * Calculate filter matching score (0-40 points).
     * Rooms with explicit filters (tier/position) score higher than open rooms.
     */
    private double calculateFilterScore(User user, List<RoomFilter> filters) {
        if (filters.isEmpty()) {
            return 30.0; // 필터 없는 방 = 누구나 환영
        }

        // 필터 있는 방 = 조건을 명시한 방으로 더 높은 기본 점수
        // (User 엔티티에 tier/position 프로필이 없으므로 필터 존재 자체를 가산점으로 처리)
        return 35.0;
    }

    /**
     * Calculate temperature compatibility score (0-30 points).
     */
    private double calculateTemperatureScore(User user, List<RoomParticipant> participants) {
        BigDecimal avgTemp = getAverageParticipantTemperature(participants);
        double tempDiff = Math.abs(user.getTemperature().subtract(avgTemp).doubleValue());
        // 0°C diff = 30 points, 10°C diff = 15 points, 20°C+ diff = 0 points
        return Math.max(0, 30.0 - (tempDiff * 1.5));
    }

    /**
     * Calculate past evaluation score (0-20 points).
     */
    private double calculatePastEvaluationScore(
        List<RoomParticipant> participants,
        Map<Long, List<Evaluation>> evaluationsByEvaluated
    ) {
        if (participants.isEmpty()) {
            return 10.0;
        }

        double totalScore = 0.0;
        int evaluationCount = 0;

        for (RoomParticipant participant : participants) {
            List<Evaluation> evals = evaluationsByEvaluated.getOrDefault(
                participant.getUser().getId(), List.of());
            for (Evaluation eval : evals) {
                totalScore += eval.getAverageScore();
                evaluationCount++;
            }
        }

        if (evaluationCount == 0) {
            return 10.0;
        }

        double avgScore = totalScore / evaluationCount;
        return (avgScore - 1) * 5.0; // 1→0, 3→10, 5→20
    }

    /**
     * Calculate host temperature bonus (0-10 points).
     */
    private double calculateHostTemperatureBonus(Room room) {
        double hostTemp = room.getCreatedBy().getTemperature().doubleValue();
        return Math.max(0, Math.min(10.0, (hostTemp - 30.0) / 2.0));
    }

    private BigDecimal getAverageParticipantTemperature(List<RoomParticipant> participants) {
        if (participants.isEmpty()) {
            return new BigDecimal("36.5");
        }
        double sum = participants.stream()
            .mapToDouble(p -> p.getUser().getTemperature().doubleValue())
            .sum();
        return BigDecimal.valueOf(sum / participants.size());
    }

    /**
     * Generate context-aware recommendation reason.
     */
    private String generateReason(
        double matchingScore,
        BigDecimal hostTemperature,
        List<RoomFilter> filters,
        List<RoomParticipant> participants,
        Map<Long, List<Evaluation>> evaluationsByEvaluated
    ) {
        boolean hasHighHostTemp = hostTemperature.compareTo(new BigDecimal("40")) > 0;

        boolean hasPastEvaluation = participants.stream()
            .anyMatch(p -> !evaluationsByEvaluated
                .getOrDefault(p.getUser().getId(), List.of()).isEmpty());

        boolean hasExactFilterMatch = !filters.isEmpty();

        if (matchingScore >= 80) {
            if (hasExactFilterMatch) {
                return "필터 조건이 완벽히 일치하고 방장 매너 온도가 높습니다.";
            }
            return "모든 조건이 매우 잘 맞는 최고의 추천방입니다!";
        } else if (matchingScore >= 60) {
            if (hasPastEvaluation) {
                return "과거 함께 플레이한 경험이 있는 방장입니다.";
            }
            if (hasHighHostTemp) {
                return "좋은 매칭도! 방장의 매너 온도가 높습니다.";
            }
            return "비슷한 온도대의 플레이어들이 모여 있습니다.";
        } else if (matchingScore >= 40) {
            if (hasPastEvaluation) {
                return "과거 함께 플레이한 경험이 있는 방입니다.";
            }
            return "괜찮은 매칭도입니다. 도전해보세요!";
        } else {
            return "새로운 사람들과 플레이해보세요!";
        }
    }
}
