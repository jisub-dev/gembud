import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Users, Crown, LogOut } from 'lucide-react';
import { useQuery, useQueryClient } from '@tanstack/react-query';
import { useState } from 'react';
import { ChatPanel } from '@/components/chat/ChatPanel';
import { chatService } from '@/services/chatService';
import { roomService } from '@/services/roomService';
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
  const relatedRoomId = chatRoomInfo?.relatedRoomId;

  const { data: myRooms = [] } = useQuery({
    queryKey: ['myRooms'],
    queryFn: roomService.getMyRooms,
    enabled: !!relatedRoomId,
    refetchInterval: 10000,
  });

  const relatedRoom = relatedRoomId
    ? myRooms.find(r => r.id === relatedRoomId)
    : null;

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

      {/* 대기방 정보 패널 (ROOM_CHAT인 경우만) */}
      {relatedRoom && (
        <div className="border-b border-gray-800 flex-shrink-0 bg-[#18181b]">
          <div className="container mx-auto px-4 py-3 max-w-4xl">
            <div className="flex items-center justify-between gap-4">
              {/* 방 제목 + 상태 */}
              <div className="flex items-center gap-3 min-w-0">
                <div className="flex items-center gap-2">
                  <Users size={16} className="text-purple-400 flex-shrink-0" />
                  <span className="font-semibold text-white truncate">{relatedRoom.title}</span>
                </div>
                <span className={`text-xs font-medium flex-shrink-0 ${STATUS_COLORS[relatedRoom.status] ?? 'text-gray-400'}`}>
                  {STATUS_LABELS[relatedRoom.status] ?? relatedRoom.status}
                </span>
              </div>
              {/* 인원 */}
              <span className="text-sm text-gray-400 flex-shrink-0">
                {relatedRoom.currentParticipants} / {relatedRoom.maxParticipants}명
              </span>
            </div>

            {/* 참여자 목록 */}
            {relatedRoom.participants && relatedRoom.participants.length > 0 && (
              <div className="flex gap-2 mt-2 flex-wrap">
                {relatedRoom.participants.map(p => (
                  <div
                    key={p.userId}
                    className="flex items-center gap-1 bg-gray-800 rounded-full px-2 py-0.5"
                  >
                    {p.isHost && <Crown size={11} className="text-yellow-400" />}
                    <span className="text-xs text-gray-300">{p.nickname}</span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* Chat Panel fills remaining height */}
      <div className="flex-1 container mx-auto px-4 py-4 max-w-4xl flex flex-col min-h-0">
        <ChatPanel
          chatRoomId={roomId}
          canChat={true}
          className="flex-1"
        />
      </div>
    </div>
  );
}
