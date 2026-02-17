package com.gembud.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gembud.entity.Notification;
import com.gembud.entity.Notification.NotificationType;
import com.gembud.entity.User;
import com.gembud.repository.NotificationRepository;
import com.gembud.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Tests for NotificationService.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationService notificationService;

    private User testUser;
    private Notification testNotification;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
            .id(1L)
            .email("test@example.com")
            .nickname("TestUser")
            .temperature(new BigDecimal("36.5"))
            .build();

        testNotification = Notification.builder()
            .id(1L)
            .user(testUser)
            .type(NotificationType.FRIEND_REQUEST)
            .content("새로운 친구 요청이 있습니다.")
            .relatedId(2L)
            .isRead(false)
            .build();
    }

    @Test
    @DisplayName("createNotification - should create and send notification")
    void createNotification_Success() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(testNotification);
        doNothing().when(messagingTemplate).convertAndSendToUser(
            anyString(), anyString(), any()
        );

        // When
        Notification result = notificationService.createNotification(
            1L,
            NotificationType.FRIEND_REQUEST,
            "새로운 친구 요청이 있습니다.",
            2L
        );

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUser().getId()).isEqualTo(1L);
        assertThat(result.getType()).isEqualTo(NotificationType.FRIEND_REQUEST);
        assertThat(result.getContent()).isEqualTo("새로운 친구 요청이 있습니다.");
        assertThat(result.getRelatedId()).isEqualTo(2L);
        assertThat(result.getIsRead()).isFalse();

        verify(notificationRepository).save(any(Notification.class));
        verify(messagingTemplate).convertAndSendToUser(
            eq("1"),
            eq("/queue/notifications"),
            any(Notification.class)
        );
    }

    @Test
    @DisplayName("createNotification - should throw exception when user not found")
    void createNotification_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            notificationService.createNotification(
                999L,
                NotificationType.FRIEND_REQUEST,
                "내용",
                2L
            ))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("createNotification - should handle WebSocket send failure gracefully")
    void createNotification_WebSocketFailure_ShouldNotThrow() {
        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(testNotification);
        doThrow(new RuntimeException("WebSocket error"))
            .when(messagingTemplate).convertAndSendToUser(anyString(), anyString(), any());

        // When & Then (should not throw)
        Notification result = notificationService.createNotification(
            1L,
            NotificationType.FRIEND_REQUEST,
            "내용",
            2L
        );

        assertThat(result).isNotNull();
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    @DisplayName("getMyNotifications - should return user's notifications")
    void getMyNotifications_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(Arrays.asList(testNotification));

        // When
        List<Notification> results = notificationService.getMyNotifications("test@example.com");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getUser().getId()).isEqualTo(1L);
    }

    @Test
    @DisplayName("getMyNotifications - should throw exception when user not found")
    void getMyNotifications_UserNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("unknown@example.com"))
            .thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            notificationService.getMyNotifications("unknown@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("User not found");
    }

    @Test
    @DisplayName("getUnreadNotifications - should return unread notifications")
    void getUnreadNotifications_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.findUnreadByUserId(1L))
            .thenReturn(Arrays.asList(testNotification));

        // When
        List<Notification> results = notificationService.getUnreadNotifications("test@example.com");

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getIsRead()).isFalse();
    }

    @Test
    @DisplayName("getUnreadCount - should return count of unread notifications")
    void getUnreadCount_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.countUnreadByUserId(1L)).thenReturn(5L);

        // When
        long count = notificationService.getUnreadCount("test@example.com");

        // Then
        assertThat(count).isEqualTo(5L);
    }

    @Test
    @DisplayName("markAsRead - should mark notification as read")
    void markAsRead_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.findById(1L))
            .thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class)))
            .thenReturn(testNotification);

        // When
        Notification result = notificationService.markAsRead(1L, "test@example.com");

        // Then
        assertThat(result.getIsRead()).isTrue();
        verify(notificationRepository).save(testNotification);
    }

    @Test
    @DisplayName("markAsRead - should throw exception when notification not found")
    void markAsRead_NotificationNotFound_ShouldThrowException() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() ->
            notificationService.markAsRead(999L, "test@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Notification not found");
    }

    @Test
    @DisplayName("markAsRead - should throw exception when notification belongs to different user")
    void markAsRead_WrongUser_ShouldThrowException() {
        // Given
        User otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .nickname("OtherUser")
            .build();

        when(userRepository.findByEmail("other@example.com"))
            .thenReturn(Optional.of(otherUser));
        when(notificationRepository.findById(1L))
            .thenReturn(Optional.of(testNotification));

        // When & Then
        assertThatThrownBy(() ->
            notificationService.markAsRead(1L, "other@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Notification does not belong to user");
    }

    @Test
    @DisplayName("markAllAsRead - should mark all notifications as read")
    void markAllAsRead_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.markAllAsReadByUserId(1L)).thenReturn(5);

        // When
        int count = notificationService.markAllAsRead("test@example.com");

        // Then
        assertThat(count).isEqualTo(5);
        verify(notificationRepository).markAllAsReadByUserId(1L);
    }

    @Test
    @DisplayName("deleteNotification - should delete notification")
    void deleteNotification_Success() {
        // Given
        when(userRepository.findByEmail("test@example.com"))
            .thenReturn(Optional.of(testUser));
        when(notificationRepository.findById(1L))
            .thenReturn(Optional.of(testNotification));
        doNothing().when(notificationRepository).delete(any(Notification.class));

        // When
        notificationService.deleteNotification(1L, "test@example.com");

        // Then
        verify(notificationRepository).delete(testNotification);
    }

    @Test
    @DisplayName("deleteNotification - should throw exception when notification belongs to different user")
    void deleteNotification_WrongUser_ShouldThrowException() {
        // Given
        User otherUser = User.builder()
            .id(2L)
            .email("other@example.com")
            .nickname("OtherUser")
            .build();

        when(userRepository.findByEmail("other@example.com"))
            .thenReturn(Optional.of(otherUser));
        when(notificationRepository.findById(1L))
            .thenReturn(Optional.of(testNotification));

        // When & Then
        assertThatThrownBy(() ->
            notificationService.deleteNotification(1L, "other@example.com"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Notification does not belong to user");
    }

    @Test
    @DisplayName("cleanupOldNotifications - should delete old read notifications")
    void cleanupOldNotifications_Success() {
        // Given
        when(notificationRepository.deleteOldReadNotifications()).thenReturn(10);

        // When
        int count = notificationService.cleanupOldNotifications();

        // Then
        assertThat(count).isEqualTo(10);
        verify(notificationRepository).deleteOldReadNotifications();
    }
}
