package com.gembud.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating an evaluation.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationRequest {

    @NotNull(message = "Evaluated user ID is required")
    private Long evaluatedId;

    @NotNull(message = "Manner score is required")
    @Min(value = 1, message = "Manner score must be at least 1")
    @Max(value = 5, message = "Manner score must be at most 5")
    private Integer mannerScore;

    @NotNull(message = "Skill score is required")
    @Min(value = 1, message = "Skill score must be at least 1")
    @Max(value = 5, message = "Skill score must be at most 5")
    private Integer skillScore;

    @NotNull(message = "Communication score is required")
    @Min(value = 1, message = "Communication score must be at least 1")
    @Max(value = 5, message = "Communication score must be at most 5")
    private Integer communicationScore;

    private String comment;
}
