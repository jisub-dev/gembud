package com.gembud.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for refreshing access token.
 *
 * @author Gembud Team
 * @since 2026-02-16
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshTokenRequest {

    /**
     * Refresh token.
     */
    @NotBlank(message = "Refresh token is required")
    private String refreshToken;
}
