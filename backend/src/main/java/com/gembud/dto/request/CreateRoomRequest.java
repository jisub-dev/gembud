package com.gembud.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for creating a room.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {

    @NotNull(message = "Game ID is required")
    private Long gameId;

    @NotBlank(message = "Title is required")
    @Size(min = 2, max = 200, message = "Title must be between 2 and 200 characters")
    private String title;

    private String description;

    @Min(value = 2, message = "Maximum participants must be at least 2")
    @Max(value = 100, message = "Maximum participants cannot exceed 100")
    private Integer maxParticipants;

    private Boolean isPrivate;

    private String password;

    /**
     * Map of filter options (key: option_key, value: option_value).
     */
    private Map<String, String> filters;
}
