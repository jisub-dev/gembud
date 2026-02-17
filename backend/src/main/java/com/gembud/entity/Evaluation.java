package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Evaluation entity for user ratings after room completion.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Entity
@Table(
    name = "evaluations",
    uniqueConstraints = @UniqueConstraint(
        columnNames = {"room_id", "evaluator_id", "evaluated_id"}
    )
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Evaluation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluator_id", nullable = false)
    private User evaluator;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "evaluated_id", nullable = false)
    private User evaluated;

    @Column(name = "manner_score", nullable = false)
    private Integer mannerScore;

    @Column(name = "skill_score", nullable = false)
    private Integer skillScore;

    @Column(name = "communication_score", nullable = false)
    private Integer communicationScore;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Calculate average score.
     *
     * @return average score (1-5)
     */
    public double getAverageScore() {
        return (mannerScore + skillScore + communicationScore) / 3.0;
    }
}
