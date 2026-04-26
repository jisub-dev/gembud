import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { createElement } from 'react';
import ChatPage from '@/pages/ChatPage';
import { chatKeys } from '@/hooks/queries/useChatQueries';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { chatService } from '@/services/chatService';
import { roomService } from '@/services/roomService';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import type { ChatRoomInfo } from '@/types/chat';
import type { Room } from '@/types/room';

const { clipboardWriteText, mockNavigate, toastSuccess, toastError } = vi.hoisted(() => ({
  clipboardWriteText: vi.fn(),
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
    getMyActiveRoom: vi.fn(),
    leaveRoom: vi.fn(),
    regenerateInviteCode: vi.fn(),
    buildInviteLink: vi.fn(),
    kickParticipant: vi.fn(),
    transferHost: vi.fn(),
    startRoom: vi.fn(),
    resetRoom: vi.fn(),
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
  RoomParticipants: ({
    onKick,
    onTransferHost,
    participants = [],
  }: {
    onKick?: (userId: number, nickname: string) => void;
    onTransferHost?: (userId: number, nickname: string) => void;
    participants?: Array<{ nickname: string; userId: number }>;
  }) => {
    const targetParticipant = participants.find((participant) => participant.userId === 2);

    return (
      <div>
        <div>RoomParticipants</div>
        {targetParticipant && onKick && (
          <button onClick={() => onKick(targetParticipant.userId, targetParticipant.nickname)}>
            강퇴 실행
          </button>
        )}
        {targetParticipant && onTransferHost && (
          <button onClick={() => onTransferHost(targetParticipant.userId, targetParticipant.nickname)}>
            방장 이전 실행
          </button>
        )}
      </div>
    );
  },
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

  const wrapper = ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient }, children);

  return { queryClient, wrapper };
}

async function renderChatPage() {
  const { queryClient, wrapper } = createWrapper();
  let utils!: ReturnType<typeof render>;
  await act(async () => {
    utils = render(<ChatPage />, { wrapper });
    await Promise.resolve();
  });
  await screen.findByText('추천 방');
  return { queryClient, ...utils };
}

async function clickAndFlush(user: ReturnType<typeof userEvent.setup>, element: Element) {
  await act(async () => {
    await user.click(element);
    await Promise.resolve();
  });
}

const chatRoomInfo: ChatRoomInfo = {
  id: 101,
  publicId: 'chat-public-101',
  type: 'ROOM_CHAT',
  name: '테스트 방 채팅',
  relatedRoomId: 33,
};

