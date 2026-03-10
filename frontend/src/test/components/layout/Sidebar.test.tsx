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
import { useAuthStore } from '@/store/authStore';

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
    getMyRooms: vi.fn(),
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
  let roomChatsLookup: any[];
  let generalChats: any[];

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
    generalChats = [{ id: 201, type: 'DIRECT_CHAT', name: 'DM Visible' }];

    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: true } as any);
    vi.mocked(useGames).mockReturnValue({ data: [] } as any);
    vi.mocked(useFriends).mockReturnValue({ data: [] } as any);
    vi.mocked(roomService.getMyRooms).mockResolvedValue([mockRoom] as any);
    vi.mocked(chatService.getMyChatRooms).mockImplementation((type?: any) => {
      if (type === 'ROOM_CHAT') {
        return Promise.resolve(roomChatsLookup as any);
      }
      return Promise.resolve(generalChats as any);
    });
  });

  it('opens ROOM_CHAT via /chat/rooms/my?type=ROOM_CHAT and navigates to /chat/{publicId}', async () => {
    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    await user.click(await screen.findByText('내 대기방 A'));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-101');
      expect(chatService.getMyChatRooms).toHaveBeenCalledWith('ROOM_CHAT');
    });
  });

  it('falls back to /games/{gameId}/rooms when ROOM_CHAT lookup result is empty', async () => {
    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    const roomButton = await screen.findByRole('button', { name: /내 대기방 A/i });
    roomChatsLookup = [];

    await act(async () => {
      await user.click(roomButton);
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/games/10/rooms');
    });
    await waitFor(() => expect(roomButton).not.toBeDisabled());
  });

  it('falls back to / when gameId is missing', async () => {
    vi.mocked(roomService.getMyRooms).mockResolvedValue([] as any);

    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    const roomButton = await screen.findByRole('button', { name: /내 대기방 A/i });
    roomChatsLookup = [];
    await act(async () => {
      await user.click(roomButton);
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/');
    });
    await waitFor(() => expect(roomButton).not.toBeDisabled());
  });

  it('renders "대기방 없음" when there is no ROOM_CHAT', async () => {
    roomChatsLookup = [];
    render(<Sidebar />, { wrapper: createWrapper() });
    expect(await screen.findByText('대기방 없음')).toBeInTheDocument();
  });

  it('shows only DIRECT_CHAT/GROUP_CHAT in 채팅방 section', async () => {
    generalChats = [
      { id: 201, type: 'DIRECT_CHAT', name: 'DM Visible' },
      { id: 301, type: 'GROUP_CHAT', name: 'GROUP Visible' },
      { id: 401, type: 'ROOM_CHAT', name: 'ROOM SHOULD HIDE', relatedRoomId: 1 },
    ];

    render(<Sidebar />, { wrapper: createWrapper() });

    expect(await screen.findByText('DM Visible')).toBeInTheDocument();
    expect(screen.getByText('GROUP Visible')).toBeInTheDocument();
    expect(screen.queryByText('ROOM SHOULD HIDE')).not.toBeInTheDocument();
  });

  it('shows "친구 없음" when no friends exist', async () => {
    render(<Sidebar />, { wrapper: createWrapper() });
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
    } as any);

    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    const friendButton = await screen.findByRole('button', { name: /친구A/i });
    await user.click(friendButton);

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
    } as any);

    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    expect(await screen.findByText('토글친구')).toBeInTheDocument();
    const friendSectionToggle = screen.getByText('친구').closest('button');
    expect(friendSectionToggle).toBeTruthy();
    await user.click(friendSectionToggle!);
    expect(screen.queryByText('토글친구')).not.toBeInTheDocument();
    await user.click(friendSectionToggle!);
    expect(await screen.findByText('토글친구')).toBeInTheDocument();
  });
});
