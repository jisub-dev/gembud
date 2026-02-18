import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { useAuthStore } from './store/authStore';

// Layout
import MainLayout from './components/layout/MainLayout';

// Auth Pages
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';

// Main Pages (페이지 파일들은 Phase별로 생성 예정)
// import HomePage from './pages/HomePage';
// import GameDetailPage from './pages/GameDetailPage';
// import RoomListPage from './pages/RoomListPage';
// import RoomDetailPage from './pages/RoomDetailPage';
// import ChatPage from './pages/ChatPage';
// import ProfilePage from './pages/ProfilePage';
// import FriendListPage from './pages/FriendListPage';
// import NotificationsPage from './pages/NotificationsPage';

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

        {/* Protected Routes with MainLayout */}
        <Route
          path="/"
          element={
            <ProtectedRoute>
              <MainLayout />
            </ProtectedRoute>
          }
        >
          {/* HomePage: 게임 목록 + 추천 방 */}
          <Route index element={<PlaceholderPage title="홈" />} />
          {/* <Route index element={<HomePage />} /> */}

          {/* Game Detail: 게임 상세 */}
          <Route path="game/:id" element={<PlaceholderPage title="게임 상세" />} />
          {/* <Route path="game/:id" element={<GameDetailPage />} /> */}

          {/* Room List: 방 목록 */}
          <Route path="rooms/game/:gameId" element={<PlaceholderPage title="방 목록" />} />
          {/* <Route path="rooms/game/:gameId" element={<RoomListPage />} /> */}

          {/* Room Detail: 방 상세 */}
          <Route path="room/:id" element={<PlaceholderPage title="방 상세" />} />
          {/* <Route path="room/:id" element={<RoomDetailPage />} /> */}

          {/* Chat: 채팅 */}
          <Route path="chat/:roomId" element={<PlaceholderPage title="채팅" />} />
          {/* <Route path="chat/:roomId" element={<ChatPage />} /> */}

          {/* Profile: 프로필 */}
          <Route path="profile/:userId" element={<PlaceholderPage title="프로필" />} />
          {/* <Route path="profile/:userId" element={<ProfilePage />} /> */}

          {/* Friends: 친구 목록 */}
          <Route path="friends" element={<PlaceholderPage title="친구 목록" />} />
          {/* <Route path="friends" element={<FriendListPage />} /> */}

          {/* Notifications: 알림 */}
          <Route path="notifications" element={<PlaceholderPage title="알림" />} />
          {/* <Route path="notifications" element={<NotificationsPage />} /> */}
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  );
}

export default App;
