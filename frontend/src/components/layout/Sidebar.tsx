import { Link } from 'react-router-dom';
import { useState } from 'react';

// TODO: Phase 3에서 실제 게임 데이터로 교체
const TEMP_GAMES = [
  { id: 1, name: '리그 오브 레전드', icon: '🎮', color: 'from-yellow-500 to-amber-600' },
  { id: 2, name: '배틀그라운드', icon: '🔫', color: 'from-orange-500 to-red-600' },
  { id: 3, name: '발로란트', icon: '⚔️', color: 'from-red-500 to-pink-600' },
  { id: 4, name: '오버워치 2', icon: '🛡️', color: 'from-blue-500 to-cyan-600' },
  { id: 5, name: 'FIFA 온라인 4', icon: '⚽', color: 'from-green-500 to-emerald-600' },
];

export default function Sidebar() {
  const [temperature] = useState(36.5); // TODO: Phase 7에서 실제 온도 데이터로 교체
  const [isCollapsed, setIsCollapsed] = useState(false);

  return (
    <aside
      className={`${
        isCollapsed ? 'w-20' : 'w-64'
      } bg-dark-secondary border-r border-neon-purple/20 min-h-[calc(100vh-4rem)] transition-all duration-300 flex flex-col`}
    >
      {/* Collapse Toggle */}
      <button
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="p-4 hover:bg-dark-tertiary transition-colors flex items-center justify-center group"
      >
        <svg
          className={`w-5 h-5 text-text-secondary group-hover:text-neon-purple transition-all ${
            isCollapsed ? 'rotate-180' : ''
          }`}
          fill="none"
          stroke="currentColor"
          viewBox="0 0 24 24"
        >
          <path
            strokeLinecap="round"
            strokeLinejoin="round"
            strokeWidth={2}
            d="M11 19l-7-7 7-7m8 14l-7-7 7-7"
          />
        </svg>
      </button>

      {/* Temperature Gauge */}
      {!isCollapsed && (
        <div className="px-4 py-6 border-b border-neon-purple/10">
          <div className="text-center mb-3">
            <p className="text-xs text-text-muted mb-1">내 온도</p>
            <div className="flex items-baseline justify-center">
              <span className="text-3xl font-gaming font-bold bg-gradient-to-r from-neon-cyan to-neon-green bg-clip-text text-transparent">
                {temperature}
              </span>
              <span className="text-xl text-neon-green ml-1">°C</span>
            </div>
          </div>

          {/* Temperature Bar */}
          <div className="relative w-full h-2 bg-dark-tertiary rounded-full overflow-hidden">
            <div
              className="absolute top-0 left-0 h-full bg-gradient-to-r from-neon-cyan via-neon-green to-neon-pink rounded-full transition-all duration-500"
              style={{ width: `${temperature}%` }}
            />
          </div>

          <div className="flex justify-between text-xs text-text-muted mt-1">
            <span>0°C</span>
            <span>100°C</span>
          </div>
        </div>
      )}

      {/* Game Categories */}
      <nav className="flex-1 py-4">
        {!isCollapsed && (
          <h3 className="px-4 mb-3 text-xs font-gaming uppercase tracking-wider text-text-muted">
            게임 카테고리
          </h3>
        )}
        <div className="space-y-1 px-2">
          {TEMP_GAMES.map((game) => (
            <Link
              key={game.id}
              to={`/rooms/game/${game.id}`}
              className="flex items-center space-x-3 px-3 py-3 rounded-lg hover:bg-dark-tertiary transition-all group relative overflow-hidden"
            >
              {/* Icon */}
              <div
                className={`w-10 h-10 bg-gradient-to-br ${game.color} rounded-lg flex items-center justify-center text-xl transform group-hover:scale-110 transition-transform duration-200 flex-shrink-0`}
              >
                {game.icon}
              </div>

              {/* Game Name */}
              {!isCollapsed && (
                <div className="flex-1 min-w-0">
                  <p className="text-sm font-medium text-text-primary truncate group-hover:text-neon-purple transition-colors">
                    {game.name}
                  </p>
                </div>
              )}

              {/* Hover Glow Effect */}
              <div className="absolute inset-0 bg-gradient-to-r from-neon-purple/0 via-neon-purple/5 to-neon-purple/0 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
            </Link>
          ))}
        </div>
      </nav>

      {/* Footer - Quick Stats */}
      {!isCollapsed && (
        <div className="p-4 border-t border-neon-purple/10">
          <div className="grid grid-cols-2 gap-3 text-center">
            <div className="bg-dark-tertiary rounded-lg p-3">
              <p className="text-xs text-text-muted mb-1">총 게임</p>
              <p className="text-lg font-gaming font-bold text-neon-purple">{TEMP_GAMES.length}</p>
            </div>
            <div className="bg-dark-tertiary rounded-lg p-3">
              <p className="text-xs text-text-muted mb-1">친구</p>
              <p className="text-lg font-gaming font-bold text-neon-cyan">12</p>
            </div>
          </div>
        </div>
      )}
    </aside>
  );
}
