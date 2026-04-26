import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import Sidebar from '@/components/layout/Sidebar';
import { roomService } from '@/services/roomService';
import { chatService } from '@/services/chatService';
import { useGames } from '@/hooks/queries/useGames';
import { useFriends } from '@/hooks/queries/useFriends';
import { useUnreadNotificationCount } from '@/hooks/queries/useNotifications';
import { useAuthStore } from '@/store/authStore';
import type { ChatRoomInfo } from '@/types/chat';
import type { Room } from '@/types/room';

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

vi.mock('@/services/roomService', () => ({
  roomService: {
    getMyActiveRoom: vi.fn(),
  },
}));

vi.mock('@/services/chatService', () => ({
  chatService: {
    getMyChatRooms: vi.fn(),
  },
}));

vi.mock('@/hooks/queries/useGames', () => ({
  useGames: vi.fn(),
}));

vi.mock('@/hooks/queries/useFriends', () => ({
  useFriends: vi.fn(),
}));

vi.mock('@/hooks/queries/useNotifications', () => ({
  useUnreadNotificationCount: vi.fn(),
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: vi.fn(),
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
        children
      )
    );
}

async function renderSidebar(waitingRoomLabel?: string) {
  let utils!: ReturnType<typeof render>;
  await act(async () => {
    utils = render(<Sidebar />, { wrapper: createWrapper() });
    await Promise.resolve();
  });
  await screen.findByText('DM Visible');
  if (waitingRoomLabel) {
    await screen.findByText(waitingRoomLabel);
  }
  return utils;
}

async function clickAndFlush(user: ReturnType<typeof userEvent.setup>, element: Element) {
  await act(async () => {
    await user.click(element);
    await Promise.resolve();
  });
}

const mockRoom = {
  id: 1,
  gameId: 10,
  gameName: 'League of Legends',
  title: '내 대기방 A',
  description: 'desc',
  maxParticipants: 5,
  currentParticipants: 3,
  isPrivate: false,
  status: 'OPEN',
  createdBy: 'host',
  createdAt: '2026-03-06T12:00:00',
};

