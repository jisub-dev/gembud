package com.gembud.dto.response;

import com.gembud.entity.Subscription.SubscriptionStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for subscription status.
 *
 * @author Gembud Team
 * @since 2026-03-03
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionStatusResponse {

    private boolean isPremium;
    private LocalDateTime premiumExpiresAt;
    private SubscriptionStatus subscriptionStatus;
    private LocalDateTime startedAt;
    private int amount;
}
