import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { useSubscriptionStatus, useActivatePremium, useCancelPremium } from '@/hooks/queries/useSubscription';
import { subscriptionService } from '@/services/subscriptionService';

vi.mock('@/services/subscriptionService');

// Use vi.hoisted to avoid temporal dead zone in vi.mock factory
const { mockAuthStore } = vi.hoisted(() => {
  const store = vi.fn(() => ({ isAuthenticated: true, user: null })) as unknown as typeof import('@/store/authStore').useAuthStore;
  store.getState = vi.fn(() => ({ user: null })) as unknown as typeof store.getState;
  store.setState = vi.fn() as unknown as typeof store.setState;
  return { mockAuthStore: store };
});

vi.mock('@/store/authStore', () => ({
  useAuthStore: mockAuthStore,
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children);
}

const mockUser = { id: 1, email: 'test@test.com', nickname: 'Test', temperature: 36.5, isPremium: false };

const mockStatus = {
  isPremium: false,
  premiumExpiresAt: null,
  subscriptionStatus: null,
  startedAt: null,
  amount: 0,
};

const mockPremiumStatus = {
  isPremium: true,
  premiumExpiresAt: '2026-04-04T00:00:00',
  subscriptionStatus: 'ACTIVE' as const,
  startedAt: '2026-03-04T00:00:00',
  amount: 2900,
};

describe('useSubscriptionStatus', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(mockAuthStore).mockReturnValue({ isAuthenticated: true, user: mockUser } as unknown as ReturnType<typeof mockAuthStore>);
    mockAuthStore.getState = vi.fn(() => ({ user: mockUser })) as unknown as typeof mockAuthStore.getState;
    mockAuthStore.setState = vi.fn() as unknown as typeof mockAuthStore.setState;
  });

  it('should return non-premium status when not subscribed', async () => {
    vi.mocked(subscriptionService.getStatus).mockResolvedValue(mockStatus);

    const { result } = renderHook(() => useSubscriptionStatus(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.isPremium).toBe(false);
  });

  it('should return premium status when subscribed', async () => {
    vi.mocked(subscriptionService.getStatus).mockResolvedValue(mockPremiumStatus);

    const { result } = renderHook(() => useSubscriptionStatus(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data?.isPremium).toBe(true);
    expect(result.current.data?.subscriptionStatus).toBe('ACTIVE');
  });

  it('should not fetch when not authenticated', async () => {
    vi.mocked(mockAuthStore).mockReturnValue({ isAuthenticated: false, user: null } as unknown as ReturnType<typeof mockAuthStore>);

    const { result } = renderHook(() => useSubscriptionStatus(), {
      wrapper: createWrapper(),
    });

    expect(result.current.fetchStatus).toBe('idle');
    expect(subscriptionService.getStatus).not.toHaveBeenCalled();
  });
});

describe('useActivatePremium', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(mockAuthStore).mockReturnValue({ isAuthenticated: true, user: mockUser } as unknown as ReturnType<typeof mockAuthStore>);
    mockAuthStore.getState = vi.fn(() => ({ user: mockUser })) as unknown as typeof mockAuthStore.getState;
    mockAuthStore.setState = vi.fn() as unknown as typeof mockAuthStore.setState;
  });

  it('should call activate service with correct months', async () => {
    vi.mocked(subscriptionService.activate).mockResolvedValue(mockPremiumStatus);

    const { result } = renderHook(() => useActivatePremium(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(1);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(subscriptionService.activate).toHaveBeenCalledWith(1);
  });
});

describe('useCancelPremium', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    const premiumUser = { ...mockUser, isPremium: true };
    vi.mocked(mockAuthStore).mockReturnValue({ isAuthenticated: true, user: premiumUser } as unknown as ReturnType<typeof mockAuthStore>);
    mockAuthStore.getState = vi.fn(() => ({ user: premiumUser })) as unknown as typeof mockAuthStore.getState;
    mockAuthStore.setState = vi.fn() as unknown as typeof mockAuthStore.setState;
  });

  it('should call cancel service', async () => {
    vi.mocked(subscriptionService.cancel).mockResolvedValue(mockStatus);

    const { result } = renderHook(() => useCancelPremium(), {
      wrapper: createWrapper(),
    });

    result.current.mutate();

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(subscriptionService.cancel).toHaveBeenCalled();
  });
});
