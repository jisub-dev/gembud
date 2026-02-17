package com.gembud.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for joining a room.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JoinRoomRequest {

    /**
     * Password for private rooms.
     */
    private String password;
}
