import type { GameOption } from '@/types/game';

interface RoomFilterProps {
  tierOptions: GameOption[];
  positionOptions: GameOption[];
  selectedTiers: number[];
  selectedPositions: number[];
  onTierChange: (tierIds: number[]) => void;
  onPositionChange: (positionIds: number[]) => void;
  onReset: () => void;
}

/**
 * Room filter component for filtering rooms by tier and position.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */
export function RoomFilter({
  tierOptions,
  positionOptions,
  selectedTiers,
  selectedPositions,
  onTierChange,
  onPositionChange,
  onReset,
}: RoomFilterProps) {
  const handleTierToggle = (tierId: number) => {
    if (selectedTiers.includes(tierId)) {
      onTierChange(selectedTiers.filter(id => id !== tierId));
    } else {
      onTierChange([...selectedTiers, tierId]);
    }
  };

  const handlePositionToggle = (positionId: number) => {
    if (selectedPositions.includes(positionId)) {
      onPositionChange(selectedPositions.filter(id => id !== positionId));
    } else {
      onPositionChange([...selectedPositions, positionId]);
    }
  };

  const hasActiveFilters = selectedTiers.length > 0 || selectedPositions.length > 0;

  return (
    <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-4 mb-6">
      <div className="flex justify-between items-center mb-4">
        <h3 className="text-lg font-bold text-white">필터</h3>
        {hasActiveFilters && (
          <button
            onClick={onReset}
            className="text-sm text-purple-400 hover:text-purple-300 transition"
          >
            초기화
          </button>
        )}
      </div>

      {/* Tier Filter */}
      {tierOptions.length > 0 && (
        <div className="mb-4">
          <h4 className="text-sm font-semibold text-gray-300 mb-2">티어</h4>
          <div className="flex flex-wrap gap-2">
            {tierOptions.map((option) => (
              <button
                key={option.id}
                onClick={() => handleTierToggle(option.id)}
                className={`
                  px-3 py-1 rounded text-sm font-medium transition
                  ${selectedTiers.includes(option.id)
                    ? 'bg-purple-500 text-white'
                    : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }
                `}
              >
                {option.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Position Filter */}
      {positionOptions.length > 0 && (
        <div>
          <h4 className="text-sm font-semibold text-gray-300 mb-2">포지션</h4>
          <div className="flex flex-wrap gap-2">
            {positionOptions.map((option) => (
              <button
                key={option.id}
                onClick={() => handlePositionToggle(option.id)}
                className={`
                  px-3 py-1 rounded text-sm font-medium transition
                  ${selectedPositions.includes(option.id)
                    ? 'bg-cyan-500 text-white'
                    : 'bg-gray-700 text-gray-300 hover:bg-gray-600'
                  }
                `}
              >
                {option.name}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Empty State */}
      {tierOptions.length === 0 && positionOptions.length === 0 && (
        <p className="text-sm text-gray-400 text-center py-2">
          필터링 옵션이 없습니다
        </p>
      )}
    </div>
  );
}
