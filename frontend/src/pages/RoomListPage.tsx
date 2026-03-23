import { useState, useMemo, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Plus, Sparkles } from 'lucide-react';
import { useMyActiveRoom, useMyRooms, useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
import { useRecommendedRooms } from '@/hooks/queries/useMatching';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { useRoomRecommendations } from '@/hooks/useRoomRecommendations';
import { useRoomJoinFlow } from '@/hooks/useRoomJoinFlow';
import { RoomGrid } from '@/components/room/RoomGrid';
import { RoomFilter } from '@/components/room/RoomFilter';
import { CreateRoomModal } from '@/components/room/CreateRoomModal';
import { PasswordModal } from '@/components/room/PasswordModal';
import LoadingSpinner from '@/components/common/LoadingSpinner';
import AdBanner from '@/components/common/AdBanner';
import { useAds } from '@/hooks/queries/useAds';
import { useRoomInviteEntry, type InviteEntryState } from '@/hooks/useRoomInviteEntry';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import { roomService } from '@/services/roomService';
import type { Room } from '@/types/room';
import { isPremiumActive } from '@/config/features';

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
  const createHandledRef = useRef(false);

  const { data: rooms, isLoading: roomsLoading, error: roomsError } = useRooms(Number(gameId));
  const { game, tierOptions, positionOptions, isLoading: gameLoading } = useGameOptions(Number(gameId));
  const recommendationLimit = isPremiumActive(user?.isPremium) ? 20 : 10;
  const { data: recommendedRooms = [] } = useRecommendedRooms(Number(gameId), recommendationLimit);
  const { data: myRooms = [] } = useMyRooms({
    enabled: !!user,
  });
  const { data: activeRoom = null } = useMyActiveRoom({
    enabled: !!user,
  });
  const myJoinedRoomPublicIds = useMemo(
    () => new Set(myRooms.map((room) => room.publicId)),
    [myRooms]
  );
  const {
    inviteEntryState,
    inviteModalRequest,
    consumeInviteModalRequest,
    clearInviteEntry,
    markInviteExpired,
    markInviteMissing,
  } = useRoomInviteEntry({
    rooms,
    roomsLoading,
    searchParams,
    setSearchParams,
    onMissingInvite: () => toast.error('초대 대상 방을 찾을 수 없습니다'),
  });
  const {
    confirmPasswordJoin,
    isJoining,
    joinRoom: attemptJoinRoom,
    joiningInviteCode,
    joiningRoom,
    openInviteEntry,
    requestRoomEntry,
    resetJoinState,
    showPasswordModal,
  } = useRoomJoinFlow({
    activeRoom,
    gameId: Number(gameId),
    myRooms,
    navigate,
    onInviteExpired: markInviteExpired,
    onInviteMissing: markInviteMissing,
    queryClient,
    toast,
  });
  const { handleRecommendedJoin } = useRoomRecommendations({
    gameId: Number(gameId),
    rooms,
    roomsLoading,
    recommendedRooms,
    myJoinedRoomPublicIds,
    searchParams,
    setSearchParams,
    onJoinRoom: async (room) => {
      await attemptJoinRoom(room, undefined, undefined, { markAsRecommended: true });
    },
    onNoRoom: (source) => {
      toast.info(
        source === 'auto'
          ? '더 이상 자동으로 추천할 방이 없습니다'
          : '지금 바로 입장 가능한 추천 방이 없습니다',
      );
    },
  });

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

  const handleRoomClick = async (roomPublicId: string) => {
    const room = filteredRooms.find(r => r.publicId === roomPublicId);
    if (!room) return;
    await requestRoomEntry(room);
  };

  useEffect(() => {
    if (!inviteModalRequest) {
      return;
    }

    openInviteEntry(inviteModalRequest.room, inviteModalRequest.inviteCode);
    consumeInviteModalRequest();
  }, [consumeInviteModalRequest, inviteModalRequest, openInviteEntry]);

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
    resetJoinState();
    clearInviteEntry();
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
          onConfirm={confirmPasswordJoin}
          inviteCode={joiningInviteCode}
          onCancel={resetJoinState}
        />
      )}
    </div>
  );
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
