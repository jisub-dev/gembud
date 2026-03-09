import api from './api';
import type { Evaluation, EvaluationRequest, TemperatureStats } from '@/types/evaluation';
import { ApiResponse } from '@/types/api';

/**
 * Evaluation service for managing user evaluations and temperature system.
 *
 * @author Gembud Team
 * @since 2026-02-21
 */
export const evaluationService = {
  // 평가 생성
  async createEvaluation(roomId: number, data: EvaluationRequest): Promise<Evaluation> {
    const response = await api.post<ApiResponse<Evaluation>>(`/evaluations/rooms/${roomId}`, data);
    return response.data.data;
  },

  // 룸 평가 조회
  async getRoomEvaluations(roomId: number): Promise<Evaluation[]> {
    const response = await api.get<ApiResponse<Evaluation[]>>(`/evaluations/rooms/${roomId}`);
    return response.data.data;
  },

  // 평가 가능한 사용자 조회
  async getEvaluatableUsers(roomId: number): Promise<number[]> {
    const response = await api.get<ApiResponse<number[]>>(`/evaluations/rooms/${roomId}/evaluatable`);
    return response.data.data;
  },

  // 평가 가능 여부 조회 (alias)
  async getEvaluatable(roomId: number): Promise<number[]> {
    return this.getEvaluatableUsers(roomId);
  },

  // 사용자가 받은 평가 조회
  async getUserEvaluations(userId: number): Promise<Evaluation[]> {
    const response = await api.get<ApiResponse<Evaluation[]>>(`/evaluations/users/${userId}`);
    return response.data.data;
  },

  // 사용자 온도 조회
  async getUserTemperature(userId: number): Promise<TemperatureStats> {
    const response = await api.get<ApiResponse<TemperatureStats>>(`/evaluations/users/${userId}/temperature`);
    return response.data.data;
  },
};
export default evaluationService;
