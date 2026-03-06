package com.gembud.dto.response;

import com.gembud.entity.SecurityEvent;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

/**
 * Admin response DTO for security event list.
 */
@Getter
@Builder
public class SecurityEventResponse {

    private Long id;
    private String eventType;
    private Long userId;
    private String ip;
    private String endpoint;
    private String result;
    private String riskScore;
    private LocalDateTime createdAt;

    public static SecurityEventResponse from(SecurityEvent event) {
        return SecurityEventResponse.builder()
            .id(event.getId())
            .eventType(event.getEventType().name())
            .userId(event.getUserId())
            .ip(event.getIp())
            .endpoint(event.getEndpoint())
            .result(event.getResult())
            .riskScore(event.getRiskScore())
            .createdAt(event.getCreatedAt())
            .build();
    }
}
