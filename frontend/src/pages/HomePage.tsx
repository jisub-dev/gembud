import GameGrid from '@/components/game/GameGrid';
import Button from '@/components/common/Button';
import { Link, useNavigate } from 'react-router-dom';
import { Search } from 'lucide-react';
import { useAuthStore } from '@/store/authStore';
import { useGames } from '@/hooks/queries/useGames';
import AdBanner from '@/components/common/AdBanner';
import { useAds } from '@/hooks/queries/useAds';
import { useMemo, useState } from 'react';
import { isPremiumActive } from '@/config/features';

/**
 * Home page focused on game discovery.
 */
const HomePage = () => {
  const { isAuthenticated, user } = useAuthStore();
  const navigate = useNavigate();

  const { data: gamesData, isLoading: gamesLoading, error: gamesError } = useGames();
  const { data: ads = [] } = useAds();
  const [searchQuery, setSearchQuery] = useState('');
  const games = Array.isArray(gamesData) ? gamesData : [];
  const showAds = !isPremiumActive(user?.isPremium);

  const filteredGames = useMemo(() => {
    const keyword = searchQuery.trim().toLowerCase();
    if (!keyword) return games;
    return games.filter((game) => game.name.toLowerCase().includes(keyword));
  }, [games, searchQuery]);

  return (
    <div className="space-y-12">
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

      <div className="h-px bg-gradient-to-r from-transparent via-neon-purple/30 to-transparent" />

      <section className="space-y-8">
        <div className="flex items-center justify-between gap-4">
          <div>
            <h2 className="font-display text-h2 text-white">게임 목록</h2>
            <p className="text-text-secondary mt-2">원하는 게임을 선택하고 바로 방 목록으로 이동하세요</p>
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
            onGameSelect={(gameId) => navigate(`/games/${gameId}/rooms`)}
            emptyMessage={
              searchQuery.trim()
                ? `'${searchQuery.trim()}'에 해당하는 게임이 없습니다`
                : '등록된 게임이 없습니다'
            }
          />
        )}
      </section>

      {showAds && (
        <div className="flex justify-center pt-4 pb-8">
          <AdBanner type="rectangle" adData={ads[0] ?? null} />
        </div>
      )}
    </div>
  );
};

export default HomePage;
