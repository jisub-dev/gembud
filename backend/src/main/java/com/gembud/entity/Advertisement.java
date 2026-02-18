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
 * Advertisement entity (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Entity
@Table(name = "advertisements")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Advertisement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ad title.
     */
    @Column(nullable = false, length = 100)
    private String title;

    /**
     * Ad description.
     */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * Ad image URL.
     */
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    /**
     * Ad target URL (click destination).
     */
    @Column(name = "target_url", nullable = false, length = 500)
    private String targetUrl;

    /**
     * Is active.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Is gaming-related (Phase 11: Only gaming ads allowed).
     */
    @Column(name = "is_gaming_related", nullable = false)
    @Builder.Default
    private Boolean isGamingRelated = true;

    /**
     * Display order (lower = higher priority).
     */
    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    /**
     * Created at.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Ad expiration date (nullable).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Check if ad is currently valid.
     *
     * @return true if valid
     */
    public boolean isValid() {
        if (!isActive) {
            return false;
        }
        if (expiresAt != null && expiresAt.isBefore(LocalDateTime.now())) {
            return false;
        }
        return true;
    }
}
