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
import jakarta.persistence.Table;
import java.time.LocalDateTime;
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

    @Column(length = 255)
    private String password;

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
     * Start the game.
     */
    public void start() {
        this.status = RoomStatus.IN_PROGRESS;
    }
}
