import axios from 'axios';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

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
  (config) => {
    // Add CSRF token from cookie to header
    const csrfToken = document.cookie
      .split('; ')
      .find(row => row.startsWith('XSRF-TOKEN='))
      ?.split('=')[1];

    if (csrfToken && config.method !== 'get') {
      config.headers['X-XSRF-TOKEN'] = decodeURIComponent(csrfToken);
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
        // Refresh 실패 - 세션 만료 모달 트리거 (window.location 사용 금지 → 무한루프 원인)
        const { useAuthStore } = await import('../store/authStore');
        useAuthStore.setState({ user: null, isAuthenticated: false, isLoading: false, isSessionExpired: true });
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);

export default api;
