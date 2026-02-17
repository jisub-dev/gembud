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
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Game option entity for game-specific matching options.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Entity
@Table(
    name = "game_options",
    uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "option_key"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameOption {

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Associated game.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /**
     * Option key identifier (e.g., "position", "tier").
     */
    @Column(name = "option_key", nullable = false, length = 50)
    private String optionKey;

    /**
     * Option type (SELECT, MULTI_SELECT, RANGE, BOOLEAN).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "option_type", nullable = false, length = 20)
    private OptionType optionType;

    /**
     * JSON array of possible values.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "option_values", columnDefinition = "jsonb")
    private String optionValues;

    /**
     * Whether this is a common option across all games.
     */
    @Column(name = "is_common", nullable = false)
    @Builder.Default
    private Boolean isCommon = false;

    /**
     * Creation timestamp.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Option type enum.
     */
    public enum OptionType {
        SELECT,
        MULTI_SELECT,
        RANGE,
        BOOLEAN
    }
}
