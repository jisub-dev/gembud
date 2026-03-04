package com.gembud.dto.response;

import com.gembud.entity.Room;
import java.math.BigDecimal;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for recommended room.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendedRoomResponse {

    /**
     * Room details.
     */
    private RoomResponse room;

    /**
     * Matching score (0-100).
     */
    private Double matchingScore;

    /**
     * Host temperature.
     */
    private BigDecimal hostTemperature;

    /**
     * Reason for recommendation.
     */
    private String reason;

    /**
     * Create from room with score and filters.
     *
     * @param room room entity
     * @param filters room filters map
     * @param matchingScore matching score
     * @param hostTemperature host's temperature
     * @param reason recommendation reason
     * @return recommended room response
     */
    public static RecommendedRoomResponse of(
        Room room,
        Map<String, String> filters,
        Double matchingScore,
        BigDecimal hostTemperature,
        String reason
    ) {
        RoomResponse roomResponse = RoomResponse.builder()
            .id(room.getId())
            .gameId(room.getGame().getId())
            .gameName(room.getGame().getName())
            .title(room.getTitle())
            .description(room.getDescription())
            .maxParticipants(room.getMaxParticipants())
            .currentParticipants(room.getCurrentParticipants())
            .isPrivate(room.getIsPrivate())
            .status(room.getStatus().name())
            .createdBy(room.getCreatedBy().getNickname())
            .createdAt(room.getCreatedAt())
            .filters(filters)
            .build();
        return RecommendedRoomResponse.builder()
            .room(roomResponse)
            .matchingScore(matchingScore)
            .hostTemperature(hostTemperature)
            .reason(reason)
            .build();
    }
}
