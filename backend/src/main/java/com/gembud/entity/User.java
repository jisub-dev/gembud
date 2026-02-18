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
    @Column(nullable = false, precision = 4, scale = 2)
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

    @Builder
    public User(String email, String password, String nickname, String profileImageUrl,
                String ageRange, BigDecimal temperature, OAuthProvider oauthProvider,
                String oauthId) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
        this.profileImageUrl = profileImageUrl;
        this.ageRange = ageRange;
        this.temperature = temperature != null ? temperature : new BigDecimal("36.5");
        this.oauthProvider = oauthProvider;
        this.oauthId = oauthId;
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

    public enum OAuthProvider {
        GOOGLE,
        DISCORD
    }
}
