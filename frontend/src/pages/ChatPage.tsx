import { useParams, useNavigate } from 'react-router-dom';
import { useEffect, useMemo, useState } from 'react';
import { ChevronLeft, LogOut } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { ChatPanel } from '@/components/chat/ChatPanel';
import { RoomParticipants } from '@/components/room/RoomParticipants';
import { EvaluateModal } from '@/components/room/EvaluateModal';
import { chatService } from '@/services/chatService';
import { roomService } from '@/services/roomService';
import evaluationService from '@/services/evaluationService';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';

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
  const { roomId: chatRoomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();
  const [activeTab, setActiveTab] = useState<'info' | 'chat'>('info');
  const [isStartingRoom, setIsStartingRoom] = useState(false);
  const [isResettingRoom, setIsResettingRoom] = useState(false);
  const [isClosingRoom, setIsClosingRoom] = useState(false);
  const [isEvaluateModalOpen, setIsEvaluateModalOpen] = useState(false);

  const roomId = Number(chatRoomId);
  const queryClient = useQueryClient();
  const toast = useToast();
  const [isLeaving, setIsLeaving] = useState(false);

  // 이 채팅방이 ROOM_CHAT인지 확인하고 관련 대기방 정보 조회
  const { data: myChatRooms = [] } = useQuery({
    queryKey: ['myChatRooms'],
    queryFn: () => chatService.getMyChatRooms(),
    enabled: !!roomId,
  });

  const chatRoomInfo = myChatRooms.find(c => c.id === roomId);
  const relatedRoomIdFromChatList = chatRoomInfo?.relatedRoomId;
  const isRoomChatByType = chatRoomInfo?.type === 'ROOM_CHAT';

  const { data: myRooms = [], refetch: refetchMyRooms } = useQuery({
    queryKey: ['myRooms'],
    queryFn: roomService.getMyRooms,
    enabled: !!roomId,
    refetchInterval: 10000,
  });

  const { data: inferredRelatedRoomId } = useQuery({
    queryKey: ['chatRoomRelatedRoom', roomId, myRooms.map(r => r.id).join(',')],
    queryFn: async () => {
      for (const room of myRooms) {
        try {
          const mappedChatRoomId = await chatService.getChatRoomByGameRoom(room.id);
          if (mappedChatRoomId === roomId) {
            return room.id;
          }
        } catch {
          // Ignore per-room lookup failure and continue
        }
      }
      return null;
    },
    enabled: !!roomId && !relatedRoomIdFromChatList && myRooms.length > 0,
    staleTime: 30000,
  });

  const resolvedRelatedRoomId = relatedRoomIdFromChatList ?? inferredRelatedRoomId ?? null;

  const relatedRoom = resolvedRelatedRoomId
    ? myRooms.find(r => r.id === resolvedRelatedRoomId)
    : null;
  const isRoomChat = isRoomChatByType || !!resolvedRelatedRoomId;
  const isHost = useMemo(() => {
    if (!relatedRoom || !user) return false;
    return relatedRoom.participants?.some(p => p.userId === user.id && p.isHost) ?? false;
  }, [relatedRoom, user]);
  const evaluatableParticipants = useMemo(() => {
    if (!relatedRoom?.participants || !user) return [];
    return relatedRoom.participants.filter((participant) => participant.userId !== user.id);
  }, [relatedRoom, user]);

  const {
    data: evaluatableUserIds = [],
    isLoading: isEvaluatableLoading,
    refetch: refetchEvaluatable,
  } = useQuery({
    queryKey: ['roomEvaluatableUsers', relatedRoom?.id, user?.id],
    queryFn: () => evaluationService.getEvaluatable(relatedRoom!.id),
    enabled: !!relatedRoom?.id && !!user?.id,
    staleTime: 15000,
  });

  const canEvaluateRoom = useMemo(() => {
    if (!relatedRoom) return false;
    return relatedRoom.status === 'IN_PROGRESS' || relatedRoom.status === 'CLOSED';
  }, [relatedRoom]);
  const isRoomClosed = relatedRoom?.status === 'CLOSED';

  const hasEvaluatableParticipants = evaluatableParticipants.some((participant) =>
    evaluatableUserIds.includes(participant.userId),
  );

  const isEvaluateButtonDisabled =
    !canEvaluateRoom ||
    isEvaluatableLoading ||
    evaluatableParticipants.length === 0 ||
    !hasEvaluatableParticipants;

  useEffect(() => {
    if (window.innerWidth < 1024) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  }, [activeTab]);

  const handleKick = async (userId: number, nickname: string) => {
    if (!relatedRoom) return;
    if (!window.confirm(`${nickname}님을 강퇴하시겠습니까?`)) return;
    try {
      await roomService.kickParticipant(relatedRoom.id, userId);
      await refetchMyRooms();
    } catch {
      window.alert('강퇴 처리에 실패했습니다.');
    }
  };

  const handleTransferHost = async (userId: number, nickname: string) => {
    if (!relatedRoom) return;
    if (!window.confirm(`${nickname}님에게 방장을 넘기시겠습니까?`)) return;
    try {
      await roomService.transferHost(relatedRoom.id, userId);
      await refetchMyRooms();
    } catch {
      window.alert('방장 이전에 실패했습니다.');
    }
  };

  const handleStartRoom = async () => {
    if (!relatedRoom) return;
    setIsStartingRoom(true);
    try {
      await roomService.startRoom(relatedRoom.id);
      await refetchMyRooms();
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
      await refetchMyRooms();
      toast.success('방 상태를 대기중으로 변경했습니다.');
    } catch {
      toast.error('대기중으로 변경에 실패했습니다.');
    } finally {
      setIsResettingRoom(false);
    }
  };

  const handleCloseRoom = async () => {
    if (!relatedRoom) return;
    if (!window.confirm('방을 종료하시겠습니까? 종료 후에는 되돌릴 수 없습니다.')) return;
    setIsClosingRoom(true);
    try {
      await roomService.closeRoom(relatedRoom.publicId);
      await refetchMyRooms();
      toast.success('방을 종료했습니다. 참가자 평가를 진행해주세요.');
    } catch {
      toast.error('방 종료에 실패했습니다.');
    } finally {
      setIsClosingRoom(false);
    }
  };

  const handleCopyInviteLink = async () => {
    if (!relatedRoom?.publicId) return;
    try {
      const updatedRoom = await roomService.regenerateInviteCode(relatedRoom.publicId);
      if (!updatedRoom.inviteCode) {
        toast.error('초대 링크 생성에 실패했습니다');
        return;
      }
      const inviteUrl = `${window.location.origin}/games/${relatedRoom.gameId}/rooms?room=${updatedRoom.publicId}&invite=${encodeURIComponent(updatedRoom.inviteCode)}`;
      await copyToClipboard(inviteUrl);
      toast.success('초대 링크가 복사되었습니다');
    } catch {
      toast.error('초대 링크 생성에 실패했습니다');
    }
  };

  const handleLeave = async () => {
    if (!relatedRoom) return;
    setIsLeaving(true);
    try {
      await roomService.leaveRoom(relatedRoom.id);
      queryClient.invalidateQueries({ queryKey: ['myRooms'] });
      queryClient.invalidateQueries({ queryKey: ['myRoomChatRooms'] });
      queryClient.invalidateQueries({ queryKey: ['myChatRooms'] });
      toast.success('대기방을 나갔습니다');
      navigate('/');
    } catch {
      toast.error('나가기에 실패했습니다');
    } finally {
      setIsLeaving(false);
    }
  };

  if (!roomId) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center text-white">
        채팅방을 찾을 수 없습니다
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
          <ChatPanel chatRoomId={roomId} canChat={true} className="flex-1" />
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
                      <button
                        type="button"
                        onClick={handleCopyInviteLink}
                        className="w-full py-2 rounded border border-blue-500/40 bg-blue-500/10 text-blue-300 hover:bg-blue-500/20 font-semibold transition"
                      >
                        초대 링크 복사
                      </button>
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
                          {(relatedRoom.status === 'OPEN' || relatedRoom.status === 'IN_PROGRESS') && (
                            <button
                              type="button"
                              onClick={handleCloseRoom}
                              disabled={isClosingRoom}
                              className="flex-1 py-2 rounded bg-red-500 hover:bg-red-600 disabled:bg-gray-700 disabled:cursor-not-allowed font-semibold transition"
                            >
                              {isClosingRoom ? '종료 중...' : '방 종료'}
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
                <ChatPanel chatRoomId={roomId} canChat={!isRoomClosed} className="h-full" />
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
