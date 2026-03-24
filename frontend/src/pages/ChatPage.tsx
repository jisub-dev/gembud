import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, LogOut } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ChatPanel } from '@/components/chat/ChatPanel';
import { RoomParticipants } from '@/components/room/RoomParticipants';
import { EvaluateModal } from '@/components/room/EvaluateModal';
import { roomService } from '@/services/roomService';
import evaluationService from '@/services/evaluationService';
import { chatKeys, useMyChatRooms, useMyRoomChatRooms } from '@/hooks/queries/useChatQueries';
import { addExcludedRecommendedRoom, consumeRecommendedRoomActive } from '@/hooks/useRoomRecommendations';
import {
  selectChatRoomByPublicId,
  selectRoomChatByPublicId,
} from '@/hooks/queries/roomSelectors';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { syncClientAfterLeavingRoom, useMyActiveRoom } from '@/hooks/queries/useRooms';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import { getInviteExpiryInfo } from '@/utils/inviteExpiry';

const STATUS_LABELS: Record<string, string> = {
  OPEN: '모집중',
  FULL: '인원 가득',
  IN_PROGRESS: '게임 중',
  CLOSED: '종료',
};

const STATUS_COLORS: Record<string, string> = {
  OPEN: 'text-green-400',
  FULL: 'text-yellow-400',
  IN_PROGRESS: 'text-cyan-400',
  CLOSED: 'text-gray-400',
};

