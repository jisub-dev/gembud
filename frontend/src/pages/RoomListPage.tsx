import { useState, useMemo, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Plus, Sparkles } from 'lucide-react';
import { useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
import { useRecommendedRooms } from '@/hooks/queries/useMatching';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { RoomGrid } from '@/components/room/RoomGrid';
import { RoomFilter } from '@/components/room/RoomFilter';
import { CreateRoomModal } from '@/components/room/CreateRoomModal';
import { PasswordModal } from '@/components/room/PasswordModal';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import AdBanner from '@/components/common/AdBanner';
import { useAds } from '@/hooks/queries/useAds';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import { roomService } from '@/services/roomService';
import { chatService } from '@/services/chatService';
import type { Room } from '@/types/room';
import { isPremiumActive } from '@/config/features';

const RECOMMENDATION_EXCLUSION_KEY = 'roomRecommendations:excluded';
const RECOMMENDATION_ACTIVE_KEY = 'roomRecommendations:active';

export function RoomListPage() {
  const { gameId } = useParams<{ gameId: string }>();
  const navigate = useNavigate();
  const [searchParams, setSearchParams] = useSearchParams();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();
  const { data: ads = [] } = useAds();
  const showAds = !isPremiumActive(user?.isPremium);
  const toast = useToast();

  const [selectedTiers, setSelectedTiers] = useState<number[]>([]);
  const [selectedPositions, setSelectedPositions] = useState<number[]>([]);
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [inviteEntryState, setInviteEntryState] = useState<InviteEntryState | null>(null);

  // Auto-join state
  const [joiningRoom, setJoiningRoom] = useState<Room | null>(null);
  const [joiningInviteCode, setJoiningInviteCode] = useState<string | undefined>(undefined);
  const [isJoining, setIsJoining] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const inviteHandledRef = useRef<string | null>(null);
  const createHandledRef = useRef(false);
  const recommendationHandledRef = useRef<string | null>(null);

  const { data: rooms, isLoading: roomsLoading, error: roomsError } = useRooms(Number(gameId));
  const { game, tierOptions, positionOptions, isLoading: gameLoading } = useGameOptions(Number(gameId));
  const recommendationLimit = isPremiumActive(user?.isPremium) ? 20 : 10;
  const { data: recommendedRooms = [] } = useRecommendedRooms(Number(gameId), recommendationLimit);
  const { data: myRooms = [] } = useQuery({
    queryKey: ['myRooms'],
    queryFn: roomService.getMyRooms,
    enabled: !!user,
  });
  const myJoinedRoomPublicIds = useMemo(
    () => new Set(myRooms.map((room) => room.publicId)),
    [myRooms]
  );
  const excludedRecommendedRoomIds = useMemo(
    () => getExcludedRecommendedRooms(Number(gameId)),
    [gameId, searchParams]
  );

  const filteredRooms = useMemo(() => {
    if (!rooms) return [];

    const selectedTierValues = selectedTiers
      .map(id => tierOptions.find(o => o.id === id)?.optionKey)
      .filter(Boolean) as string[];

    const selectedPositionValues = selectedPositions
      .map(id => positionOptions.find(o => o.id === id)?.optionKey)
      .filter(Boolean) as string[];

    return rooms.filter(room => {
      if (room.status !== 'OPEN' && room.status !== 'FULL') return false;

      const filters = room.filters ?? {};

      if (selectedTierValues.length > 0) {
        const roomTierValue = filters['tier'] ?? filters['TIER'];
        if (!roomTierValue || !selectedTierValues.includes(roomTierValue)) return false;
      }

      if (selectedPositionValues.length > 0) {
        const roomPositionValue = filters['position'] ?? filters['POSITION'];
        if (!roomPositionValue || !selectedPositionValues.includes(roomPositionValue)) return false;
      }

      return true;
    });
  }, [rooms, selectedTiers, selectedPositions, tierOptions, positionOptions]);

  const doJoin = async (
    room: Room,
    password?: string,
    inviteCode?: string,
    options?: { markAsRecommended?: boolean }
  ) => {
    setIsJoining(true);
    try {
      const result = await roomService.joinRoom(room.publicId, password, inviteCode);
      setShowPasswordModal(false);
      setJoiningRoom(null);
      setJoiningInviteCode(undefined);
      if (options?.markAsRecommended) {
        markRecommendedRoomActive(room.publicId, Number(gameId));
      }
      navigate(`/chat/${result.chatRoomId}`);
    } catch (err: unknown) {
      const errorCode = extractErrorCode(err);
      if (errorCode === 'ROOM006' || errorCode === 'ROOM012') {
        // Invalid password or invite code — keep modal open
        if (errorCode === 'ROOM012') {
          toast.error('초대 코드가 유효하지 않거나 만료되었습니다');
          if (inviteCode) {
            setInviteEntryState((prev) => ({
              inviteCode,
              roomPublicId: room.publicId,
              targetRoom: prev?.targetRoom ?? room,
              status: 'expired',
            }));
            setShowPasswordModal(false);
            setJoiningRoom(null);
            setJoiningInviteCode(undefined);
          }
        } else {
          toast.error('비밀번호가 올바르지 않습니다');
        }
      } else if (errorCode === 'ROOM001') {
        toast.error('방을 찾을 수 없습니다');
        if (inviteCode) {
          setInviteEntryState({
            inviteCode,
            roomPublicId: room.publicId,
            targetRoom: room,
            status: 'missing',
          });
        }
        setShowPasswordModal(false);
        setJoiningRoom(null);
        setJoiningInviteCode(undefined);
        queryClient.invalidateQueries({ queryKey: roomKeys.list(Number(gameId)) });
        navigate(`/games/${gameId}/rooms`);
      } else if (errorCode === 'ROOM002') {
        toast.error('방이 꽉 찼습니다');
        setShowPasswordModal(false);
        setJoiningRoom(null);
        setJoiningInviteCode(undefined);
      } else if (errorCode === 'ROOM010') {
        toast.error('이미 게임이 시작된 방입니다');
        setShowPasswordModal(false);
        setJoiningRoom(null);
        setJoiningInviteCode(undefined);
      } else if (errorCode === 'ROOM008') {
        toast.error('이미 다른 대기방에 참가 중입니다');
        setShowPasswordModal(false);
        setJoiningRoom(null);
        setJoiningInviteCode(undefined);
      } else {
        toast.error('방 입장에 실패했습니다');
        setShowPasswordModal(false);
        setJoiningRoom(null);
        setJoiningInviteCode(undefined);
      }
    } finally {
      setIsJoining(false);
    }
  };

  const getNextRecommendedRoom = (extraExcludedRoomIds?: Set<string>) => {
    const excludedRoomIds = extraExcludedRoomIds ?? excludedRecommendedRoomIds;
    for (const recommendation of recommendedRooms) {
      const room = recommendation.room;
      if (!room) continue;
      if (room.status !== 'OPEN') continue;
      if (room.isPrivate) continue;
      if (myJoinedRoomPublicIds.has(room.publicId)) continue;
      if (excludedRoomIds.has(room.publicId)) continue;
      return room;
    }
    return null;
  };

  const handleRecommendedJoin = async (
    source: 'manual' | 'auto',
    extraExcludedRoomIds?: Set<string>
  ) => {
    const nextRecommendedRoom = getNextRecommendedRoom(extraExcludedRoomIds);
    if (!nextRecommendedRoom) {
      toast.info(source === 'auto'
        ? '더 이상 자동으로 추천할 방이 없습니다'
        : '지금 바로 입장 가능한 추천 방이 없습니다');
      return;
    }

    setJoiningRoom(nextRecommendedRoom);
    await doJoin(nextRecommendedRoom, undefined, undefined, { markAsRecommended: true });
  };

  const handleRoomClick = async (roomPublicId: string) => {
    const room = filteredRooms.find(r => r.publicId === roomPublicId);
    if (!room) return;

    // 이미 참여 중인 방이면 join 없이 채팅방으로 바로 이동
    const alreadyIn = myRooms.find(r => r.publicId === roomPublicId);
    if (alreadyIn) {
      try {
        const chatPublicId = await chatService.getChatRoomByGameRoom(alreadyIn.id);
        navigate(`/chat/${chatPublicId}`);
        return;
      } catch {
        // 조회 실패 시 일반 입장 플로우로 진행
      }
    }

    if (room.isPrivate) {
      setJoiningRoom(room);
      setJoiningInviteCode(undefined);
      setShowPasswordModal(true);
    } else {
      setJoiningRoom(room);
      doJoin(room);
    }
  };

  const handlePasswordConfirm = (password?: string) => {
    if (!joiningRoom) return;
    doJoin(joiningRoom, password, joiningInviteCode);
  };

  useEffect(() => {
    const shouldOpenCreateModal = searchParams.get('create') === 'true';
    if (shouldOpenCreateModal && !createHandledRef.current) {
      setShowCreateModal(true);
      createHandledRef.current = true;
    }
    if (!shouldOpenCreateModal) {
      createHandledRef.current = false;
    }
  }, [searchParams]);

  useEffect(() => {
    const shouldAutoRecommend = searchParams.get('recommend') === 'true';
    const excludedRoomPublicId = searchParams.get('exclude')?.trim();
    if (!shouldAutoRecommend || roomsLoading) {
      return;
    }

    const recommendationKey = `${searchParams.toString()}::${rooms?.length ?? 0}`;
    if (recommendationHandledRef.current === recommendationKey) {
      return;
    }
    recommendationHandledRef.current = recommendationKey;

    const nextExcludedRoomIds = new Set(excludedRecommendedRoomIds);
    if (excludedRoomPublicId) {
      addExcludedRecommendedRoom(Number(gameId), excludedRoomPublicId);
      nextExcludedRoomIds.add(excludedRoomPublicId);
    }

    handleRecommendedJoin('auto', nextExcludedRoomIds).finally(() => {
      const nextParams = new URLSearchParams(searchParams);
      nextParams.delete('recommend');
      nextParams.delete('exclude');
      setSearchParams(nextParams, { replace: true });
    });
  }, [gameId, rooms, roomsLoading, searchParams, setSearchParams]);

  useEffect(() => {
    const inviteCode = searchParams.get('invite')?.trim();
    const roomPublicId = searchParams.get('room')?.trim();
    if (!inviteCode) {
      setInviteEntryState(null);
    }
    if (!inviteCode || roomsLoading) {
      return;
    }

    const inviteKey = `${roomPublicId ?? 'none'}:${inviteCode}`;
    if (inviteHandledRef.current === inviteKey) {
      return;
    }
    inviteHandledRef.current = inviteKey;

    const openInviteModal = async () => {
      let targetRoom: Room | undefined;
      if (roomPublicId) {
        targetRoom = rooms?.find(r => r.publicId === roomPublicId);
        if (!targetRoom) {
          try {
            targetRoom = await roomService.getRoom(roomPublicId);
          } catch {
            targetRoom = undefined;
          }
        }
      } else {
        targetRoom = rooms?.find(r => r.inviteCode === inviteCode);
      }

      if (!targetRoom) {
        setInviteEntryState({
          inviteCode,
          roomPublicId,
          status: 'missing',
        });
        toast.error('초대 대상 방을 찾을 수 없습니다');
        return;
      }

      setInviteEntryState({
        inviteCode,
        roomPublicId: targetRoom.publicId,
        targetRoom,
        status: 'ready',
      });

      setJoiningRoom(targetRoom);
      setJoiningInviteCode(inviteCode);
      setShowPasswordModal(true);
    };

    openInviteModal();
  }, [rooms, roomsLoading, searchParams, setSearchParams, toast]);

  const handleRegenerateInviteCode = async (roomPublicId: string) => {
    try {
      const updatedRoom = await roomService.regenerateInviteCode(roomPublicId);
      if (!updatedRoom.inviteCode) {
        toast.error('초대코드 생성에 실패했습니다');
        return;
      }

      const inviteUrl = `${window.location.origin}/games/${gameId}/rooms?room=${updatedRoom.publicId}&invite=${encodeURIComponent(updatedRoom.inviteCode)}`;
      await copyToClipboard(inviteUrl);
      toast.success('초대 링크가 클립보드에 복사되었습니다');
      queryClient.invalidateQueries({ queryKey: roomKeys.list(Number(gameId)) });
    } catch {
      toast.error('초대코드 재발급에 실패했습니다');
    }
  };

  const shouldShowRegenerateInviteButton = (room: Room) =>
    Boolean(room.isPrivate && user?.nickname && room.createdBy === user.nickname);

  const handleClearInviteState = () => {
    setInviteEntryState(null);
    setShowPasswordModal(false);
    setJoiningRoom(null);
    setJoiningInviteCode(undefined);
    setSearchParams({}, { replace: true });
  };

  const handleRequestNewInvite = () => {
    toast.info('초대한 방장에게 새 초대 링크를 요청해주세요.');
  };

  const handleReset = () => {
    setSelectedTiers([]);
    setSelectedPositions([]);
  };

  if (gameLoading) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <LoadingSpinner size="lg" label="게임 정보를 불러오는 중..." />
      </div>
    );
  }

  if (!game) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-white text-xl">게임을 찾을 수 없습니다</div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white">
      {/* Header */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-6">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div className="min-w-0">
              <button
                onClick={() => navigate('/')}
                className="flex items-center gap-1.5 text-gray-400 hover:text-white mb-2 transition"
              >
                <ChevronLeft size={18} />
                뒤로가기
              </button>
              <h1 className="text-2xl sm:text-3xl font-bold text-white truncate">{game.name}</h1>
              <p className="text-gray-400 mt-1 text-sm sm:text-base truncate">{game.description}</p>
            </div>
            <div className="ml-3 flex flex-wrap gap-2">
              <button
                className="flex-shrink-0 flex items-center gap-2 px-4 sm:px-6 py-2.5 sm:py-3 bg-cyan-500 hover:bg-cyan-600 text-white font-bold rounded-lg transition text-sm sm:text-base"
                onClick={() => handleRecommendedJoin('manual')}
              >
                <Sparkles size={18} />
                추천 방 바로 입장
              </button>
              <button
                className="flex-shrink-0 flex items-center gap-2 px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition text-sm sm:text-base"
                onClick={() => setShowCreateModal(true)}
              >
                <Plus size={18} />
                방 만들기
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-8">
        {/* Leaderboard banner — 방 목록 상단 */}
        {showAds && (
          <div className="mb-8 flex justify-center">
            <AdBanner type="leaderboard" adData={ads[0] ?? null} />
          </div>
        )}

        <RoomFilter
          tierOptions={tierOptions}
          positionOptions={positionOptions}
          selectedTiers={selectedTiers}
          selectedPositions={selectedPositions}
          onTierChange={setSelectedTiers}
          onPositionChange={setSelectedPositions}
          onReset={handleReset}
        />

        {roomsError && (
          <div className="bg-red-500/20 border border-red-500 rounded-lg p-4 mb-6">
            <p className="text-red-400">방 목록을 불러오는 중 오류가 발생했습니다.</p>
          </div>
        )}

        {inviteEntryState && (
          <InviteEntryBanner
            state={inviteEntryState}
            onClear={handleClearInviteState}
            onRequestNewInvite={handleRequestNewInvite}
          />
        )}

        <RoomGrid
          rooms={filteredRooms}
          isLoading={roomsLoading || isJoining}
          onRoomClick={handleRoomClick}
          shouldShowRegenerateInviteButton={shouldShowRegenerateInviteButton}
          onRegenerateInviteCode={handleRegenerateInviteCode}
          isMyRoom={(room) => myJoinedRoomPublicIds.has(room.publicId)}
        />

        {/* Inline banner — 방 5개 이상일 때 목록 아래 */}
        {showAds && filteredRooms.length >= 5 && (
          <div className="mt-8 flex justify-center">
            <AdBanner type="inline" adData={ads[1] ?? null} />
          </div>
        )}

        {!roomsLoading && rooms && rooms.length > 0 && filteredRooms.length === 0 && (
          <div className="text-center py-12">
            <p className="text-gray-400 mb-4">조건에 맞는 방이 없습니다</p>
            <button onClick={handleReset} className="text-purple-400 hover:text-purple-300 transition">
              필터 초기화
            </button>
          </div>
        )}
      </div>

      {showCreateModal && game && (
        <CreateRoomModal
          gameId={Number(gameId)}
          gameName={game.name}
          onClose={() => setShowCreateModal(false)}
          onSuccess={() => {
            queryClient.invalidateQueries({ queryKey: roomKeys.list(Number(gameId)) });
            setShowCreateModal(false);
          }}
        />
      )}

      {showPasswordModal && joiningRoom && (
        <PasswordModal
          onConfirm={handlePasswordConfirm}
          inviteCode={joiningInviteCode}
          onCancel={() => {
            setShowPasswordModal(false);
            setJoiningRoom(null);
            setJoiningInviteCode(undefined);
          }}
        />
      )}
    </div>
  );
}

