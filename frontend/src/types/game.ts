/**
 * Game entity types.
 *
 * @author Gembud Team
 * @since 2026-02-19
 */

export interface GameOption {
  id: number;
  name: string;
  type: 'TIER' | 'POSITION' | 'OTHER';
  value: string;
  displayOrder: number;
}

export interface Game {
  id: number;
  name: string;
  imageUrl: string;
  genre: string;
  description: string;
  options: GameOption[];
}

export interface GameWithStats extends Game {
  activeRoomCount?: number;
  totalPlayers?: number;
}
