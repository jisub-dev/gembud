import api from './api';
import type { Friend, FriendRequest } from '@/types/friend';
import { ApiResponse } from '@/types/api';

/**
 * Friend service for managing friend relationships.
 *
 * @author Gembud Team
 * @since 2026-02-21
 */
export const friendService = {
  // 친구 목록 조회
  async getFriends(): Promise<Friend[]> {
    const response = await api.get<ApiResponse<Friend[]>>('/friends');
    return response.data.data;
  },

  // 친구 요청 발송 (이메일로)
  async sendFriendRequest(email: string): Promise<void> {
    await api.post('/friends/requests', { email });
  },

  // 친구 요청 수락
  async acceptFriendRequest(requestId: number): Promise<void> {
    await api.put(`/friends/requests/${requestId}/accept`);
  },

  // 친구 요청 거절
  async rejectFriendRequest(requestId: number): Promise<void> {
    await api.put(`/friends/requests/${requestId}/reject`);
  },

  // 친구 삭제
  async removeFriend(friendId: number): Promise<void> {
    await api.delete(`/friends/${friendId}`);
  },

  // 대기 중인 요청 조회
  async getPendingRequests(): Promise<FriendRequest[]> {
    const response = await api.get<ApiResponse<FriendRequest[]>>('/friends/requests/pending');
    return response.data.data;
  },

  // 발송한 요청 조회
  async getSentRequests(): Promise<FriendRequest[]> {
    const response = await api.get<ApiResponse<FriendRequest[]>>('/friends/requests/sent');
    return response.data.data;
  },
};

export default friendService;
