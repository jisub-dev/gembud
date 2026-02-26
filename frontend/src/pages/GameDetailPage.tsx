import { useParams, useNavigate } from 'react-router-dom';
import { useGame } from '@/hooks/queries/useGames';

/**
 * Game detail page with description and navigation to rooms
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
export default function GameDetailPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { data: game, isLoading, error } = useGame(Number(id));

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  if (error || !game) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-center">
          <div className="text-white text-xl mb-4">게임을 찾을 수 없습니다</div>
          <button
            onClick={() => navigate('/')}
            className="text-purple-400 hover:text-purple-300"
          >
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
          className="text-gray-400 hover:text-white mb-6 transition"
        >
          ← 뒤로가기
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
          <h1 className="text-4xl font-bold mb-4">{game.name}</h1>
          <p className="text-gray-400 text-lg mb-6">{game.description}</p>

          <div className="flex items-center gap-4">
            <span className="px-4 py-2 bg-purple-500/20 border border-purple-500 text-purple-300 rounded-full">
              {game.genre}
            </span>
          </div>
        </div>

        {/* Game Options */}
        {game.options && game.options.length > 0 && (
          <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
            <h2 className="text-2xl font-semibold mb-4">게임 옵션</h2>
            <div className="space-y-4">
              {game.options.map((option: any) => (
                <div key={option.id}>
                  <h3 className="text-lg font-semibold text-purple-400 mb-2">{option.optionKey}</h3>
                  <div className="flex flex-wrap gap-2">
                    {JSON.parse(option.optionValues).map((value: string, index: number) => (
                      <span
                        key={index}
                        className="px-3 py-1 bg-[#0e0e10] border border-gray-600 rounded text-sm"
                      >
                        {value}
                      </span>
                    ))}
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* Call to Action */}
        <div className="bg-gradient-to-r from-purple-500/20 to-pink-500/20 border-2 border-purple-500 rounded-lg p-8 text-center">
          <h2 className="text-2xl font-bold mb-4">함께 게임하실 분을 찾고 계신가요?</h2>
          <p className="text-gray-300 mb-6">
            {game.name}을(를) 즐기는 게이머들과 함께하세요!
          </p>
          <button
            onClick={() => navigate(`/games/${game.id}/rooms`)}
            className="px-8 py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition text-lg"
          >
            방 목록 보기
          </button>
        </div>
      </div>
    </div>
  );
}
