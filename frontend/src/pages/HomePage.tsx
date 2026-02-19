import GameGrid from '@/components/game/GameGrid';
import Button from '@/components/common/Button';
import { Link } from 'react-router-dom';
import { useAuthStore } from '@/store/authStore';

/**
 * Home page with game grid and hero section.
 */
const HomePage = () => {
  const { isAuthenticated } = useAuthStore();

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

      {/* Recommended Rooms Section (Phase 5에서 추가) */}
      {isAuthenticated && (
        <section className="space-y-8 pt-12">
          <div className="flex items-center justify-between">
            <div>
              <h2 className="font-display text-h2 text-white flex items-center space-x-3">
                <span>추천 방</span>
                <span className="text-neon-cyan text-sm font-gaming">Coming Soon</span>
              </h2>
              <p className="text-text-secondary mt-2">
                당신에게 맞는 팀원을 AI가 추천합니다
              </p>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {[...Array(3)].map((_, i) => (
              <div
                key={i}
                className="h-40 bg-dark-secondary border border-dashed border-neon-purple/30 rounded-card flex items-center justify-center"
              >
                <p className="text-text-muted font-gaming">Phase 5에서 구현 예정</p>
              </div>
            ))}
          </div>
        </section>
      )}
    </div>
  );
};

export default HomePage;
