/**
 * Game entity types.
 *
 * @author Gembud Team
 * @since 2026-02-19
 */

export interface GameOption {
  id: number;
  optionKey: string;
  optionType: 'TIER' | 'POSITION' | 'OTHER';
  optionValues: string; // JSON-encoded string array, e.g. '["Gold","Platinum"]'
  isCommon: boolean;
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