describe('Sidebar', () => {
  let roomChatsLookup: ChatRoomInfo[];
  let generalChats: ChatRoomInfo[];

  beforeEach(() => {
    vi.clearAllMocks();
    roomChatsLookup = [
      {
        id: 101,
        publicId: 'chat-public-101',
        type: 'ROOM_CHAT',
        relatedRoomId: 1,
        relatedRoomTitle: '내 대기방 A',
      },
    ];
    generalChats = [{ id: 201, publicId: 'dm-public-201', type: 'DIRECT_CHAT', name: 'DM Visible' }];

    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: true } as unknown as ReturnType<typeof useAuthStore>);
    vi.mocked(useGames).mockReturnValue({ data: [] } as unknown as ReturnType<typeof useGames>);
    vi.mocked(useFriends).mockReturnValue({ data: [] } as unknown as ReturnType<typeof useFriends>);
    vi.mocked(useUnreadNotificationCount).mockReturnValue({ data: 3 } as unknown as ReturnType<typeof useUnreadNotificationCount>);
    vi.mocked(roomService.getMyActiveRoom).mockResolvedValue(mockRoom as unknown as Room);
    vi.mocked(chatService.getMyChatRooms).mockImplementation((type?: Parameters<typeof chatService.getMyChatRooms>[0]) => {
      if (type === 'ROOM_CHAT') {
        return Promise.resolve(roomChatsLookup);
      }
      return Promise.resolve(generalChats);
    });
  });

  it('opens ROOM_CHAT via /chat/rooms/my?type=ROOM_CHAT and navigates to /chat/{publicId}', async () => {
    const user = userEvent.setup();
    await renderSidebar('내 대기방 A');

    await clickAndFlush(user, await screen.findByText('내 대기방 A'));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-101');
      expect(chatService.getMyChatRooms).toHaveBeenCalledWith('ROOM_CHAT');
    });
  });

  it('reuses the currently matched ROOM_CHAT even when the refresh lookup is empty', async () => {
    const user = userEvent.setup();
    await renderSidebar('내 대기방 A');

    const roomButton = await screen.findByRole('button', { name: /내 대기방 A/i });
    roomChatsLookup = [];

    await act(async () => {
      await clickAndFlush(user, roomButton);
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-101');
    });
    await waitFor(() => expect(roomButton).not.toBeDisabled());
  });

  it('falls back to / when gameId is missing', async () => {
    vi.mocked(roomService.getMyActiveRoom).mockResolvedValue(null);

    await renderSidebar('대기방 없음');

    expect(await screen.findByText('대기방 없음')).toBeInTheDocument();
    expect(mockNavigate).not.toHaveBeenCalled();
  });

  it('renders "대기방 없음" when there is no ROOM_CHAT', async () => {
    roomChatsLookup = [];
    await renderSidebar();
    expect(await screen.findByText('대기방 없음')).toBeInTheDocument();
  });

  it('renders "대기방 없음" when ROOM_CHAT exists but does not match the active room', async () => {
    roomChatsLookup = [
      {
        id: 102,
        publicId: 'chat-public-stale',
        type: 'ROOM_CHAT',
        relatedRoomId: 999,
        relatedRoomTitle: '오래된 방',
      },
    ];

    await renderSidebar();

    expect(await screen.findByText('대기방 없음')).toBeInTheDocument();
    expect(screen.queryByText('오래된 방')).not.toBeInTheDocument();
  });

  it('shows only DIRECT_CHAT/GROUP_CHAT in 채팅방 section', async () => {
    generalChats = [
      { id: 201, publicId: 'dm-201', type: 'DIRECT_CHAT', name: 'DM Visible' },
      { id: 301, publicId: 'group-301', type: 'GROUP_CHAT', name: 'GROUP Visible' },
      { id: 401, publicId: 'room-401', type: 'ROOM_CHAT', name: 'ROOM SHOULD HIDE', relatedRoomId: 1 },
    ];

    await renderSidebar();

    expect(await screen.findByText('DM Visible')).toBeInTheDocument();
    expect(screen.getByText('GROUP Visible')).toBeInTheDocument();
    expect(screen.queryByText('ROOM SHOULD HIDE')).not.toBeInTheDocument();
  });

  it('shows "친구 없음" when no friends exist', async () => {
    await renderSidebar();
    expect(await screen.findByText('친구')).toBeInTheDocument();
    expect(screen.getByText('친구 없음')).toBeInTheDocument();
  });

  it('renders friend list and navigates to the friend profile on click', async () => {
    vi.mocked(useFriends).mockReturnValue({
      data: [
        {
          id: 1,
          userId: 10,
          friendId: 11,
          friendNickname: '친구A',
          status: 'ACCEPTED',
        },
      ],
    } as unknown as ReturnType<typeof useFriends>);

    const user = userEvent.setup();
    await renderSidebar();

    const friendButton = await screen.findByRole('button', { name: /친구A/i });
    await clickAndFlush(user, friendButton);

    expect(mockNavigate).toHaveBeenCalledWith('/profile/11');
  });

  it('toggles friend section open/closed', async () => {
    vi.mocked(useFriends).mockReturnValue({
      data: [
        {
          id: 1,
          userId: 10,
          friendId: 11,
          friendNickname: '토글친구',
          status: 'ACCEPTED',
        },
      ],
    } as unknown as ReturnType<typeof useFriends>);

    const user = userEvent.setup();
    await renderSidebar();

    expect(await screen.findByText('토글친구')).toBeInTheDocument();
    const friendSectionToggle = screen.getByText('친구').closest('button');
    expect(friendSectionToggle).toBeTruthy();
    await clickAndFlush(user, friendSectionToggle!);
    expect(screen.queryByText('토글친구')).not.toBeInTheDocument();
    await clickAndFlush(user, friendSectionToggle!);
    expect(await screen.findByText('토글친구')).toBeInTheDocument();
  });

  it('shows unread notification badge in sidebar and hides when zero', async () => {
    const { rerender } = await renderSidebar();
    expect(await screen.findByText('알림 센터')).toBeInTheDocument();
    expect(screen.getByText('3')).toBeInTheDocument();

    vi.mocked(useUnreadNotificationCount).mockReturnValue({ data: 0 } as unknown as ReturnType<typeof useUnreadNotificationCount>);
    await act(async () => {
      rerender(<Sidebar />);
      await Promise.resolve();
    });

    expect(screen.queryByText('3')).not.toBeInTheDocument();
  });
});
