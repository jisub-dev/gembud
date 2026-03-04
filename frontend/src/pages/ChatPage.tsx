import { useEffect, useRef, useState, useCallback } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { ChevronLeft, Send, Wifi, WifiOff, Loader2 } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { chatService } from '@/services/chatService';
import { useAuthStore } from '@/store/authStore';
import type { ChatMessage } from '@/types/chat';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const WS_URL = API_BASE_URL.replace('/api', '') + '/ws';

export default function ChatPage() {
  const { roomId: chatRoomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { user } = useAuthStore();

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [connected, setConnected] = useState(false);
  const [connecting, setConnecting] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  const roomId = Number(chatRoomId);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!roomId) return;
    chatService.getMessages(roomId, 50)
      .then((history) => setMessages(history))
      .catch(() => {});
  }, [roomId]);

  useEffect(() => {
    if (!roomId) return;

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL, null, { transports: ['websocket', 'xhr-streaming', 'xhr-polling'] }),
      reconnectDelay: 5000,
      onConnect: () => {
        setConnected(true);
        setConnecting(false);
        setError(null);

        client.subscribe(`/topic/chat/${roomId}`, (frame) => {
          try {
            const msg: ChatMessage = JSON.parse(frame.body);
            setMessages((prev) => [...prev, msg]);
          } catch {}
        });

        client.publish({ destination: `/app/chat.join/${roomId}`, body: '' });
      },
      onDisconnect: () => setConnected(false),
      onStompError: (frame) => {
        setError('채팅 서버 연결에 실패했습니다: ' + (frame.headers?.message || '알 수 없는 오류'));
        setConnecting(false);
      },
      onWebSocketError: () => {
        setError('WebSocket 연결에 실패했습니다');
        setConnecting(false);
      },
    });

    stompClientRef.current = client;
    client.activate();

    return () => {
      if (client.connected) {
        client.publish({ destination: `/app/chat.leave/${roomId}`, body: '' });
      }
      client.deactivate();
    };
  }, [roomId]);

  const sendMessage = useCallback(() => {
    const text = inputText.trim();
    if (!text || !stompClientRef.current?.connected) return;

    stompClientRef.current.publish({
      destination: `/app/chat.send/${roomId}`,
      body: JSON.stringify({ chatRoomId: roomId, message: text }),
    });

    setInputText('');
    inputRef.current?.focus();
  }, [inputText, roomId]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      sendMessage();
    }
  };

  const formatTime = (isoString: string) => {
    return new Date(isoString).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });
  };

  const isSystemMessage = (msg: ChatMessage) =>
    msg.message === 'User joined the chat' || msg.message === 'User left the chat';

  return (
    <div className="min-h-screen bg-[#0e0e10] text-white flex flex-col">
      {/* Header */}
      <div className="border-b border-gray-800 flex-shrink-0">
        <div className="container mx-auto px-4 py-4 flex items-center justify-between max-w-4xl">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate(-1)}
              className="flex items-center gap-1.5 text-gray-400 hover:text-white transition"
            >
              <ChevronLeft size={18} />
              뒤로가기
            </button>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold">채팅방 #{chatRoomId}</h1>
              {connected ? (
                <span className="flex items-center gap-1 text-xs text-green-400">
                  <Wifi size={13} />
                  연결됨
                </span>
              ) : connecting ? (
                <span className="flex items-center gap-1 text-xs text-yellow-400">
                  <Loader2 size={13} className="animate-spin" />
                  연결 중...
                </span>
              ) : (
                <span className="flex items-center gap-1 text-xs text-red-400">
                  <WifiOff size={13} />
                  연결 끊김
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Error Banner */}
      {error && (
        <div className="bg-red-500/20 border-b border-red-500/50 px-4 py-2 text-red-400 text-sm text-center flex-shrink-0">
          {error}
        </div>
      )}

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto container mx-auto px-4 py-4 max-w-4xl">
        {connecting && messages.length === 0 && (
          <div className="flex items-center justify-center h-full text-gray-400">
            <div className="text-center">
              <Loader2 size={32} className="animate-spin mx-auto mb-3 text-purple-500" />
              <p>채팅 서버에 연결 중...</p>
            </div>
          </div>
        )}

        <div className="space-y-3">
          {messages.map((msg, idx) => {
            if (isSystemMessage(msg)) {
              return (
                <div key={idx} className="text-center">
                  <span className="text-xs text-gray-500 bg-gray-800 px-3 py-1 rounded-full">
                    {msg.senderNickname || `사용자 ${msg.senderId}`}님이{' '}
                    {msg.message === 'User joined the chat' ? '입장했습니다' : '나갔습니다'}
                  </span>
                </div>
              );
            }

            const isOwn = user && msg.senderId === user.id;

            return (
              <div
                key={idx}
                className={`flex items-end gap-2 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}
              >
                {!isOwn && (
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-sm font-bold flex-shrink-0">
                    {msg.senderNickname?.[0] || '?'}
                  </div>
                )}

                <div className={`flex flex-col max-w-[70%] ${isOwn ? 'items-end' : 'items-start'}`}>
                  {!isOwn && (
                    <span className="text-xs text-gray-400 mb-1 ml-1">{msg.senderNickname}</span>
                  )}
                  <div
                    className={`px-4 py-2 rounded-2xl text-sm leading-relaxed ${
                      isOwn
                        ? 'bg-purple-600 text-white rounded-br-sm'
                        : 'bg-[#2a2a2f] text-gray-100 rounded-bl-sm'
                    }`}
                  >
                    {msg.message}
                  </div>
                  {msg.createdAt && (
                    <span className="text-xs text-gray-500 mt-1 mx-1">
                      {formatTime(msg.createdAt)}
                    </span>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        <div ref={messagesEndRef} />
      </div>

      {/* Input Area */}
      <div className="border-t border-gray-800 flex-shrink-0">
        <div className="container mx-auto px-4 py-4 max-w-4xl">
          <div className="flex gap-3">
            <input
              ref={inputRef}
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={connected ? '메시지를 입력하세요... (Enter로 전송)' : '연결 중...'}
              disabled={!connected}
              className="flex-1 bg-[#18181b] border border-gray-700 rounded-lg px-4 py-3 text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
            />
            <button
              onClick={sendMessage}
              disabled={!connected || !inputText.trim()}
              className="flex items-center gap-2 px-6 py-3 bg-purple-600 hover:bg-purple-500 disabled:bg-gray-700 disabled:cursor-not-allowed text-white font-semibold rounded-lg transition"
            >
              <Send size={17} />
              전송
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
