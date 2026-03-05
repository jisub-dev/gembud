package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.Evaluation;
import com.gembud.exception.BusinessException;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.repository.EvaluationRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Tests for TemperatureService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class TemperatureServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private EvaluationRepository evaluationRepository;

    @InjectMocks
    private TemperatureService temperatureService;

    private User testUser;
    private Evaluation positiveEvaluation;
    private Evaluation neutralEvaluation;
    private Evaluation negativeEvaluation;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .email("test@example.com")
            .nickname("TestUser")
            .temperature(new BigDecimal("36.5"))
            .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        // Average score > 3.5 (positive)
        positiveEvaluation = Evaluation.builder()
            .id(1L)
            .mannerScore(5)
            .skillScore(4)
            .communicationScore(5)
            .build();

        // Average score between 2.5 and 3.5 (neutral)
        neutralEvaluation = Evaluation.builder()
            .id(2L)
            .mannerScore(3)
            .skillScore(3)
            .communicationScore(3)
            .build();

        // Average score < 2.5 (negative)
        negativeEvaluation = Evaluation.builder()
            .id(3L)
            .mannerScore(2)
            .skillScore(1)
            .communicationScore(2)
            .build();
    }

    @Test
    @DisplayName("calculateTemperatureDelta - should return +0.5 for high average score")
    void calculateTemperatureDelta_HighScore_ShouldReturnPositiveDelta() {
        // Given
        double highScore = 4.5;

        // When
        BigDecimal delta = temperatureService.calculateTemperatureDelta(highScore);

        // Then
        assertThat(delta).isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("calculateTemperatureDelta - should return -0.5 for low average score")
    void calculateTemperatureDelta_LowScore_ShouldReturnNegativeDelta() {
        // Given
        double lowScore = 2.0;

        // When
        BigDecimal delta = temperatureService.calculateTemperatureDelta(lowScore);

        // Then
        assertThat(delta).isEqualByComparingTo("-0.5");
    }

    @Test
    @DisplayName("calculateTemperatureDelta - should return 0 for neutral average score")
    void calculateTemperatureDelta_NeutralScore_ShouldReturnZero() {
        // Given
        double neutralScore = 3.0;

        // When
        BigDecimal delta = temperatureService.calculateTemperatureDelta(neutralScore);

        // Then
        assertThat(delta).isEqualByComparingTo("0");
    }

    @Test
    @DisplayName("calculateTemperatureDelta - boundary test at 2.5")
    void calculateTemperatureDelta_BoundaryAt2Point5() {
        // Exactly 2.5 should return 0
        assertThat(temperatureService.calculateTemperatureDelta(2.5))
            .isEqualByComparingTo("0");

        // Just below 2.5 should return -0.5
        assertThat(temperatureService.calculateTemperatureDelta(2.4))
            .isEqualByComparingTo("-0.5");
    }

    @Test
    @DisplayName("calculateTemperatureDelta - boundary test at 3.5")
    void calculateTemperatureDelta_BoundaryAt3Point5() {
        // Exactly 3.5 should return 0
        assertThat(temperatureService.calculateTemperatureDelta(3.5))
            .isEqualByComparingTo("0");

        // Just above 3.5 should return +0.5
        assertThat(temperatureService.calculateTemperatureDelta(3.6))
            .isEqualByComparingTo("0.5");
    }

    @Test
    @DisplayName("updateTemperatureFromEvaluation - should increase temperature for positive evaluation")
    void updateTemperatureFromEvaluation_PositiveEvaluation_ShouldIncreaseTemperature() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        temperatureService.updateTemperatureFromEvaluation(1L, positiveEvaluation);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
        // Temperature should be updated (36.5 + 0.5 = 37.0)
        assertThat(testUser.getTemperature()).isEqualByComparingTo("37.0");
    }

    @Test
    @DisplayName("updateTemperatureFromEvaluation - should decrease temperature for negative evaluation")
    void updateTemperatureFromEvaluation_NegativeEvaluation_ShouldDecreaseTemperature() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        temperatureService.updateTemperatureFromEvaluation(1L, negativeEvaluation);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
        // Temperature should be updated (36.5 - 0.5 = 36.0)
        assertThat(testUser.getTemperature()).isEqualByComparingTo("36.0");
    }

    @Test
    @DisplayName("updateTemperatureFromEvaluation - should not change temperature for neutral evaluation")
    void updateTemperatureFromEvaluation_NeutralEvaluation_ShouldNotChangeTemperature() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        temperatureService.updateTemperatureFromEvaluation(1L, neutralEvaluation);

        // Then
        verify(userRepository, times(1)).save(any(User.class));
        // Temperature should remain the same
        assertThat(testUser.getTemperature()).isEqualByComparingTo("36.5");
    }

    @Test
    @DisplayName("updateTemperatureFromEvaluation - should throw exception when user not found")
    void updateTemperatureFromEvaluation_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            temperatureService.updateTemperatureFromEvaluation(999L, positiveEvaluation))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getAverageEvaluationScore - should return average of all evaluations")
    void getAverageEvaluationScore_ShouldReturnAverage() {
        // Given
        when(evaluationRepository.findByEvaluatedId(1L))
            .thenReturn(Arrays.asList(positiveEvaluation, neutralEvaluation, negativeEvaluation));

        // When
        double average = temperatureService.getAverageEvaluationScore(1L);

        // Then
        // (4.67 + 3.0 + 1.67) / 3 ≈ 3.11
        assertThat(average).isBetween(3.0, 3.2);
    }

    @Test
    @DisplayName("getAverageEvaluationScore - should return 3.0 when no evaluations exist")
    void getAverageEvaluationScore_NoEvaluations_ShouldReturnDefault() {
        // Given
        when(evaluationRepository.findByEvaluatedId(1L))
            .thenReturn(Collections.emptyList());

        // When
        double average = temperatureService.getAverageEvaluationScore(1L);

        // Then
        assertThat(average).isEqualTo(3.0);
    }

    @Test
    @DisplayName("canCreateRoom - should return true when temperature >= 30")
    void canCreateRoom_TemperatureAbove30_ShouldReturnTrue() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        boolean canCreate = temperatureService.canCreateRoom(1L);

        // Then
        assertThat(canCreate).isTrue();
    }

    @Test
    @DisplayName("canCreateRoom - should return false when temperature < 30")
    void canCreateRoom_TemperatureBelow30_ShouldReturnFalse() {
        // Given
        User lowTempUser = User.builder()
            .email("lowtemp@example.com")
            .temperature(new BigDecimal("25.0"))
            .build();
        ReflectionTestUtils.setField(lowTempUser, "id", 2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(lowTempUser));

        // When
        boolean canCreate = temperatureService.canCreateRoom(2L);

        // Then
        assertThat(canCreate).isFalse();
    }

    @Test
    @DisplayName("canCreateRoom - boundary test at exactly 30")
    void canCreateRoom_ExactlyAt30_ShouldReturnTrue() {
        // Given
        User boundaryUser = User.builder()
            .email("boundary@example.com")
            .temperature(new BigDecimal("30.0"))
            .build();
        ReflectionTestUtils.setField(boundaryUser, "id", 3L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(boundaryUser));

        // When
        boolean canCreate = temperatureService.canCreateRoom(3L);

        // Then
        assertThat(canCreate).isTrue();
    }

    @Test
    @DisplayName("canCreateRoom - should throw exception when user not found")
    void canCreateRoom_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> temperatureService.canCreateRoom(999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getUserTemperature - should return user's current temperature")
    void getUserTemperature_ShouldReturnTemperature() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When
        BigDecimal temperature = temperatureService.getUserTemperature(1L);

        // Then
        assertThat(temperature).isEqualByComparingTo("36.5");
    }

    @Test
    @DisplayName("getUserTemperature - should throw exception when user not found")
    void getUserTemperature_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> temperatureService.getUserTemperature(999L))
            .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getTemperatureStats - should return comprehensive statistics")
    void getTemperatureStats_ShouldReturnStats() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(evaluationRepository.findByEvaluatedId(1L))
            .thenReturn(Arrays.asList(positiveEvaluation, neutralEvaluation, negativeEvaluation));

        // When
        TemperatureService.TemperatureStats stats =
            temperatureService.getTemperatureStats(1L);

        // Then
        assertThat(stats.getCurrentTemperature()).isEqualByComparingTo("36.5");
        assertThat(stats.getTotalEvaluations()).isEqualTo(3);
        assertThat(stats.getPositiveEvaluations()).isEqualTo(1);
        assertThat(stats.getNegativeEvaluations()).isEqualTo(1);
        assertThat(stats.getAverageScore()).isNotNull();
    }

    @Test
    @DisplayName("getTemperatureStats - should handle no evaluations")
    void getTemperatureStats_NoEvaluations_ShouldReturnStatsWithDefaults() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(evaluationRepository.findByEvaluatedId(1L))
            .thenReturn(Collections.emptyList());

        // When
        TemperatureService.TemperatureStats stats =
            temperatureService.getTemperatureStats(1L);

        // Then
        assertThat(stats.getCurrentTemperature()).isEqualByComparingTo("36.5");
        assertThat(stats.getTotalEvaluations()).isEqualTo(0);
        assertThat(stats.getPositiveEvaluations()).isEqualTo(0);
        assertThat(stats.getNegativeEvaluations()).isEqualTo(0);
        assertThat(stats.getAverageScore()).isEqualByComparingTo("3.00");
    }

    @Test
    @DisplayName("getTemperatureStats - should throw exception when user not found")
    void getTemperatureStats_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> temperatureService.getTemperatureStats(999L))
            .isInstanceOf(BusinessException.class);
    }
}
