import axios from 'axios';
import { notifySessionExpired } from '@/lib/sessionExpiryBridge';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const CSRF_COOKIE_NAME = 'XSRF-TOKEN';

const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
  // Phase 12: Cookie-based authentication (HTTP-only cookies)
  withCredentials: true,
});

// Phase 12: Cookie-based authentication + CSRF protection
api.interceptors.request.use(
  async (config) => {
    const method = config.method?.toLowerCase();
    const url = config.url ?? '';
    const isAuthMutation = typeof url === 'string' && url.startsWith('/auth/');

    if (method && method !== 'get') {
      await ensureCsrfToken({ force: isAuthMutation });
      const csrfToken = getCsrfTokenFromCookie();

      if (csrfToken) {
        config.headers['X-XSRF-TOKEN'] = decodeURIComponent(csrfToken);
      }
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor to handle token refresh
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // /auth/refresh, /auth/login 요청 자체의 401은 재시도하지 않음
    const isAuthEndpoint = originalRequest?.url?.includes('/auth/');

    if (error.response?.status === 401 && !originalRequest._retry && !isAuthEndpoint) {
      originalRequest._retry = true;

      try {
        await axios.post(
          `${API_BASE_URL}/auth/refresh`,
          {},
          { withCredentials: true }
        );
        return api(originalRequest);
      } catch (refreshError) {
        // Refresh 실패 - 세션 만료 브리지 핸들러 호출
        notifySessionExpired();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;

function getCsrfTokenFromCookie(): string | null {
  return document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${CSRF_COOKIE_NAME}=`))
    ?.split('=')[1] ?? null;
}

async function ensureCsrfToken(options?: { force?: boolean }): Promise<void> {
  if (!options?.force && getCsrfTokenFromCookie()) {
    return;
  }

  await axios.get(`${API_BASE_URL}/auth/csrf`, {
    withCredentials: true,
  });
}
