import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Gamepad2, ArrowRight, Plus } from 'lucide-react';
import { useGame } from '@/hooks/queries/useGames';
import { useRooms } from '@/hooks/queries/useRooms';
import LoadingSpinner from '@/components/common/LoadingSpinner';

function parseOptionValues(json: string): string[] {
  try {
    const parsed = JSON.parse(json);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

export default function GameDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const gameId = Number(id);
  const { data: game, isLoading, error } = useGame(gameId);
  const { data: rooms = [] } = useRooms(gameId);

  const tierOption = game?.options.find((option) => option.optionType === 'TIER');
  const positionOption = game?.options.find((option) => option.optionType === 'POSITION');
  const tiers = tierOption ? parseOptionValues(tierOption.optionValues) : [];
  const positions = positionOption ? parseOptionValues(positionOption.optionValues) : [];
  const openRoomCount = rooms.filter((room) => room.status === 'OPEN').length;

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <LoadingSpinner size="lg" label="게임 정보를 불러오는 중..." />
      </div>
    );
  }

  if (error || !game) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-center">
          <Gamepad2 size={48} className="text-gray-600 mx-auto mb-4" />
          <div className="text-white text-xl mb-4">게임을 찾을 수 없습니다</div>
          <button onClick={() => navigate('/')} className="text-purple-400 hover:text-purple-300">
            홈으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Back Button */}
        <button
          onClick={() => navigate('/')}
          className="flex items-center gap-1.5 text-gray-400 hover:text-white mb-6 transition"
        >
          <ChevronLeft size={18} />
          뒤로가기
        </button>

        {/* Game Header */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-8 mb-6">
          {game.imageUrl && (
            <img
              src={game.imageUrl}
              alt={game.name}
              className="w-full h-64 object-cover rounded-lg mb-6"
            />
          )}
          <h1 className="text-4xl font-bold mb-3">{game.name}</h1>
          <p className="text-gray-400 text-lg mb-6">{game.description}</p>

          <div className="flex flex-wrap items-center gap-4">
            <span className="px-4 py-2 bg-purple-500/20 border border-purple-500 text-purple-300 rounded-full">
              {game.genre}
            </span>
            <span className="px-4 py-2 bg-emerald-500/20 border border-emerald-500/50 text-emerald-300 rounded-full">
              OPEN 방 {openRoomCount}개
            </span>
          </div>
        </div>

        {/* Tier / Position Options */}
        {(tiers.length > 0 || positions.length > 0) && (
          <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
            <h2 className="text-2xl font-semibold mb-4">추천 매칭 옵션</h2>
            <div className="space-y-4">
              {tiers.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-purple-400 mb-2">티어</h3>
                  <div className="flex flex-wrap gap-2">
                    {tiers.map((value, index) => (
                      <span
                        key={index}
                        className="px-3 py-1 bg-[#0e0e10] border border-purple-500/40 text-purple-200 rounded text-sm font-semibold"
                      >
                        {value}
                      </span>
                    ))}
                  </div>
                </div>
              )}
              {positions.length > 0 && (
                <div>
                  <h3 className="text-lg font-semibold text-cyan-400 mb-2">포지션</h3>
                  <div className="flex flex-wrap gap-2">
                    {positions.map((value, index) => (
                      <span
                        key={index}
                        className="px-3 py-1 bg-[#0e0e10] border border-cyan-500/40 text-cyan-200 rounded text-sm font-semibold"
                      >
                        {value}
                      </span>
                    ))}
                  </div>
                </div>
              )}
            </div>
          </div>
        )}

        {/* Call to Action */}
        <div className="bg-gradient-to-r from-purple-500/20 to-pink-500/20 border-2 border-purple-500 rounded-lg p-8 text-center">
          <Gamepad2 size={40} className="text-purple-400 mx-auto mb-4" />
          <h2 className="text-2xl font-bold mb-4">함께 게임하실 분을 찾고 계신가요?</h2>
          <p className="text-gray-300 mb-6">
            {game.name}을(를) 즐기는 게이머들과 함께하세요!
          </p>
          <div className="flex flex-wrap items-center justify-center gap-3">
            <button
              onClick={() => navigate(`/games/${game.id}/rooms`)}
              className="inline-flex items-center gap-2 px-6 py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition"
            >
              방 목록 보기
              <ArrowRight size={18} />
            </button>
            <button
              onClick={() => navigate(`/games/${game.id}/rooms?create=true`)}
              className="inline-flex items-center gap-2 px-6 py-3 bg-cyan-500 hover:bg-cyan-600 text-white font-bold rounded-lg transition"
            >
              방 만들기
              <Plus size={18} />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
