import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import ChatPage from '@/pages/ChatPage';
import { chatService } from '@/services/chatService';
import { roomService } from '@/services/roomService';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import type { ChatRoomInfo } from '@/types/chat';
import type { Room } from '@/types/room';

const { mockNavigate, toastSuccess, toastError } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  toastSuccess: vi.fn(),
  toastError: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
    useParams: () => ({ roomId: 'chat-public-101' }),
  };
});

vi.mock('@/services/chatService', () => ({
  chatService: {
    getMyChatRooms: vi.fn(),
    getChatRoomByGameRoom: vi.fn(),
    getMessages: vi.fn(),
  },
}));

vi.mock('@/services/roomService', () => ({
  roomService: {
    getMyRooms: vi.fn(),
    leaveRoom: vi.fn(),
    regenerateInviteCode: vi.fn(),
    buildInviteLink: vi.fn(),
    kickParticipant: vi.fn(),
    transferHost: vi.fn(),
    startRoom: vi.fn(),
    resetRoom: vi.fn(),
    closeRoom: vi.fn(),
  },
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/hooks/useToast', () => ({
  useToast: vi.fn(),
}));

vi.mock('@/components/chat/ChatPanel', () => ({
  ChatPanel: () => <div>ChatPanel</div>,
}));

vi.mock('@/components/room/RoomParticipants', () => ({
  RoomParticipants: () => <div>RoomParticipants</div>,
}));

vi.mock('@/components/room/EvaluateModal', () => ({
  EvaluateModal: () => null,
}));

vi.mock('@/services/evaluationService', () => ({
  default: {
    getEvaluatable: vi.fn().mockResolvedValue([]),
  },
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children);
}

const chatRoomInfo: ChatRoomInfo = {
  id: 101,
  publicId: 'chat-public-101',
  type: 'ROOM_CHAT',
  name: '테스트 방 채팅',
  relatedRoomId: 33,
};

const relatedRoom: Room = {
  id: 33,
  publicId: 'room-public-33',
  title: '추천 방',
  description: '추천된 방',
  gameId: 7,
  gameName: 'LOL',
  maxParticipants: 5,
  currentParticipants: 2,
  isPrivate: false,
  status: 'OPEN',
  createdBy: 'host',
  createdAt: '2026-03-10T10:00:00',
  participants: [
    { userId: 1, nickname: 'me', isHost: false },
    { userId: 2, nickname: 'host', isHost: true },
  ],
};

function createHostPrivateRoom(inviteExpiresAt: string): Room {
  return {
    ...relatedRoom,
    isPrivate: true,
    inviteCode: 'INV123',
    inviteCodeExpiresAt: inviteExpiresAt,
    participants: [
      { userId: 1, nickname: 'me', isHost: true },
      { userId: 2, nickname: 'guest', isHost: false },
    ],
  };
}

describe('ChatPage recommendation leave flow', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
    vi.mocked(chatService.getMyChatRooms).mockResolvedValue([chatRoomInfo]);
    vi.mocked(roomService.getMyRooms).mockResolvedValue([relatedRoom]);
    vi.mocked(roomService.leaveRoom).mockResolvedValue(undefined);
    vi.mocked(useAuthStore).mockReturnValue({
      user: { id: 1, nickname: 'me' },
    } as any);
    vi.mocked(useToast).mockReturnValue({
      success: toastSuccess,
      error: toastError,
      info: vi.fn(),
    } as any);
  });

  it('navigates to next recommendation flow when leaving a recommended room', async () => {
    window.localStorage.setItem(
      'roomRecommendations:active',
      JSON.stringify({ 'room-public-33': { gameId: 7 } }),
    );
    const user = userEvent.setup();

    render(<ChatPage />, { wrapper: createWrapper() });
    await user.click(await screen.findByRole('button', { name: '대기방 나가기' }));

    await waitFor(() => {
      expect(roomService.leaveRoom).toHaveBeenCalledWith(33);
      expect(toastSuccess).toHaveBeenCalledWith('대기방을 나갔습니다');
      expect(mockNavigate).toHaveBeenCalledWith('/games/7/rooms?recommend=true&exclude=room-public-33');
    });

    expect(window.localStorage.getItem('roomRecommendations:active')).toBe('{}');
    expect(window.localStorage.getItem('roomRecommendations:excluded')).toContain('room-public-33');
  });

  it('shows invite expiry warning for host when link is expiring soon', async () => {
    const soon = new Date(Date.now() + 5 * 60 * 1000).toISOString();
    vi.mocked(roomService.getMyRooms).mockResolvedValue([createHostPrivateRoom(soon)]);
    vi.mocked(roomService.buildInviteLink).mockReturnValue('https://example.com/invite');

    render(<ChatPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('초대 링크 관리')).toBeInTheDocument();
    expect(screen.getByText(/초대 링크 만료 임박/)).toBeInTheDocument();
    expect(screen.getByText(/남은 시간:/)).toBeInTheDocument();
  });

  it('shows expired message for host when invite link is already expired', async () => {
    const expired = new Date(Date.now() - 2 * 60 * 1000).toISOString();
    vi.mocked(roomService.getMyRooms).mockResolvedValue([createHostPrivateRoom(expired)]);
    vi.mocked(roomService.buildInviteLink).mockReturnValue('https://example.com/invite');

    render(<ChatPage />, { wrapper: createWrapper() });

    expect(await screen.findByText('초대 링크 관리')).toBeInTheDocument();
    expect(screen.getByText('초대 링크가 만료되었습니다. 재발급 후 공유해주세요.')).toBeInTheDocument();
    expect(screen.getByText(/남은 시간: 만료됨/)).toBeInTheDocument();
  });
});
