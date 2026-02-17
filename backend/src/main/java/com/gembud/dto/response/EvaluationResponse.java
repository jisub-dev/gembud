package com.gembud.dto.response;

import com.gembud.entity.Evaluation;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for Evaluation.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationResponse {

    private Long id;
    private Long roomId;
    private String evaluatorNickname;
    private String evaluatedNickname;
    private Integer mannerScore;
    private Integer skillScore;
    private Integer communicationScore;
    private Double averageScore;
    private String comment;
    private LocalDateTime createdAt;

    /**
     * Convert Evaluation entity to EvaluationResponse.
     *
     * @param evaluation evaluation entity
     * @return evaluation response
     */
    public static EvaluationResponse from(Evaluation evaluation) {
        return EvaluationResponse.builder()
            .id(evaluation.getId())
            .roomId(evaluation.getRoom().getId())
            .evaluatorNickname(evaluation.getEvaluator().getNickname())
            .evaluatedNickname(evaluation.getEvaluated().getNickname())
            .mannerScore(evaluation.getMannerScore())
            .skillScore(evaluation.getSkillScore())
            .communicationScore(evaluation.getCommunicationScore())
            .averageScore(evaluation.getAverageScore())
            .comment(evaluation.getComment())
            .createdAt(evaluation.getCreatedAt())
            .build();
    }
}
