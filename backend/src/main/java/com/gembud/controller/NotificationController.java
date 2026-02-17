package com.gembud.controller;

import com.gembud.dto.response.NotificationResponse;
import com.gembud.entity.Notification;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.NotificationService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for notification management.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for current user.
     *
     * @param userDetails authenticated user
     * @return list of notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getMyNotifications(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Notification> notifications = notificationService.getMyNotifications(
            userDetails.getUsername()
        );

        List<NotificationResponse> responses = notifications.stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get unread notifications.
     *
     * @param userDetails authenticated user
     * @return list of unread notifications
     */
    @GetMapping("/unread")
    public ResponseEntity<List<NotificationResponse>> getUnreadNotifications(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Notification> notifications = notificationService.getUnreadNotifications(
            userDetails.getUsername()
        );

        List<NotificationResponse> responses = notifications.stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    /**
     * Get unread notification count.
     *
     * @param userDetails authenticated user
     * @return count of unread notifications
     */
    @GetMapping("/unread/count")
    public ResponseEntity<Long> getUnreadCount(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(count);
    }

    /**
     * Mark notification as read.
     *
     * @param notificationId notification ID
     * @param userDetails authenticated user
     * @return updated notification
     */
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<NotificationResponse> markAsRead(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Notification notification = notificationService.markAsRead(
            notificationId,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(NotificationResponse.from(notification));
    }

    /**
     * Mark all notifications as read.
     *
     * @param userDetails authenticated user
     * @return number of updated notifications
     */
    @PutMapping("/read-all")
    public ResponseEntity<Integer> markAllAsRead(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int count = notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(count);
    }

    /**
     * Delete notification.
     *
     * @param notificationId notification ID
     * @param userDetails authenticated user
     * @return no content
     */
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<Void> deleteNotification(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.deleteNotification(notificationId, userDetails.getUsername());
        return ResponseEntity.noContent().build();
    }
}
