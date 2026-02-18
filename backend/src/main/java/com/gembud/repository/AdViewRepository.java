package com.gembud.repository;

import com.gembud.entity.AdView;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for AdView entity (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Repository
public interface AdViewRepository extends JpaRepository<AdView, Long> {

    /**
     * Count ad views by user since specified time (Phase 11: 1-day 3x limit).
     *
     * @param userId user ID
     * @param since since time
     * @return view count
     */
    @Query("SELECT COUNT(v) FROM AdView v " +
           "WHERE v.user.id = :userId " +
           "AND v.viewedAt >= :since")
    long countByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
