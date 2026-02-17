package com.gembud.repository;

import com.gembud.entity.Evaluation;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Evaluation entity.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Repository
public interface EvaluationRepository extends JpaRepository<Evaluation, Long> {

    /**
     * Find all evaluations for a room.
     *
     * @param roomId room ID
     * @return list of evaluations
     */
    List<Evaluation> findByRoomId(Long roomId);

    /**
     * Find all evaluations received by a user.
     *
     * @param userId user ID
     * @return list of evaluations
     */
    @Query("SELECT e FROM Evaluation e WHERE e.evaluated.id = :userId")
    List<Evaluation> findByEvaluatedId(@Param("userId") Long userId);

    /**
     * Find all evaluations given by a user.
     *
     * @param userId user ID
     * @return list of evaluations
     */
    @Query("SELECT e FROM Evaluation e WHERE e.evaluator.id = :userId")
    List<Evaluation> findByEvaluatorId(@Param("userId") Long userId);

    /**
     * Check if evaluation exists.
     *
     * @param roomId room ID
     * @param evaluatorId evaluator ID
     * @param evaluatedId evaluated ID
     * @return optional evaluation
     */
    @Query("SELECT e FROM Evaluation e WHERE e.room.id = :roomId AND e.evaluator.id = :evaluatorId AND e.evaluated.id = :evaluatedId")
    Optional<Evaluation> findByRoomIdAndEvaluatorIdAndEvaluatedId(
        @Param("roomId") Long roomId,
        @Param("evaluatorId") Long evaluatorId,
        @Param("evaluatedId") Long evaluatedId
    );

    /**
     * Count evaluations received by a user.
     *
     * @param userId user ID
     * @return count
     */
    @Query("SELECT COUNT(e) FROM Evaluation e WHERE e.evaluated.id = :userId")
    long countByEvaluatedId(@Param("userId") Long userId);

    /**
     * Find evaluations given by evaluator to evaluated user.
     *
     * @param evaluatorId evaluator ID
     * @param evaluatedId evaluated ID
     * @return list of evaluations
     */
    @Query("SELECT e FROM Evaluation e WHERE e.evaluator.id = :evaluatorId AND e.evaluated.id = :evaluatedId")
    List<Evaluation> findByEvaluatorIdAndEvaluatedId(
        @Param("evaluatorId") Long evaluatorId,
        @Param("evaluatedId") Long evaluatedId
    );
}
