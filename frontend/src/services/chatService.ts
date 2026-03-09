import api from './api';
import type { ChatMessage, ChatRoomInfo, CreateDirectChatRequest, CreateGroupChatRequest } from '@/types/chat';
import { ApiResponse } from '@/types/api';

/**
 * Chat service for managing chat rooms and messages.
 *
 * @author Gembud Team
 * @since 2026-02-21
 */
export const chatService = {
  // 채팅 메시지 조회
  async getMessages(chatPublicId: string, limit: number = 50): Promise<ChatMessage[]> {
    const response = await api.get<ApiResponse<ChatMessage[]>>(
      `/chat/rooms/${chatPublicId}/messages`,
      { params: { limit } }
    );
    return response.data.data;
  },

  // 게임 룸의 채팅룸 publicId 조회
  async getChatRoomByGameRoom(gameRoomId: number): Promise<string> {
    const response = await api.get<ApiResponse<string>>(
      `/chat/rooms/by-game-room/${gameRoomId}`
    );
    return response.data.data;
  },

  // 1:1 채팅방 생성
  async createDirectChat(data: CreateDirectChatRequest): Promise<number> {
    const response = await api.post<ApiResponse<{ chatRoomId: number }>>('/chat/rooms/direct', data);
    return response.data.data.chatRoomId;
  },

  // 그룹 채팅방 생성
  async createGroupChat(data: CreateGroupChatRequest): Promise<number> {
    const response = await api.post<ApiResponse<{ chatRoomId: number }>>('/chat/rooms/group', data);
    return response.data.data.chatRoomId;
  },

  // 채팅방 멤버 추가
  async addMember(chatRoomId: number, userId: number): Promise<void> {
    await api.post(`/chat/rooms/${chatRoomId}/members`, { userId });
  },

  // 내가 참여 중인 채팅방 목록
  async getMyChatRooms(type?: 'ROOM_CHAT' | 'DIRECT_CHAT' | 'GROUP_CHAT'): Promise<ChatRoomInfo[]> {
    const response = await api.get<ApiResponse<ChatRoomInfo[]>>('/chat/rooms/my', {
      params: type ? { type } : undefined,
    });
    return response.data.data;
  },
};
export default chatService;
