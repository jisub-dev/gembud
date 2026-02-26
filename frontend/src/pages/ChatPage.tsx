import { useParams, useNavigate } from 'react-router-dom';

/**
 * Chat page (WebSocket implementation needed)
 *
 * @author Gembud Team
 * @since 2026-02-26
 */
export default function ChatPage() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white flex flex-col">
      {/* Header */}
      <div className="border-b border-gray-800">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(`/rooms/${roomId}`)}
              className="text-gray-400 hover:text-white transition"
            >
              ← 뒤로가기
            </button>
            <h1 className="text-xl font-bold">방 #{roomId} 채팅</h1>
          </div>
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 container mx-auto px-4 py-8 max-w-4xl flex flex-col">
        {/* Placeholder */}
        <div className="flex-1 flex items-center justify-center bg-[#18181b] border-2 border-gray-700 rounded-lg">
          <div className="text-center">
            <div className="text-6xl mb-4">💬</div>
            <h2 className="text-2xl font-bold mb-2">채팅 기능 구현 예정</h2>
            <p className="text-gray-400 mb-6">
              WebSocket 연결을 통한 실시간 채팅이 구현될 예정입니다
            </p>
            <div className="space-y-2 text-left text-gray-400 text-sm max-w-md mx-auto">
              <p>• 실시간 메시지 송수신</p>
              <p>• 참가자 목록 표시</p>
              <p>• 방 정보 표시</p>
              <p>• 방 나가기/종료 기능</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
