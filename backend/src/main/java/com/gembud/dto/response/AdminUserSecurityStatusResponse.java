package com.gembud.dto.response;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * Admin response DTO for user security status.
 */
@Getter
@Builder
public class AdminUserSecurityStatusResponse {

    private Long userId;
    private String email;
    private boolean loginLocked;
    private LocalDateTime loginLockedUntil;
    private long failedLoginCountInWindow;
    private int windowMinutes;
}
