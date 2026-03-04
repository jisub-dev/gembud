import { create } from 'zustand';
import { authService, type SignupRequest, type LoginRequest } from '../services/authService';
import type { User } from '../types/user';

interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;

  signup: (data: SignupRequest) => Promise<void>;
  login: (data: LoginRequest) => Promise<void>;
  logout: () => void;
  handleOAuth2Callback: (params: URLSearchParams) => void;
  clearError: () => void;
}

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  isLoading: true, // true until session restore attempt completes
  error: null,

  signup: async (data: SignupRequest) => {
    set({ isLoading: true, error: null });
    try {
      await authService.signup(data);
      // Fetch full user info after signup (id, temperature included)
      const user = await authService.getCurrentUser();
      set({
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
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Signup failed',
        isLoading: false,
      });
      throw error;
    }
  },

  login: async (data: LoginRequest) => {
    set({ isLoading: true, error: null });
    try {
      await authService.login(data);
      // Fetch full user info after login (id, temperature included)
      const user = await authService.getCurrentUser();
      set({
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
    } catch (error: any) {
      set({
        error: error.response?.data?.message || 'Login failed',
        isLoading: false,
      });
      throw error;
    }
  },

  logout: async () => {
    try {
      await authService.logout();
      set({
        user: null,
        isAuthenticated: false,
        error: null,
      });
    } catch (error: any) {
      // Logout failed, but clear state anyway
      set({
        user: null,
        isAuthenticated: false,
        error: null,
      });
    }
  },

  handleOAuth2Callback: async (params: URLSearchParams) => {
    // Phase 12: No tokens/PII in URL, only success flag
    const success = params.get('success');

    if (success === 'true') {
      try {
        // Fetch user info (cookies are sent automatically)
        const u = await authService.getCurrentUser();
        set({
          user: {
            id: u.id,
            email: u.email,
            nickname: u.nickname,
            temperature: u.temperature,
            isPremium: u.isPremium,
            premiumExpiresAt: u.premiumExpiresAt,
          },
          isAuthenticated: true,
        });
      } catch (error: any) {
        set({ error: 'Failed to get user info after OAuth2 login' });
      }
    } else {
      set({ error: 'OAuth2 authentication failed' });
    }
  },

  clearError: () => set({ error: null }),
}));
