package com.gembud.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO containing authentication tokens.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    /**
     * Access token (1 hour validity).
     */
    private String accessToken;

    /**
     * Refresh token (7 days validity).
     */
    private String refreshToken;

    /**
     * Token type (always "Bearer").
     */
    @Builder.Default
    private String tokenType = "Bearer";

    /**
     * User email.
     */
    private String email;

    /**
     * User nickname.
     */
    private String nickname;
}
