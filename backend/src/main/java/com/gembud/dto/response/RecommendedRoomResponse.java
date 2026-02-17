package com.gembud.dto.response;

import com.gembud.entity.Room;
import java.math.BigDecimal;
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
     * Create from room with score.
     *
     * @param room room entity
     * @param matchingScore matching score
     * @param hostTemperature host's temperature
     * @param reason recommendation reason
     * @return recommended room response
     */
    public static RecommendedRoomResponse of(
        Room room,
        Double matchingScore,
        BigDecimal hostTemperature,
        String reason
    ) {
        return RecommendedRoomResponse.builder()
            .room(RoomResponse.from(room))
            .matchingScore(matchingScore)
            .hostTemperature(hostTemperature)
            .reason(reason)
            .build();
    }
}
