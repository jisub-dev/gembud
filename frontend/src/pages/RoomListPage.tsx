import { useState, useMemo, useEffect, useRef } from 'react';
import { useParams, useNavigate, useSearchParams } from 'react-router-dom';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Plus } from 'lucide-react';
import { useRooms } from '@/hooks/queries/useRooms';
import { useGameOptions } from '@/hooks/queries/useGames';
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

  // Auto-join state
  const [joiningRoom, setJoiningRoom] = useState<Room | null>(null);
  const [joiningInviteCode, setJoiningInviteCode] = useState<string | undefined>(undefined);
  const [isJoining, setIsJoining] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const inviteHandledRef = useRef<string | null>(null);

  const { data: rooms, isLoading: roomsLoading, error: roomsError } = useRooms(Number(gameId));
  const { game, tierOptions, positionOptions, isLoading: gameLoading } = useGameOptions(Number(gameId));
  const { data: myRooms = [] } = useQuery({
    queryKey: ['myRooms'],
    queryFn: roomService.getMyRooms,
    enabled: !!user,
  });
  const myJoinedRoomPublicIds = useMemo(
    () => new Set(myRooms.map((room) => room.publicId)),
    [myRooms]
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

  const doJoin = async (room: Room, password?: string, inviteCode?: string) => {
    setIsJoining(true);
    try {
      const result = await roomService.joinRoom(room.publicId, password, inviteCode);
      setShowPasswordModal(false);
      setJoiningRoom(null);
      setJoiningInviteCode(undefined);
      navigate(`/chat/${result.room.publicId}`);
    } catch (err: unknown) {
      const errorCode = extractErrorCode(err);
      if (errorCode === 'ROOM006' || errorCode === 'ROOM012') {
        // Invalid password or invite code — keep modal open
        if (errorCode === 'ROOM012') {
          toast.error('초대 코드가 유효하지 않거나 만료되었습니다');
        } else {
          toast.error('비밀번호가 올바르지 않습니다');
        }
      } else if (errorCode === 'ROOM001') {
        toast.error('방을 찾을 수 없습니다');
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
    const inviteCode = searchParams.get('invite')?.trim();
    const roomPublicId = searchParams.get('room')?.trim();
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
        toast.error('목록에 없는 방입니다');
        setSearchParams({}, { replace: true });
        return;
      }

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
            <button
              className="ml-3 flex-shrink-0 flex items-center gap-2 px-4 sm:px-6 py-2.5 sm:py-3 bg-purple-500 hover:bg-purple-600 text-white font-bold rounded-lg transition text-sm sm:text-base"
              onClick={() => setShowCreateModal(true)}
            >
              <Plus size={18} />
              방 만들기
            </button>
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
