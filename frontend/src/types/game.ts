/**
 * Game entity types.
 *
 * @author Gembud Team
 * @since 2026-02-19
 */

export interface Game {
  id: number;
  name: string;
  imageUrl: string;
  genre: string;
  description: string;
  createdAt?: string;
}

export interface GameWithStats extends Game {
  activeRoomCount?: number;
  totalPlayers?: number;
}
