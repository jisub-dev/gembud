import { useParams, useNavigate } from 'react-router-dom';
import { useRoom, useJoinRoom, useLeaveRoom } from '@/hooks/queries/useRooms';

/**
 * Room detail page for viewing room information and joining/leaving.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */
export function RoomDetailPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();

  const { data: room, isLoading, error } = useRoom(Number(roomId));
  const joinRoomMutation = useJoinRoom();
  const leaveRoomMutation = useLeaveRoom();

  const handleJoin = () => {
    if (!room) return;

    if (room.isPrivate) {
      // 비밀번호 입력 프롬프트
      const password = prompt('비밀번호를 입력하세요');
      if (!password) return;

      joinRoomMutation.mutate(
        { roomId: room.id, password },
        {
          onSuccess: () => {
            alert('방에 입장했습니다');
            // TODO: Navigate to chat page or show chat
          },
          onError: (error: any) => {
            alert(error.response?.data?.message || '입장에 실패했습니다');
          },
        }
      );
    } else {
      joinRoomMutation.mutate(
        { roomId: room.id },
        {
          onSuccess: () => {
            alert('방에 입장했습니다');
            // TODO: Navigate to chat page or show chat
          },
          onError: (error: any) => {
            alert(error.response?.data?.message || '입장에 실패했습니다');
          },
        }
      );
    }
  };

  const handleLeave = () => {
    if (!room) return;

    if (confirm('방을 나가시겠습니까?')) {
      leaveRoomMutation.mutate(room.id, {
        onSuccess: () => {
          alert('방에서 나갔습니다');
          navigate(`/games/${room.gameId}/rooms`);
        },
        onError: (error: any) => {
          alert(error.response?.data?.message || '나가기에 실패했습니다');
        },
      });
    }
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
          <button
            onClick={() => navigate('/')}
            className="text-purple-400 hover:text-purple-300"
          >
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
    <div className="min-h-screen bg-[#0e0e10] text-white">
      {/* Header */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-6">
          <button
            onClick={() => navigate(`/games/${room.gameId}/rooms`)}
            className="text-gray-400 hover:text-white mb-4 transition"
          >
            ← 뒤로가기
          </button>
        </div>
      </div>

      {/* Content */}
      <div className="container mx-auto px-4 py-8 max-w-4xl">
        {/* Room Info Card */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
          {/* Title & Status */}
          <div className="flex items-start justify-between mb-4">
            <h1 className="text-3xl font-bold text-white">{room.title}</h1>
            <span className={`px-3 py-1 rounded border-2 font-bold ${statusColors[room.status]}`}>
              {statusLabels[room.status]}
            </span>
          </div>

          {/* Game Info */}
          <div className="flex items-center gap-6 mb-4 text-gray-300">
            <div className="flex items-center gap-2">
              <span className="text-2xl">🎮</span>
              <span>{room.gameName}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-2xl">👤</span>
              <span>{room.createdBy}</span>
            </div>
            <div className="flex items-center gap-2">
              <span className="text-2xl">👥</span>
              <span>{room.currentParticipants}/{room.maxParticipants}명</span>
            </div>
            {room.isPrivate && (
              <div className="flex items-center gap-2">
                <span className="text-2xl">🔒</span>
                <span>비공개</span>
              </div>
            )}
          </div>

          {/* Description */}
          {room.description && (
            <div className="mt-6 pt-6 border-t border-gray-700">
              <h3 className="text-lg font-semibold mb-2">방 설명</h3>
              <p className="text-gray-300 whitespace-pre-wrap">{room.description}</p>
            </div>
          )}
        </div>

        {/* Participants Section */}
        <div className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-6 mb-6">
          <h3 className="text-lg font-semibold mb-4">참가자</h3>
          <div className="text-gray-400 text-center py-4">
            참가자 목록 (구현 예정)
          </div>
          {/* TODO: Add RoomParticipants component */}
        </div>

        {/* Action Buttons */}
        <div className="flex gap-4">
          {room.status === 'OPEN' && (
            <button
              onClick={handleJoin}
              disabled={joinRoomMutation.isPending}
              className="flex-1 px-6 py-3 bg-purple-500 hover:bg-purple-600 disabled:bg-gray-600 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
            >
              {joinRoomMutation.isPending ? '입장 중...' : '입장하기'}
            </button>
          )}
          <button
            onClick={handleLeave}
            disabled={leaveRoomMutation.isPending}
            className="flex-1 px-6 py-3 bg-gray-700 hover:bg-gray-600 disabled:bg-gray-800 disabled:cursor-not-allowed text-white font-bold rounded-lg transition"
          >
            {leaveRoomMutation.isPending ? '나가는 중...' : '나가기'}
          </button>
        </div>

        {/* Room Full Notice */}
        {room.status === 'FULL' && (
          <div className="mt-4 bg-orange-500/20 border border-orange-500 rounded-lg p-4 text-center">
            <p className="text-orange-400">방이 가득 찼습니다</p>
          </div>
        )}

        {/* In Progress Notice */}
        {room.status === 'IN_PROGRESS' && (
          <div className="mt-4 bg-purple-500/20 border border-purple-500 rounded-lg p-4 text-center">
            <p className="text-purple-400">게임이 진행 중입니다</p>
          </div>
        )}

        {/* Closed Notice */}
        {room.status === 'CLOSED' && (
          <div className="mt-4 bg-gray-500/20 border border-gray-500 rounded-lg p-4 text-center">
            <p className="text-gray-400">종료된 방입니다</p>
          </div>
        )}
      </div>
    </div>
  );
}
