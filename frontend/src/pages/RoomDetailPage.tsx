import { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Gamepad2, User, Users, Lock, LogOut, LogIn } from 'lucide-react';
import { useRoom, useJoinRoom, useLeaveRoom } from '@/hooks/queries/useRooms';
import { RoomParticipants } from '@/components/room/RoomParticipants';
import { chatService } from '@/services/chatService';
import { useToast } from '@/hooks/useToast';
import { ConfirmModal } from '@/components/common/ConfirmModal';
import { PasswordModal } from '@/components/room/PasswordModal';

export function RoomDetailPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();

  const { data: room, isLoading, error } = useRoom(Number(roomId));
  const joinRoomMutation = useJoinRoom();
  const leaveRoomMutation = useLeaveRoom();
  const toast = useToast();

  const [showPasswordModal, setShowPasswordModal] = useState(false);
  const [showLeaveConfirm, setShowLeaveConfirm] = useState(false);

  const navigateToChat = async (gameRoomId: number) => {
    try {
      const chatRoomId = await chatService.getChatRoomByGameRoom(gameRoomId);
      navigate(`/chat/${chatRoomId}`);
    } catch {
      navigate(`/chat/${gameRoomId}`);
    }
  };

  const handleJoin = () => {
    if (!room) return;
    if (room.isPrivate) {
      setShowPasswordModal(true);
    } else {
      joinRoomMutation.mutate(
        { roomId: room.id },
        {
          onSuccess: () => navigateToChat(room.id),
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
        onSuccess: () => navigateToChat(room.id),
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
    <div className="min-h-screen bg-[#0e0e10] text-white">
      {/* Header */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-6">
          <button
            onClick={() => navigate(`/games/${room.gameId}/rooms`)}
            className="flex items-center gap-1.5 text-gray-400 hover:text-white mb-4 transition"
          >
            <ChevronLeft size={18} />
            뒤로가기
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Room Info Card */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
          <div className="flex items-start justify-between mb-4">
            <h1 className="text-3xl font-bold text-white">{room.title}</h1>
            <span className={`px-3 py-1 rounded border-2 font-bold ${statusColors[room.status]}`}>
              {statusLabels[room.status]}
            </span>
          </div>

          <div className="flex flex-wrap items-center gap-5 mb-4 text-gray-300">
            <div className="flex items-center gap-2">
              <Gamepad2 size={18} className="text-purple-400" />
              <span>{room.gameName}</span>
            </div>
            <div className="flex items-center gap-2">
              <User size={18} className="text-gray-400" />
              <span>{room.createdBy}</span>
            </div>
            <div className="flex items-center gap-2">
              <Users size={18} className="text-gray-400" />
              <span>{room.currentParticipants}/{room.maxParticipants}명</span>
            </div>
            {room.isPrivate && (
              <div className="flex items-center gap-2 text-red-400">
                <Lock size={18} />
                <span>비공개</span>
              </div>
            )}
          </div>

          {room.description && (
            <div className="mt-6 pt-6 border-t border-gray-700">
              <h3 className="text-lg font-semibold mb-2">방 설명</h3>
              <p className="text-gray-300 whitespace-pre-wrap">{room.description}</p>
            </div>
          )}
        </div>

        {/* Participants Section */}
        <div className="bg-dark-secondary border-2 border-neon-purple/30 rounded-lg p-6 mb-6 shadow-glow-purple">
          <h3 className="text-xl font-gaming mb-4 text-neon-purple flex items-center gap-2">
            <Users size={20} />
            참가자
          </h3>
          <RoomParticipants
            participants={room.participants}
            maxParticipants={room.maxParticipants}
          />
        </div>

        {/* Action Buttons */}
        <div className="flex gap-4">
          {room.status === 'OPEN' && (
            <button
              onClick={handleJoin}
              disabled={joinRoomMutation.isPending}
              className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
            >
              <LogIn size={18} />
              {joinRoomMutation.isPending ? '입장 중...' : '입장하기'}
            </button>
          )}
          <button
            onClick={handleLeave}
            disabled={leaveRoomMutation.isPending}
            className="flex-1 flex items-center justify-center gap-2 px-6 py-3 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
          >
            <LogOut size={18} />
            {leaveRoomMutation.isPending ? '나가는 중...' : '나가기'}
          </button>
        </div>

        {room.status === 'FULL' && (
          <div className="mt-4 bg-orange-500/20 border border-orange-500 rounded-lg p-4 text-center">
            <p className="text-orange-400">방이 가득 찼습니다</p>
          </div>
        )}
        {room.status === 'IN_PROGRESS' && (
          <div className="mt-4 bg-purple-500/20 border border-purple-500 rounded-lg p-4 text-center">
            <p className="text-purple-400">게임이 진행 중입니다</p>
          </div>
        )}
        {room.status === 'CLOSED' && (
          <div className="mt-4 bg-gray-500/20 border border-gray-500 rounded-lg p-4 text-center">
            <p className="text-gray-400">종료된 방입니다</p>
          </div>
        )}
      </div>
    </div>
    </>
  );
}
