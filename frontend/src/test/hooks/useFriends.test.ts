import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import {
  useFriends,
  useFriendRequests,
  useSentFriendRequests,
  useSendFriendRequest,
  useAcceptFriendRequest,
  useRejectFriendRequest,
  useRemoveFriend,
} from '@/hooks/queries/useFriends';
import friendService from '@/services/friendService';

vi.mock('@/services/friendService', () => ({
  default: {
    getFriends: vi.fn(),
    getPendingRequests: vi.fn(),
    getSentRequests: vi.fn(),
    sendFriendRequest: vi.fn(),
    acceptFriendRequest: vi.fn(),
    rejectFriendRequest: vi.fn(),
    removeFriend: vi.fn(),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: { queries: { retry: false }, mutations: { retry: false } },
  });
  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children);
}

const mockFriend = {
  id: 1,
  userId: 1,
  friendId: 2,
  nickname: 'Friend1',
  status: 'ACCEPTED',
  temperature: 36.5,
};

const mockPendingRequest = {
  id: 2,
  userId: 3,
  friendId: 1,
  nickname: 'Sender',
  status: 'PENDING',
  temperature: 36.5,
};

describe('useFriends', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('should return friends list', async () => {
    vi.mocked(friendService.getFriends).mockResolvedValue([mockFriend] as any);

    const { result } = renderHook(() => useFriends(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });

  it('should return empty list when no friends', async () => {
    vi.mocked(friendService.getFriends).mockResolvedValue([]);

    const { result } = renderHook(() => useFriends(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(0);
  });
});

describe('useFriendRequests', () => {
  it('should return pending friend requests', async () => {
    vi.mocked(friendService.getPendingRequests).mockResolvedValue([mockPendingRequest] as any);

    const { result } = renderHook(() => useFriendRequests(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });
});

describe('useSentFriendRequests', () => {
  it('should return sent requests', async () => {
    vi.mocked(friendService.getSentRequests).mockResolvedValue([mockPendingRequest] as any);

    const { result } = renderHook(() => useSentFriendRequests(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });
});

describe('useSendFriendRequest', () => {
  it('should call sendFriendRequest with friendId', async () => {
    vi.mocked(friendService.sendFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useSendFriendRequest(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(2);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.sendFriendRequest).toHaveBeenCalledWith(2);
  });
});

describe('useAcceptFriendRequest', () => {
  it('should call acceptFriendRequest with requestId', async () => {
    vi.mocked(friendService.acceptFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useAcceptFriendRequest(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(2);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.acceptFriendRequest).toHaveBeenCalledWith(2);
  });
});

describe('useRejectFriendRequest', () => {
  it('should call rejectFriendRequest with requestId', async () => {
    vi.mocked(friendService.rejectFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useRejectFriendRequest(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(2);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.rejectFriendRequest).toHaveBeenCalledWith(2);
  });
});

describe('useRemoveFriend', () => {
  it('should call removeFriend with friendId', async () => {
    vi.mocked(friendService.removeFriend).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useRemoveFriend(), {
      wrapper: createWrapper(),
    });

    result.current.mutate(2);

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.removeFriend).toHaveBeenCalledWith(2);
  });
});
