package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.dto.response.SubscriptionStatusResponse;
import com.gembud.entity.Subscription;
import com.gembud.entity.Subscription.SubscriptionStatus;
import com.gembud.entity.User;
import com.gembud.repository.SubscriptionRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Unit tests for SubscriptionService.
 *
 * @author Gembud Team
 * @since 2026-03-04
 */
@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SubscriptionService subscriptionService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder()
            .email("user@example.com")
            .nickname("TestUser")
            .temperature(new BigDecimal("36.5"))
            .build();
    }

    // ──────────────────────────────────────────────
    // activatePremium
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("activatePremium - should activate premium and save subscription")
    void activatePremium_Valid_ShouldActivate() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        SubscriptionStatusResponse response = subscriptionService.activatePremium(1L, 1);

        // Then
        assertThat(response.isPremium()).isTrue();
        verify(subscriptionRepository, times(1)).save(any(Subscription.class));
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("activatePremium - should cancel existing active subscription before creating new one")
    void activatePremium_ExistingActive_ShouldCancelFirst() {
        // Given
        Subscription existingSub = Subscription.builder()
            .user(user)
            .startedAt(LocalDateTime.now().minusDays(10))
            .expiresAt(LocalDateTime.now().plusDays(20))
            .amount(2900)
            .status(SubscriptionStatus.ACTIVE)
            .paymentMethod("ADMIN")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.of(existingSub));
        when(subscriptionRepository.save(any(Subscription.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        subscriptionService.activatePremium(1L, 1);

        // Then: existing subscription should be cancelled
        assertThat(existingSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
    }

    @Test
    @DisplayName("activatePremium - should throw when user not found")
    void activatePremium_UserNotFound_ShouldThrow() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> subscriptionService.activatePremium(99L, 1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    // ──────────────────────────────────────────────
    // cancelPremium
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("cancelPremium - should cancel subscription and deactivate premium")
    void cancelPremium_Active_ShouldCancel() {
        // Given
        user.activatePremium(LocalDateTime.now().plusDays(30));
        Subscription activeSub = Subscription.builder()
            .user(user)
            .startedAt(LocalDateTime.now().minusDays(1))
            .expiresAt(LocalDateTime.now().plusDays(30))
            .amount(2900)
            .status(SubscriptionStatus.ACTIVE)
            .paymentMethod("ADMIN")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.of(activeSub));
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        SubscriptionStatusResponse response = subscriptionService.cancelPremium(1L);

        // Then
        assertThat(response.isPremium()).isFalse();
        assertThat(activeSub.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(userRepository, times(1)).save(user);
    }

    // ──────────────────────────────────────────────
    // getStatus
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("getStatus - should return active premium status")
    void getStatus_ActivePremium_ShouldReturnPremium() {
        // Given
        user.activatePremium(LocalDateTime.now().plusDays(30));
        Subscription activeSub = Subscription.builder()
            .user(user)
            .startedAt(LocalDateTime.now().minusDays(1))
            .expiresAt(LocalDateTime.now().plusDays(30))
            .amount(2900)
            .status(SubscriptionStatus.ACTIVE)
            .paymentMethod("ADMIN")
            .build();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.of(activeSub));

        // When
        SubscriptionStatusResponse response = subscriptionService.getStatus(1L);

        // Then
        assertThat(response.isPremium()).isTrue();
        assertThat(response.getSubscriptionStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    }

    @Test
    @DisplayName("getStatus - should auto-deactivate when premiumExpiresAt is in the past and no active subscription")
    void getStatus_ExpiredNoSubscription_ShouldAutoDeactivate() {
        // Given: user has premium flag set but expiry is past, no active subscription record
        user.activatePremium(LocalDateTime.now().minusDays(1));

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(user);

        // When
        SubscriptionStatusResponse response = subscriptionService.getStatus(1L);

        // Then: premium should be deactivated
        assertThat(user.isPremium()).isFalse();
        assertThat(response.isPremium()).isFalse();
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("getStatus - non-premium user should return non-premium status")
    void getStatus_NonPremium_ShouldReturnNonPremium() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(subscriptionRepository.findTopByUserIdAndStatusOrderByExpiresAtDesc(1L, SubscriptionStatus.ACTIVE))
            .thenReturn(Optional.empty());

        // When
        SubscriptionStatusResponse response = subscriptionService.getStatus(1L);

        // Then
        assertThat(response.isPremium()).isFalse();
        verify(userRepository, never()).save(any());
    }

    // ──────────────────────────────────────────────
    // expireStaleSubscriptions (scheduler)
    // ──────────────────────────────────────────────

    @Test
    @DisplayName("expireStaleSubscriptions - should expire subscription and deactivate premium")
    void expireStaleSubscriptions_ExpiredSub_ShouldDeactivate() {
        // Given
        user.activatePremium(LocalDateTime.now().minusDays(1));
        Subscription expiredSub = Subscription.builder()
            .user(user)
            .startedAt(LocalDateTime.now().minusDays(31))
            .expiresAt(LocalDateTime.now().minusDays(1))
            .amount(2900)
            .status(SubscriptionStatus.ACTIVE)
            .paymentMethod("ADMIN")
            .build();

        when(subscriptionRepository.findExpiredActiveSubscriptions(any(LocalDateTime.class)))
            .thenReturn(List.of(expiredSub));

        // When
        subscriptionService.expireStaleSubscriptions();

        // Then
        assertThat(expiredSub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        assertThat(user.isPremium()).isFalse();
        verify(userRepository, times(1)).save(user);
    }

    @Test
    @DisplayName("expireStaleSubscriptions - should do nothing when no expired subscriptions")
    void expireStaleSubscriptions_NoneExpired_ShouldDoNothing() {
        // Given
        when(subscriptionRepository.findExpiredActiveSubscriptions(any(LocalDateTime.class)))
            .thenReturn(Collections.emptyList());

        // When
        subscriptionService.expireStaleSubscriptions();

        // Then
        verify(userRepository, never()).save(any());
    }
}
