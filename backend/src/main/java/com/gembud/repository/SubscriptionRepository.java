package com.gembud.repository;

import com.gembud.entity.Subscription;
import com.gembud.entity.Subscription.SubscriptionStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Subscription entity.
 *
 * @author Gembud Team
 * @since 2026-03-03
 */
@Repository
public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findTopByUserIdAndStatusOrderByExpiresAtDesc(
        Long userId, SubscriptionStatus status);

    List<Subscription> findByUserId(Long userId);

    @Query("SELECT s FROM Subscription s WHERE s.status = 'ACTIVE' AND s.expiresAt < :now")
    List<Subscription> findExpiredActiveSubscriptions(@Param("now") LocalDateTime now);
}
