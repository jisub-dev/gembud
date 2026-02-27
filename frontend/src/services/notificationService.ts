import api from './api';
import type { Notification } from '@/types/notification';
import { ApiResponse } from '@/types/api';

/**
 * Notification service for managing user notifications.
 *
 * @author Gembud Team
 * @since 2026-02-21
 */
export const notificationService = {
  // 모든 알림 조회
  async getNotifications(): Promise<Notification[]> {
    const response = await api.get<ApiResponse<Notification[]>>('/notifications');
    return response.data.data;
  },

  // 읽지 않은 알림 조회
  async getUnreadNotifications(): Promise<Notification[]> {
    const response = await api.get<ApiResponse<Notification[]>>('/notifications/unread');
    return response.data.data;
  },

  // 읽지 않은 알림 개수 조회
  async getUnreadCount(): Promise<number> {
    const response = await api.get<ApiResponse<number>>('/notifications/unread/count');
    return response.data.data;
  },

  // 알림 읽음 표시
  async markAsRead(notificationId: number): Promise<void> {
    await api.put(`/notifications/${notificationId}/read`);
  },

  // 모든 알림 읽음 표시
  async markAllAsRead(): Promise<void> {
    await api.put('/notifications/read-all');
  },

  // 알림 삭제
  async deleteNotification(notificationId: number): Promise<void> {
    await api.delete(`/notifications/${notificationId}`);
  },
};
export default notificationService;
