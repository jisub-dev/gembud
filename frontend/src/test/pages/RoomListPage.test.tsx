import { describe, it, expect, vi, beforeEach } from 'vitest';
import { act, render, screen, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { MemoryRouter } from 'react-router-dom';
import { createElement } from 'react';
import { RoomListPage } from '@/pages/RoomListPage';
import { useMyActiveRoom, useMyRooms, useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
import { useRecommendedRooms } from '@/hooks/queries/useMatching';
import { useAds } from '@/hooks/queries/useAds';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import { roomService } from '@/services/roomService';
import type { Room } from '@/types/room';

const { mockNavigate, toastError, toastSuccess, clipboardWriteText } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
  toastError: vi.fn(),
  toastSuccess: vi.fn(),
  clipboardWriteText: vi.fn(),
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
  useMyRooms: vi.fn(),
  useMyActiveRoom: vi.fn(),
}));

vi.mock('@/hooks/queries/useGames', () => ({
  useGameOptions: vi.fn(),
}));

vi.mock('@/hooks/queries/useMatching', () => ({
  useRecommendedRooms: vi.fn(),
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
    getMyRooms: vi.fn(),
    getMyActiveRoom: vi.fn(),
    getRoom: vi.fn(),
    regenerateInviteCode: vi.fn(),
    leaveRoom: vi.fn(),
  },
}));

vi.mock('@/services/chatService', () => ({
  chatService: {
    getChatRoomByGameRoom: vi.fn(),
  },
}));

vi.mock('@/components/room/RoomFilter', () => ({
  RoomFilter: () => null,
}));

vi.mock('@/components/room/CreateRoomModal', () => ({
  CreateRoomModal: ({
    onClose,
    onSuccess,
  }: {
    onClose: () => void;
    onSuccess: () => void;
  }) => (
    <div>
      <p>방 생성 모달</p>
      <button onClick={onClose}>생성 모달 닫기</button>
      <button onClick={onSuccess}>생성 완료</button>
    </div>
  ),
}));

vi.mock('@/components/common/AdBanner', () => ({
  default: () => null,
}));

vi.mock('@/components/room/RoomGrid', () => ({
  RoomGrid: ({
    rooms,
    onRoomClick,
    shouldShowRegenerateInviteButton,
    onRegenerateInviteCode,
  }: {
    rooms: Room[];
    onRoomClick: (publicId: string) => void;
    shouldShowRegenerateInviteButton?: (room: Room) => boolean;
    onRegenerateInviteCode?: (publicId: string) => void;
  }) => (
    <div>
      {rooms.map((room) => (
        <div key={room.id}>
          <button onClick={() => onRoomClick(room.publicId)}>{room.title}</button>
          {shouldShowRegenerateInviteButton?.(room) && (
            <button onClick={() => onRegenerateInviteCode?.(room.publicId)}>
              {room.title} 재발급
            </button>
          )}
        </div>
      ))}
    </div>
  ),
}));

function createWrapper(initialPath = '/') {
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
        { initialEntries: [initialPath], future: { v7_startTransition: true, v7_relativeSplatPath: true } },
        children,
      ),
    );
}

async function renderRoomListPage(initialPath = '/') {
  let utils!: ReturnType<typeof render>;
  await act(async () => {
    utils = render(<RoomListPage />, { wrapper: createWrapper(initialPath) });
    await Promise.resolve();
  });
  await screen.findByRole('button', { name: '공개 방' });
  return utils;
}

async function clickAndFlush(user: ReturnType<typeof userEvent.setup>, element: Element) {
  await act(async () => {
    await user.click(element);
    await Promise.resolve();
  });
}

