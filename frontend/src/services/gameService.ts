import api from './api';
import { Game } from '@/types/game';
import { ApiResponse } from '@/types/api';

/**
 * Game service for API integration.
 *
 * @author Gembud Team
 * @since 2026-02-19
 */

export const gameService = {
  /**
   * Get all games.
   */
  async getGames(): Promise<Game[]> {
    const response = await api.get<ApiResponse<Game[]>>('/games');
    return response.data.data;
  },

  /**
   * Get game by ID.
   */
  async getGameById(id: number): Promise<Game> {
    const response = await api.get<ApiResponse<Game>>(`/games/${id}`);
    return response.data.data;
  },
};
export default gameService;
