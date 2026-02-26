import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';

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

// Protected Route Wrapper
interface ProtectedRouteProps {
  children: React.ReactNode;
}

function ProtectedRoute({ children }: ProtectedRouteProps) {
  const { isAuthenticated } = useAuthStore();

  if (!isAuthenticated) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
}

// Temporary placeholder component (Phase별로 실제 페이지로 교체)
function PlaceholderPage({ title }: { title: string }) {
  return (
    <div className="flex items-center justify-center min-h-screen bg-dark-primary">
      <div className="text-center">
        <h1 className="text-4xl font-display text-text-primary mb-4">{title}</h1>
        <p className="text-text-secondary">이 페이지는 Phase별로 구현 예정입니다.</p>
      </div>
    </div>
  );
}

function App() {
  return (
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
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