type InviteEntryStatus = 'ready' | 'expired' | 'missing';

interface InviteEntryState {
  inviteCode: string;
  roomPublicId?: string;
  targetRoom?: Room;
  status: InviteEntryStatus;
}

function InviteEntryBanner({
  state,
  onClear,
  onRequestNewInvite,
}: {
  state: InviteEntryState;
  onClear: () => void;
  onRequestNewInvite: () => void;
}) {
  const isProblemState = state.status === 'expired' || state.status === 'missing';

  return (
    <div className={`mb-6 rounded-xl border p-4 ${isProblemState ? 'border-red-500/40 bg-red-500/10' : 'border-blue-500/40 bg-blue-500/10'}`}>
      <div className="flex flex-wrap items-start justify-between gap-3">
        <div>
          <p className={`text-sm font-semibold ${isProblemState ? 'text-red-200' : 'text-blue-200'}`}>
            {state.status === 'ready'
              ? '초대 링크로 입장 중입니다'
              : '초대 링크가 만료되었거나 유효하지 않습니다'}
          </p>
          <p className="mt-1 text-xs text-gray-300">
            {state.status === 'ready'
              ? '입장 버튼을 누르면 해당 방으로 바로 연결됩니다.'
              : '방장이 새 초대 링크를 발급한 뒤 다시 시도해주세요.'}
          </p>
        </div>
        <div className="flex gap-2">
          <button
            type="button"
            onClick={onClear}
            className="px-3 py-1.5 rounded border border-gray-500/60 text-xs font-semibold text-gray-200 hover:bg-gray-700/40 transition"
          >
            방 목록으로 돌아가기
          </button>
          {isProblemState && (
            <button
              type="button"
              onClick={onRequestNewInvite}
              className="px-3 py-1.5 rounded bg-red-500/80 text-xs font-semibold text-white hover:bg-red-500 transition"
            >
              새 초대 링크 요청
            </button>
          )}
        </div>
      </div>

      <div className="mt-3 rounded-lg border border-gray-700/70 bg-[#111115] px-3 py-2 text-sm">
        {state.targetRoom ? (
          <>
            <p className="font-semibold text-white">대상 방: {state.targetRoom.title}</p>
            <p className="mt-1 text-xs text-gray-300">
              {state.targetRoom.gameName} · {state.targetRoom.currentParticipants}/{state.targetRoom.maxParticipants}명 · {state.targetRoom.isPrivate ? '비공개' : '공개'}
            </p>
          </>
        ) : (
          <p className="text-xs text-gray-300">
            대상 방 정보를 불러오지 못했습니다.
            {state.roomPublicId ? ` (room: ${state.roomPublicId})` : ''}
          </p>
        )}
      </div>
    </div>
  );
}

