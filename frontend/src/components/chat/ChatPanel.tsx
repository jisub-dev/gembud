import { useEffect, useRef, useState, useCallback } from 'react';
import { Send, Wifi, WifiOff, Loader2 } from 'lucide-react';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import { chatService } from '@/services/chatService';
import { useAuthStore } from '@/store/authStore';
import { ReportModal } from '@/components/common/ReportModal';
import { notifySessionExpired } from '@/lib/sessionExpiryBridge';
import type { ChatMessage } from '@/types/chat';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api';
const WS_URL = API_BASE_URL + '/ws';
const EMOJIS = ['😀', '🎮', '👍', '💪', '🔥', '😂', '👋', '🎉', '😎', '🤝', '🙏', '😅', '👏', '🫡', '💯'];

interface ChatPanelProps {
  chatRoomId: number;
  chatPublicId: string;
  canChat?: boolean;
  className?: string;
  onRoomUpdate?: () => void;
}

export function ChatPanel({
  chatRoomId,
  chatPublicId,
  canChat = true,
  className = '',
  onRoomUpdate,
}: ChatPanelProps) {
  const { user } = useAuthStore();

  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [inputText, setInputText] = useState('');
  const [connected, setConnected] = useState(false);
  const [connecting, setConnecting] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [showEmojiPicker, setShowEmojiPicker] = useState(false);
  const [newMessageCount, setNewMessageCount] = useState(0);
  const [showNewMessageButton, setShowNewMessageButton] = useState(false);
  const [reportTarget, setReportTarget] = useState<{
    userId: number;
    nickname: string;
    chatMessageId?: number;
  } | null>(null);

  const stompClientRef = useRef<Client | null>(null);
  const messagesContainerRef = useRef<HTMLDivElement>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const isAtBottomRef = useRef(true);
  const onRoomUpdateRef = useRef(onRoomUpdate);
  const failCountRef = useRef(0);

  useEffect(() => {
    onRoomUpdateRef.current = onRoomUpdate;
  }, [onRoomUpdate]);

  const isNearBottom = useCallback(() => {
    const container = messagesContainerRef.current;
    if (!container) return true;
    return container.scrollHeight - container.scrollTop - container.clientHeight < 48;
  }, []);

  const scrollToBottom = useCallback((behavior: ScrollBehavior = 'smooth') => {
    messagesEndRef.current?.scrollIntoView({ behavior });
    isAtBottomRef.current = true;
    setNewMessageCount(0);
    setShowNewMessageButton(false);
  }, []);

  const adjustTextareaHeight = useCallback(() => {
    const textarea = inputRef.current;
    if (!textarea) return;

    textarea.style.height = 'auto';
    const lineHeight = Number.parseInt(window.getComputedStyle(textarea).lineHeight || '20', 10);
    const maxHeight = lineHeight * 4 + 12;
    textarea.style.height = `${Math.min(textarea.scrollHeight, maxHeight)}px`;
    textarea.style.overflowY = textarea.scrollHeight > maxHeight ? 'auto' : 'hidden';
  }, []);

  useEffect(() => {
    adjustTextareaHeight();
  }, [inputText, adjustTextareaHeight]);

  useEffect(() => {
    if (!canChat) return;
    chatService
      .getMessages(chatPublicId, 50)
      .then((history) => {
        setMessages([...history].reverse());
        requestAnimationFrame(() => scrollToBottom('auto'));
      })
      .catch(() => {});
  }, [chatPublicId, canChat, scrollToBottom]);

  useEffect(() => {
    if (stompClientRef.current) {
      stompClientRef.current.deactivate();
      stompClientRef.current = null;
    }

    let active = true;

    const client = new Client({
      webSocketFactory: () =>
        new SockJS(WS_URL, null, { transports: ['websocket', 'xhr-streaming', 'xhr-polling'] }),
      reconnectDelay: 5000,
      onConnect: () => {
        if (!active) return;
        setConnected(true);
        setConnecting(false);
        setError(null);

        client.subscribe('/user/queue/session-expired', () => {
          if (!active) return;
          notifySessionExpired();
          client.deactivate();
        });

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
              const next = [...prev, msg as ChatMessage];

              if (isAtBottomRef.current) {
                requestAnimationFrame(() => scrollToBottom('smooth'));
              } else {
                setNewMessageCount((count) => count + 1);
                setShowNewMessageButton(true);
              }

              return next;
            });
          } catch {
            // no-op
          }
        });

        client.publish({ destination: `/app/chat.join/${chatRoomId}`, body: '' });
      },
      onDisconnect: () => {
        if (active) setConnected(false);
      },
      onStompError: (frame) => {
        if (!active) return;
        setError('연결 실패: ' + (frame.headers?.message || '알 수 없는 오류'));
        setConnecting(false);
      },
      onWebSocketError: () => {
        if (!active) return;
        failCountRef.current += 1;
        if (failCountRef.current >= 5) {
          setError('채팅 서버와 연결이 끊겼습니다');
          setConnecting(false);
          client.deactivate();
        }
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
  }, [chatRoomId, scrollToBottom]);

  useEffect(() => {
    const handleEsc = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        setShowEmojiPicker(false);
      }
    };

    window.addEventListener('keydown', handleEsc);
    return () => window.removeEventListener('keydown', handleEsc);
  }, []);

  const sendMessage = useCallback(() => {
    const text = inputText.trim();
    if (!text || !stompClientRef.current?.connected || !canChat) return;

    stompClientRef.current.publish({
      destination: `/app/chat.send/${chatRoomId}`,
      body: JSON.stringify({ chatRoomId, message: text }),
    });

    setInputText('');
    requestAnimationFrame(() => {
      adjustTextareaHeight();
      inputRef.current?.focus();
    });
  }, [adjustTextareaHeight, inputText, chatRoomId, canChat]);

  const handleKeyDown = (e: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (e.key === 'Enter' && !e.shiftKey && !e.nativeEvent.isComposing) {
      e.preventDefault();
      sendMessage();
    }
  };

  const handleScroll = () => {
    const atBottom = isNearBottom();
    isAtBottomRef.current = atBottom;

    if (atBottom) {
      setNewMessageCount(0);
      setShowNewMessageButton(false);
    }
  };

  const appendEmoji = (emoji: string) => {
    setInputText((prev) => prev + emoji);
    inputRef.current?.focus();
  };

  const formatTime = (isoString: string) =>
    new Date(isoString).toLocaleTimeString('ko-KR', { hour: '2-digit', minute: '2-digit' });

  const formatDateDivider = (isoString: string) => {
    const date = new Date(isoString);
    return `${date.getMonth() + 1}월 ${date.getDate()}일`;
  };

  const isSystemMessage = (msg: ChatMessage) =>
    msg.message === 'User joined the chat' || msg.message === 'User left the chat';

  return (
    <div className={`flex flex-col bg-[#18181b] border border-gray-700 rounded-lg overflow-hidden ${className}`}>
      {reportTarget && (
        <ReportModal
          reportedUserId={reportTarget.userId}
          reportedNickname={reportTarget.nickname}
          chatMessageId={reportTarget.chatMessageId}
          onClose={() => setReportTarget(null)}
        />
      )}

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
        <div className="bg-red-500/20 px-3 py-2 text-red-400 text-xs text-center flex-shrink-0 flex items-center justify-center gap-3">
          <span>{error}</span>
          <button
            type="button"
            onClick={() => window.location.reload()}
            className="px-2 py-0.5 bg-red-500/30 hover:bg-red-500/50 rounded text-red-300 text-xs transition"
          >
            새로고침
          </button>
        </div>
      )}

      {/* Messages */}
      <div className="relative flex-1 min-h-0">
        <div
          ref={messagesContainerRef}
          onScroll={handleScroll}
          className="h-full overflow-y-auto px-3 py-3 space-y-2"
        >
          {connecting && messages.length === 0 && (
            <div className="flex items-center justify-center h-full text-gray-500 text-sm">
              <Loader2 size={20} className="animate-spin mr-2 text-purple-500" />
              채팅 연결 중...
            </div>
          )}

          {messages.map((msg, idx) => {
            const prev = messages[idx - 1];
            const showDateDivider = Boolean(
              msg.createdAt &&
                (!prev?.createdAt ||
                  new Date(prev.createdAt).toDateString() !== new Date(msg.createdAt).toDateString())
            );

            if (isSystemMessage(msg)) {
              return (
                <div key={`system-${msg.id ?? idx}`}>
                  {showDateDivider && msg.createdAt && (
                    <div className="flex justify-center py-1">
                      <span className="text-[11px] text-gray-400 bg-gray-800/80 px-2 py-0.5 rounded-full">
                        {formatDateDivider(msg.createdAt)}
                      </span>
                    </div>
                  )}
                  <div className="text-center">
                    <span className="text-xs text-gray-500 bg-gray-800 px-2 py-0.5 rounded-full">
                      {msg.username || `사용자 ${msg.userId}`}님이{' '}
                      {msg.message === 'User joined the chat' ? '입장' : '퇴장'}했습니다
                    </span>
                  </div>
                </div>
              );
            }

            const isOwn = user && msg.userId === user.id;

            return (
              <div key={msg.id ?? `msg-${idx}`}>
                {showDateDivider && msg.createdAt && (
                  <div className="flex justify-center py-1">
                    <span className="text-[11px] text-gray-400 bg-gray-800/80 px-2 py-0.5 rounded-full">
                      {formatDateDivider(msg.createdAt)}
                    </span>
                  </div>
                )}

                <div
                  className={`group flex items-end gap-1.5 ${isOwn ? 'flex-row-reverse' : 'flex-row'}`}
                  onContextMenu={(e) => {
                    if (isOwn) return;
                    e.preventDefault();
                    setReportTarget({
                      userId: msg.userId,
                      nickname: msg.username || `사용자 ${msg.userId}`,
                      chatMessageId: msg.id,
                    });
                  }}
                >
                  {!isOwn && (
                    <div className="w-7 h-7 rounded-full bg-gradient-to-br from-purple-500 to-pink-500 flex items-center justify-center text-xs font-bold flex-shrink-0">
                      {msg.username?.[0] || '?'}
                    </div>
                  )}
                  <div className={`flex flex-col max-w-[75%] ${isOwn ? 'items-end' : 'items-start'}`}>
                    {!isOwn && <span className="text-xs text-gray-400 mb-0.5 ml-1">{msg.username}</span>}
                    <div
                      className={`px-3 py-1.5 rounded-xl text-sm leading-relaxed whitespace-pre-wrap break-words ${
                        isOwn
                          ? 'bg-purple-600 text-white rounded-br-sm'
                          : 'bg-[#2a2a2f] text-gray-100 rounded-bl-sm'
                      }`}
                    >
                      {msg.message}
                    </div>
                    {msg.createdAt && (
                      <span className="text-xs text-gray-500 mt-0.5 mx-1">{formatTime(msg.createdAt)}</span>
                    )}
                    {!isOwn && (
                      <button
                        type="button"
                        onClick={() =>
                          setReportTarget({
                            userId: msg.userId,
                            nickname: msg.username || `사용자 ${msg.userId}`,
                            chatMessageId: msg.id,
                          })
                        }
                        className="text-[11px] text-red-400 opacity-0 group-hover:opacity-100 hover:text-red-300 transition"
                      >
                        신고
                      </button>
                    )}
                  </div>
                </div>
              </div>
            );
          })}

          <div ref={messagesEndRef} />
        </div>

        {showNewMessageButton && newMessageCount > 0 && (
          <button
            type="button"
            onClick={() => scrollToBottom('smooth')}
            className="absolute bottom-3 right-3 bg-neon-purple/90 hover:bg-neon-purple text-white text-xs font-semibold px-3 py-1.5 rounded-full shadow-lg"
          >
            새 메시지 {newMessageCount}개 ↓
          </button>
        )}
      </div>

      {/* Input */}
      <div className="border-t border-gray-700 px-3 py-2.5 flex-shrink-0">
        {!canChat ? (
          <p className="text-center text-xs text-gray-500 py-1">입장 후 채팅 가능합니다</p>
        ) : (
          <div className="relative">
            {showEmojiPicker && (
              <div className="absolute bottom-14 left-0 z-20 grid grid-cols-5 gap-1 rounded-lg border border-gray-700 bg-[#0e0e10] p-2 shadow-xl">
                {EMOJIS.map((emoji) => (
                  <button
                    key={emoji}
                    type="button"
                    onClick={() => appendEmoji(emoji)}
                    className="h-8 w-8 rounded hover:bg-gray-800 text-lg"
                  >
                    {emoji}
                  </button>
                ))}
              </div>
            )}

            <div className="flex items-end gap-2">
              <button
                type="button"
                onClick={() => setShowEmojiPicker((prev) => !prev)}
                disabled={!connected}
                className="px-2.5 py-2 text-lg bg-[#0e0e10] border border-gray-700 rounded-lg hover:border-purple-500 disabled:opacity-50"
                aria-label="이모지 팔레트"
              >
                😊
              </button>

              <textarea
                ref={inputRef}
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={connected ? '메시지 입력... (Enter 전송 / Shift+Enter 줄바꿈)' : '연결 중...'}
                disabled={!connected}
                rows={1}
                className="flex-1 resize-none bg-[#0e0e10] border border-gray-700 rounded-lg px-3 py-2 text-sm text-white placeholder-gray-500 focus:outline-none focus:border-purple-500 disabled:opacity-50 disabled:cursor-not-allowed transition"
              />

              <button
                onClick={sendMessage}
                disabled={!connected || !inputText.trim()}
                className="px-3 py-2 bg-purple-600 hover:bg-purple-500 disabled:bg-gray-700 disabled:cursor-not-allowed text-white rounded-lg transition"
              >
                <Send size={15} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
