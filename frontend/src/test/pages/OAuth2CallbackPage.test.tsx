import { beforeEach, describe, expect, it, vi } from 'vitest';
import { act, render, waitFor } from '@testing-library/react';
import OAuth2CallbackPage from '@/pages/OAuth2CallbackPage';
import { authService } from '@/services/authService';
import { useAuthStore } from '@/store/authStore';

const routerMocks = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  searchParams: new URLSearchParams('success=true'),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => routerMocks.mockNavigate,
    useSearchParams: () => [routerMocks.searchParams],
  };
});

vi.mock('@/services/authService', () => ({
  authService: {
    getCurrentUser: vi.fn(),
  },
}));

describe('OAuth2CallbackPage', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    localStorage.clear();
    routerMocks.searchParams = new URLSearchParams('success=true');
    useAuthStore.setState({
      user: null,
      isAuthenticated: false,
      isLoading: true,
      isSessionExpired: false,
      error: null,
    });
  });

  it('restores auth state and navigates home after successful OAuth2 callback', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue({
      id: 11,
      email: 'oauth@test.com',
      nickname: 'oauth-user',
      temperature: 36.5,
      isPremium: false,
      premiumExpiresAt: null,
    });

    await act(async () => {
      render(<OAuth2CallbackPage />);
      await Promise.resolve();
    });

    await waitFor(() => {
      expect(routerMocks.mockNavigate).toHaveBeenCalledWith('/', { replace: true });
    });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.email).toBe('oauth@test.com');
  });

  it('navigates to onboarding when OAuth user still has generated nickname', async () => {
    vi.mocked(authService.getCurrentUser).mockResolvedValue({
      id: 22,
      email: 'oauth@test.com',
      nickname: 'user_abc123',
      temperature: 36.5,
      isPremium: false,
      premiumExpiresAt: null,
    });

    await act(async () => {
      render(<OAuth2CallbackPage />);
      await Promise.resolve();
    });

    await waitFor(() => {
      expect(routerMocks.mockNavigate).toHaveBeenCalledWith('/onboarding', { replace: true });
    });
    expect(useAuthStore.getState().isAuthenticated).toBe(true);
    expect(useAuthStore.getState().user?.nickname).toBe('user_abc123');
  });

  it('navigates to login when session restoration fails after OAuth2 callback', async () => {
    vi.mocked(authService.getCurrentUser).mockRejectedValue(new Error('restore failed'));

    await act(async () => {
      render(<OAuth2CallbackPage />);
      await Promise.resolve();
    });

    await waitFor(() => {
      expect(routerMocks.mockNavigate).toHaveBeenCalledWith('/login', { replace: true });
    });
    expect(useAuthStore.getState().isLoading).toBe(false);
    expect(useAuthStore.getState().isAuthenticated).toBe(false);
  });
});
