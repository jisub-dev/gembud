import { act, renderHook } from '@testing-library/react';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import {
  notificationKeys,
  useMarkAllNotificationsAsRead,
} from '@/hooks/queries/useNotifications';
import notificationService from '@/services/notificationService';

vi.mock('@/services/notificationService', () => ({
  default: {
    getNotifications: vi.fn(),
    getUnreadCount: vi.fn(),
    markAsRead: vi.fn(),
    markAllAsRead: vi.fn(),
    deleteNotification: vi.fn(),
  },
}));

function createWrapper(queryClient: QueryClient) {
  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children);
}

describe('useMarkAllNotificationsAsRead', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('optimistically updates list and unread count immediately', async () => {
    const queryClient = new QueryClient({
      defaultOptions: {
        queries: { retry: false },
        mutations: { retry: false },
      },
    });

    queryClient.setQueryData(notificationKeys.list(), [
      { id: 1, isRead: false, message: 'a' },
      { id: 2, isRead: false, message: 'b' },
      { id: 3, isRead: true, message: 'c' },
    ]);
    queryClient.setQueryData(notificationKeys.unreadCount(), 2);

    vi.mocked(notificationService.markAllAsRead).mockResolvedValue(undefined);

    const { result } = renderHook(() => useMarkAllNotificationsAsRead(), {
      wrapper: createWrapper(queryClient),
    });

    await act(async () => {
      result.current.mutate();
    });

    const updatedList = queryClient.getQueryData<Array<{ id: number; isRead: boolean }>>(notificationKeys.list()) ?? [];
    const updatedUnreadCount = queryClient.getQueryData<number>(notificationKeys.unreadCount());

    expect(updatedList.every((notification) => notification.isRead)).toBe(true);
    expect(updatedUnreadCount).toBe(0);
  });
});
