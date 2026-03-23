import { BrowserRouter, Routes, Route, Navigate, useNavigate, useParams } from 'react-router-dom';
import { lazy, Suspense, useEffect } from 'react';
import { useAuthStore } from './store/authStore';
import { ToastContainer } from './components/common/ToastContainer';
import { PwaInstallPrompt } from './components/common/PwaInstallPrompt';
import { PwaUpdateBanner } from './components/common/PwaUpdateBanner';
import { OfflineStatusBanner } from './components/common/OfflineStatusBanner';
import RouteLoadingFallback from './components/common/RouteLoadingFallback';
import { authService } from './services/authService';
import { featureFlags, isPremiumActive } from './config/features';
import { useNotificationSocket } from './hooks/useNotificationSocket';
import { setSessionExpiredHandler } from './lib/sessionExpiryBridge';

const MainLayout = lazy(() => import('./components/layout/MainLayout'));

const LoginPage = lazy(() => import('./pages/LoginPage'));
const SignupPage = lazy(() => import('./pages/SignupPage'));
const OAuth2CallbackPage = lazy(() => import('./pages/OAuth2CallbackPage'));
const OnboardingPage = lazy(() => import('./pages/OnboardingPage'));

const HomePage = lazy(() => import('./pages/HomePage'));
const RoomListPage = lazy(() =>
  import('./pages/RoomListPage').then((module) => ({ default: module.RoomListPage }))
);
const ChatPage = lazy(() => import('./pages/ChatPage'));
const ProfilePage = lazy(() => import('./pages/ProfilePage'));
const FriendListPage = lazy(() => import('./pages/FriendListPage'));
const NotificationsPage = lazy(() => import('./pages/NotificationsPage'));
const PremiumPage = lazy(() => import('./pages/PremiumPage'));
const AdminPage = lazy(() => import('./pages/AdminPage'));
const AboutPage = lazy(() => import('./pages/AboutPage'));
const PrivacyPolicyPage = lazy(() => import('./pages/PrivacyPolicyPage'));
const TermsPage = lazy(() => import('./pages/TermsPage'));
const ErrorPage = lazy(() => import('./pages/ErrorPage'));

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

  useEffect(() => {
    if (!isSessionExpired) return;

    useAuthStore.setState({ isSessionExpired: false });
    navigate('/login', { replace: true });
  }, [isSessionExpired, navigate]);

  return null;
}

function LegacyGameDetailRedirect() {
  const { id } = useParams<{ id: string }>();
  return <Navigate to={id ? `/games/${id}/rooms` : '/'} replace />;
}

function App() {
  useNotificationSocket();

  useEffect(() => {
    setSessionExpiredHandler(() => {
      useAuthStore.setState({ user: null, isAuthenticated: false, isLoading: false, isSessionExpired: true });
    });

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

    return () => {
      setSessionExpiredHandler(null);
    };
  }, []);

  return (
    <>
    <ToastContainer />
    <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
      <SessionExpiredHandler />
      <OfflineStatusBanner />
      <PwaUpdateBanner />
      <PwaInstallPrompt />
      <Suspense fallback={<RouteLoadingFallback />}>
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
      </Suspense>
    </BrowserRouter>
    </>
  );
}

export default App;
