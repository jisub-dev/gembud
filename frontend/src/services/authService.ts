import api from './api';
import { ApiResponse } from '@/types/api';

export interface SignupRequest {
  email: string;
  password: string;
  nickname: string;
  ageRange?: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  tokenType: string;
  email: string;
  nickname: string;
}

export const authService = {
  /**
   * Sign up a new user
   * Phase 12: Tokens delivered via HTTP-only cookies
   */
  async signup(data: SignupRequest): Promise<AuthResponse> {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/signup', data);
    return response.data.data;
  },

  /**
   * Log in with email and password
   * Phase 12: Tokens delivered via HTTP-only cookies
   */
  async login(data: LoginRequest): Promise<AuthResponse> {
    const response = await api.post<ApiResponse<AuthResponse>>('/auth/login', data);
    return response.data.data;
  },

  /**
   * Get current user info
   * Phase 12: New endpoint to get user info after OAuth callback
   */
  async getCurrentUser(): Promise<{ id: number; email: string; nickname: string; temperature: number; isPremium?: boolean; premiumExpiresAt?: string | null }> {
    const response = await api.get('/users/me');
    return response.data.data;
  },

  /**
   * Log out (clears HTTP-only cookies on backend)
   */
  async logout(): Promise<void> {
    await api.post('/auth/logout');
  },

  /**
   * Get OAuth2 login URL
   */
  getOAuth2LoginUrl(provider: 'google' | 'discord'): string {
    const baseUrl = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
    return `${baseUrl}/oauth2/authorization/${provider}`;
  },

  /**
   * Phase 12: Cookie-based authentication
   * Tokens are stored in HTTP-only cookies (not accessible from JavaScript)
   * Authentication state is checked by calling /users/me endpoint
   */
  async isAuthenticated(): Promise<boolean> {
    try {
      await this.getCurrentUser();
      return true;
    } catch {
      return false;
    }
  },
};
export default authService;
