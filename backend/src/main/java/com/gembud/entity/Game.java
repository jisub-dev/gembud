package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

/**
 * Game entity representing supported games.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Entity
@Table(name = "games")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Game {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Game name (unique).
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Game image URL.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Game genre (e.g., FPS, MOBA, Battle Royale).
     */
    @Column(length = 50)
    private String genre;

    /**
     * Game description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
