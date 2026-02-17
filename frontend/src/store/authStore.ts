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
  isAuthenticated: authService.isAuthenticated(),
  isLoading: false,
  error: null,

  signup: async (data: SignupRequest) => {
    set({ isLoading: true, error: null });
    try {
      const response = await authService.signup(data);
      authService.storeTokens(response.accessToken, response.refreshToken);
      set({
        user: {
          email: response.email,
          nickname: response.nickname,
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
      const response = await authService.login(data);
      authService.storeTokens(response.accessToken, response.refreshToken);
      set({
        user: {
          email: response.email,
          nickname: response.nickname,
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

  logout: () => {
    authService.clearTokens();
    set({
      user: null,
      isAuthenticated: false,
      error: null,
    });
  },

  handleOAuth2Callback: (params: URLSearchParams) => {
    const accessToken = params.get('accessToken');
    const refreshToken = params.get('refreshToken');
    const email = params.get('email');
    const nickname = params.get('nickname');

    if (accessToken && refreshToken && email && nickname) {
      authService.storeTokens(accessToken, refreshToken);
      set({
        user: { email, nickname },
        isAuthenticated: true,
      });
    } else {
      set({ error: 'OAuth2 authentication failed' });
    }
  },

  clearError: () => set({ error: null }),
}));
