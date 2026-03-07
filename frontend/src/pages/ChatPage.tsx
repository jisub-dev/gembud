import { useParams, useNavigate } from 'react-router-dom';
import { useMemo, useState } from 'react';
import { ChevronLeft } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { ChatPanel } from '@/components/chat/ChatPanel';
import { RoomParticipants } from '@/components/room/RoomParticipants';
import { chatService } from '@/services/chatService';
import { roomService } from '@/services/roomService';
import { useAuthStore } from '@/store/authStore';

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
  const [activeTab, setActiveTab] = useState<'info' | 'chat'>('chat');
  const [isStartingRoom, setIsStartingRoom] = useState(false);

  const roomId = Number(chatRoomId);

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
        <div className="container mx-auto px-4 py-4 max-w-4xl">
          <button
            onClick={() => navigate(-1)}
            className="flex items-center gap-1.5 text-gray-400 hover:text-white transition"
          >
            <ChevronLeft size={18} />
            뒤로가기
          </button>
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
              <section className={`${activeTab === 'info' ? 'block' : 'hidden'} lg:block lg:col-span-2 bg-[#18181b] border border-gray-800 rounded-lg p-4 overflow-y-auto`}>
                {relatedRoom ? (
                  <div className="space-y-4">
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

                    {isHost && (
                      <button
                        type="button"
                        onClick={handleStartRoom}
                        disabled={isStartingRoom || relatedRoom.status !== 'OPEN'}
                        className="w-full py-2 rounded bg-purple-500 hover:bg-purple-600 disabled:bg-gray-700 disabled:cursor-not-allowed font-semibold transition"
                      >
                        {isStartingRoom ? '시작 중...' : '게임 시작'}
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="h-full flex items-center justify-center text-sm text-gray-400">
                    방 정보를 불러오지 못했습니다
                  </div>
                )}
              </section>

              <section className={`${activeTab === 'chat' ? 'block' : 'hidden'} lg:block lg:col-span-3 min-h-0`}>
                <ChatPanel chatRoomId={roomId} canChat={true} className="h-full" />
              </section>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