export default function ChatPage() {
  const { roomId: chatPublicId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [activeTab, setActiveTab] = useState<'info' | 'chat'>('info');
  const [isStartingRoom, setIsStartingRoom] = useState(false);
  const [isResettingRoom, setIsResettingRoom] = useState(false);
  const [isEvaluateModalOpen, setIsEvaluateModalOpen] = useState(false);
  const [inviteLink, setInviteLink] = useState('');
  const [inviteExpiresAt, setInviteExpiresAt] = useState<string | undefined>(undefined);
  const [isRegeneratingInvite, setIsRegeneratingInvite] = useState(false);
  const [inviteNowMs, setInviteNowMs] = useState(Date.now());

  const queryClient = useQueryClient();
  const toast = useToast();
  const [isLeaving, setIsLeaving] = useState(false);
  const hasChatPublicId = !!chatPublicId;

  const {
    data: myChatRooms = [],
    isLoading: isMyChatRoomsLoading,
  } = useMyChatRooms({
    enabled: hasChatPublicId,
  });

  const {
    data: myRoomChatRooms = [],
    isLoading: isMyRoomChatRoomsLoading,
  } = useMyRoomChatRooms({
    enabled: hasChatPublicId,
  });

  const {
    data: myActiveRoom = null,
    refetch: refetchMyActiveRoom,
  } = useMyActiveRoom({
    enabled: hasChatPublicId,
  });

  const roomChatInfo = selectRoomChatByPublicId(myRoomChatRooms, chatPublicId);
  const chatRoomInfo = selectChatRoomByPublicId(myChatRooms, chatPublicId) ?? roomChatInfo;
  const chatRoomId = chatRoomInfo?.id;
  const relatedRoom = roomChatInfo ? myActiveRoom : null;
  const isRoomChat = chatRoomInfo?.type === 'ROOM_CHAT' || roomChatInfo?.type === 'ROOM_CHAT';
  const isHost = useMemo(() => {
    if (!relatedRoom || !user) return false;
    return relatedRoom.participants?.some((participant) => participant.userId === user.id && participant.isHost) ?? false;
  }, [relatedRoom, user]);
  const evaluatableParticipants = useMemo(() => {
    if (!relatedRoom?.participants || !user) return [];
    return relatedRoom.participants.filter((participant) => participant.userId !== user.id);
  }, [relatedRoom, user]);
  const canEvaluateRoom = useMemo(() => {
    if (!relatedRoom) return false;
    return relatedRoom.status === 'IN_PROGRESS' || relatedRoom.status === 'CLOSED';
  }, [relatedRoom]);

  const {
    data: evaluatableUserIds = [],
    isLoading: isEvaluatableLoading,
    refetch: refetchEvaluatable,
  } = useQuery({
    queryKey: ['roomEvaluatableUsers', relatedRoom?.id, user?.id],
    queryFn: () => evaluationService.getEvaluatable(relatedRoom!.id),
    enabled: !!relatedRoom?.id && !!user?.id && canEvaluateRoom,
    staleTime: 15000,
  });

  const isRoomClosed = relatedRoom?.status === 'CLOSED';

  const hasEvaluatableParticipants = evaluatableParticipants.some((participant) =>
    evaluatableUserIds.includes(participant.userId),
  );

  const isEvaluateButtonDisabled =
    !canEvaluateRoom ||
    isEvaluatableLoading ||
    evaluatableParticipants.length === 0 ||
    !hasEvaluatableParticipants;
  const inviteExpiryInfo = useMemo(
    () => getInviteExpiryInfo(inviteExpiresAt, inviteNowMs),
    [inviteExpiresAt, inviteNowMs],
  );

  useEffect(() => {
    if (window.innerWidth < 1024) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }, [activeTab]);

  useEffect(() => {
    if (!isHost || !relatedRoom?.isPrivate) {
      setInviteLink('');
      setInviteExpiresAt(undefined);
      return;
    }

    setInviteExpiresAt(relatedRoom.inviteCodeExpiresAt);
    if (relatedRoom.inviteCode) {
      try {
        setInviteLink(roomService.buildInviteLink(relatedRoom));
      } catch {
        setInviteLink('');
      }
    } else {
      setInviteLink('');
    }
  }, [isHost, relatedRoom]);

  useEffect(() => {
    if (!inviteExpiresAt) return;
    setInviteNowMs(Date.now());
    const timerId = window.setInterval(() => {
      setInviteNowMs(Date.now());
    }, 30000);
    return () => {
      window.clearInterval(timerId);
    };
  }, [inviteExpiresAt]);

  const handleKick = async (userId: number, nickname: string) => {
    if (!relatedRoom) return;
    if (!window.confirm(`${nickname}님을 강퇴하시겠습니까?`)) return;
    try {
      await roomService.kickParticipant(relatedRoom.id, userId);
      await refetchMyActiveRoom();
      await queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      await queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() });
    } catch {
      window.alert('강퇴 처리에 실패했습니다.');
    }
  };

  const handleTransferHost = async (userId: number, nickname: string) => {
    if (!relatedRoom) return;
    if (!window.confirm(`${nickname}님에게 방장을 넘기시겠습니까?`)) return;
    try {
      await roomService.transferHost(relatedRoom.id, userId);
      await refetchMyActiveRoom();
      await queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      await queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() });
    } catch {
      window.alert('방장 이전에 실패했습니다.');
    }
  };

  const handleStartRoom = async () => {
    if (!relatedRoom) return;
    setIsStartingRoom(true);
    try {
      await roomService.startRoom(relatedRoom.id);
      await refetchMyActiveRoom();
      await queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      await queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() });
    } catch {
      window.alert('게임 시작에 실패했습니다.');
    } finally {
      setIsStartingRoom(false);
    }
  };

  const handleResetRoom = async () => {
    if (!relatedRoom) return;
    setIsResettingRoom(true);
    try {
      await roomService.resetRoom(relatedRoom.publicId);
      await refetchMyActiveRoom();
      await queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      await queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() });
      toast.success('방 상태를 대기중으로 변경했습니다.');
    } catch {
      toast.error('대기중으로 변경에 실패했습니다.');
    } finally {
      setIsResettingRoom(false);
    }
  };

  const handleCopyInviteLink = async () => {
    if (!inviteLink) {
      toast.error('현재 사용할 수 있는 초대 링크가 없습니다');
      return;
    }
    try {
      await copyToClipboard(inviteLink);
      toast.success('초대 링크가 복사되었습니다');
    } catch {
      toast.error('초대 링크 복사에 실패했습니다');
    }
  };

  const handleRegenerateInviteLink = async () => {
    if (!relatedRoom?.publicId) return;
    if (!window.confirm('재발급하면 이전 링크가 무효화됩니다')) return;

    setIsRegeneratingInvite(true);
    try {
      const updatedRoom = await roomService.regenerateInviteCode(relatedRoom.publicId);
      if (!updatedRoom.inviteCode) {
        toast.error('초대 링크 재발급에 실패했습니다');
        return;
      }

      setInviteLink(roomService.buildInviteLink(updatedRoom));
      setInviteExpiresAt(updatedRoom.inviteCodeExpiresAt);
      toast.success('초대 링크를 재발급했습니다');
    } catch {
      toast.error('초대 링크 재발급에 실패했습니다');
    } finally {
      setIsRegeneratingInvite(false);
    }
  };

  const handleLeave = async () => {
    if (!relatedRoom) return;
    setIsLeaving(true);
    try {
      await roomService.leaveRoom(relatedRoom.id);
      await syncClientAfterLeavingRoom(queryClient, relatedRoom.id, {
        gameId: relatedRoom.gameId,
        roomPublicId: relatedRoom.publicId,
      });
      toast.success('대기방을 나갔습니다');
      if (consumeRecommendedRoomActive(relatedRoom.publicId, relatedRoom.gameId)) {
        addExcludedRecommendedRoom(relatedRoom.gameId, relatedRoom.publicId);
        navigate(`/games/${relatedRoom.gameId}/rooms?recommend=true&exclude=${encodeURIComponent(relatedRoom.publicId)}`);
      } else {
        navigate(`/games/${relatedRoom.gameId}/rooms`);
      }
    } catch {
      toast.error('나가기에 실패했습니다');
    } finally {
      setIsLeaving(false);
    }
  };

  if (!hasChatPublicId) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center text-white">
        채팅방을 찾을 수 없습니다
      </div>
    );
  }

  if (isMyChatRoomsLoading || isMyRoomChatRoomsLoading) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center text-white">
        채팅방 정보를 불러오는 중입니다...
      </div>
    );
  }

  if (!chatRoomInfo || !chatRoomId) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center text-white">
        접근할 수 없는 채팅방입니다
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white flex flex-col">
      {/* Header */}
      <div className="border-b border-gray-800 flex-shrink-0">
        <div className="container mx-auto px-4 py-4 max-w-4xl flex items-center justify-between">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-1.5 text-gray-400 hover:text-white transition"
          >
            <ChevronLeft size={18} />
            뒤로가기
          </button>
          {relatedRoom && (
            <button
              onClick={handleLeave}
              disabled={isLeaving}
              className="flex items-center gap-1.5 px-3 py-1.5 text-sm text-red-400 hover:text-white hover:bg-red-500/20 border border-red-500/40 rounded transition disabled:opacity-50"
            >
              <LogOut size={15} />
              {isLeaving ? '나가는 중...' : '대기방 나가기'}
            </button>
          )}
        </div>
      </div>

      {!isRoomChat && (
        <div className="flex-1 container mx-auto px-4 py-4 max-w-4xl flex flex-col min-h-0">
          <ChatPanel
            chatRoomId={chatRoomId}
            chatPublicId={chatPublicId!}
            canChat={true}
            className="flex-1"
          />
        </div>
      )}

      {isRoomChat && (
        <>
          {/* 모바일 탭 */}
          <div className="lg:hidden border-b border-gray-800 px-4">
            <div className="flex gap-2 py-2">
              <button
                type="button"
                onClick={() => setActiveTab('info')}
                className={`px-3 py-1.5 rounded text-sm ${activeTab === 'info' ? 'bg-purple-500 text-white' : 'bg-gray-800 text-gray-300'}`}
              >
                방 정보
              </button>
              <button
                type="button"
                onClick={() => setActiveTab('chat')}
                className={`px-3 py-1.5 rounded text-sm ${activeTab === 'chat' ? 'bg-purple-500 text-white' : 'bg-gray-800 text-gray-300'}`}
              >
                채팅
              </button>
            </div>
          </div>

          <div className="flex-1 container mx-auto px-4 py-4 max-w-6xl min-h-0">
            <div className="h-full grid grid-cols-1 lg:grid-cols-5 gap-4">
              <section className={`${activeTab === 'info' ? 'block' : 'hidden'} lg:block lg:col-span-3 bg-[#18181b] border border-gray-800 rounded-lg p-4 overflow-y-auto`}>
                {relatedRoom ? (
                  <div className="space-y-4">
                    {isRoomClosed && (
                      <div className="rounded-md border border-amber-500/40 bg-amber-500/10 px-3 py-2 text-sm text-amber-200">
                        종료된 방입니다. 채팅은 읽기 전용으로 전환되었습니다.
                      </div>
                    )}
                    <div className="flex items-start justify-between gap-2">
                      <div className="min-w-0">
                        <h2 className="text-lg font-bold text-white truncate">{relatedRoom.title}</h2>
                        <p className="text-sm text-gray-400">{relatedRoom.gameName}</p>
                      </div>
                      <span className={`text-xs font-semibold ${STATUS_COLORS[relatedRoom.status] ?? 'text-gray-400'}`}>
                        {STATUS_LABELS[relatedRoom.status] ?? relatedRoom.status}
                      </span>
                    </div>

                    <RoomParticipants
                      participants={relatedRoom.participants}
                      maxParticipants={relatedRoom.maxParticipants}
                      currentUserId={user?.id}
                      isCurrentUserHost={isHost}
                      onKick={handleKick}
                      onTransferHost={handleTransferHost}
                    />

                    {isHost && relatedRoom.isPrivate && (
                      <div className="rounded-lg border border-blue-500/30 bg-blue-500/5 p-3 space-y-3">
                        <p className="text-sm font-semibold text-blue-200">초대 링크 관리</p>
                        {inviteExpiryInfo.isExpiringSoon && !inviteExpiryInfo.isExpired && (
                          <div className="rounded border border-amber-400/50 bg-amber-500/10 px-3 py-2 text-xs text-amber-200">
                            초대 링크 만료 임박: {inviteExpiryInfo.remainingLabel} 남았습니다.
                          </div>
                        )}
                        {inviteExpiryInfo.isExpired && (
                          <div className="rounded border border-red-500/50 bg-red-500/10 px-3 py-2 text-xs text-red-200">
                            초대 링크가 만료되었습니다. 재발급 후 공유해주세요.
                          </div>
                        )}
                        <div className="flex gap-2">
                          <input
                            readOnly
                            value={inviteLink || '초대 링크가 없습니다. 재발급해주세요.'}
                            className="flex-1 min-w-0 rounded border border-gray-700 bg-[#0e0e10] px-3 py-2 text-xs text-gray-200"
                          />
                          <button
                            type="button"
                            onClick={handleCopyInviteLink}
                            disabled={!inviteLink}
                            className="px-3 py-2 rounded bg-blue-500 hover:bg-blue-600 disabled:bg-gray-700 disabled:cursor-not-allowed text-sm font-semibold transition"
                          >
                            복사
                          </button>
                        </div>
                        <div className="flex items-center justify-between gap-2">
                          <span className={`text-xs ${inviteExpiryInfo.isExpired ? 'text-red-300' : inviteExpiryInfo.isExpiringSoon ? 'text-amber-300' : 'text-gray-400'}`}>
                            남은 시간: {inviteExpiryInfo.remainingLabel} · 만료 시각: {inviteExpiryInfo.expiresAtLabel}
                          </span>
                          <button
                            type="button"
                            onClick={handleRegenerateInviteLink}
                            disabled={isRegeneratingInvite}
                            className="px-3 py-1.5 rounded border border-blue-400/40 bg-blue-500/10 hover:bg-blue-500/20 disabled:opacity-60 text-xs font-semibold text-blue-200 transition"
                          >
                            {isRegeneratingInvite ? '재발급 중...' : '초대 링크 재발급'}
                          </button>
                        </div>
                      </div>
                    )}

                    <div className="flex flex-col gap-2">
                      {(relatedRoom.status === 'IN_PROGRESS' || relatedRoom.status === 'CLOSED') && (
                        <button
                          type="button"
                          onClick={() => setIsEvaluateModalOpen(true)}
                          disabled={isEvaluateButtonDisabled}
                          className="w-full py-2 rounded bg-emerald-500 hover:bg-emerald-600 disabled:bg-gray-700 disabled:cursor-not-allowed font-semibold transition"
                        >
                          {isEvaluatableLoading
                            ? '평가 가능 여부 확인 중...'
                            : hasEvaluatableParticipants
                              ? '평가하기'
                              : '이미 평가 완료'}
                        </button>
                      )}

                      {isHost && (
                        <div className="flex gap-2">
                          {relatedRoom.status === 'OPEN' && (
                            <button
                              type="button"
                              onClick={handleStartRoom}
                              disabled={isStartingRoom}
                              className="flex-1 py-2 rounded bg-purple-500 hover:bg-purple-600 disabled:bg-gray-700 disabled:cursor-not-allowed font-semibold transition"
                            >
                              {isStartingRoom ? '시작 중...' : '게임 시작'}
                            </button>
                          )}
                          {relatedRoom.status === 'IN_PROGRESS' && (
                            <button
                              type="button"
                              onClick={handleResetRoom}
                              disabled={isResettingRoom}
                              className="flex-1 py-2 rounded bg-blue-500 hover:bg-blue-600 disabled:bg-gray-700 disabled:cursor-not-allowed font-semibold transition"
                            >
                              {isResettingRoom ? '변경 중...' : '대기중으로 변경'}
                            </button>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm text-gray-400">
                    방 정보를 불러오지 못했습니다
                  </div>
                )}
              </section>

              <section className={`${activeTab === 'chat' ? 'block' : 'hidden'} lg:block lg:col-span-2 min-h-0`}>
                <ChatPanel
                  chatRoomId={chatRoomId}
                  chatPublicId={chatPublicId!}
                  canChat={!isRoomClosed}
                  className="h-full"
                />
              </section>
            </div>
          </div>
        </>
      )}

      {isEvaluateModalOpen && relatedRoom && (
        <EvaluateModal
          roomId={relatedRoom.id}
          participants={evaluatableParticipants.filter((participant) =>
            evaluatableUserIds.includes(participant.userId),
          )}
          onClose={async () => {
            setIsEvaluateModalOpen(false);
            await refetchEvaluatable();
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
