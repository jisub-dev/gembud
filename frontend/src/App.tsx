import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import { ToastContainer } from './components/common/ToastContainer';
import { authService } from './services/authService';

// Layout
import MainLayout from './components/layout/MainLayout';

// Auth Pages
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';

// Main Pages
import HomePage from './pages/HomePage';
import { RoomListPage } from './pages/RoomListPage';
import { RoomDetailPage } from './pages/RoomDetailPage';
import GameDetailPage from './pages/GameDetailPage';
import ChatPage from './pages/ChatPage';
import ProfilePage from './pages/ProfilePage';
import FriendListPage from './pages/FriendListPage';
import NotificationsPage from './pages/NotificationsPage';
import PremiumPage from './pages/PremiumPage';

// Protected Route Wrapper
interface ProtectedRouteProps {
  children: React.ReactNode;
}

function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated, isLoading } = useAuthStore();

  if (isLoading) return null;

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

function App() {
  const { isAuthenticated } = useAuthStore();

  useEffect(() => {
    // 새로고침 시 HTTP-only 쿠키로 세션 복원
    if (!isAuthenticated) {
      authService.getCurrentUser()
        .then((user) => {
          useAuthStore.setState({
            user: {
              id: user.id,
              email: user.email,
              nickname: user.nickname,
              temperature: user.temperature,
              isPremium: user.isPremium,
              premiumExpiresAt: user.premiumExpiresAt,
            },
            isAuthenticated: true,
            isLoading: false,
          });
        })
        .catch(() => {
          useAuthStore.setState({ isLoading: false });
        });
    }
  }, []);

  return (
    <>
    <ToastContainer />
    <BrowserRouter>
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />

        {/* Public Routes with MainLayout */}
        <Route path="/" element={<MainLayout />}>
          {/* HomePage: 게임 목록 + 추천 방 (Public) */}
          <Route index element={<HomePage />} />

          {/* Game Detail: 게임 상세 (Public) */}
          <Route path="game/:id" element={<GameDetailPage />} />

          {/* Room List: 방 목록 (Protected) */}
          <Route
            path="games/:gameId/rooms"
            element={
              <ProtectedRoute>
                <RoomListPage />
              </ProtectedRoute>
            }
          />

          {/* Room Detail: 방 상세 (Protected) */}
          <Route
            path="rooms/:roomId"
            element={
              <ProtectedRoute>
                <RoomDetailPage />
              </ProtectedRoute>
            }
          />

          {/* Chat: 채팅 (Protected) */}
          <Route
            path="chat/:roomId"
            element={
              <ProtectedRoute>
                <ChatPage />
              </ProtectedRoute>
            }
          />

          {/* Profile: 프로필 (Protected) */}
          <Route
            path="profile/:userId"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />

          {/* Friends: 친구 목록 (Protected) */}
          <Route
            path="friends"
            element={
              <ProtectedRoute>
                <FriendListPage />
              </ProtectedRoute>
            }
          />

          {/* Notifications: 알림 (Protected) */}
          <Route
            path="notifications"
            element={
              <ProtectedRoute>
                <NotificationsPage />
              </ProtectedRoute>
            }
          />

          {/* Premium: 프리미엄 구독 (Protected) */}
          <Route
            path="premium"
            element={
              <ProtectedRoute>
                <PremiumPage />
              </ProtectedRoute>
            }
          />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
    </>
  );
}

export default App;
