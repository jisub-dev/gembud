import { beforeEach, describe, expect, it, vi } from 'vitest';

const { requestUse, responseUse, axiosGet } = vi.hoisted(() => ({
  requestUse: vi.fn(),
  responseUse: vi.fn(),
  axiosGet: vi.fn(),
}));

vi.mock('axios', () => {
  const axiosInstance = {
    interceptors: {
      request: { use: requestUse },
      response: { use: responseUse },
    },
  };

  return {
    default: {
      create: vi.fn(() => axiosInstance),
      get: axiosGet,
      post: vi.fn(),
    },
  };
});

// Import after mocking so interceptors are registered on the mocked axios instance.
import '@/services/api';

const requestInterceptor = requestUse.mock.calls[0][0] as (config: any) => Promise<any>;

describe('api csrf request interceptor', () => {
  beforeEach(() => {
    axiosGet.mockReset();
    document.cookie = 'XSRF-TOKEN=; expires=Thu, 01 Jan 1970 00:00:00 GMT; path=/';
  });

  it('prefetches a csrf cookie before the first non-get request', async () => {
    axiosGet.mockImplementation(async () => {
      document.cookie = 'XSRF-TOKEN=prefetched-token; path=/';
      return { data: [] };
    });

    const config = await requestInterceptor({
      method: 'post',
      headers: {},
    });

    expect(axiosGet).toHaveBeenCalledWith('http://localhost:8080/api/auth/csrf', {
      withCredentials: true,
    });
    expect(config.headers['X-XSRF-TOKEN']).toBe('prefetched-token');
  });

  it('reuses the existing csrf cookie without prefetching again', async () => {
    document.cookie = 'XSRF-TOKEN=existing-token; path=/';

    const config = await requestInterceptor({
      method: 'post',
      headers: {},
    });

    expect(axiosGet).not.toHaveBeenCalled();
    expect(config.headers['X-XSRF-TOKEN']).toBe('existing-token');
  });
});
