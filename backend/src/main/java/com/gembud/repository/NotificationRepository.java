package com.gembud.repository;

import com.gembud.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * Repository for Notification entity.
 *
 * @author Gembud Team
 * @since 2026-02-17
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    /**
     * Find all notifications for a user, ordered by creation time (newest first).
     *
     * @param userId user ID
     * @return list of notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<Notification> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId);

    /**
     * Find unread notifications for a user.
     *
     * @param userId user ID
     * @return list of unread notifications
     */
    @Query("SELECT n FROM Notification n WHERE n.user.id = :userId AND n.isRead = false ORDER BY n.createdAt DESC")
    List<Notification> findUnreadByUserId(@Param("userId") Long userId);

    /**
     * Count unread notifications for a user.
     *
     * @param userId user ID
     * @return count of unread notifications
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user.id = :userId AND n.isRead = false")
    long countUnreadByUserId(@Param("userId") Long userId);

    /**
     * Mark all notifications as read for a user.
     *
     * @param userId user ID
     * @return number of updated notifications
     */
    @Modifying
    @Query("UPDATE Notification n SET n.isRead = true WHERE n.user.id = :userId AND n.isRead = false")
    int markAllAsReadByUserId(@Param("userId") Long userId);

    /**
     * Delete old notifications (older than 30 days and read).
     *
     * @return number of deleted notifications
     */
    @Modifying
    @Query(value = "DELETE FROM notifications WHERE is_read = true AND created_at < CURRENT_TIMESTAMP - INTERVAL '30 days'", nativeQuery = true)
    int deleteOldReadNotifications();
}
