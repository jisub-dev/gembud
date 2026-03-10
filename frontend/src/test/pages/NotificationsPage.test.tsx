import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import NotificationsPage from '@/pages/NotificationsPage';
import {
  useNotifications,
  useMarkNotificationAsRead,
  useMarkAllNotificationsAsRead,
  useDeleteNotification,
} from '@/hooks/queries/useNotifications';
import type { Notification } from '@/types/notification';

const { mockNavigate } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/hooks/queries/useNotifications', () => ({
  useNotifications: vi.fn(),
  useMarkNotificationAsRead: vi.fn(),
  useMarkAllNotificationsAsRead: vi.fn(),
  useDeleteNotification: vi.fn(),
}));

vi.mock('@/components/common/ConfirmModal', () => ({
  ConfirmModal: () => null,
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient },
      createElement(
        MemoryRouter,
        { future: { v7_startTransition: true, v7_relativeSplatPath: true } },
        children,
      ),
    );
}

describe('NotificationsPage filtering', () => {
  const markAsReadMutate = vi.fn();
  const markAllAsReadMutate = vi.fn();
  const deleteMutate = vi.fn();

  beforeEach(() => {
    vi.clearAllMocks();

    const notifications: Notification[] = [
      {
        id: 1,
        userId: 10,
        type: 'FRIEND_REQUEST',
        title: '친구 요청',
        message: '테스터님이 친구 요청을 보냈습니다',
        isRead: false,
        createdAt: '2026-03-10T10:00:00',
      },
      {
        id: 2,
        userId: 10,
        type: 'ROOM_INVITATION',
        title: '방 초대',
        message: '방에 초대되었습니다',
        isRead: false,
        relatedUrl: '/chat/public-id-1',
        createdAt: '2026-03-10T09:00:00',
      },
      {
        id: 3,
        userId: 10,
        type: 'SYSTEM',
        title: '시스템',
        message: '점검 공지',
        isRead: false,
        createdAt: '2026-03-10T08:00:00',
      },
    ];

    vi.mocked(useNotifications).mockReturnValue({
      data: notifications,
      isLoading: false,
    } as any);

    vi.mocked(useMarkNotificationAsRead).mockReturnValue({
      mutate: markAsReadMutate,
      isPending: false,
    } as any);

    vi.mocked(useMarkAllNotificationsAsRead).mockReturnValue({
      mutate: markAllAsReadMutate,
      isPending: false,
    } as any);

    vi.mocked(useDeleteNotification).mockReturnValue({
      mutate: deleteMutate,
      isPending: false,
    } as any);
  });

  it('shows summary counts and room CTA label', () => {
    render(<NotificationsPage />, { wrapper: createWrapper() });

    expect(screen.getByText('총 알림 수')).toBeInTheDocument();
    expect(screen.getByText('안 읽은 알림')).toBeInTheDocument();
    expect(screen.getAllByText('3').length).toBeGreaterThan(0);
    expect(screen.getByRole('button', { name: /방으로 이동/i })).toBeInTheDocument();
  });

  it('filters by unread tab and friend type', async () => {
    const user = userEvent.setup();
    render(<NotificationsPage />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button', { name: '안 읽음' }));
    await user.click(screen.getByRole('button', { name: '친구' }));

    expect(screen.getByText('테스터님이 친구 요청을 보냈습니다')).toBeInTheDocument();
    expect(screen.queryByText('방에 초대되었습니다')).not.toBeInTheDocument();
    expect(screen.queryByText('점검 공지')).not.toBeInTheDocument();
  });

  it('marks as read and navigates when relatedUrl CTA is clicked', async () => {
    const user = userEvent.setup();
    render(<NotificationsPage />, { wrapper: createWrapper() });

    await user.click(screen.getByRole('button', { name: /방으로 이동/i }));

    expect(markAsReadMutate).toHaveBeenCalledWith(2);
    expect(mockNavigate).toHaveBeenCalledWith('/chat/public-id-1');
  });
});
