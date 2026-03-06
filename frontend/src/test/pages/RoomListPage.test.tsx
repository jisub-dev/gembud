import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { createElement } from 'react';
import { RoomListPage } from '@/pages/RoomListPage';
import { useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
import { useAds } from '@/hooks/queries/useAds';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import { roomService } from '@/services/roomService';
import type { Room } from '@/types/room';

const { mockNavigate, toastError } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ gameId: '1' }),
  };
});

vi.mock('@/hooks/queries/useRooms', () => ({
  useRooms: vi.fn(),
}));

vi.mock('@/hooks/queries/useGames', () => ({
  useGameOptions: vi.fn(),
}));

vi.mock('@/hooks/queries/useAds', () => ({
  useAds: vi.fn(),
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/useToast', () => ({
  useToast: vi.fn(),
}));

vi.mock('@/services/roomService', () => ({
  roomService: {
    joinRoom: vi.fn(),
  },
}));

vi.mock('@/components/room/RoomFilter', () => ({
  RoomFilter: () => null,
}));

vi.mock('@/components/room/CreateRoomModal', () => ({
  CreateRoomModal: () => null,
}));

vi.mock('@/components/common/AdBanner', () => ({
  default: () => null,
}));

vi.mock('@/components/room/RoomGrid', () => ({
  RoomGrid: ({ rooms, onRoomClick }: { rooms: Room[]; onRoomClick: (publicId: string) => void }) => (
    <div>
      {rooms.map((room) => (
        <button key={room.id} onClick={() => onRoomClick(room.publicId)}>
          {room.title}
        </button>
      ))}
    </div>
  ),
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

const publicRoom: Room = {
  id: 1,
  publicId: 'public-room-1',
  title: '공개 방',
  description: 'desc',
  gameId: 1,
  gameName: 'LOL',
  maxParticipants: 5,
  currentParticipants: 2,
  isPrivate: false,
  status: 'OPEN',
  createdBy: 'host',
  createdAt: '2026-03-06T10:00:00',
};

const privateRoom: Room = {
  ...publicRoom,
  id: 2,
  publicId: 'private-room-2',
  title: '비공개 방',
  isPrivate: true,
};

function createApiError(code: string) {
  return {
    response: {
      data: {
        code,
      },
    },
  };
}

describe('RoomListPage auto-join UX', () => {
  beforeEach(() => {
    vi.clearAllMocks();

    vi.mocked(useRooms).mockReturnValue({
      data: [publicRoom, privateRoom],
      isLoading: false,
      error: null,
    } as any);
    vi.mocked(useGameOptions).mockReturnValue({
      game: { id: 1, name: 'LOL', description: 'desc' },
      tierOptions: [],
      positionOptions: [],
      isLoading: false,
    } as any);
    vi.mocked(useAds).mockReturnValue({ data: [] } as any);
    vi.mocked(useAuthStore).mockReturnValue({ user: null } as any);
    vi.mocked(useToast).mockReturnValue({
      error: toastError,
      success: vi.fn(),
      info: vi.fn(),
    } as any);
  });

  it('auto-joins public room and navigates to chat on success', async () => {
    vi.mocked(roomService.joinRoom).mockResolvedValue({
      room: publicRoom,
      chatRoomId: 555,
    } as any);
    const user = userEvent.setup();

    render(<RoomListPage />, { wrapper: createWrapper() });
    await user.click(screen.getByRole('button', { name: '공개 방' }));

    await waitFor(() => {
      expect(roomService.joinRoom).toHaveBeenCalledWith('public-room-1', undefined);
      expect(mockNavigate).toHaveBeenCalledWith('/chat/555');
    });
  });

  it('keeps password input when password is invalid (401)', async () => {
    vi.mocked(roomService.joinRoom).mockRejectedValue(createApiError('ROOM006'));
    const user = userEvent.setup();

    render(<RoomListPage />, { wrapper: createWrapper() });
    await user.click(screen.getByRole('button', { name: '비공개 방' }));

    const input = await screen.findByPlaceholderText('비밀번호를 입력하세요');
    await user.type(input, 'secret');
    await user.click(screen.getByRole('button', { name: '입장' }));

    await waitFor(() => {
      expect(toastError).toHaveBeenCalledWith('비밀번호가 올바르지 않습니다');
    });
    expect(screen.getByText('비밀번호 입력')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('비밀번호를 입력하세요')).toHaveValue('secret');
  });

  it('shows toast and keeps user on room list when room is full (409)', async () => {
    vi.mocked(roomService.joinRoom).mockRejectedValue(createApiError('ROOM002'));
    const user = userEvent.setup();

    render(<RoomListPage />, { wrapper: createWrapper() });
    await user.click(screen.getByRole('button', { name: '비공개 방' }));

    await user.type(await screen.findByPlaceholderText('비밀번호를 입력하세요'), 'pw');
    await user.click(screen.getByRole('button', { name: '입장' }));

    await waitFor(() => {
      expect(toastError).toHaveBeenCalledWith('방이 꽉 찼습니다');
    });
    await waitFor(() => {
      expect(screen.queryByText('비밀번호 입력')).not.toBeInTheDocument();
    });
    expect(mockNavigate).not.toHaveBeenCalledWith('/chat/555');
  });

  it('shows 404 toast and routes to room list path', async () => {
    vi.mocked(roomService.joinRoom).mockRejectedValue(createApiError('ROOM001'));
    const user = userEvent.setup();

    render(<RoomListPage />, { wrapper: createWrapper() });
    await user.click(screen.getByRole('button', { name: '공개 방' }));

    await waitFor(() => {
      expect(toastError).toHaveBeenCalledWith('방을 찾을 수 없습니다');
      expect(mockNavigate).toHaveBeenCalledWith('/games/1/rooms');
    });
  });
});
