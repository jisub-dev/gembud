import GameGrid from '@/components/game/GameGrid';
import Button from '@/components/common/Button';
import { RoomFilter } from '@/components/room/RoomFilter';
import { Link, useNavigate } from 'react-router-dom';
import { ArrowRight, Sparkles, Thermometer, Search } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useGames } from '@/hooks/queries/useGames';
import { useRecommendedRooms } from '@/hooks/queries/useMatching';
import AdBanner from '@/components/common/AdBanner';
import { useAds } from '@/hooks/queries/useAds';
import { useEffect, useMemo, useState } from 'react';
import { isPremiumActive } from '@/config/features';

/**
 * Home page with game grid and recommended rooms section.
 */
const HomePage = () => {
  const { isAuthenticated, user } = useAuthStore();
  const navigate = useNavigate();

  const { data: games = [], isLoading: gamesLoading, error: gamesError } = useGames();
  const [selectedGameId, setSelectedGameId] = useState<number | undefined>();
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedTiers, setSelectedTiers] = useState<number[]>([]);
  const [selectedPositions, setSelectedPositions] = useState<number[]>([]);

  const filteredGames = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();
    if (!keyword) return games;
    return games.filter((game) => game.name.toLowerCase().includes(keyword));
  }, [games, searchQuery]);

  useEffect(() => {
    if (!filteredGames.length) {
      setSelectedGameId(undefined);
      return;
    }

    if (!selectedGameId || !filteredGames.some((game) => game.id === selectedGameId)) {
      setSelectedGameId(filteredGames[0].id);
    }
  }, [filteredGames, selectedGameId]);

  const recommendationLimit = isPremiumActive(user?.isPremium) ? 20 : 10;
  const { data: recommendedRooms = [], isLoading: recsLoading } = useRecommendedRooms(selectedGameId, recommendationLimit);
  const { data: ads = [] } = useAds();
  const showAds = !isPremiumActive(user?.isPremium);

  const selectedGame = games.find((g) => g.id === selectedGameId);
  const tierOptions = selectedGame?.options.filter((option) => option.optionType === 'TIER') ?? [];
  const positionOptions = selectedGame?.options.filter((option) => option.optionType === 'POSITION') ?? [];

  useEffect(() => {
    setSelectedTiers([]);
    setSelectedPositions([]);
  }, [selectedGameId]);

  const filteredRecommendedRooms = useMemo(() => {
    const selectedTierValues = selectedTiers
      .map((id) => tierOptions.find((option) => option.id === id)?.optionKey)
      .filter(Boolean) as string[];
    const selectedPositionValues = selectedPositions
      .map((id) => positionOptions.find((option) => option.id === id)?.optionKey)
      .filter(Boolean) as string[];

    return recommendedRooms.filter((rec) => {
      const filters = rec.room?.filters ?? {};

      if (selectedTierValues.length > 0) {
        const roomTier = filters.tier ?? filters.TIER;
        if (!roomTier || !selectedTierValues.includes(roomTier)) return false;
      }

      if (selectedPositionValues.length > 0) {
        const roomPosition = filters.position ?? filters.POSITION;
        if (!roomPosition || !selectedPositionValues.includes(roomPosition)) return false;
      }

      return true;
    });
  }, [recommendedRooms, selectedTiers, selectedPositions, tierOptions, positionOptions]);

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
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="font-display text-h2 text-white">게임 목록</h2>
            <p className="text-text-secondary mt-2">원하는 게임을 선택하고 방을 찾아보세요</p>
          </div>
          <div className="w-full max-w-xs">
            <label htmlFor="home-game-search" className="sr-only">
              게임 검색
            </label>
            <div className="relative">
              <Search size={16} className="absolute left-3 top-1/2 -translate-y-1/2 text-text-muted" />
              <input
                id="home-game-search"
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="게임 이름으로 검색"
                className="w-full rounded-lg border border-neon-purple/30 bg-dark-secondary py-2 pl-9 pr-3 text-sm text-text-primary placeholder:text-text-muted focus:border-neon-purple focus:outline-none"
              />
            </div>
          </div>
        </div>

        {gamesLoading ? (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6">
            {[...Array(8)].map((_, i) => (
              <div key={i} className="aspect-[3/4] bg-dark-secondary rounded-card animate-pulse" />
            ))}
          </div>
        ) : gamesError ? (
          <div className="text-center py-12 text-neon-pink font-gaming">게임 목록을 불러올 수 없습니다</div>
        ) : (
          <GameGrid
            games={filteredGames}
            selectedGameId={selectedGameId}
            onGameSelect={setSelectedGameId}
            emptyMessage={
              searchQuery.trim()
                ? `'${searchQuery.trim()}'에 해당하는 게임이 없습니다`
                : '등록된 게임이 없습니다'
            }
          />
        )}
      </section>

      {/* Recommended Rooms Section */}
      {isAuthenticated && (
        <section className="space-y-6 pt-12">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-display text-h2 text-white">추천 방</h2>
              <p className="text-text-secondary mt-2">
                {selectedGame ? `${selectedGame.name} 기준 추천 방입니다` : '게임을 선택하면 추천 방을 보여드립니다'}
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

          {selectedGameId && (
            <RoomFilter
              tierOptions={tierOptions}
              positionOptions={positionOptions}
              selectedTiers={selectedTiers}
              selectedPositions={selectedPositions}
              onTierChange={setSelectedTiers}
              onPositionChange={setSelectedPositions}
              onReset={() => {
                setSelectedTiers([]);
                setSelectedPositions([]);
              }}
            />
          )}

          {!selectedGameId ? (
            <div className="text-center py-12 text-text-muted font-gaming">게임을 선택해주세요</div>
          ) : recsLoading ? (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {[...Array(3)].map((_, i) => (
                <div
                  key={i}
                  className="h-52 bg-dark-secondary border border-neon-purple/20 rounded-card animate-pulse"
                />
              ))}
            </div>
          ) : filteredRecommendedRooms.length === 0 ? (
            <div className="text-center py-12 text-text-muted font-gaming">
              현재 모집 중인 방이 없습니다. 방을 만들어보세요!
            </div>
          ) : (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {filteredRecommendedRooms.map((rec) => {
                const score = Math.round(rec.matchingScore ?? 0);
                const scoreColor =
                  score >= 80
                    ? 'bg-neon-cyan'
                    : score >= 60
                    ? 'bg-neon-purple'
                    : score >= 40
                    ? 'bg-neon-pink'
                    : 'bg-text-muted';

                const room = rec.room;
                const roomPublicId = room?.publicId;

                return (
                  <div
                    key={room?.publicId ?? room?.id ?? rec.roomId}
                    className="bg-dark-secondary border border-neon-purple/30 rounded-card p-5 hover:border-neon-purple hover:shadow-glow-purple transition-all"
                  >
                    <div className="flex items-center justify-between mb-2">
                      <span className="text-xs font-gaming text-neon-cyan">{room?.gameName ?? selectedGame?.name}</span>
                      <span className="text-xs font-gaming text-neon-cyan bg-neon-cyan/10 px-2 py-1 rounded-full flex-shrink-0">
                        매칭
                      </span>
                    </div>

                    <h3 className="font-semibold text-text-primary truncate mb-3">{room?.title ?? '방'}</h3>

                    <div className="mb-3">
                      <div className="flex items-center gap-2 mb-1">
                        <Sparkles size={11} className="text-neon-cyan flex-shrink-0" />
                        <span className="text-xs text-text-muted font-gaming">매칭 점수</span>
                      </div>
                      <div className="h-1.5 bg-dark-primary rounded-full overflow-hidden">
                        <div className={`h-full rounded-full transition-all ${scoreColor}`} style={{ width: `${score}%` }} />
                      </div>
                    </div>

                    <div className="flex items-center gap-3 text-sm text-text-secondary mb-4">
                      <span>
                        {room?.currentParticipants ?? 0}/{room?.maxParticipants ?? '?'}명
                      </span>
                      {rec.hostTemperature !== undefined && (
                        <span className="flex items-center gap-0.5 text-neon-pink">
                          <Thermometer size={12} />
                          {Number(rec.hostTemperature).toFixed(1)}°C
                        </span>
                      )}
                    </div>

                    <button
                      type="button"
                      onClick={() => {
                        if (!selectedGameId) return;
                        const path = `/games/${selectedGameId}/rooms`;
                        navigate(roomPublicId ? `${path}?room=${roomPublicId}` : path);
                      }}
                      className="w-full rounded-lg bg-neon-purple px-4 py-2 text-sm font-semibold text-white hover:bg-neon-pink transition-colors"
                    >
                      입장하기
                    </button>
                  </div>
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
