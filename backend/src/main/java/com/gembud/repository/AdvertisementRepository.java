package com.gembud.repository;

import com.gembud.entity.Advertisement;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Advertisement entity (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Repository
public interface AdvertisementRepository extends JpaRepository<Advertisement, Long> {

    /**
     * Find active gaming-related ads ordered by display order.
     *
     * @param now current time
     * @return list of ads
     */
    @Query("SELECT a FROM Advertisement a " +
           "WHERE a.isActive = true " +
           "AND a.isGamingRelated = true " +
           "AND (a.expiresAt IS NULL OR a.expiresAt > :now) " +
           "ORDER BY a.displayOrder ASC, a.createdAt DESC")
    List<Advertisement> findActiveGamingAds(@Param("now") LocalDateTime now);
}
