package com.gembud.service;

import com.gembud.entity.Notification;
import com.gembud.entity.Notification.NotificationType;
import com.gembud.entity.User;
import com.gembud.exception.BusinessException;
import com.gembud.exception.ErrorCode;
import com.gembud.repository.NotificationRepository;
import com.gembud.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for notification management.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Create and send notification to user.
     *
     * @param userId user ID
     * @param type notification type
     * @param content notification content
     * @param relatedId related entity ID
     * @return created notification
     */
    @Transactional
    public Notification createNotification(
        Long userId,
        NotificationType type,
        String content,
        Long relatedId
    ) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notification notification = Notification.builder()
            .user(user)
            .type(type)
            .content(content)
            .relatedId(relatedId)
            .isRead(false)
            .build();

        Notification savedNotification = notificationRepository.save(notification);

        // Send notification via WebSocket
        sendNotificationViaWebSocket(userId, savedNotification);

        log.info("Notification created and sent to user {}: {}", userId, content);

        return savedNotification;
    }

    /**
     * Get all notifications for a user.
     *
     * @param userEmail user email
     * @return list of notifications
     */
    public List<Notification> getMyNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
    }

    /**
     * Get unread notifications for a user.
     *
     * @param userEmail user email
     * @return list of unread notifications
     */
    public List<Notification> getUnreadNotifications(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.findUnreadByUserId(user.getId());
    }

    /**
     * Get unread notification count.
     *
     * @param userEmail user email
     * @return count of unread notifications
     */
    public long getUnreadCount(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return notificationRepository.countUnreadByUserId(user.getId());
    }

    /**
     * Mark notification as read.
     *
     * @param notificationId notification ID
     * @param userEmail user email
     * @return updated notification
     */
    @Transactional
    public Notification markAsRead(Long notificationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // Verify notification belongs to user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_BELONG_TO_USER);
        }

        notification.markAsRead();
        return notificationRepository.save(notification);
    }

    /**
     * Mark all notifications as read for a user.
     *
     * @param userEmail user email
     * @return number of updated notifications
     */
    @Transactional
    public int markAllAsRead(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        int count = notificationRepository.markAllAsReadByUserId(user.getId());
        log.info("Marked {} notifications as read for user {}", count, user.getNickname());

        return count;
    }

    /**
     * Delete notification.
     *
     * @param notificationId notification ID
     * @param userEmail user email
     */
    @Transactional
    public void deleteNotification(Long notificationId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Notification notification = notificationRepository.findById(notificationId)
            .orElseThrow(() -> new BusinessException(ErrorCode.NOTIFICATION_NOT_FOUND));

        // Verify notification belongs to user
        if (!notification.getUser().getId().equals(user.getId())) {
            throw new BusinessException(ErrorCode.NOTIFICATION_NOT_BELONG_TO_USER);
        }

        notificationRepository.delete(notification);
        log.info("Notification {} deleted by user {}", notificationId, user.getNickname());
    }

    /**
     * Notify user that their account has been suspended.
     *
     * @param user       suspended user
     * @param suspendUntil suspension expiration time
     */
    @Transactional
    public void notifyUserSuspended(User user, java.time.LocalDateTime suspendUntil) {
        String content = String.format(
            "신고 누적으로 인해 %s까지 계정이 정지되었습니다.",
            suspendUntil.toLocalDate()
        );
        createNotification(user.getId(), NotificationType.ACCOUNT_SUSPENDED, content, null);
        log.warn("Suspension notification sent to user {}", user.getNickname());
    }

    /**
     * Notify user that an admin warning has been issued.
     *
     * @param user target user
     * @param reportId related report ID
     * @param warningMessage admin warning message
     */
    @Transactional
    public void notifyUserWarned(User user, Long reportId, String warningMessage) {
        String content = String.format(
            "운영자 경고가 발송되었습니다: %s",
            warningMessage
        );
        createNotification(user.getId(), NotificationType.REPORT_WARNED, content, reportId);
        log.info("Warning notification sent to user {}", user.getNickname());
    }

    /**
     * Clean up old read notifications (scheduled task).
     *
     * @return number of deleted notifications
     */
    @Transactional
    public int cleanupOldNotifications() {
        int count = notificationRepository.deleteOldReadNotifications();
        log.info("Cleaned up {} old read notifications", count);
        return count;
    }

    /**
     * Scheduled cleanup for old read notifications (older than 30 days).
     * Runs daily at 03:30.
     */
    @Scheduled(cron = "0 30 3 * * *")
    @Transactional
    public void scheduledCleanupOldNotifications() {
        int count = notificationRepository.deleteOldReadNotifications();
        if (count > 0) {
            log.info("Scheduled cleanup removed {} old notifications", count);
        }
    }

    /**
     * Send notification via WebSocket.
     *
     * @param userId user ID
     * @param notification notification
     */
    private void sendNotificationViaWebSocket(Long userId, Notification notification) {
        try {
            messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/notifications",
                notification
            );
        } catch (Exception e) {
            log.error("Failed to send notification via WebSocket to user {}: {}",
                userId, e.getMessage());
        }
    }
}
