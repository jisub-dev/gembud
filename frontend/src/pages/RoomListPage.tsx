import { useState, useMemo } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Plus } from 'lucide-react';
import { useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { RoomGrid } from '@/components/room/RoomGrid';
import { RoomFilter } from '@/components/room/RoomFilter';
import { CreateRoomModal } from '@/components/room/CreateRoomModal';
import AdBanner from '@/components/common/AdBanner';
import { useAds } from '@/hooks/queries/useAds';
import { useAuthStore } from '@/store/authStore';

export function RoomListPage() {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { data: ads = [] } = useAds();
  const showAds = !user?.isPremium;

  const [selectedTiers, setSelectedTiers] = useState<number[]>([]);
  const [selectedPositions, setSelectedPositions] = useState<number[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);

  const { data: rooms, isLoading: roomsLoading, error: roomsError } = useRooms(Number(gameId));
  const { game, tierOptions, positionOptions, isLoading: gameLoading } = useGameOptions(Number(gameId));

  const filteredRooms = useMemo(() => {
    if (!rooms) return [];

    const selectedTierValues = selectedTiers
      .map(id => tierOptions.find(o => o.id === id)?.optionKey)
      .filter(Boolean) as string[];

    const selectedPositionValues = selectedPositions
      .map(id => positionOptions.find(o => o.id === id)?.optionKey)
      .filter(Boolean) as string[];

    return rooms.filter(room => {
      if (room.status !== 'OPEN') return false;

      const filters = room.filters ?? {};

      if (selectedTierValues.length > 0) {
        const roomTierValue = filters['tier'] ?? filters['TIER'];
        if (!roomTierValue || !selectedTierValues.includes(roomTierValue)) return false;
      }

      if (selectedPositionValues.length > 0) {
        const roomPositionValue = filters['position'] ?? filters['POSITION'];
        if (!roomPositionValue || !selectedPositionValues.includes(roomPositionValue)) return false;
      }

      return true;
    });
  }, [rooms, selectedTiers, selectedPositions, tierOptions, positionOptions]);

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
                className="flex items-center gap-1.5 text-gray-400 hover:text-white mb-2 transition"
              >
                <ChevronLeft size={18} />
                뒤로가기
              </button>
              <h1 className="text-3xl font-bold text-white">{game.name}</h1>
              <p className="text-gray-400 mt-1">{game.description}</p>
            </div>
            <button
              className="flex items-center gap-2 px-6 py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition"
              onClick={() => setShowCreateModal(true)}
            >
              <Plus size={18} />
              방 만들기
            </button>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-8">
        {/* Leaderboard banner — 방 목록 상단 */}
        {showAds && (
          <div className="mb-8 flex justify-center">
            <AdBanner type="leaderboard" adData={ads[0] ?? null} />
          </div>
        )}

        <RoomFilter
          tierOptions={tierOptions}
          positionOptions={positionOptions}
          selectedTiers={selectedTiers}
          selectedPositions={selectedPositions}
          onTierChange={setSelectedTiers}
          onPositionChange={setSelectedPositions}
          onReset={handleReset}
        />

        {roomsError && (
          <div className="bg-red-500/20 border border-red-500 rounded-lg p-4 mb-6">
            <p className="text-red-400">방 목록을 불러오는 중 오류가 발생했습니다.</p>
          </div>
        )}

        <RoomGrid rooms={filteredRooms} isLoading={roomsLoading} onRoomClick={handleRoomClick} />

        {/* Inline banner — 방 5개 이상일 때 목록 아래 */}
        {showAds && filteredRooms.length >= 5 && (
          <div className="mt-8 flex justify-center">
            <AdBanner type="inline" adData={ads[1] ?? null} />
          </div>
        )}

        {!roomsLoading && rooms && rooms.length > 0 && filteredRooms.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-400 mb-4">조건에 맞는 방이 없습니다</p>
            <button onClick={handleReset} className="text-purple-400 hover:text-purple-300 transition">
              필터 초기화
            </button>
          </div>
        )}
      </div>

      {showCreateModal && game && (
        <CreateRoomModal
          gameId={Number(gameId)}
          gameName={game.name}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: roomKeys.list(Number(gameId)) });
            setShowCreateModal(false);
          }}
        />
      )}
    </div>
  );
}
