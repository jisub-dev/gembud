package com.gembud.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/**
 * Ad view tracking entity (Phase 11).
 *
 * @author Gembud Team
 * @since 2026-02-18
 */
@Entity
@Table(name = "ad_views")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdView {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Advertisement.
     */
    @ManyToOne
    @JoinColumn(name = "ad_id", nullable = false)
    private Advertisement advertisement;

    /**
     * User who viewed the ad.
     */
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Viewed at.
     */
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime viewedAt;
}
