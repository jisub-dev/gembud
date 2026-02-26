import api from './api';
import type { RecommendedRoom } from '@/types/room';
import { ApiResponse } from '@/types/api';

/**
 * Matching service for AI-based room recommendations.
 *
 * @author Gembud Team
 * @since 2026-02-21
 */
export const matchingService = {
  // 추천 방 조회
  async getRecommendedRooms(gameId: number, limit: number = 10): Promise<RecommendedRoom[]> {
    const response = await api.get<ApiResponse<RecommendedRoom[]>>(
      `/matching/recommendations/game/${gameId}`,
      { params: { limit } }
    );
    return response.data.data;
  },
};