async function typeAndFlush(
  user: ReturnType<typeof userEvent.setup>,
  element: Element,
  text: string,
) {
  await act(async () => {
    await user.type(element, text);
    await Promise.resolve();
  });
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

const activeRoom: Room = {
  ...publicRoom,
  id: 99,
  publicId: 'my-room-99',
  title: '내 현재 방',
};

const fullRoom: Room = {
  ...publicRoom,
  id: 3,
  publicId: 'full-room-3',
  title: '풀방',
  status: 'FULL',
  currentParticipants: 5,
};

const inProgressRoom: Room = {
  ...publicRoom,
  id: 4,
  publicId: 'in-progress-room-4',
  title: '게임중 방',
  status: 'IN_PROGRESS',
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
    vi.resetAllMocks();
    window.localStorage.clear();
    vi.spyOn(window, 'confirm').mockReturnValue(true);

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
    vi.mocked(useRecommendedRooms).mockReturnValue({
      data: [
        { room: publicRoom, roomId: publicRoom.id, matchingScore: 95, reason: 'good' },
        { room: privateRoom, roomId: privateRoom.id, matchingScore: 85, reason: 'private' },
      ],
      isLoading: false,
    } as any);
    vi.mocked(useAds).mockReturnValue({ data: [] } as any);
    vi.mocked(useAuthStore).mockReturnValue({ user: null } as any);
    vi.mocked(useMyRooms).mockReturnValue({ data: [], isLoading: false } as any);
    vi.mocked(useMyActiveRoom).mockReturnValue({ data: null, isLoading: false } as any);
    vi.mocked(useToast).mockReturnValue({
      error: toastError,
      success: toastSuccess,
      info: vi.fn(),
    } as any);

    Object.defineProperty(navigator, 'clipboard', {
      value: {
        writeText: clipboardWriteText.mockResolvedValue(undefined),
      },
      configurable: true,
    });
    Object.defineProperty(document, 'execCommand', {
      value: vi.fn(() => true),
      configurable: true,
    });
  });

  it('leaves the current room and retries when ROOM008 is returned for another room', async () => {
    vi.mocked(useAuthStore).mockReturnValue({ user: { id: 1, nickname: 'me' } } as any);
    vi.mocked(useMyRooms).mockReturnValue({
      data: [
      {
        ...publicRoom,
        id: 88,
        publicId: 'my-room-88',
        title: '먼저 로드된 다른 방',
      },
      activeRoom,
      ],
      isLoading: false,
    } as any);
    vi.mocked(useMyActiveRoom).mockReturnValue({ data: activeRoom, isLoading: false } as any);
    vi.mocked(roomService.joinRoom)
      .mockRejectedValueOnce(createApiError('ROOM008'))
      .mockResolvedValueOnce({
        room: publicRoom,
        chatRoomId: 'chat-public-555',
      } as any);
    vi.mocked(roomService.leaveRoom).mockResolvedValue(undefined);

    const user = userEvent.setup();
    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '공개 방' }));

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled();
      expect(roomService.leaveRoom).toHaveBeenCalledWith(99);
      expect(roomService.joinRoom).toHaveBeenCalledTimes(2);
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-555');
    });
  });

  it('does not leave or retry when ROOM008 is returned and the move is canceled', async () => {
    vi.mocked(useAuthStore).mockReturnValue({ user: { id: 1, nickname: 'me' } } as any);
    vi.mocked(window.confirm).mockReturnValue(false);
    vi.mocked(useMyRooms).mockReturnValue({ data: [activeRoom], isLoading: false } as any);
    vi.mocked(useMyActiveRoom).mockReturnValue({ data: activeRoom, isLoading: false } as any);
    vi.mocked(roomService.joinRoom).mockRejectedValueOnce(createApiError('ROOM008'));

    const user = userEvent.setup();
    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '공개 방' }));

    await waitFor(() => {
      expect(window.confirm).toHaveBeenCalled();
      expect(roomService.leaveRoom).not.toHaveBeenCalled();
      expect(roomService.joinRoom).toHaveBeenCalledTimes(1);
      expect(mockNavigate).not.toHaveBeenCalled();
    });
  });

  it('auto-joins public room and navigates to chat on success', async () => {
    vi.mocked(roomService.joinRoom).mockResolvedValue({
      room: publicRoom,
      chatRoomId: 'chat-public-555',
    } as any);
    const user = userEvent.setup();

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '공개 방' }));

    await waitFor(() => {
      expect(roomService.joinRoom).toHaveBeenCalledWith('public-room-1', undefined, undefined);
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-555');
    });
  });

  it('keeps password input when password is invalid (401)', async () => {
    vi.mocked(roomService.joinRoom).mockRejectedValue(createApiError('ROOM006'));
    const user = userEvent.setup();

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '비공개 방' }));

    const input = await screen.findByPlaceholderText('비밀번호를 입력하세요');
    await typeAndFlush(user, input, 'secret');
    await clickAndFlush(user, screen.getByRole('button', { name: '입장' }));

    await waitFor(() => {
      expect(toastError).toHaveBeenCalledWith('비밀번호가 올바르지 않습니다');
    });
    expect(screen.getByText('비밀번호 입력')).toBeInTheDocument();
    expect(screen.getByPlaceholderText('비밀번호를 입력하세요')).toHaveValue('secret');
  });

  it('shows toast and keeps user on room list when room is full (409)', async () => {
    vi.mocked(roomService.joinRoom).mockRejectedValue(createApiError('ROOM002'));
    const user = userEvent.setup();

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '비공개 방' }));

    await typeAndFlush(user, await screen.findByPlaceholderText('비밀번호를 입력하세요'), 'pw');
    await clickAndFlush(user, screen.getByRole('button', { name: '입장' }));

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

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '공개 방' }));

    await waitFor(() => {
      expect(toastError).toHaveBeenCalledWith('방을 찾을 수 없습니다');
      expect(mockNavigate).toHaveBeenCalledWith('/games/1/rooms');
    });
  });

  it('opens invite mode modal from URL params and joins with inviteCode', async () => {
    vi.mocked(roomService.joinRoom).mockResolvedValue({
      room: privateRoom,
      chatRoomId: 'chat-private-777',
    } as any);
    const user = userEvent.setup();

    await renderRoomListPage('/games/1/rooms?room=private-room-2&invite=INVITE123');

    await clickAndFlush(user, await screen.findByRole('button', { name: '초대코드로 입장' }));

    await waitFor(() => {
      expect(roomService.joinRoom).toHaveBeenCalledWith('private-room-2', undefined, 'INVITE123');
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-private-777');
    });
  });

  it('shows invite-code-expired message when invite code is invalid', async () => {
    vi.mocked(roomService.joinRoom).mockRejectedValue(createApiError('ROOM012'));
    const user = userEvent.setup();

    await renderRoomListPage('/games/1/rooms?room=private-room-2&invite=INVITE123');

    await clickAndFlush(user, await screen.findByRole('button', { name: '초대코드로 입장' }));

    await waitFor(() => {
      expect(toastError).toHaveBeenCalledWith('초대 코드가 유효하지 않거나 만료되었습니다');
    });
    expect(screen.getByText('초대 링크가 만료되었거나 유효하지 않습니다')).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '방 목록으로 돌아가기' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '새 초대 링크 요청' })).toBeInTheDocument();
  });

  it('shows invite target summary panel when entering with invite params', async () => {
    await renderRoomListPage('/games/1/rooms?room=private-room-2&invite=INVITE123');

    expect(await screen.findByText('초대 링크로 입장 중입니다')).toBeInTheDocument();
    expect(screen.getByText('대상 방: 비공개 방')).toBeInTheDocument();
  });

  it('shows missing target room guide when invite room does not exist', async () => {
    await renderRoomListPage('/games/1/rooms?room=missing-room&invite=INVITE123');

    expect(await screen.findByText('초대 링크가 만료되었거나 유효하지 않습니다')).toBeInTheDocument();
    expect(screen.getByText(/대상 방 정보를 불러오지 못했습니다/)).toBeInTheDocument();
  });

  it('opens create modal when create=true query param is present', async () => {
    await renderRoomListPage('/games/1/rooms?create=true');

    expect(await screen.findByText('방 생성 모달')).toBeInTheDocument();
  });

  it('opens and closes create modal from the create CTA', async () => {
    const user = userEvent.setup();

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '방 만들기' }));

    expect(await screen.findByText('방 생성 모달')).toBeInTheDocument();

    await clickAndFlush(user, screen.getByRole('button', { name: '생성 모달 닫기' }));

    await waitFor(() => {
      expect(screen.queryByText('방 생성 모달')).not.toBeInTheDocument();
    });
  });

  it('regenerates invite code for host private room and copies invite URL', async () => {
    vi.mocked(useAuthStore).mockReturnValue({
      user: { nickname: 'host' },
    } as any);
    vi.mocked(roomService.regenerateInviteCode).mockResolvedValue({
      ...privateRoom,
      inviteCode: 'NEWCODE123',
    } as any);
    const user = userEvent.setup();

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '비공개 방 재발급' }));

    await waitFor(() => {
      expect(roomService.regenerateInviteCode).toHaveBeenCalledWith('private-room-2');
      expect(toastSuccess).toHaveBeenCalledWith('초대 링크가 클립보드에 복사되었습니다');
    });
  });

  it('shows OPEN and FULL rooms, but hides IN_PROGRESS rooms in list', async () => {
    vi.mocked(useRooms).mockReturnValue({
      data: [publicRoom, fullRoom, inProgressRoom],
      isLoading: false,
      error: null,
    } as any);

    await renderRoomListPage();

    expect(await screen.findByRole('button', { name: '공개 방' })).toBeInTheDocument();
    expect(screen.getByRole('button', { name: '풀방' })).toBeInTheDocument();
    expect(screen.queryByRole('button', { name: '게임중 방' })).not.toBeInTheDocument();
  });

  it('joins the first available public recommended room when clicking recommendation CTA', async () => {
    vi.mocked(roomService.joinRoom).mockResolvedValue({
      room: publicRoom,
      chatRoomId: 'chat-public-999',
    } as any);
    const user = userEvent.setup();

    await renderRoomListPage();
    await clickAndFlush(user, screen.getByRole('button', { name: '추천 방 바로 입장' }));

    await waitFor(() => {
      expect(roomService.joinRoom).toHaveBeenCalledWith('public-room-1', undefined, undefined);
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-999');
    });
  });

  it('auto-recommends the next room when excluded room is provided in URL', async () => {
    const anotherRoom: Room = {
      ...publicRoom,
      id: 5,
      publicId: 'public-room-5',
      title: '대체 추천 방',
    };
    vi.mocked(useRecommendedRooms).mockReturnValue({
      data: [
        { room: publicRoom, roomId: publicRoom.id, matchingScore: 99, reason: 'top' },
        { room: anotherRoom, roomId: anotherRoom.id, matchingScore: 80, reason: 'next' },
      ],
      isLoading: false,
    } as any);
    vi.mocked(roomService.joinRoom).mockResolvedValue({
      room: anotherRoom,
      chatRoomId: 'chat-public-next',
    } as any);

    await renderRoomListPage('/games/1/rooms?recommend=true&exclude=public-room-1');

    await waitFor(() => {
      expect(roomService.joinRoom).toHaveBeenCalledWith('public-room-5', undefined, undefined);
      expect(mockNavigate).toHaveBeenCalledWith('/chat/chat-public-next');
    });
  });
});
