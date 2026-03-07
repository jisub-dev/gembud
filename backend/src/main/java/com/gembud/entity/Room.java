package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Room entity for game party matching.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Entity
@Table(name = "rooms")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Room {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "max_participants", nullable = false)
    @Builder.Default
    private Integer maxParticipants = 5;

    @Column(name = "current_participants", nullable = false)
    @Builder.Default
    private Integer currentParticipants = 0;

    @Column(name = "is_private", nullable = false)
    @Builder.Default
    private Boolean isPrivate = false;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private RoomStatus status = RoomStatus.OPEN;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "invite_code", unique = true, length = 32)
    private String inviteCode;

    @Column(name = "invite_code_expires_at")
    private LocalDateTime inviteCodeExpiresAt;

    @PrePersist
    protected void onPrePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID().toString();
        }
    }

    /**
     * Room status enum.
     */
    public enum RoomStatus {
        OPEN,
        FULL,
        IN_PROGRESS,
        CLOSED
    }

    /**
     * Increment current participants count.
     */
    public void incrementParticipants() {
        this.currentParticipants++;
        if (this.currentParticipants >= this.maxParticipants) {
            this.status = RoomStatus.FULL;
        }
    }

    /**
     * Decrement current participants count.
     */
    public void decrementParticipants() {
        if (this.currentParticipants > 0) {
            this.currentParticipants--;
            if (this.status == RoomStatus.FULL && this.currentParticipants < this.maxParticipants) {
                this.status = RoomStatus.OPEN;
            }
        }
    }

    /**
     * Close the room.
     */
    public void close() {
        this.status = RoomStatus.CLOSED;
    }

    /**
     * Soft-delete the room (all participants left).
     */
    public void softDelete() {
        this.status = RoomStatus.CLOSED;
        this.deletedAt = LocalDateTime.now();
    }

    /**
     * Start the game.
     */
    public void start() {
        this.status = RoomStatus.IN_PROGRESS;
    }

    /**
     * Reset room status back to OPEN.
     */
    public void resetToOpen() {
        this.status = RoomStatus.OPEN;
    }

    /**
     * Generate an invite code valid for the given number of hours.
     *
     * @param ttlHours time-to-live in hours
     */
    public void generateInviteCode(int ttlHours) {
        byte[] bytes = new byte[12];
        new SecureRandom().nextBytes(bytes);
        this.inviteCode = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        this.inviteCodeExpiresAt = LocalDateTime.now().plusHours(ttlHours);
    }

    /**
     * Regenerate invite code, replacing the existing one.
     *
     * @param ttlHours time-to-live in hours
     */
    public void regenerateInviteCode(int ttlHours) {
        generateInviteCode(ttlHours);
    }

    /**
     * Check if the current invite code is still valid.
     *
     * @return true if invite code exists and has not expired
     */
    public boolean isInviteCodeValid() {
        return inviteCode != null
            && inviteCodeExpiresAt != null
            && inviteCodeExpiresAt.isAfter(LocalDateTime.now());
    }
}
