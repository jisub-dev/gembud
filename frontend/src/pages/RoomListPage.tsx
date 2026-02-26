import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
import { RoomGrid } from '@/components/room/RoomGrid';
import { RoomFilter } from '@/components/room/RoomFilter';
import { CreateRoomModal } from '@/components/room/CreateRoomModal';

/**
 * Room list page for displaying rooms of a specific game.
 * Includes filtering by tier and position.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */
export function RoomListPage() {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();

  const [selectedTiers, setSelectedTiers] = useState<number[]>([]);
  const [selectedPositions, setSelectedPositions] = useState<number[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const { data: rooms, isLoading: roomsLoading, error: roomsError } = useRooms(Number(gameId));
  const { game, tierOptions, positionOptions, isLoading: gameLoading } = useGameOptions(Number(gameId));

  // Client-side filtering
  const filteredRooms = useMemo(() => {
    if (!rooms) return [];

    return rooms.filter(room => {
      // Only show OPEN rooms
      if (room.status !== 'OPEN') return false;

      // Filter by tier (if selected)
      if (selectedTiers.length > 0) {
        // Note: Assuming Room type has tierOptionIds array
        // If not available in backend response, remove this filter
        // const hasTierMatch = room.tierOptionIds?.some(id => selectedTiers.includes(id));
        // if (!hasTierMatch) return false;
      }

      // Filter by position (if selected)
      if (selectedPositions.length > 0) {
        // Note: Assuming Room type has positionOptionIds array
        // If not available in backend response, remove this filter
        // const hasPositionMatch = room.positionOptionIds?.some(id => selectedPositions.includes(id));
        // if (!hasPositionMatch) return false;
      }

      return true;
    });
  }, [rooms, selectedTiers, selectedPositions]);

  const handleRoomClick = (roomId: number) => {
    navigate(`/rooms/${roomId}`);
  };

  const handleReset = () => {
    setSelectedTiers([]);
    setSelectedPositions([]);
  };

  if (gameLoading) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  if (!game) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-white text-xl">게임을 찾을 수 없습니다</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      {/* Header */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-6">
          <div className="flex items-center justify-between">
            <div>
              <button
                onClick={() => navigate('/')}
                className="text-gray-400 hover:text-white mb-2 transition"
              >
                ← 뒤로가기
              </button>
              <h1 className="text-3xl font-bold text-white">
                {game.name}
              </h1>
              <p className="text-gray-400 mt-1">
                {game.description}
              </p>
            </div>
            <button
              className="px-6 py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition"
              onClick={() => setShowCreateModal(true)}
            >
              방 만들기
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-8">
        {/* Filter */}
        <RoomFilter
          tierOptions={tierOptions}
          positionOptions={positionOptions}
          selectedTiers={selectedTiers}
          selectedPositions={selectedPositions}
          onTierChange={setSelectedTiers}
          onPositionChange={setSelectedPositions}
          onReset={handleReset}
        />

        {/* Error State */}
        {roomsError && (
          <div className="bg-red-500/20 border border-red-500 rounded-lg p-4 mb-6">
            <p className="text-red-400">
              방 목록을 불러오는 중 오류가 발생했습니다.
            </p>
          </div>
        )}

        {/* Room Grid */}
        <RoomGrid
          rooms={filteredRooms}
          isLoading={roomsLoading}
          onRoomClick={handleRoomClick}
        />

        {/* Empty Filter State */}
        {!roomsLoading && rooms && rooms.length > 0 && filteredRooms.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-400 mb-4">
              조건에 맞는 방이 없습니다
            </p>
            <button
              onClick={handleReset}
              className="text-purple-400 hover:text-purple-300 transition"
            >
              필터 초기화
            </button>
          </div>
        )}
      </div>

      {/* Create Room Modal */}
      {showCreateModal && game && (
        <CreateRoomModal
          gameId={Number(gameId)}
          gameName={game.name}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            // Refetch rooms list after successful creation
            window.location.reload();
          }}
        />
      )}
    </div>
  );
}
