package com.gembud.dto.response;

import com.gembud.entity.Notification;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Response DTO for notification.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {

    /**
     * Notification ID.
     */
    private Long id;

    /**
     * Notification type.
     */
    private String type;

    /**
     * Notification content.
     */
    private String content;

    /**
     * Related entity ID.
     */
    private Long relatedId;

    /**
     * Is read.
     */
    private Boolean isRead;

    /**
     * Created at.
     */
    private LocalDateTime createdAt;

    /**
     * Create from Notification entity.
     *
     * @param notification notification entity
     * @return notification response
     */
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
            .id(notification.getId())
            .type(notification.getType().name())
            .content(notification.getContent())
            .relatedId(notification.getRelatedId())
            .isRead(notification.getIsRead())
            .createdAt(notification.getCreatedAt())
            .build();
    }
}
