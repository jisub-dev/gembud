package com.gembud.dto.response;

import lombok.Builder;
import lombok.Getter;

/**
 * Admin response DTO for security event summary.
 */
@Getter
@Builder
public class SecurityEventSummaryResponse {

    private long loginFailCount;
    private long loginLockedCount;
    private long refreshReuseCount;
    private long rateLimitHitCount;
}
