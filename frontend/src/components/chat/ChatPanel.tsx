import { useEffect, useRef, useState, useCallback } from 'react';
import { Send, Wifi, WifiOff, Loader2 } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { chatService } from '@/services/chatService';
import { useAuthStore } from '@/store/authStore';
import type { ChatMessage } from '@/types/chat';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const WS_URL = API_BASE_URL + '/ws';

interface ChatPanelProps {
  chatRoomId: number;
  canChat?: boolean;
  className?: string;
  onRoomUpdate?: () => void;
}

export function ChatPanel({ chatRoomId, canChat = true, className = '', onRoomUpdate }: ChatPanelProps) {
  const { user } = useAuthStore();

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [connected, setConnected] = useState(false);
  const [connecting, setConnecting] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const stompClientRef = useRef<Client | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);
  const onRoomUpdateRef = useRef(onRoomUpdate);
  useEffect(() => { onRoomUpdateRef.current = onRoomUpdate; }, [onRoomUpdate]);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  useEffect(() => {
    if (!canChat) return;
    chatService.getMessages(chatRoomId, 50)
      .then((history) => setMessages([...history].reverse()))
      .catch(() => {});
  }, [chatRoomId, canChat]);

  useEffect(() => {
    // StrictMode에서 double-invoke 방지: 이전 클라이언트가 아직 살아있으면 먼저 정리
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    let active = true; // cleanup 후 콜백이 state를 건드리지 않도록

    const client = new Client({
      webSocketFactory: () => new SockJS(WS_URL, null, { transports: ['websocket', 'xhr-streaming', 'xhr-polling'] }),
      reconnectDelay: 5000,
      onConnect: () => {
        if (!active) return;
        setConnected(true);
        setConnecting(false);
        setError(null);

        client.subscribe(`/topic/chat/${chatRoomId}`, (frame) => {
          if (!active) return;
          try {
            const msg = JSON.parse(frame.body);
            if (msg.type === 'ROOM_UPDATE') {
              onRoomUpdateRef.current?.();
              return;
            }
            setMessages((prev) => {
              if (msg.id && prev.some((m: ChatMessage) => m.id === msg.id)) return prev;
              return [...prev, msg as ChatMessage];
            });
          } catch {}
        });

        client.publish({ destination: `/app/chat.join/${chatRoomId}`, body: '' });
      },
      onDisconnect: () => { if (active) setConnected(false); },
      onStompError: (frame) => {
        if (!active) return;
        setError('연결 실패: ' + (frame.headers?.message || '알 수 없는 오류'));
        setConnecting(false);
      },
      onWebSocketError: () => {
        if (!active) return;
        setError('WebSocket 연결 실패');
        setConnecting(false);
      },
    });

    stompClientRef.current = client;
    client.activate();

    return () => {
      active = false;
      stompClientRef.current = null;
      if (client.connected) {
        client.publish({ destination: `/app/chat.leave/${chatRoomId}`, body: '' });
      }
      client.deactivate();
    };
  }, [chatRoomId]);

  const sendMessage = useCallback(() => {
    const text = inputText.trim();
    if (!text || !stompClientRef.current?.connected || !canChat) return;

    stompClientRef.current.publish({
      destination: `/app/chat.send/${chatRoomId}`,
      body: JSON.stringify({ chatRoomId, message: text }),
    });

    setInputText('');
    inputRef.current?.focus();
  }, [inputText, chatRoomId, canChat]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLInputElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      sendMessage();
    }
  };

  const formatTime = (isoString: string) =>
    new Date(isoString).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

  const isSystemMessage = (msg: ChatMessage) =>
    msg.message === 'User joined the chat' || msg.message === 'User left the chat';

  return (
    <div className={`flex flex-col bg-[#18181b] border border-gray-700 rounded-lg overflow-hidden ${className}`}>
      {/* Chat Header */}
      <div className="flex items-center justify-between px-4 py-3 border-b border-gray-700 flex-shrink-0">
        <span className="text-sm font-semibold text-gray-300">대기방 채팅</span>
        <span>
          {connected ? (
            <span className="flex items-center gap-1 text-xs text-green-400">
              <Wifi size={12} /> 연결됨
            </span>
          ) : connecting ? (
            <span className="flex items-center gap-1 text-xs text-yellow-400">
              <Loader2 size={12} className="animate-spin" /> 연결 중...
            </span>
          ) : (
            <span className="flex items-center gap-1 text-xs text-red-400">
              <WifiOff size={12} /> 연결 끊김
            </span>
          )}
        </span>
      </div>


      {/* Error */}
      {error && (
        <div className="bg-red-500/20 px-3 py-1.5 text-red-400 text-xs text-center flex-shrink-0">
          {error}
        </div>
      )}

      {/* Messages */}
      <div className="flex-1 overflow-y-auto px-3 py-3 space-y-2 min-h-0">
        {connecting && messages.length === 0 && (
          <div className="flex items-center justify-center h-full text-gray-500 text-sm">
            <Loader2 size={20} className="animate-spin mr-2 text-purple-500" />
            채팅 연결 중...
          </div>
        )}

        {messages.map((msg, idx) => {
          if (isSystemMessage(msg)) {
            return (
              <div key={idx} className="text-center">
                <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded-full">
                  {msg.username || `사용자 ${msg.userId}`}님이{' '}
                  {msg.message === 'User joined the chat' ? '입장' : '퇴장'}했습니다
                </span>
              </div>
            );
          }

          const isOwn = user && msg.userId === user.id;

          return (
            <div key={idx} className={`flex items-end gap-1.5 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}>
              {!isOwn && (
                <div className="w-7 h-7 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-xs font-bold flex-shrink-0">
                  {msg.username?.[0] || '?'}
                </div>
              )}
              <div className={`flex flex-col max-w-[75%] ${isOwn ? 'items-end' : 'items-start'}`}>
                {!isOwn && (
                  <span className="text-xs text-gray-400 mb-0.5 ml-1">{msg.username}</span>
                )}
                <div
                  className={`px-3 py-1.5 rounded-xl text-sm leading-relaxed ${
                    isOwn
                      ? 'bg-purple-600 text-white rounded-br-sm'
                      : 'bg-[#2a2a2f] text-gray-100 rounded-bl-sm'
                  }`}
                >
                  {msg.message}
                </div>
                {msg.createdAt && (
                  <span className="text-xs text-gray-500 mt-0.5 mx-1">
                    {formatTime(msg.createdAt)}
                  </span>
                )}
              </div>
            </div>
          );
        })}

        <div ref={messagesEndRef} />
      </div>

      {/* Input */}
      <div className="border-t border-gray-700 px-3 py-2.5 flex-shrink-0">
        {!canChat ? (
          <p className="text-center text-xs text-gray-500 py-1">입장 후 채팅 가능합니다</p>
        ) : (
          <div className="flex gap-2">
            <input
              ref={inputRef}
              type="text"
              value={inputText}
              onChange={(e) => setInputText(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={connected ? '메시지 입력... (Enter)' : '연결 중...'}
              disabled={!connected}
              className="flex-1 bg-[#0e0e10] border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
            />
            <button
              onClick={sendMessage}
              disabled={!connected || !inputText.trim()}
              className="px-3 py-2 bg-purple-600 hover:bg-purple-500 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg transition"
            >
              <Send size={15} />
            </button>
          </div>
        )}
      </div>
    </div>
  );
}
