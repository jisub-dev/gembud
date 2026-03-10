import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import notificationService from '@/services/notificationService';

/**
 * React Query hooks for notifications
 *
 * @author Gembud Team
 * @since 2026-02-26
 */

// Query keys
export const notificationKeys = {
  all: ['notifications'] as const,
  list: () => [...notificationKeys.all, 'list'] as const,
  unreadCount: () => [...notificationKeys.all, 'unread-count'] as const,
};

// Get notifications
export function useNotifications() {
  return useQuery({
    queryKey: notificationKeys.list(),
    queryFn: notificationService.getNotifications,
  });
}

// Get unread notification count
export function useUnreadNotificationCount() {
  return useQuery({
    queryKey: notificationKeys.unreadCount(),
    queryFn: notificationService.getUnreadCount,
    refetchInterval: 30000, // Poll every 30 seconds
  });
}

// Mark notification as read
export function useMarkNotificationAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) => notificationService.markAsRead(notificationId),
    onMutate: async (notificationId: number) => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.list() });
      await queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount() });

      const previousNotifications = queryClient.getQueryData(notificationKeys.list());
      const previousUnreadCount = queryClient.getQueryData(notificationKeys.unreadCount());

      const notifications = (previousNotifications as Array<{ id: number; isRead: boolean }> | undefined) ?? [];
      const target = notifications.find((notification) => notification.id === notificationId);
      const wasUnread = !!target && !target.isRead;

      queryClient.setQueryData(
        notificationKeys.list(),
        notifications.map((notification) =>
          notification.id === notificationId ? { ...notification, isRead: true } : notification
        )
      );

      const unreadCountValue =
        typeof previousUnreadCount === 'number'
          ? previousUnreadCount
          : notifications.filter((notification) => !notification.isRead).length;
      queryClient.setQueryData(notificationKeys.unreadCount(), wasUnread ? Math.max(0, unreadCountValue - 1) : unreadCountValue);

      return { previousNotifications, previousUnreadCount };
    },
    onError: (_error, _notificationId, context) => {
      if (context?.previousNotifications !== undefined) {
        queryClient.setQueryData(notificationKeys.list(), context.previousNotifications);
      }
      if (context?.previousUnreadCount !== undefined) {
        queryClient.setQueryData(notificationKeys.unreadCount(), context.previousUnreadCount);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.list() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
}

// Mark all notifications as read
export function useMarkAllNotificationsAsRead() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: notificationService.markAllAsRead,
    onMutate: async () => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.list() });
      await queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount() });

      const previousNotifications = queryClient.getQueryData(notificationKeys.list());
      const previousUnreadCount = queryClient.getQueryData(notificationKeys.unreadCount());

      const notifications = (previousNotifications as Array<{ isRead: boolean }> | undefined) ?? [];
      queryClient.setQueryData(
        notificationKeys.list(),
        notifications.map((notification) => ({ ...notification, isRead: true }))
      );
      queryClient.setQueryData(notificationKeys.unreadCount(), 0);

      return { previousNotifications, previousUnreadCount };
    },
    onError: (_error, _variables, context) => {
      if (context?.previousNotifications !== undefined) {
        queryClient.setQueryData(notificationKeys.list(), context.previousNotifications);
      }
      if (context?.previousUnreadCount !== undefined) {
        queryClient.setQueryData(notificationKeys.unreadCount(), context.previousUnreadCount);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.list() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
}

// Delete notification
export function useDeleteNotification() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (notificationId: number) => notificationService.deleteNotification(notificationId),
    onMutate: async (notificationId: number) => {
      await queryClient.cancelQueries({ queryKey: notificationKeys.list() });
      await queryClient.cancelQueries({ queryKey: notificationKeys.unreadCount() });

      const previousNotifications = queryClient.getQueryData(notificationKeys.list());
      const previousUnreadCount = queryClient.getQueryData(notificationKeys.unreadCount());

      const notifications = (previousNotifications as Array<{ id: number; isRead: boolean }> | undefined) ?? [];
      const target = notifications.find((notification) => notification.id === notificationId);
      const wasUnread = !!target && !target.isRead;

      queryClient.setQueryData(
        notificationKeys.list(),
        notifications.filter((notification) => notification.id !== notificationId)
      );

      const unreadCountValue =
        typeof previousUnreadCount === 'number'
          ? previousUnreadCount
          : notifications.filter((notification) => !notification.isRead).length;
      queryClient.setQueryData(notificationKeys.unreadCount(), wasUnread ? Math.max(0, unreadCountValue - 1) : unreadCountValue);

      return { previousNotifications, previousUnreadCount };
    },
    onError: (_error, _notificationId, context) => {
      if (context?.previousNotifications !== undefined) {
        queryClient.setQueryData(notificationKeys.list(), context.previousNotifications);
      }
      if (context?.previousUnreadCount !== undefined) {
        queryClient.setQueryData(notificationKeys.unreadCount(), context.previousUnreadCount);
      }
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: notificationKeys.list() });
      queryClient.invalidateQueries({ queryKey: notificationKeys.unreadCount() });
    },
  });
}