async function copyToClipboard(text: string): Promise<void> {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(text);
    return;
  }

  const textarea = document.createElement('textarea');
  textarea.value = text;
  textarea.style.position = 'fixed';
  textarea.style.left = '-9999px';
  document.body.appendChild(textarea);
  textarea.select();
  document.execCommand('copy');
  document.body.removeChild(textarea);
}

function extractErrorCode(err: unknown): string | null {
  if (
    err &&
    typeof err === 'object' &&
    'response' in err
  ) {
    const response = (err as { response?: { data?: { code?: string } } }).response;
    return response?.data?.code ?? null;
  }
  return null;
}

function getExcludedRecommendedRooms(gameId: number) {
  if (!gameId) return new Set<string>();
  try {
    const raw = localStorage.getItem(RECOMMENDATION_EXCLUSION_KEY);
    if (!raw) return new Set<string>();
    const parsed = JSON.parse(raw) as Record<string, string[]>;
    return new Set(parsed[String(gameId)] ?? []);
  } catch {
    return new Set<string>();
  }
}

function addExcludedRecommendedRoom(gameId: number, roomPublicId: string) {
  if (!gameId || !roomPublicId) return;
  try {
    const raw = localStorage.getItem(RECOMMENDATION_EXCLUSION_KEY);
    const parsed = raw ? JSON.parse(raw) as Record<string, string[]> : {};
    const gameKey = String(gameId);
    const nextValues = new Set(parsed[gameKey] ?? []);
    nextValues.add(roomPublicId);
    parsed[gameKey] = [...nextValues];
    localStorage.setItem(RECOMMENDATION_EXCLUSION_KEY, JSON.stringify(parsed));
  } catch {
    // Ignore localStorage failures for non-critical recommendation history.
  }
}

function markRecommendedRoomActive(roomPublicId: string, gameId: number) {
  if (!roomPublicId || !gameId) return;
  try {
    const raw = localStorage.getItem(RECOMMENDATION_ACTIVE_KEY);
    const parsed = raw ? JSON.parse(raw) as Record<string, { gameId: number }> : {};
    parsed[roomPublicId] = { gameId };
    localStorage.setItem(RECOMMENDATION_ACTIVE_KEY, JSON.stringify(parsed));
  } catch {
    // Ignore localStorage failures for non-critical recommendation tracking.
  }
}
