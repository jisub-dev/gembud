package com.gembud.service;

import com.gembud.dto.response.RecommendedRoomResponse;
import com.gembud.entity.Evaluation;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        // Get all open rooms for this game
        List<Room> openRooms = roomRepository.findByGameIdAndStatus(
            gameId,
            Room.RoomStatus.OPEN
        );

        // Calculate matching score for each room
        List<RecommendedRoomResponse> recommendations = new ArrayList<>();

        for (Room room : openRooms) {
            // Skip if user is already in the room
            if (participantRepository.findByRoomIdAndUserId(room.getId(), user.getId()).isPresent()) {
                continue;
            }

            // Calculate matching score
            double matchingScore = calculateMatchingScore(user, room);

            // Get host temperature
            BigDecimal hostTemperature = room.getCreatedBy().getTemperature();

            // Generate reason
            String reason = generateReason(matchingScore, hostTemperature);

            recommendations.add(
                RecommendedRoomResponse.of(room, matchingScore, hostTemperature, reason)
            );
        }

        // Sort by matching score (descending) and return top N
        return recommendations.stream()
            .sorted(Comparator.comparing(RecommendedRoomResponse::getMatchingScore).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * Calculate matching score between user and room.
     * Score components:
     * - Filter matching: 40 points
     * - Temperature compatibility: 30 points
     * - Past evaluations: 20 points
     * - Host temperature bonus: 10 points
     *
     * @param user user
     * @param room room
     * @return matching score (0-100)
     */
    private double calculateMatchingScore(User user, Room room) {
        double score = 0.0;

        // 1. Filter matching (40 points)
        score += calculateFilterScore(user, room);

        // 2. Temperature compatibility (30 points)
        score += calculateTemperatureScore(user, room);

        // 3. Past evaluations (20 points)
        score += calculatePastEvaluationScore(user, room);

        // 4. Host temperature bonus (10 points)
        score += calculateHostTemperatureBonus(room);

        return Math.min(100.0, score);
    }

    /**
     * Calculate filter matching score.
     *
     * @param user user
     * @param room room
     * @return filter score (0-40)
     */
    private double calculateFilterScore(User user, Room room) {
        List<RoomFilter> filters = filterRepository.findByRoomId(room.getId());

        if (filters.isEmpty()) {
            return 40.0; // No filters = all users match
        }

        int totalFilters = filters.size();
        int matchedFilters = 0;

        for (RoomFilter filter : filters) {
            // Age range matching
            if ("ageRange".equals(filter.getOptionKey())) {
                if (user.getAgeRange() != null &&
                    user.getAgeRange().equals(filter.getOptionValue())) {
                    matchedFilters++;
                }
            }
            // Add more filter matching logic as needed
            // For MVP, we'll keep it simple
        }

        return (40.0 * matchedFilters) / totalFilters;
    }

    /**
     * Calculate temperature compatibility score.
     *
     * @param user user
     * @param room room
     * @return temperature score (0-30)
     */
    private double calculateTemperatureScore(User user, Room room) {
        BigDecimal userTemp = user.getTemperature();
        BigDecimal avgParticipantTemp = getAverageParticipantTemperature(room);

        // Calculate temperature difference
        double tempDiff = Math.abs(userTemp.subtract(avgParticipantTemp).doubleValue());

        // Score decreases as temperature difference increases
        // 0°C diff = 30 points, 10°C diff = 15 points, 20°C+ diff = 0 points
        double score = Math.max(0, 30.0 - (tempDiff * 1.5));

        return score;
    }

    /**
     * Calculate past evaluation score.
     *
     * @param user user
     * @param room room
     * @return past evaluation score (0-20)
     */
    private double calculatePastEvaluationScore(User user, Room room) {
        // Get room participants
        List<RoomParticipant> participants = participantRepository.findByRoomId(room.getId());

        if (participants.isEmpty()) {
            return 10.0; // Neutral score for empty rooms
        }

        double totalScore = 0.0;
        int evaluationCount = 0;

        for (RoomParticipant participant : participants) {
            Long participantId = participant.getUser().getId();

            // Find past evaluations between user and this participant
            List<Evaluation> evaluations = evaluationRepository
                .findByEvaluatorIdAndEvaluatedId(user.getId(), participantId);

            for (Evaluation eval : evaluations) {
                totalScore += eval.getAverageScore();
                evaluationCount++;
            }
        }

        if (evaluationCount == 0) {
            return 10.0; // Neutral score for no past evaluations
        }

        // Average score: 1-5 → Scale to 0-20
        double avgScore = totalScore / evaluationCount;
        return (avgScore - 1) * 5.0; // 1→0, 3→10, 5→20
    }

    /**
     * Calculate host temperature bonus.
     *
     * @param room room
     * @return host temperature bonus (0-10)
     */
    private double calculateHostTemperatureBonus(Room room) {
        BigDecimal hostTemp = room.getCreatedBy().getTemperature();

        // 36.5°C (default) = 5 points
        // 50°C+ = 10 points
        // 30°C- = 0 points
        double bonus = (hostTemp.doubleValue() - 30.0) / 2.0;
        return Math.max(0, Math.min(10.0, bonus));
    }

    /**
     * Get average participant temperature in a room.
     *
     * @param room room
     * @return average temperature
     */
    private BigDecimal getAverageParticipantTemperature(Room room) {
        List<RoomParticipant> participants = participantRepository.findByRoomId(room.getId());

        if (participants.isEmpty()) {
            return new BigDecimal("36.5"); // Default temperature
        }

        double sum = participants.stream()
            .mapToDouble(p -> p.getUser().getTemperature().doubleValue())
            .sum();

        return BigDecimal.valueOf(sum / participants.size());
    }

    /**
     * Generate recommendation reason.
     *
     * @param matchingScore matching score
     * @param hostTemperature host temperature
     * @return reason string
     */
    private String generateReason(double matchingScore, BigDecimal hostTemperature) {
        if (matchingScore >= 80) {
            return "매우 높은 매칭도! 조건이 잘 맞습니다.";
        } else if (matchingScore >= 60) {
            if (hostTemperature.compareTo(new BigDecimal("40")) > 0) {
                return "좋은 매칭도! 방장의 온도가 높습니다.";
            }
            return "좋은 매칭도! 조건이 맞습니다.";
        } else if (matchingScore >= 40) {
            return "괜찮은 매칭도입니다.";
        } else {
            return "새로운 사람들과 플레이해보세요!";
        }
    }
}
