package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * User entity representing user accounts with authentication and profile information
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 255)
    private String email;

    @Column(length = 255)
    private String password;

    @Column(nullable = false, length = 50)
    private String nickname;

    @Column(name = "profile_image_url", length = 500)
    private String profileImageUrl;

    @Column(name = "age_range", length = 20)
    private String ageRange;

    /**
     * User reputation score (0-100), default 36.5 like Karrot Market temperature system
     */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal temperature = new BigDecimal("36.5");

    @Enumerated(EnumType.STRING)
    @Column(name = "oauth_provider", length = 20)
    private OAuthProvider oauthProvider;

    @Column(name = "oauth_id", length = 255)
    private String oauthId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * User suspension expiration time (Phase 11: Auto-sanction).
     * Null if not suspended.
     */
    @Column(name = "suspended_until")
    private LocalDateTime suspendedUntil;


    /**
     * Login lock expiration time (Phase 3: Brute-force protection).
     * Null if not locked.
     */
    @Column(name = "login_locked_until")
    private LocalDateTime loginLockedUntil;

    @Column(name = "last_nickname_changed_at")
    private LocalDateTime lastNicknameChangedAt;

    @Column(name = "is_premium", nullable = false)
    private boolean premium = false;

    @Column(name = "premium_expires_at")
    private LocalDateTime premiumExpiresAt;

    /**
     * User role (Phase 12: ADMIN separation).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private UserRole role = UserRole.USER;

    @Builder
    public User(String email, String password, String nickname, String profileImageUrl,
                String ageRange, BigDecimal temperature, OAuthProvider oauthProvider,
                String oauthId, UserRole role) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.ageRange = ageRange;
        this.temperature = temperature != null ? temperature : new BigDecimal("36.5");
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
        this.role = role != null ? role : UserRole.USER;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * Set user role (Phase 12: ADMIN promotion).
     *
     * @param role new role
     */
    public void setRole(UserRole role) {
        this.role = role;
    }

    /**
     * Update user profile
     */
    public void updateProfile(String nickname, String profileImageUrl, String ageRange) {
        if (nickname != null) {
            this.nickname = nickname;
        }
        if (profileImageUrl != null) {
            this.profileImageUrl = profileImageUrl;
        }
        if (ageRange != null) {
            this.ageRange = ageRange;
        }
    }

    /**
     * Check whether user is allowed to change nickname (30-day cooldown).
     *
     * @return true if no change has been made, or last change was 30+ days ago
     */
    public boolean canChangeNickname() {
        LocalDateTime threshold = LocalDateTime.now().minusDays(30);
        return lastNicknameChangedAt == null
            || !lastNicknameChangedAt.isAfter(threshold);
    }

    /**
     * Update nickname and record the change timestamp.
     *
     * @param newNickname new nickname value
     */
    public void updateNickname(String newNickname) {
        this.nickname = newNickname;
        this.lastNicknameChangedAt = LocalDateTime.now();
    }

    /**
     * Update user temperature based on evaluation
     */
    public void updateTemperature(BigDecimal delta) {
        BigDecimal newTemperature = this.temperature.add(delta);
        // Ensure temperature stays within 0-100 range
        if (newTemperature.compareTo(BigDecimal.ZERO) < 0) {
            newTemperature = BigDecimal.ZERO;
        } else if (newTemperature.compareTo(new BigDecimal("100")) > 0) {
            newTemperature = new BigDecimal("100");
        }
        this.temperature = newTemperature;
    }

    /**
     * Check if user is OAuth user
     */
    public boolean isOAuthUser() {
        return oauthProvider != null && oauthId != null;
    }

    /**
     * Check if user is email/password user
     */
    public boolean isEmailUser() {
        return email != null && password != null;
    }

    /**
     * Check if user is currently suspended (Phase 11).
     *
     * @return true if suspended
     */
    public boolean isSuspended() {
        return suspendedUntil != null && suspendedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Suspend user until specified date (Phase 11).
     *
     * @param until suspension expiration time
     */
    public void suspend(LocalDateTime until) {
        this.suspendedUntil = until;
    }

    /**
     * Lift suspension (Phase 11).
     */
    public void liftSuspension() {
        this.suspendedUntil = null;
    }

    /**
     * Check if login is currently locked (Phase 3: brute-force protection).
     */
    public boolean isLoginLocked() {
        return loginLockedUntil != null && loginLockedUntil.isAfter(LocalDateTime.now());
    }

    /**
     * Lock login for the specified number of minutes (Phase 3).
     */
    public void lock(int minutes) {
        this.loginLockedUntil = LocalDateTime.now().plusMinutes(minutes);
    }

    /**
     * Unlock login (Phase 3: admin action).
     */
    public void unlock() {
        this.loginLockedUntil = null;
    }

    /**
     * Check if user has an active premium subscription.
     */
    public boolean isPremium() {
        return premium && (premiumExpiresAt == null || premiumExpiresAt.isAfter(LocalDateTime.now()));
    }

    /**
     * Activate premium subscription until the given expiry date.
     */
    public void activatePremium(LocalDateTime expiresAt) {
        this.premium = true;
        this.premiumExpiresAt = expiresAt;
    }

    /**
     * Deactivate premium subscription.
     */
    public void deactivatePremium() {
        this.premium = false;
        this.premiumExpiresAt = null;
    }

    /**
     * User role enum (Phase 12).
     */
    public enum UserRole {
        USER,   // 일반 사용자
        ADMIN   // 관리자
    }

    public enum OAuthProvider {
        GOOGLE,
        DISCORD
    }
}