const chatRoomInfoWithoutRelatedRoomId: ChatRoomInfo = {
  id: 101,
  publicId: 'chat-public-101',
  type: 'ROOM_CHAT',
  name: '테스트 방 채팅',
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

function createHostRoom(status: Room['status'] = 'OPEN'): Room {
  return {
    ...relatedRoom,
    status,
    participants: [
      { userId: 1, nickname: 'me', isHost: true },
      { userId: 2, nickname: 'guest', isHost: false },
    ],
  };
}

function createHostPrivateRoom(inviteExpiresAt: string): Room {
  return {
    ...createHostRoom('OPEN'),
    isPrivate: true,
    inviteCode: 'INV123',
    inviteCodeExpiresAt: inviteExpiresAt,
  };
}

describe('ChatPage recommendation leave flow', () => {
  let currentActiveRoom: Room | null;
  let roomChatRoomsState: ChatRoomInfo[];
  let myChatRoomsState: ChatRoomInfo[];

  beforeEach(() => {
    vi.clearAllMocks();
    window.localStorage.clear();
    vi.spyOn(window, 'confirm').mockReturnValue(true);
    vi.spyOn(window, 'alert').mockImplementation(() => undefined);
    currentActiveRoom = relatedRoom;
    roomChatRoomsState = [chatRoomInfo];
    myChatRoomsState = [chatRoomInfo];
    vi.mocked(chatService.getMyChatRooms).mockImplementation((type?: Parameters<typeof chatService.getMyChatRooms>[0]) => {
      if (type === 'ROOM_CHAT') {
        return Promise.resolve(roomChatRoomsState);
      }
      return Promise.resolve(myChatRoomsState);
    });
    vi.mocked(roomService.getMyActiveRoom).mockImplementation(async () => currentActiveRoom);
    vi.mocked(roomService.leaveRoom).mockImplementation(async () => {
      currentActiveRoom = null;
      roomChatRoomsState = [];
      myChatRoomsState = [];
    });
    vi.mocked(roomService.kickParticipant).mockImplementation(async (_roomId, userId) => {
      if (!currentActiveRoom) return;
      currentActiveRoom = {
        ...currentActiveRoom,
        currentParticipants: Math.max(0, currentActiveRoom.currentParticipants - 1),
        participants: currentActiveRoom.participants?.filter((participant) => participant.userId !== userId),
      };
    });
    vi.mocked(roomService.transferHost).mockImplementation(async (_roomId, userId) => {
      if (!currentActiveRoom) return;
      currentActiveRoom = {
        ...currentActiveRoom,
        participants: currentActiveRoom.participants?.map((participant) => ({
          ...participant,
          isHost: participant.userId === userId,
        })),
      };
    });
    vi.mocked(roomService.startRoom).mockImplementation(async () => {
      if (!currentActiveRoom) return;
      currentActiveRoom = {
        ...currentActiveRoom,
        status: 'IN_PROGRESS',
      };
    });
    vi.mocked(roomService.resetRoom).mockImplementation(async () => {
      if (!currentActiveRoom) return;
      currentActiveRoom = {
        ...currentActiveRoom,
        status: 'OPEN',
      };
    });
    vi.mocked(roomService.regenerateInviteCode).mockImplementation(async () => {
      if (!currentActiveRoom) {
        throw new Error('No active room');
      }
      currentActiveRoom = {
        ...currentActiveRoom,
        inviteCode: 'NEWCODE123',
        inviteCodeExpiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
      };
      return currentActiveRoom!;
    });
    vi.mocked(roomService.buildInviteLink).mockImplementation((room) =>
      `https://example.com/invite/${room.inviteCode ?? 'missing'}`,
    );
    vi.mocked(useAuthStore).mockReturnValue({
      user: { id: 1, nickname: 'me' },
    } as unknown as ReturnType<typeof useAuthStore>);
    vi.mocked(useToast).mockReturnValue({
      success: toastSuccess,
      error: toastError,
      info: vi.fn(),
    } as unknown as ReturnType<typeof useToast>);
    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: clipboardWriteText.mockResolvedValue(undefined),
      },
      configurable: true,
    });
  });

  it('navigates to next recommendation flow when leaving a recommended room', async () => {
    window.localStorage.setItem(
      'roomRecommendations:active',
      JSON.stringify({ 'room-public-33': { gameId: 7 } }),
    );
    const user = userEvent.setup();

    await renderChatPage();
    await clickAndFlush(user, await screen.findByRole('button', { name: '대기방 나가기' }));

    await waitFor(() => {
      expect(roomService.leaveRoom).toHaveBeenCalledWith(33);
      expect(toastSuccess).toHaveBeenCalledWith('대기방을 나갔습니다');
      expect(mockNavigate).toHaveBeenCalledWith('/games/7/rooms?recommend=true&exclude=room-public-33');
    });

    expect(window.localStorage.getItem('roomRecommendations:active')).toBe('{}');
    expect(window.localStorage.getItem('roomRecommendations:excluded')).toContain('room-public-33');
  });

  it('clears active room and room chat caches immediately after leaving', async () => {
    const user = userEvent.setup();
    const { queryClient } = await renderChatPage();

    await clickAndFlush(user, await screen.findByRole('button', { name: '대기방 나가기' }));

    await waitFor(() => {
      expect(roomService.leaveRoom).toHaveBeenCalledWith(33);
      expect(queryClient.getQueryData(roomKeys.myActive())).toBeNull();
      expect(queryClient.getQueryData(chatKeys.myRoomChats())).toEqual([]);
      expect(queryClient.getQueryData(chatKeys.myList())).toEqual([]);
    });
  });

  it('updates active room cache when host starts the room', async () => {
    currentActiveRoom = createHostRoom('OPEN');
    const user = userEvent.setup();
    const { queryClient } = await renderChatPage();

    await clickAndFlush(user, await screen.findByRole('button', { name: '게임 시작' }));

    await waitFor(() => {
      expect(roomService.startRoom).toHaveBeenCalledWith(33);
      expect((queryClient.getQueryData(roomKeys.myActive()) as Room | null)?.status).toBe('IN_PROGRESS');
    });
  });

  it('updates active room cache when host resets the room to OPEN', async () => {
    currentActiveRoom = createHostRoom('IN_PROGRESS');
    const user = userEvent.setup();
    const { queryClient } = await renderChatPage();

    await clickAndFlush(user, await screen.findByRole('button', { name: '대기중으로 변경' }));

    await waitFor(() => {
      expect(roomService.resetRoom).toHaveBeenCalledWith('room-public-33');
      expect((queryClient.getQueryData(roomKeys.myActive()) as Room | null)?.status).toBe('OPEN');
      expect(toastSuccess).toHaveBeenCalledWith('방 상태를 대기중으로 변경했습니다.');
    });
  });

  it('updates participants cache when host transfers the host role', async () => {
    currentActiveRoom = createHostRoom('OPEN');
    const user = userEvent.setup();
    const { queryClient } = await renderChatPage();

    await clickAndFlush(user, await screen.findByRole('button', { name: '방장 이전 실행' }));

    await waitFor(() => {
      expect(roomService.transferHost).toHaveBeenCalledWith(33, 2);
      const activeRoom = queryClient.getQueryData(roomKeys.myActive()) as Room | null;
      expect(activeRoom?.participants?.find((participant) => participant.userId === 1)?.isHost).toBe(false);
      expect(activeRoom?.participants?.find((participant) => participant.userId === 2)?.isHost).toBe(true);
    });
  });

  it('updates participants cache when host kicks a participant', async () => {
    currentActiveRoom = createHostRoom('OPEN');
    const user = userEvent.setup();
    const { queryClient } = await renderChatPage();

    await clickAndFlush(user, await screen.findByRole('button', { name: '강퇴 실행' }));

    await waitFor(() => {
      expect(roomService.kickParticipant).toHaveBeenCalledWith(33, 2);
      const activeRoom = queryClient.getQueryData(roomKeys.myActive()) as Room | null;
      expect(activeRoom?.currentParticipants).toBe(1);
      expect(activeRoom?.participants).toEqual([{ userId: 1, nickname: 'me', isHost: true }]);
    });
  });

  it('regenerates invite link through the shared invite action hook', async () => {
    currentActiveRoom = createHostPrivateRoom(new Date(Date.now() + 5 * 60 * 1000).toISOString());
    const user = userEvent.setup();

    await renderChatPage();
    await clickAndFlush(user, await screen.findByRole('button', { name: '초대 링크 재발급' }));

    await waitFor(() => {
      expect(roomService.regenerateInviteCode).toHaveBeenCalledWith('room-public-33');
      expect(toastSuccess).toHaveBeenCalledWith('초대 링크를 재발급했습니다');
      expect(screen.getByDisplayValue('https://example.com/invite/NEWCODE123')).toBeInTheDocument();
    });
  });

  it('shows invite expiry warning for host when link is expiring soon', async () => {
    const soon = new Date(Date.now() + 5 * 60 * 1000).toISOString();
    vi.mocked(roomService.getMyActiveRoom).mockResolvedValue(createHostPrivateRoom(soon));
    vi.mocked(roomService.buildInviteLink).mockReturnValue('https://example.com/invite');

    await renderChatPage();

    expect(await screen.findByText('초대 링크 관리')).toBeInTheDocument();
    expect(screen.getByText(/초대 링크 만료 임박/)).toBeInTheDocument();
    expect(screen.getByText(/남은 시간:/)).toBeInTheDocument();
  });

  it('shows expired message for host when invite link is already expired', async () => {
    const expired = new Date(Date.now() - 2 * 60 * 1000).toISOString();
    vi.mocked(roomService.getMyActiveRoom).mockResolvedValue(createHostPrivateRoom(expired));
    vi.mocked(roomService.buildInviteLink).mockReturnValue('https://example.com/invite');

    await renderChatPage();

    expect(await screen.findByText('초대 링크 관리')).toBeInTheDocument();
    expect(screen.getByText('초대 링크가 만료되었습니다. 재발급 후 공유해주세요.')).toBeInTheDocument();
    expect(screen.getByText(/남은 시간: 만료됨/)).toBeInTheDocument();
  });

  it('does not render room close action for host', async () => {
    vi.mocked(roomService.getMyActiveRoom).mockResolvedValue(createHostPrivateRoom(new Date(Date.now() + 3600000).toISOString()));
    vi.mocked(roomService.buildInviteLink).mockReturnValue('https://example.com/invite');

    await renderChatPage();

    expect(await screen.findByText('초대 링크 관리')).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '방 종료' })).not.toBeInTheDocument();
  });

  it('uses ROOM_CHAT mapping without per-room lookup when relatedRoomId is missing from the general chat payload', async () => {
    vi.mocked(chatService.getMyChatRooms).mockImplementation((type?: Parameters<typeof chatService.getMyChatRooms>[0]) => {
      if (type === 'ROOM_CHAT') {
        return Promise.resolve([chatRoomInfo]);
      }
      return Promise.resolve([chatRoomInfoWithoutRelatedRoomId]);
    });
    vi.mocked(chatService.getChatRoomByGameRoom).mockResolvedValue('unused');

    await renderChatPage();

    expect(await screen.findByText('추천 방')).toBeInTheDocument();
    expect(chatService.getChatRoomByGameRoom).not.toHaveBeenCalled();
  });
});
