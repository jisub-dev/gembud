import GameGrid from '@/components/game/GameGrid';
import Button from '@/components/common/Button';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles, Thermometer } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useGames } from '@/hooks/queries/useGames';
import { useRecommendedRooms } from '@/hooks/queries/useMatching';
import AdBanner from '@/components/common/AdBanner';
import { useAds } from '@/hooks/queries/useAds';
import { useState, useEffect } from 'react';

/**
 * Home page with game grid and recommended rooms section.
 */
const HomePage = () => {
  const { isAuthenticated, user } = useAuthStore();
  const navigate = useNavigate();

  const { data: games = [] } = useGames();
  const [selectedGameId, setSelectedGameId] = useState<number | undefined>();

  useEffect(() => {
    if (games[0] && !selectedGameId) {
      setSelectedGameId(games[0].id);
    }
  }, [games, selectedGameId]);

  const { data: recommendedRooms = [], isLoading: recsLoading } = useRecommendedRooms(selectedGameId, 3);
  const { data: ads = [] } = useAds();
  const showAds = !user?.isPremium;

  const selectedGame = games.find((g) => g.id === selectedGameId);

  return (
    <div className="space-y-12">
      {/* Hero Section */}
      <section className="text-center py-12 space-y-6">
        <h1 className="font-display text-hero bg-gradient-to-r from-neon-purple via-neon-pink to-neon-cyan bg-clip-text text-transparent animate-fade-in">
          GEMBUD
        </h1>
        <p className="text-text-secondary text-xl font-gaming max-w-2xl mx-auto animate-fade-in">
          함께 게임할 친구를 찾고, 팀을 만들고, 소통하세요
        </p>

        {!isAuthenticated && (
          <div className="flex items-center justify-center space-x-4 pt-4 animate-fade-in">
            <Link to="/signup">
              <Button variant="primary" size="lg">
                시작하기
              </Button>
            </Link>
            <Link to="/login">
              <Button variant="secondary" size="lg">
                로그인
              </Button>
            </Link>
          </div>
        )}
      </section>

      {/* Divider */}
      <div className="h-px bg-gradient-to-r from-transparent via-neon-purple/30 to-transparent" />

      {/* Games Section */}
      <section className="space-y-8">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="font-display text-h2 text-white">게임 목록</h2>
            <p className="text-text-secondary mt-2">
              원하는 게임을 선택하고 방을 찾아보세요
            </p>
          </div>
        </div>

        <GameGrid />
      </section>

      {/* Recommended Rooms Section */}
      {isAuthenticated && (
        <section className="space-y-6 pt-12">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-display text-h2 text-white">추천 방</h2>
              <p className="text-text-secondary mt-2">
                당신에게 맞는 팀원을 AI가 추천합니다
              </p>
            </div>
            {selectedGameId && (
              <Link
                to={`/games/${selectedGameId}/rooms`}
                className="flex items-center gap-1 text-neon-purple hover:text-neon-pink text-sm font-gaming transition-colors"
              >
                모두 보기
                <ArrowRight size={14} />
              </Link>
            )}
          </div>

          {/* Game Tabs */}
          {games.length > 0 && (
            <div className="flex gap-2 overflow-x-auto pb-1 scrollbar-hide">
              {games.map((game) => (
                <button
                  key={game.id}
                  onClick={() => setSelectedGameId(game.id)}
                  className={`flex-shrink-0 px-4 py-1.5 rounded-full text-sm font-gaming transition-all ${
                    selectedGameId === game.id
                      ? 'bg-neon-purple text-white shadow-glow-purple'
                      : 'bg-dark-secondary border border-neon-purple/30 text-text-secondary hover:border-neon-purple hover:text-text-primary'
                  }`}
                >
                  {game.name}
                </button>
              ))}
            </div>
          )}

          {recsLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(3)].map((_, i) => (
                <div
                  key={i}
                  className="h-52 bg-dark-secondary border border-neon-purple/20 rounded-card animate-pulse"
                />
              ))}
            </div>
          ) : recommendedRooms.length === 0 ? (
            <div className="text-center py-12 text-text-muted font-gaming">
              추천할 방이 없습니다. 방을 먼저 만들어보세요!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {recommendedRooms.map((rec) => {
                const score = Math.round(rec.matchingScore ?? 0);
                const scoreColor =
                  score >= 80
                    ? 'bg-neon-cyan'
                    : score >= 60
                    ? 'bg-neon-purple'
                    : score >= 40
                    ? 'bg-neon-pink'
                    : 'bg-text-muted';

                const filters = rec.room?.filters ?? {};
                const filterEntries = Object.entries(filters);

                return (
                  <button
                    key={rec.room?.id ?? rec.roomId}
                    onClick={() => navigate(`/rooms/${rec.room?.id ?? rec.roomId}`)}
                    className="bg-dark-secondary border border-neon-purple/30 rounded-card p-5 text-left hover:border-neon-purple hover:shadow-glow-purple transition-all group"
                  >
                    {/* Game name + score badge */}
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-gaming text-neon-cyan">
                        {rec.room?.gameName ?? selectedGame?.name}
                      </span>
                      {rec.matchingScore !== undefined && (
                        <span className="text-xs font-gaming text-neon-cyan bg-neon-cyan/10 px-2 py-1 rounded-full flex-shrink-0">
                          {score}점
                        </span>
                      )}
                    </div>

                    <h3 className="font-semibold text-text-primary truncate group-hover:text-neon-purple transition-colors mb-3">
                      {rec.room?.title ?? '방'}
                    </h3>

                    {/* Matching score progress bar */}
                    {rec.matchingScore !== undefined && (
                      <div className="mb-3">
                        <div className="flex items-center gap-2 mb-1">
                          <Sparkles size={11} className="text-neon-cyan flex-shrink-0" />
                          <span className="text-xs text-text-muted font-gaming">매칭도</span>
                        </div>
                        <div className="h-1.5 bg-dark-primary rounded-full overflow-hidden">
                          <div
                            className={`h-full rounded-full transition-all ${scoreColor}`}
                            style={{ width: `${score}%` }}
                          />
                        </div>
                      </div>
                    )}

                    {/* Filter tags */}
                    {filterEntries.length > 0 && (
                      <div className="flex flex-wrap gap-1 mb-3">
                        {filterEntries.map(([key, value]) => (
                          <span
                            key={key}
                            className="text-xs bg-neon-purple/10 text-neon-purple border border-neon-purple/30 px-2 py-0.5 rounded-full font-gaming"
                          >
                            {value}
                          </span>
                        ))}
                      </div>
                    )}

                    <div className="flex items-center gap-3 text-sm text-text-secondary mb-3">
                      <span>
                        {rec.room?.currentParticipants ?? 0}/{rec.room?.maxParticipants ?? '?'}명
                      </span>
                      <span>•</span>
                      <span>{rec.room?.createdBy}</span>
                      {rec.hostTemperature !== undefined && (
                        <>
                          <span>•</span>
                          <span className="flex items-center gap-0.5 text-neon-pink">
                            <Thermometer size={12} />
                            {Number(rec.hostTemperature).toFixed(1)}°C
                          </span>
                        </>
                      )}
                    </div>
                    {rec.reason && (
                      <p className="text-xs text-text-muted line-clamp-2">{rec.reason}</p>
                    )}
                  </button>
                );
              })}
            </div>
          )}
        </section>
      )}

      {/* Rectangle banner — 홈 하단 (비프리미엄) */}
      {showAds && (
        <div className="flex justify-center pt-4 pb-8">
          <AdBanner type="rectangle" adData={ads[0] ?? null} />
        </div>
      )}
    </div>
  );
};

export default HomePage;
