package com.gembud.controller;

import com.gembud.dto.ApiResponse;
import com.gembud.dto.response.NotificationResponse;
import com.gembud.entity.Notification;
import com.gembud.security.CustomUserDetails;
import com.gembud.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "Notification", description = "알림 관리 API")
@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    /**
     * Get all notifications for current user.
     *
     * @param userDetails authenticated user
     * @return list of notifications
     */
    @Operation(summary = "Get all notifications", description = "내 모든 알림 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 목록 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Notification> notifications = notificationService.getMyNotifications(
            userDetails.getUsername()
        );

        List<NotificationResponse> responses = notifications.stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get unread notifications.
     *
     * @param userDetails authenticated user
     * @return list of unread notifications
     */
    @Operation(summary = "Get unread notifications", description = "읽지 않은 알림 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽지 않은 알림 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        List<Notification> notifications = notificationService.getUnreadNotifications(
            userDetails.getUsername()
        );

        List<NotificationResponse> responses = notifications.stream()
            .map(NotificationResponse::from)
            .collect(Collectors.toList());

        return ResponseEntity.ok(ApiResponse.success(responses));
    }

    /**
     * Get unread notification count.
     *
     * @param userDetails authenticated user
     * @return count of unread notifications
     */
    @Operation(summary = "Get unread count", description = "읽지 않은 알림 개수 조회")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 개수 조회 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @GetMapping("/unread/count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        long count = notificationService.getUnreadCount(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Mark notification as read.
     *
     * @param notificationId notification ID
     * @param userDetails authenticated user
     * @return updated notification
     */
    @Operation(summary = "Mark as read", description = "알림을 읽음으로 표시")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "알림 읽음 처리 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<NotificationResponse>> markAsRead(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        Notification notification = notificationService.markAsRead(
            notificationId,
            userDetails.getUsername()
        );

        return ResponseEntity.ok(ApiResponse.success(NotificationResponse.from(notification)));
    }

    /**
     * Mark all notifications as read.
     *
     * @param userDetails authenticated user
     * @return number of updated notifications
     */
    @Operation(summary = "Mark all as read", description = "모든 알림을 읽음으로 표시")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "모든 알림 읽음 처리 성공 (처리된 개수 반환)"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패")
    })
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        int count = notificationService.markAllAsRead(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    /**
     * Delete notification.
     *
     * @param notificationId notification ID
     * @param userDetails authenticated user
     * @return no content
     */
    @Operation(summary = "Delete notification", description = "알림 삭제")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "알림 삭제 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림을 찾을 수 없음")
    })
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
        @PathVariable Long notificationId,
        @AuthenticationPrincipal CustomUserDetails userDetails
    ) {
        notificationService.deleteNotification(notificationId, userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.noContent());
    }
}
