package com.gembud.repository;

import com.gembud.entity.SecurityEvent;
import com.gembud.entity.SecurityEvent.EventType;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for SecurityEvent entity.
 *
 * @author Gembud Team
 * @since 2026-03-05
 */
@Repository
public interface SecurityEventRepository
        extends JpaRepository<SecurityEvent, Long>, JpaSpecificationExecutor<SecurityEvent> {

    long countByEventTypeAndCreatedAtAfter(EventType eventType, LocalDateTime after);

    @Query("""
        SELECT e FROM SecurityEvent e
        WHERE (:eventType IS NULL OR e.eventType = :eventType)
          AND (:riskScore IS NULL OR e.riskScore = :riskScore)
          AND (:from IS NULL OR e.createdAt >= :from)
          AND (:to IS NULL OR e.createdAt <= :to)
        ORDER BY e.createdAt DESC
        """)
    Page<SecurityEvent> search(
        @Param("eventType") EventType eventType,
        @Param("riskScore") String riskScore,
        @Param("from") LocalDateTime from,
        @Param("to") LocalDateTime to,
        Pageable pageable
    );

    @Modifying
    @Query("DELETE FROM SecurityEvent e WHERE e.createdAt < :before")
    void deleteByCreatedAtBefore(@Param("before") LocalDateTime before);
}
