import { ArrowRight } from 'lucide-react';
import Card from '../common/Card';
import { Game } from '@/types/game';

interface GameCardProps {
  game: Game;
  isSelected?: boolean;
  onSelect?: (gameId: number) => void;
}

const GameCard = ({ game, isSelected = false, onSelect }: GameCardProps) => {
  return (
    <button type="button" onClick={() => onSelect?.(game.id)} className="w-full text-left">
      <Card
        className={`group overflow-hidden p-0 relative aspect-[3/4] ${
          isSelected ? 'ring-2 ring-neon-cyan shadow-glow-cyan' : ''
        }`}
      >
        <div className="relative w-full h-full">
          <img
            src={game.imageUrl}
            alt={game.name}
            className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-110"
          />

          {/* Neon Gradient Overlay */}
          <div className="absolute inset-0 bg-gradient-to-t from-dark-primary via-dark-primary/50 to-transparent opacity-80 group-hover:opacity-60 transition-opacity" />

          {/* Content */}
          <div className="absolute bottom-0 left-0 right-0 p-6 space-y-2">
            <h3 className="font-display text-2xl text-white group-hover:text-neon-purple transition-colors">
              {game.name}
            </h3>
            <p className="text-text-secondary text-sm line-clamp-2">{game.description}</p>
            <div className="flex items-center justify-between pt-2">
              <span className="px-3 py-1 rounded-full bg-dark-secondary/80 border border-neon-purple/30 text-neon-purple text-xs font-gaming">
                {game.genre}
              </span>
              <span className="flex items-center gap-1 text-text-secondary text-sm font-gaming">
                추천 방 보기
                <ArrowRight size={14} />
              </span>
            </div>
          </div>

          {/* Glow Effect on Hover */}
          <div className="absolute inset-0 ring-2 ring-neon-purple/0 group-hover:ring-neon-purple/50 transition-all rounded-card" />
        </div>
      </Card>
    </button>
  );
};

export default GameCard;
