import { Link } from 'react-router-dom';
import { useAuthStore } from '../../store/authStore';
import { useState } from 'react';
import { useQueryClient } from '@tanstack/react-query';
import { Bell, ChevronDown, User, LogOut, Crown } from 'lucide-react';
import { notificationKeys, useUnreadNotificationCount } from '@/hooks/queries/useNotifications';
import PremiumBadge from '@/components/common/PremiumBadge';
import { featureFlags } from '@/config/features';

function getTemperatureColor(temperature: number): string {
  if (temperature >= 40) return 'text-neon-cyan';
  if (temperature >= 36.5) return 'text-neon-green';
  if (temperature >= 30) return 'text-orange-400';
  return 'text-neon-pink';
}

export default function Header() {
  const { user, logout } = useAuthStore();
  const [showProfileMenu, setShowProfileMenu] = useState(false);
  const queryClient = useQueryClient();
  const { data: unreadCount = 0 } = useUnreadNotificationCount();

  return (
    <header className="sticky top-0 z-50 bg-dark-secondary border-b border-neon-purple/30 shadow-glow-purple backdrop-blur-sm">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link to="/" className="flex items-center space-x-3 group">
            <div className="w-10 h-10 bg-gradient-to-br from-neon-purple to-neon-pink rounded-lg flex items-center justify-center transform group-hover:scale-110 transition-transform duration-200">
              <span className="text-2xl font-bold text-white">G</span>
            </div>
            <span className="text-2xl font-display tracking-wider bg-gradient-to-r from-neon-purple to-neon-pink bg-clip-text text-transparent">
              GEMBUD
            </span>
          </Link>

          {/* Navigation */}
          <nav className="hidden md:flex items-center space-x-1">
            <NavLink to="/">홈</NavLink>
            <NavLink to="/friends">친구</NavLink>
          </nav>

          {/* Right Section */}
          <div className="flex items-center space-x-4">
            {user ? (
              <>
                {/* Notification Button */}
                <Link
                  to="/notifications"
                  onClick={() => {
                    queryClient.setQueryData(notificationKeys.unreadCount(), 0);
                  }}
                  className="relative p-2 rounded-lg hover:bg-dark-tertiary transition-colors group"
                >
                  <Bell className="w-6 h-6 text-text-secondary group-hover:text-neon-cyan transition-colors" />
                  {unreadCount > 0 && (
                    <span className="absolute -top-1 -right-1 w-5 h-5 bg-neon-pink rounded-full flex items-center justify-center text-xs font-gaming font-bold animate-glow-pulse">
                      {unreadCount}
                    </span>
                  )}
                </Link>

                {/* Profile Dropdown */}
                <div className="relative">
                  <button
                    onClick={() => setShowProfileMenu(!showProfileMenu)}
                    className="flex items-center space-x-3 p-2 rounded-lg hover:bg-dark-tertiary transition-colors group"
                  >
                    <div className="w-8 h-8 rounded-full bg-gradient-to-br from-neon-purple to-neon-cyan flex items-center justify-center ring-2 ring-neon-purple/50 group-hover:ring-neon-purple transition-all">
                      <span className="text-sm font-bold text-white">
                        {user.nickname[0]}
                      </span>
                    </div>
                    <div className="hidden sm:block text-left">
                      <div className="flex items-center gap-1.5">
                        <p className="text-sm font-medium text-text-primary">{user.nickname}</p>
                        {featureFlags.premium && user.isPremium && <PremiumBadge />}
                      </div>
                      <p className={`text-xs font-gaming ${getTemperatureColor(user.temperature)}`}>
                        {user.temperature}°C
                      </p>
                    </div>
                    <ChevronDown
                      className={`w-4 h-4 text-text-secondary transition-transform ${showProfileMenu ? 'rotate-180' : ''}`}
                    />
                  </button>

                  {/* Dropdown Menu */}
                  {showProfileMenu && (
                    <div className="absolute right-0 mt-2 w-48 bg-dark-secondary border border-neon-purple/30 rounded-lg shadow-glow-purple overflow-hidden animate-fade-in">
                      <Link
                        to={`/profile/${user.id}`}
                        className="flex items-center gap-3 px-4 py-3 hover:bg-dark-tertiary transition-colors text-text-primary"
                        onClick={() => setShowProfileMenu(false)}
                      >
                        <User size={16} className="text-gray-400" />
                        내 프로필
                      </Link>
                      {featureFlags.premium && !user.isPremium && (
                        <Link
                          to="/premium"
                          className="flex items-center gap-3 px-4 py-3 hover:bg-dark-tertiary transition-colors text-yellow-400"
                          onClick={() => setShowProfileMenu(false)}
                        >
                          <Crown size={16} />
                          PRO 업그레이드
                        </Link>
                      )}
                      <button
                        onClick={() => {
                          logout();
                          setShowProfileMenu(false);
                        }}
                        className="flex items-center gap-3 w-full text-left px-4 py-3 hover:bg-dark-tertiary transition-colors text-neon-pink"
                      >
                        <LogOut size={16} />
                        로그아웃
                      </button>
                    </div>
                  )}
                </div>
              </>
            ) : (
              <div className="flex items-center space-x-3">
                <Link
                  to="/login"
                  className="px-4 py-2 text-sm font-medium text-text-primary hover:text-neon-purple transition-colors"
                >
                  Sign In
                </Link>
                <span className="text-text-secondary">|</span>
                <Link
                  to="/signup"
                  className="px-4 py-2 text-sm font-medium bg-gradient-to-r from-neon-purple to-neon-pink rounded-lg hover:shadow-glow-purple transition-all transform hover:scale-105"
                >
                  Sign Up
                </Link>
              </div>
            )}
          </div>
        </div>
      </div>
    </header>
  );
}

function NavLink({ to, children }: { to: string; children: React.ReactNode }) {
  return (
    <Link
      to={to}
      className="px-4 py-2 rounded-lg font-medium text-text-secondary hover:text-text-primary hover:bg-dark-tertiary transition-all relative group"
    >
      {children}
      <span className="absolute bottom-0 left-1/2 -translate-x-1/2 w-0 h-0.5 bg-gradient-to-r from-neon-purple to-neon-pink group-hover:w-3/4 transition-all duration-300" />
    </Link>
  );
}
