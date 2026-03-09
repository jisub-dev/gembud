import { describe, it, expect, vi, beforeEach } from 'vitest';
import { renderHook, waitFor, act } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import {
  useFriends,
  useFriendRequests,
  useSentFriendRequests,
  useSendFriendRequest,
  useAcceptFriendRequest,
  useRejectFriendRequest,
  useCancelSentFriendRequest,
  useRemoveFriend,
} from '@/hooks/queries/useFriends';
import friendService from '@/services/friendService';
import type { FriendRequest } from '@/types/friend';

vi.mock('@/services/friendService', () => ({
  default: {
    getFriends: vi.fn(),
    getPendingRequests: vi.fn(),
    getSentRequests: vi.fn(),
    sendFriendRequest: vi.fn(),
    acceptFriendRequest: vi.fn(),
    rejectFriendRequest: vi.fn(),
    cancelSentFriendRequest: vi.fn(),
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
  userNickname: 'Me',
  friendId: 2,
  friendNickname: 'Friend1',
  status: 'ACCEPTED',
  createdAt: '2026-03-06T12:00:00',
  updatedAt: '2026-03-06T12:00:00',
};

const mockPendingRequest = {
  id: 2,
  userId: 3,
  userNickname: 'Sender',
  friendId: 1,
  friendNickname: 'Me',
  status: 'PENDING',
  createdAt: '2026-03-06T12:00:00',
  updatedAt: '2026-03-06T12:00:00',
};

describe('useFriends queries', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('returns friends list', async () => {
    vi.mocked(friendService.getFriends).mockResolvedValue([mockFriend] as any);

    const { result } = renderHook(() => useFriends(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });

  it('returns pending friend requests', async () => {
    vi.mocked(friendService.getPendingRequests).mockResolvedValue([mockPendingRequest] as any);

    const { result } = renderHook(() => useFriendRequests(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
    expect(result.current.data?.[0].status).toBe('PENDING');
  });

  it('returns sent requests', async () => {
    vi.mocked(friendService.getSentRequests).mockResolvedValue([mockPendingRequest] as any);

    const { result } = renderHook(() => useSentFriendRequests(), {
      wrapper: createWrapper(),
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(result.current.data).toHaveLength(1);
  });
});

describe('useFriends mutations', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('calls sendFriendRequest with friendId', async () => {
    vi.mocked(friendService.sendFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useSendFriendRequest(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.mutate(2);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.sendFriendRequest).toHaveBeenCalledWith(2);
  });

  it('calls acceptFriendRequest with requestId', async () => {
    vi.mocked(friendService.acceptFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useAcceptFriendRequest(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.mutate(2);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.acceptFriendRequest).toHaveBeenCalledWith(2);
  });

  it('calls rejectFriendRequest with requestId', async () => {
    vi.mocked(friendService.rejectFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useRejectFriendRequest(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.mutate(2);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.rejectFriendRequest).toHaveBeenCalledWith(2);
  });

  it('calls cancelSentFriendRequest with requestId', async () => {
    vi.mocked(friendService.cancelSentFriendRequest).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useCancelSentFriendRequest(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.mutate(2);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.cancelSentFriendRequest).toHaveBeenCalledWith(2);
  });

  it('calls removeFriend with friendId', async () => {
    vi.mocked(friendService.removeFriend).mockResolvedValue(undefined as any);

    const { result } = renderHook(() => useRemoveFriend(), {
      wrapper: createWrapper(),
    });

    act(() => {
      result.current.mutate(2);
    });

    await waitFor(() => expect(result.current.isSuccess).toBe(true));
    expect(friendService.removeFriend).toHaveBeenCalledWith(2);
  });

  it('runs request-send/accept/reject flow and reflects status changes', async () => {
    const received: FriendRequest[] = [
      {
        id: 100,
        userId: 3,
        userNickname: 'Alice',
        friendId: 1,
        friendNickname: 'Me',
        status: 'PENDING',
        createdAt: '2026-03-06T10:00:00',
        updatedAt: '2026-03-06T10:00:00',
      },
      {
        id: 101,
        userId: 4,
        userNickname: 'Bob',
        friendId: 1,
        friendNickname: 'Me',
        status: 'REJECTED',
        createdAt: '2026-03-06T09:00:00',
        updatedAt: '2026-03-06T09:00:00',
      },
    ];

    const sent: FriendRequest[] = [
      {
        id: 200,
        userId: 1,
        userNickname: 'Me',
        friendId: 6,
        friendNickname: 'Chris',
        status: 'ACCEPTED',
        createdAt: '2026-03-05T08:00:00',
        updatedAt: '2026-03-05T08:00:00',
      },
    ];

    const friends: typeof mockFriend[] = [];

    vi.mocked(friendService.getPendingRequests).mockImplementation(async () => [...received]);
    vi.mocked(friendService.getSentRequests).mockImplementation(async () => [...sent]);
    vi.mocked(friendService.getFriends).mockImplementation(async () => [...friends] as any);

    vi.mocked(friendService.sendFriendRequest).mockImplementation(async (friendId: number) => {
      sent.push({
        id: 201,
        userId: 1,
        userNickname: 'Me',
        friendId,
        friendNickname: 'Target',
        status: 'PENDING',
        createdAt: '2026-03-06T11:00:00',
        updatedAt: '2026-03-06T11:00:00',
      });
    });

    vi.mocked(friendService.acceptFriendRequest).mockImplementation(async (requestId: number) => {
      const target = received.find((request) => request.id === requestId);
      if (!target) return;
      target.status = 'ACCEPTED';
      target.updatedAt = '2026-03-06T11:10:00';
      friends.push({
        id: 300,
        userId: 1,
        userNickname: 'Me',
        friendId: target.userId,
        friendNickname: target.userNickname,
        status: 'ACCEPTED',
        createdAt: '2026-03-06T11:10:00',
        updatedAt: '2026-03-06T11:10:00',
      });
    });

    vi.mocked(friendService.rejectFriendRequest).mockImplementation(async (requestId: number) => {
      const target = received.find((request) => request.id === requestId);
      if (!target) return;
      target.status = 'REJECTED';
      target.updatedAt = '2026-03-06T11:20:00';
    });

    const wrapper = createWrapper();

    const receivedHook = renderHook(() => useFriendRequests(), { wrapper });
    const sentHook = renderHook(() => useSentFriendRequests(), { wrapper });
    const friendHook = renderHook(() => useFriends(), { wrapper });
    const sendMutation = renderHook(() => useSendFriendRequest(), { wrapper });
    const acceptMutation = renderHook(() => useAcceptFriendRequest(), { wrapper });
    const rejectMutation = renderHook(() => useRejectFriendRequest(), { wrapper });

    await waitFor(() => expect(receivedHook.result.current.isSuccess).toBe(true));
    await waitFor(() => expect(sentHook.result.current.isSuccess).toBe(true));

    act(() => {
      sendMutation.result.current.mutate(9);
    });

    await waitFor(() => expect(sentHook.result.current.data?.[0].status).toBe('PENDING'));

    act(() => {
      acceptMutation.result.current.mutate(100);
    });

    await waitFor(() => expect(friendHook.result.current.data).toHaveLength(1));
    await waitFor(() => expect(receivedHook.result.current.data?.[0].status).toBe('ACCEPTED'));

    act(() => {
      rejectMutation.result.current.mutate(100);
    });

    await waitFor(() => {
      const statuses = receivedHook.result.current.data?.map((request) => request.status);
      expect(statuses).toEqual(['REJECTED', 'REJECTED']);
    });
  });
});
