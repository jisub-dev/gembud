package com.gembud.dto.response;

import com.gembud.entity.GameOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for GameOption.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameOptionResponse {

    private Long id;
    private String optionKey;
    private String optionType;
    private String optionValues;
    private Boolean isCommon;

    /**
     * Convert GameOption entity to GameOptionResponse.
     *
     * @param gameOption game option entity
     * @return game option response
     */
    public static GameOptionResponse from(GameOption gameOption) {
        return GameOptionResponse.builder()
            .id(gameOption.getId())
            .optionKey(gameOption.getOptionKey())
            .optionType(gameOption.getOptionType().name())
            .optionValues(gameOption.getOptionValues())
            .isCommon(gameOption.getIsCommon())
            .build();
    }
}
