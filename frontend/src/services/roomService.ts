import api from './api';
import type { Room, CreateRoomRequest } from '@/types/room';
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

  // 방 상세 조회
  async getRoomById(roomId: number): Promise<Room> {
    const response = await api.get<ApiResponse<Room>>(`/rooms/${roomId}`);
    return response.data.data;
  },

  // 방 생성
  async createRoom(data: CreateRoomRequest): Promise<Room> {
    const response = await api.post<ApiResponse<Room>>('/rooms', data);
    return response.data.data;
  },

  // 방 입장
  async joinRoom(roomId: number, password?: string): Promise<void> {
    await api.post(`/rooms/${roomId}/join`, { password });
  },

  // 방 퇴장
  async leaveRoom(roomId: number): Promise<void> {
    await api.post(`/rooms/${roomId}/leave`);
  },

  // 방 닫기 (호스트만)
  async closeRoom(roomId: number): Promise<void> {
    await api.delete(`/rooms/${roomId}`);
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
};
export default roomService;
