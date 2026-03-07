import api from './api';
import type { Room, CreateRoomRequest, JoinRoomResult } from '@/types/room';
import { ApiResponse } from '@/types/api';

/**
 * Room service for managing game rooms.
 *
 * @author Gembud Team
 * @since 2026-02-21
 */
export const roomService = {
  // 게임별 방 목록 조회
  async getRoomsByGame(gameId: number): Promise<Room[]> {
    const response = await api.get<ApiResponse<Room[]>>('/rooms', { params: { gameId } });
    return response.data.data;
  },

  // 방 상세 조회 (publicId 기반)
  async getRoom(publicId: string): Promise<Room> {
    const response = await api.get<ApiResponse<Room>>(`/rooms/${publicId}`);
    return response.data.data;
  },

  // 방 생성
  async createRoom(data: CreateRoomRequest): Promise<Room> {
    const response = await api.post<ApiResponse<Room>>('/rooms', data);
    return response.data.data;
  },

  // 방 입장 (publicId 기반, chatRoomId 반환)
  async joinRoom(publicId: string, password?: string, inviteCode?: string): Promise<JoinRoomResult> {
    const response = await api.post<ApiResponse<JoinRoomResult>>(
      `/rooms/${publicId}/join`,
      { password, inviteCode },
    );
    return response.data.data;
  },

  // 초대코드 재발급 (방장만, publicId 기반)
  async regenerateInviteCode(publicId: string): Promise<Room> {
    const response = await api.post<ApiResponse<Room>>(`/rooms/${publicId}/invite/regenerate`);
    return response.data.data;
  },

  // 방 퇴장
  async leaveRoom(roomId: number): Promise<void> {
    await api.post(`/rooms/${roomId}/leave`);
  },

  // 내가 참여 중인 방 목록
  async getMyRooms(): Promise<Room[]> {
    const response = await api.get<ApiResponse<Room[]>>('/rooms/my');
    return response.data.data;
  },

  // 참여자 강퇴 (방장만)
  async kickParticipant(roomId: number, userId: number): Promise<void> {
    await api.post(`/rooms/${roomId}/kick/${userId}`);
  },

  // 방 시작 (방장만)
  async startRoom(roomId: number): Promise<void> {
    await api.post(`/rooms/${roomId}/start`);
  },

  // 방장 이전 (방장만)
  async transferHost(roomId: number, userId: number): Promise<void> {
    await api.post(`/rooms/${roomId}/transfer/${userId}`);
  },

  // 방 상태 초기화 (IN_PROGRESS -> OPEN, 방장만, publicId 기반)
  async resetRoom(publicId: string): Promise<void> {
    await api.post(`/rooms/${publicId}/reset`);
  },
};
export default roomService;
