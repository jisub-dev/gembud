import type { Game } from '@/types/game';
import GameCard from './GameCard';

interface GameGridProps {
  games: Game[];
  selectedGameId?: number;
  onGameSelect?: (gameId: number) => void;
  emptyMessage?: string;
}

/**
 * Grid layout for games.
 */
const GameGrid = ({ games, selectedGameId, onGameSelect, emptyMessage }: GameGridProps) => {
  if (!games || games.length === 0) {
    return (
      <div className="text-center py-12">
        <div className="inline-block p-6 bg-dark-secondary border border-neon-purple/30 rounded-card">
          <p className="text-text-primary font-gaming">
            {emptyMessage ?? '등록된 게임이 없습니다'}
          </p>
        </div>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 animate-fade-in">
      {games.map((game) => (
        <GameCard
          key={game.id}
          game={game}
          isSelected={selectedGameId === game.id}
          onSelect={onGameSelect}
        />
      ))}
    </div>
  );
};

export default GameGrid;
