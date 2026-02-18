package com.gembud.service;

import com.gembud.entity.Evaluation;
import com.gembud.entity.User;
import com.gembud.repository.EvaluationRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for temperature calculation and management.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Service
@RequiredArgsConstructor
public class TemperatureService {

    private final UserRepository userRepository;
    private final EvaluationRepository evaluationRepository;

    /**
     * Calculate evaluator credibility weight based on their temperature.
     * Higher temperature = more trustworthy evaluation.
     * - Temperature >= 40°C: 1.2x weight
     * - Temperature 30-40°C: 1.0x weight
     * - Temperature < 30°C: 0.5x weight
     *
     * @param evaluatorId evaluator user ID
     * @return weight multiplier
     */
    public double calculateEvaluatorWeight(Long evaluatorId) {
        User evaluator = userRepository.findById(evaluatorId)
            .orElseThrow(() -> new IllegalArgumentException("Evaluator not found"));

        double temperature = evaluator.getTemperature().doubleValue();

        if (temperature >= 40.0) {
            return 1.2;
        } else if (temperature < 30.0) {
            return 0.5;
        } else {
            return 1.0;
        }
    }

    /**
     * Calculate temperature delta from evaluation score.
     * Score 1-2: -0.5°C
     * Score 3: 0°C
     * Score 4-5: +0.5°C
     *
     * @param averageScore average score (1-5)
     * @return temperature delta
     */
    public BigDecimal calculateTemperatureDelta(double averageScore) {
        if (averageScore < 2.5) {
            return new BigDecimal("-0.5");
        } else if (averageScore > 3.5) {
            return new BigDecimal("0.5");
        } else {
            return BigDecimal.ZERO;
        }
    }

    /**
     * Calculate temperature delta with evaluator credibility weight applied.
     *
     * @param averageScore average evaluation score (1-5)
     * @param evaluatorId evaluator user ID
     * @return weighted temperature delta
     */
    public BigDecimal calculateWeightedTemperatureDelta(double averageScore, Long evaluatorId) {
        BigDecimal baseDelta = calculateTemperatureDelta(averageScore);
        double weight = calculateEvaluatorWeight(evaluatorId);

        return baseDelta.multiply(new BigDecimal(weight))
            .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Update user temperature based on new evaluation.
     *
     * @param userId user ID
     * @param evaluation new evaluation
     */
    @Transactional
    public void updateTemperatureFromEvaluation(Long userId, Evaluation evaluation) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        double averageScore = evaluation.getAverageScore();
        BigDecimal delta = calculateTemperatureDelta(averageScore);

        user.updateTemperature(delta);
        userRepository.save(user);
    }

    /**
     * Get user's average received evaluation score.
     *
     * @param userId user ID
     * @return average score
     */
    @Transactional(readOnly = true)
    public double getAverageEvaluationScore(Long userId) {
        List<Evaluation> evaluations = evaluationRepository.findByEvaluatedId(userId);

        if (evaluations.isEmpty()) {
            return 3.0; // Default neutral score
        }

        return evaluations.stream()
            .mapToDouble(Evaluation::getAverageScore)
            .average()
            .orElse(3.0);
    }

    /**
     * Check if user can create a room based on temperature.
     * Users with temperature < 30°C cannot create rooms.
     *
     * @param userId user ID
     * @return true if user can create room
     */
    @Transactional(readOnly = true)
    public boolean canCreateRoom(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getTemperature().compareTo(new BigDecimal("30")) >= 0;
    }

    /**
     * Get user's current temperature.
     *
     * @param userId user ID
     * @return current temperature
     */
    @Transactional(readOnly = true)
    public BigDecimal getUserTemperature(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        return user.getTemperature();
    }

    /**
     * Calculate temperature statistics for user.
     *
     * @param userId user ID
     * @return temperature stats
     */
    @Transactional(readOnly = true)
    public TemperatureStats getTemperatureStats(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        List<Evaluation> evaluations = evaluationRepository.findByEvaluatedId(userId);

        long totalEvaluations = evaluations.size();
        double averageScore = getAverageEvaluationScore(userId);

        long positiveEvaluations = evaluations.stream()
            .filter(e -> e.getAverageScore() > 3.5)
            .count();

        long negativeEvaluations = evaluations.stream()
            .filter(e -> e.getAverageScore() < 2.5)
            .count();

        return TemperatureStats.builder()
            .currentTemperature(user.getTemperature())
            .totalEvaluations(totalEvaluations)
            .averageScore(BigDecimal.valueOf(averageScore).setScale(2, RoundingMode.HALF_UP))
            .positiveEvaluations(positiveEvaluations)
            .negativeEvaluations(negativeEvaluations)
            .build();
    }

    /**
     * Temperature statistics DTO.
     */
    @lombok.Builder
    @lombok.Getter
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TemperatureStats {
        private BigDecimal currentTemperature;
        private Long totalEvaluations;
        private BigDecimal averageScore;
        private Long positiveEvaluations;
        private Long negativeEvaluations;
    }
}
