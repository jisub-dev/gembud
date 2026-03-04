import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft } from 'lucide-react';
import { ChatPanel } from '@/components/chat/ChatPanel';

export default function ChatPage() {
  const { roomId: chatRoomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();

  const roomId = Number(chatRoomId);

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
