import api from './api';
import { Game } from '@/types/game';

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
    const response = await api.get<Game[]>('/games');
    return response.data;
  },

  /**
   * Get game by ID.
   */
  async getGameById(id: number): Promise<Game> {
    const response = await api.get<Game>(`/games/${id}`);
    return response.data;
  },
};
