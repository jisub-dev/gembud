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
    getChatRoomByGameRoom: vi.fn(),
  },
}));

vi.mock('@/hooks/queries/useGames', () => ({
  useGames: vi.fn(),
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
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(useAuthStore).mockReturnValue({ isAuthenticated: true } as any);
    vi.mocked(useGames).mockReturnValue({ data: [] } as any);
    vi.mocked(roomService.getMyRooms).mockResolvedValue([mockRoom] as any);
    vi.mocked(chatService.getMyChatRooms).mockResolvedValue([] as any);
  });

  it('navigates using ROOM_CHAT mapping first for 내 대기방 click', async () => {
    vi.mocked(chatService.getMyChatRooms).mockResolvedValue([
      {
        id: 101,
        type: 'ROOM_CHAT',
        relatedRoomId: 1,
        relatedRoomTitle: '내 대기방 A',
      },
    ] as any);
    vi.mocked(chatService.getChatRoomByGameRoom).mockResolvedValue(999);

    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    await user.click(await screen.findByText('내 대기방 A'));

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/chat/101');
    });
    expect(chatService.getChatRoomByGameRoom).not.toHaveBeenCalled();
  });

  it('falls back to getChatRoomByGameRoom when ROOM_CHAT mapping is missing', async () => {
    vi.mocked(chatService.getMyChatRooms).mockResolvedValue([
      { id: 301, type: 'DIRECT_CHAT', name: 'DM A' },
    ] as any);
    vi.mocked(chatService.getChatRoomByGameRoom).mockResolvedValue(555);

    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    const roomButton = await screen.findByRole('button', { name: /내 대기방 A/i });
    await act(async () => {
      await user.click(roomButton);
    });

    await waitFor(() => {
      expect(chatService.getChatRoomByGameRoom).toHaveBeenCalledWith(1);
      expect(mockNavigate).toHaveBeenCalledWith('/chat/555');
    });
    await waitFor(() => expect(roomButton).not.toBeDisabled());
  });

  it('navigates to game room list when mapping lookup fails', async () => {
    vi.mocked(chatService.getMyChatRooms).mockResolvedValue([] as any);
    vi.mocked(chatService.getChatRoomByGameRoom).mockRejectedValue(new Error('not found'));

    const user = userEvent.setup();
    render(<Sidebar />, { wrapper: createWrapper() });

    const roomButton = await screen.findByRole('button', { name: /내 대기방 A/i });
    await act(async () => {
      await user.click(roomButton);
    });

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith('/games/10/rooms');
    });
    await waitFor(() => expect(roomButton).not.toBeDisabled());
  });

  it('shows only DIRECT_CHAT/GROUP_CHAT in 채팅방 section', async () => {
    vi.mocked(roomService.getMyRooms).mockResolvedValue([] as any);
    vi.mocked(chatService.getMyChatRooms).mockResolvedValue([
      { id: 101, type: 'ROOM_CHAT', name: 'ROOM SHOULD HIDE', relatedRoomId: 1 },
      { id: 201, type: 'DIRECT_CHAT', name: 'DM Visible' },
      { id: 301, type: 'GROUP_CHAT', name: 'GROUP Visible' },
    ] as any);

    render(<Sidebar />, { wrapper: createWrapper() });

    expect(await screen.findByText('DM Visible')).toBeInTheDocument();
    expect(screen.getByText('GROUP Visible')).toBeInTheDocument();
    expect(screen.queryByText('ROOM SHOULD HIDE')).not.toBeInTheDocument();
  });
});
