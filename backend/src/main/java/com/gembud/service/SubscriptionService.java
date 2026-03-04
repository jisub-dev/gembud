package com.gembud.service;

import com.gembud.dto.response.SubscriptionStatusResponse;
import com.gembud.entity.Subscription;
import com.gembud.entity.Subscription.SubscriptionStatus;
import com.gembud.entity.User;
import com.gembud.repository.SubscriptionRepository;
import com.gembud.repository.UserRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for managing premium subscriptions.
 *
 * @author Gembud Team
 * @since 2026-03-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    /**
     * Activate premium subscription for user (admin/test use).
     *
     * @param userId user ID
     * @param months number of months
     */
    @Transactional
    public SubscriptionStatusResponse activatePremium(Long userId, int months) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusMonths(months);

        // Cancel any existing active subscription
        Optional<Subscription> existing = subscriptionRepository
            .findTopByUserIdAndStatusOrderByExpiresAtDesc(userId, SubscriptionStatus.ACTIVE);
        existing.ifPresent(Subscription::cancel);

        Subscription subscription = Subscription.builder()
            .user(user)
            .startedAt(now)
            .expiresAt(expiresAt)
            .amount(2900)
            .status(SubscriptionStatus.ACTIVE)
            .paymentMethod("ADMIN")
            .build();

        subscriptionRepository.save(subscription);
        user.activatePremium(expiresAt);
        userRepository.save(user);

        return buildStatusResponse(user, subscription);
    }

    /**
     * Cancel premium subscription for user.
     *
     * @param userId user ID
     */
    @Transactional
    public SubscriptionStatusResponse cancelPremium(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        subscriptionRepository
            .findTopByUserIdAndStatusOrderByExpiresAtDesc(userId, SubscriptionStatus.ACTIVE)
            .ifPresent(Subscription::cancel);

        user.deactivatePremium();
        userRepository.save(user);

        return SubscriptionStatusResponse.builder()
            .isPremium(false)
            .premiumExpiresAt(null)
            .subscriptionStatus(null)
            .build();
    }

    /**
     * Get subscription status for user.
     * Auto-deactivates premium if premiumExpiresAt is in the past but flag is still set.
     *
     * @param userId user ID
     * @return subscription status
     */
    @Transactional
    public SubscriptionStatusResponse getStatus(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));

        Optional<Subscription> active = subscriptionRepository
            .findTopByUserIdAndStatusOrderByExpiresAtDesc(userId, SubscriptionStatus.ACTIVE);

        if (active.isPresent()) {
            return buildStatusResponse(user, active.get());
        }

        // Auto-deactivate if DB flag is set but expiry has passed (data inconsistency guard)
        if (user.getPremiumExpiresAt() != null
                && user.getPremiumExpiresAt().isBefore(LocalDateTime.now())) {
            user.deactivatePremium();
            userRepository.save(user);
        }

        return SubscriptionStatusResponse.builder()
            .isPremium(user.isPremium())
            .premiumExpiresAt(user.getPremiumExpiresAt())
            .subscriptionStatus(null)
            .build();
    }

    /**
     * Expire stale subscriptions (runs daily at midnight).
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void expireStaleSubscriptions() {
        List<Subscription> expired = subscriptionRepository
            .findExpiredActiveSubscriptions(LocalDateTime.now());

        for (Subscription sub : expired) {
            sub.expire();
            User user = sub.getUser();
            user.deactivatePremium();
            userRepository.save(user);
        }

        if (!expired.isEmpty()) {
            log.info("Expired {} stale subscriptions", expired.size());
        }
    }

    private SubscriptionStatusResponse buildStatusResponse(User user, Subscription subscription) {
        return SubscriptionStatusResponse.builder()
            .isPremium(user.isPremium())
            .premiumExpiresAt(user.getPremiumExpiresAt())
            .subscriptionStatus(subscription.getStatus())
            .startedAt(subscription.getStartedAt())
            .amount(subscription.getAmount())
            .build();
    }
}
