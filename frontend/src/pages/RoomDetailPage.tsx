import { useState, useEffect, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useQueryClient } from '@tanstack/react-query';
import { ChevronLeft, Gamepad2, User, Users, Lock, LogOut, LogIn, Play } from 'lucide-react';
import { useRoom, useJoinRoom, useLeaveRoom, useKickParticipant, useStartRoom, useTransferHost } from '@/hooks/queries/useRooms';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { RoomParticipants } from '@/components/room/RoomParticipants';
import { ChatPanel } from '@/components/chat/ChatPanel';
import { chatService } from '@/services/chatService';
import { useAuthStore } from '@/store/authStore';
import { useToast } from '@/hooks/useToast';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { PasswordModal } from '@/components/room/PasswordModal';

export function RoomDetailPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const queryClient = useQueryClient();
  const { user } = useAuthStore();

  const { data: room, isLoading, error } = useRoom(Number(roomId));
  const joinRoomMutation = useJoinRoom();
  const leaveRoomMutation = useLeaveRoom();
  const kickMutation = useKickParticipant();
  const startRoomMutation = useStartRoom();
  const transferHostMutation = useTransferHost();
  const toast = useToast();

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);
  const [kickTarget, setKickTarget] = useState<{ userId: number; nickname: string } | null>(null);
  const [transferTarget, setTransferTarget] = useState<{ userId: number; nickname: string } | null>(null);
  const [chatRoomId, setChatRoomId] = useState<number | null>(null);

  const handleRoomUpdate = useCallback(() => {
    queryClient.invalidateQueries({ queryKey: roomKeys.detail(Number(roomId)) });
  }, [queryClient, roomId]);

  // Load chat room ID linked to this game room
  useEffect(() => {
    if (!roomId) return;
    chatService.getChatRoomByGameRoom(Number(roomId))
      .then((id) => setChatRoomId(id))
      .catch(() => {});
  }, [roomId]);

  const isParticipant = !!(user && room?.participants?.some((p) => p.userId === user.id));
  const isHost = !!(user && room?.participants?.some((p) => p.userId === user.id && p.isHost));

  const handleJoin = () => {
    if (!room) return;
    if (room.isPrivate) {
      setShowPasswordModal(true);
    } else {
      joinRoomMutation.mutate(
        { roomId: room.id },
        {
          onSuccess: () => toast.success('방에 입장했습니다'),
          onError: (error: any) => {
            toast.error(error.response?.data?.message || '입장에 실패했습니다');
          },
        }
      );
    }
  };

  const handlePasswordConfirm = (password: string) => {
    if (!room) return;
    setShowPasswordModal(false);
    joinRoomMutation.mutate(
      { roomId: room.id, password },
      {
        onSuccess: () => toast.success('방에 입장했습니다'),
        onError: (error: any) => {
          toast.error(error.response?.data?.message || '입장에 실패했습니다');
        },
      }
    );
  };

  const handleLeave = () => {
    if (!room) return;
    setShowLeaveConfirm(true);
  };

  const confirmLeave = () => {
    if (!room) return;
    setShowLeaveConfirm(false);
    leaveRoomMutation.mutate(room.id, {
      onSuccess: () => {
        toast.info('방에서 나갔습니다');
        navigate(`/games/${room.gameId}/rooms`);
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || '나가기에 실패했습니다');
      },
    });
  };

  const handleKick = (userId: number, nickname: string) => {
    setKickTarget({ userId, nickname });
  };

  const confirmKick = () => {
    if (!room || !kickTarget) return;
    setKickTarget(null);
    kickMutation.mutate(
      { roomId: room.id, userId: kickTarget.userId },
      {
        onSuccess: () => toast.success(`${kickTarget.nickname}님을 강퇴했습니다`),
        onError: (error: any) => toast.error(error.response?.data?.message || '강퇴에 실패했습니다'),
      }
    );
  };

  const handleTransferHost = (userId: number, nickname: string) => {
    setTransferTarget({ userId, nickname });
  };

  const confirmTransferHost = () => {
    if (!room || !transferTarget) return;
    setTransferTarget(null);
    transferHostMutation.mutate(
      { roomId: room.id, userId: transferTarget.userId },
      {
        onSuccess: () => toast.success(`${transferTarget.nickname}님에게 방장을 넘겼습니다`),
        onError: (error: any) => toast.error(error.response?.data?.message || '방장 이전에 실패했습니다'),
      }
    );
  };

  const handleStart = () => {
    if (!room) return;
    startRoomMutation.mutate(room.id, {
      onSuccess: () => toast.success('게임을 시작했습니다'),
      onError: (error: any) => toast.error(error.response?.data?.message || '시작에 실패했습니다'),
    });
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-white text-xl">Loading...</div>
      </div>
    );
  }

  if (error || !room) {
    return (
      <div className="min-h-screen bg-[#0e0e10] flex items-center justify-center">
        <div className="text-center">
          <div className="text-white text-xl mb-4">방을 찾을 수 없습니다</div>
          <button onClick={() => navigate('/')} className="text-purple-400 hover:text-purple-300">
            홈으로 돌아가기
          </button>
        </div>
      </div>
    );
  }

  const statusLabels = {
    OPEN: '모집중',
    FULL: '인원마감',
    IN_PROGRESS: '게임중',
    CLOSED: '종료',
  };

  const statusColors = {
    OPEN: 'bg-green-500/20 text-green-400 border-green-500',
    FULL: 'bg-orange-500/20 text-orange-400 border-orange-500',
    IN_PROGRESS: 'bg-purple-500/20 text-purple-400 border-purple-500',
    CLOSED: 'bg-gray-500/20 text-gray-400 border-gray-500',
  };

  return (
    <>
    {showPasswordModal && (
      <PasswordModal
        onConfirm={handlePasswordConfirm}
        onCancel={() => setShowPasswordModal(false)}
      />
    )}
    {showLeaveConfirm && (
      <ConfirmModal
        message="방을 나가시겠습니까?"
        onConfirm={confirmLeave}
        onCancel={() => setShowLeaveConfirm(false)}
        confirmLabel="나가기"
        danger
      />
    )}
    {kickTarget && (
      <ConfirmModal
        message={`${kickTarget.nickname}님을 강퇴하시겠습니까?`}
        onConfirm={confirmKick}
        onCancel={() => setKickTarget(null)}
        confirmLabel="강퇴"
        danger
      />
    )}
    {transferTarget && (
      <ConfirmModal
        message={`${transferTarget.nickname}님에게 방장을 넘기시겠습니까?`}
        onConfirm={confirmTransferHost}
        onCancel={() => setTransferTarget(null)}
        confirmLabel="넘기기"
      />
    )}
    <div className="min-h-screen bg-[#0e0e10] text-white">
      {/* Header */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-4">
          <button
            onClick={() => navigate(`/games/${room.gameId}/rooms`)}
            className="flex items-center gap-1.5 text-gray-400 hover:text-white transition"
          >
            <ChevronLeft size={18} />
            뒤로가기
          </button>
        </div>
      </div>

      {/* Content: split-screen on desktop */}
      <div className="container mx-auto px-4 py-6 max-w-7xl">
        <div className="flex flex-col lg:flex-row gap-6">
          {/* Left Column (60%) */}
          <div className="flex-1 min-w-0 space-y-5">
            {/* Room Info Card */}
            <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6">
              <div className="flex items-start justify-between mb-4">
                <h1 className="text-2xl font-bold text-white">{room.title}</h1>
                <span className={`px-3 py-1 rounded border-2 font-bold text-sm flex-shrink-0 ${statusColors[room.status]}`}>
                  {statusLabels[room.status]}
                </span>
              </div>

              <div className="flex flex-wrap items-center gap-4 mb-4 text-gray-300">
                <div className="flex items-center gap-2">
                  <Gamepad2 size={17} className="text-purple-400" />
                  <span>{room.gameName}</span>
                </div>
                <div className="flex items-center gap-2">
                  <User size={17} className="text-gray-400" />
                  <span>{room.createdBy}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Users size={17} className="text-gray-400" />
                  <span>{room.currentParticipants}/{room.maxParticipants}명</span>
                </div>
                {room.isPrivate && (
                  <div className="flex items-center gap-2 text-red-400">
                    <Lock size={17} />
                    <span>비공개</span>
                  </div>
                )}
              </div>

              {room.description && (
                <div className="mt-4 pt-4 border-t border-gray-700">
                  <h3 className="text-base font-semibold mb-2">방 설명</h3>
                  <p className="text-gray-300 whitespace-pre-wrap text-sm">{room.description}</p>
                </div>
              )}
            </div>

            {/* Participants Section */}
            <div className="bg-dark-secondary border-2 border-neon-purple/30 rounded-lg p-5 shadow-glow-purple">
              <h3 className="text-lg font-gaming mb-4 text-neon-purple flex items-center gap-2">
                <Users size={18} />
                참가자
              </h3>
              <RoomParticipants
                participants={room.participants}
                maxParticipants={room.maxParticipants}
                currentUserId={user?.id}
                isCurrentUserHost={isHost}
                onKick={isHost ? handleKick : undefined}
                onTransferHost={isHost ? handleTransferHost : undefined}
              />
            </div>

            {/* Action Buttons */}
            <div className="flex gap-3 flex-wrap">
              {room.status === 'OPEN' && !isParticipant && (
                <button
                  onClick={handleJoin}
                  disabled={joinRoomMutation.isPending}
                  className="flex-1 flex items-center justify-center gap-2 px-5 py-3 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
                >
                  <LogIn size={17} />
                  {joinRoomMutation.isPending ? '입장 중...' : '입장하기'}
                </button>
              )}
              {isHost && room.status === 'OPEN' && (
                <button
                  onClick={handleStart}
                  disabled={startRoomMutation.isPending}
                  className="flex-1 flex items-center justify-center gap-2 px-5 py-3 bg-green-600 hover:bg-green-500 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
                >
                  <Play size={17} />
                  {startRoomMutation.isPending ? '시작 중...' : '게임 시작'}
                </button>
              )}
              {isHost && (
                <button
                  onClick={handleLeave}
                  disabled={leaveRoomMutation.isPending}
                  className="flex items-center justify-center gap-2 px-5 py-3 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
                >
                  <LogOut size={17} />
                  {leaveRoomMutation.isPending ? '나가는 중...' : '나가기'}
                </button>
              )}
              {isParticipant && !isHost && (
                <button
                  onClick={handleLeave}
                  disabled={leaveRoomMutation.isPending}
                  className="flex-1 flex items-center justify-center gap-2 px-5 py-3 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
                >
                  <LogOut size={17} />
                  {leaveRoomMutation.isPending ? '나가는 중...' : '나가기'}
                </button>
              )}
            </div>

            {room.status === 'FULL' && (
              <div className="bg-orange-500/20 border border-orange-500 rounded-lg p-4 text-center">
                <p className="text-orange-400">방이 가득 찼습니다</p>
              </div>
            )}
            {room.status === 'IN_PROGRESS' && (
              <div className="bg-purple-500/20 border border-purple-500 rounded-lg p-4 text-center">
                <p className="text-purple-400">게임이 진행 중입니다</p>
              </div>
            )}
            {room.status === 'CLOSED' && (
              <div className="bg-gray-500/20 border border-gray-500 rounded-lg p-4 text-center">
                <p className="text-gray-400">종료된 방입니다</p>
              </div>
            )}
          </div>

          {/* Right Column: Chat Panel (40%) */}
          <div className="lg:w-[420px] flex-shrink-0">
            {chatRoomId ? (
              <ChatPanel
                chatRoomId={chatRoomId}
                canChat={isParticipant}
                className="h-[500px] lg:h-[calc(100vh-200px)] lg:sticky lg:top-6"
                onRoomUpdate={handleRoomUpdate}
              />
            ) : (
              <div className="h-[300px] lg:h-[500px] bg-[#18181b] border border-gray-700 rounded-lg flex items-center justify-center">
                <p className="text-gray-500 text-sm">채팅방 로딩 중...</p>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
    </>
  );
}
