package com.gembud.dto.request;

import jakarta.validation.constraints.Email;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Request DTO for friend operations.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FriendRequest {

    /**
     * Friend user ID.
     */
    private Long friendId;

    /**
     * Friend user email (legacy compatibility).
     */
    @Email(message = "Invalid email format")
    private String email;
}
