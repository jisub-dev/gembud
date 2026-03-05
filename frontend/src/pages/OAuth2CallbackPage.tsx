import { useEffect, useRef } from 'react';
import { useNavigate, useSearchParams } from 'react-router-dom';
import { authService } from '../services/authService';
import { useAuthStore } from '../store/authStore';

function OAuth2CallbackPage() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const handled = useRef(false);

  useEffect(() => {
    if (handled.current) return;
    handled.current = true;

    const success = searchParams.get('success');

    if (success === 'true') {
      authService.getCurrentUser()
        .then((u) => {
          useAuthStore.setState({
            user: {
              id: u.id,
              email: u.email,
              nickname: u.nickname,
              temperature: u.temperature,
              isPremium: u.isPremium,
              premiumExpiresAt: u.premiumExpiresAt,
            },
            isAuthenticated: true,
            isLoading: false,
          });
          navigate('/', { replace: true });
        })
        .catch(() => {
          useAuthStore.setState({ isLoading: false });
          navigate('/login', { replace: true });
        });
    } else {
      useAuthStore.setState({ isLoading: false });
      navigate('/login', { replace: true });
    }
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-50">
      <div className="text-center">
        <div className="inline-block animate-spin rounded-full h-12 w-12 border-t-2 border-b-2 border-indigo-600"></div>
        <p className="mt-4 text-gray-600">Completing sign in...</p>
      </div>
    </div>
  );
}

export default OAuth2CallbackPage;
