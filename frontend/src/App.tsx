import { BrowserRouter, Routes, Route, Navigate, useNavigate, useParams } from 'react-router-dom';
import { useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import { ToastContainer } from './components/common/ToastContainer';
import { SessionExpiredModal } from './components/common/SessionExpiredModal';
import { PwaInstallPrompt } from './components/common/PwaInstallPrompt';
import { PwaUpdateBanner } from './components/common/PwaUpdateBanner';
import { OfflineStatusBanner } from './components/common/OfflineStatusBanner';
import { authService } from './services/authService';
import { featureFlags, isPremiumActive } from './config/features';
import { useNotificationSocket } from './hooks/useNotificationSocket';

// Layout
import MainLayout from './components/layout/MainLayout';

// Auth Pages
import LoginPage from './pages/LoginPage';
import SignupPage from './pages/SignupPage';
import OAuth2CallbackPage from './pages/OAuth2CallbackPage';
import OnboardingPage from './pages/OnboardingPage';

// Main Pages
import HomePage from './pages/HomePage';
import { RoomListPage } from './pages/RoomListPage';
import ChatPage from './pages/ChatPage';
import ProfilePage from './pages/ProfilePage';
import FriendListPage from './pages/FriendListPage';
import NotificationsPage from './pages/NotificationsPage';
import PremiumPage from './pages/PremiumPage';
import AdminPage from './pages/AdminPage';
import AboutPage from './pages/AboutPage';
import PrivacyPolicyPage from './pages/PrivacyPolicyPage';
import TermsPage from './pages/TermsPage';
import ErrorPage from './pages/ErrorPage';

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

function SessionExpiredHandler() {
  const isSessionExpired = useAuthStore((s) => s.isSessionExpired);
  const navigate = useNavigate();

  const handleConfirm = () => {
    useAuthStore.setState({ isSessionExpired: false });
    navigate('/login', { replace: true });
  };

  return <SessionExpiredModal isOpen={isSessionExpired} onConfirm={handleConfirm} />;
}

function LegacyGameDetailRedirect() {
  const { id } = useParams<{ id: string }>();
  return <Navigate to={id ? `/games/${id}/rooms` : '/'} replace />;
}

function App() {
  useNotificationSocket();

  useEffect(() => {
    // 새로고침 시 HTTP-only 쿠키로 세션 복원 (1회만 실행)
    authService.getCurrentUser()
      .then((user) => {
        useAuthStore.setState({
          user: {
            id: user.id,
            email: user.email,
            nickname: user.nickname,
            temperature: user.temperature,
            isPremium: isPremiumActive(user.isPremium),
            premiumExpiresAt: featureFlags.premium ? user.premiumExpiresAt : null,
          },
          isAuthenticated: true,
          isLoading: false,
        });
      })
      .catch(() => {
        // 비로그인 상태 - 정상적인 경우
        useAuthStore.setState({ isLoading: false });
      });
  }, []);

  return (
    <>
    <ToastContainer />
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <SessionExpiredHandler />
      <OfflineStatusBanner />
      <PwaUpdateBanner />
      <PwaInstallPrompt />
      <Routes>
        {/* Public Routes */}
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route path="/oauth2/callback" element={<OAuth2CallbackPage />} />
        <Route
          path="/onboarding"
          element={(
            <ProtectedRoute>
              <OnboardingPage />
            </ProtectedRoute>
          )}
        />

        {/* Public Routes with MainLayout */}
        <Route path="/" element={<MainLayout />}>
          {/* HomePage: 게임 목록 + 추천 방 (Public) */}
          <Route index element={<HomePage />} />

          {/* Game redirect: 상세 대신 바로 방 목록으로 이동 */}
          <Route path="games/:id" element={<LegacyGameDetailRedirect />} />
          <Route path="game/:id" element={<LegacyGameDetailRedirect />} />

          {/* Room List: 방 목록 (Protected) */}
          <Route
            path="games/:gameId/rooms"
            element={
              <ProtectedRoute>
                <RoomListPage />
              </ProtectedRoute>
            }
          />

          {/* Room Detail: deprecated — redirect to home */}
          <Route path="rooms/:roomId" element={<Navigate to="/" replace />} />

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
            path="profile"
            element={
              <ProtectedRoute>
                <ProfilePage />
              </ProtectedRoute>
            }
          />
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

          {/* Admin: 관리자 페이지 (Protected) */}
          <Route
            path="admin"
            element={
              <ProtectedRoute>
                <AdminPage />
              </ProtectedRoute>
            }
          />

          {/* Premium: 프리미엄 구독 (Protected) */}
          <Route
            path="premium"
            element={
              featureFlags.premium ? (
                <ProtectedRoute>
                  <PremiumPage />
                </ProtectedRoute>
              ) : (
                <Navigate to="/error/404" replace />
              )
            }
          />

          {/* Static / Policy Pages (Public) */}
          <Route path="about" element={<AboutPage />} />
          <Route path="privacy" element={<PrivacyPolicyPage />} />
          <Route path="terms" element={<TermsPage />} />

          {/* Error Pages */}
          <Route path="error/:code" element={<ErrorPage />} />
        </Route>

        {/* Fallback */}
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
    </>
  );
}

export default App;
