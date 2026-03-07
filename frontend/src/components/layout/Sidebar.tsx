import { Link, useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { ChevronDown, ChevronLeft, Users, MessageSquare, User } from 'lucide-react';
import { useQuery } from '@tanstack/react-query';
import { roomService } from '@/services/roomService';
import { chatService } from '@/services/chatService';
import { useAuthStore } from '@/store/authStore';
import { useGames } from '@/hooks/queries/useGames';
import { useFriends } from '@/hooks/queries/useFriends';
import type { Room } from '@/types/room';
import type { ChatRoomInfo } from '@/types/chat';

const STATUS_COLORS: Record<string, string> = {
  OPEN: 'bg-neon-green',
  FULL: 'bg-yellow-500',
  IN_PROGRESS: 'bg-neon-cyan',
  CLOSED: 'bg-gray-500',
};

const CHAT_TYPE_LABELS: Record<string, string> = {
  ROOM_CHAT: '방 채팅',
  GROUP_CHAT: '그룹',
  DIRECT_CHAT: 'DM',
};

function SectionHeader({
  title,
  isOpen,
  onToggle,
  count,
}: {
  title: string;
  isOpen: boolean;
  onToggle: () => void;
  count?: number;
}) {
  return (
    <button
      onClick={onToggle}
      className="w-full flex items-center justify-between px-4 py-2 text-xs font-gaming uppercase tracking-wider text-text-muted hover:text-text-secondary transition-colors"
    >
      <span className="flex items-center gap-2">
        <ChevronDown
          size={12}
          className={`transition-transform ${isOpen ? '' : '-rotate-90'}`}
        />
        {title}
      </span>
      {count !== undefined && count > 0 && (
        <span className="bg-neon-purple/30 text-neon-purple text-xs px-1.5 py-0.5 rounded-full">
          {count}
        </span>
      )}
    </button>
  );
}

export default function Sidebar() {
  const { isAuthenticated } = useAuthStore();
  const navigate = useNavigate();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [roomsOpen, setRoomsOpen] = useState(true);
  const [chatsOpen, setChatsOpen] = useState(true);
  const [friendsOpen, setFriendsOpen] = useState(true);
  const [gamesOpen, setGamesOpen] = useState(true);
  const [isOpeningRoomChat, setIsOpeningRoomChat] = useState(false);

  const { data: games = [] } = useGames();

  const { data: myRooms = [] } = useQuery<Room[]>({
    queryKey: ['myRooms'],
    queryFn: roomService.getMyRooms,
    enabled: isAuthenticated,
    refetchInterval: 15000,
  });

  const { data: myRoomChatRooms = [] } = useQuery<ChatRoomInfo[]>({
    queryKey: ['myRoomChatRooms'],
    queryFn: () => chatService.getMyChatRooms('ROOM_CHAT'),
    enabled: isAuthenticated,
    refetchInterval: 15000,
  });

  const { data: myChatRooms = [] } = useQuery<ChatRoomInfo[]>({
    queryKey: ['myChatRooms'],
    queryFn: () => chatService.getMyChatRooms(),
    enabled: isAuthenticated,
    refetchInterval: 15000,
  });
  const { data: friends = [] } = useFriends();

  const myWaitingRoomChat = myRoomChatRooms[0] ?? null;

  const handleMyWaitingRoomClick = async () => {
    if (isOpeningRoomChat) return;
    setIsOpeningRoomChat(true);
    try {
      const roomChats = await chatService.getMyChatRooms('ROOM_CHAT');
      const roomChat = roomChats[0];
      if (roomChat) {
        navigate(`/chat/${roomChat.id}`);
        return;
      }

      const fallbackGameId = myRooms.find((room) => room.id === myWaitingRoomChat?.relatedRoomId)?.gameId;
      navigate(fallbackGameId ? `/games/${fallbackGameId}/rooms` : '/');
    } catch {
      const fallbackGameId = myRooms.find((room) => room.id === myWaitingRoomChat?.relatedRoomId)?.gameId;
      navigate(fallbackGameId ? `/games/${fallbackGameId}/rooms` : '/');
    } finally {
      setIsOpeningRoomChat(false);
    }
  };

  return (
    <aside
      className={`${
        isCollapsed ? 'w-16' : 'w-64'
      } bg-dark-secondary border-r border-neon-purple/20 min-h-[calc(100vh-4rem)] transition-all duration-300 flex flex-col overflow-hidden`}
    >
      {/* Collapse Toggle */}
      <button
        onClick={() => setIsCollapsed(!isCollapsed)}
        className="p-4 hover:bg-dark-tertiary transition-colors flex items-center justify-center group flex-shrink-0"
      >
        <ChevronLeft
          size={20}
          className={`text-text-secondary group-hover:text-neon-purple transition-all ${
            isCollapsed ? 'rotate-180' : ''
          }`}
        />
      </button>

      <div className="flex-1 overflow-y-auto scrollbar-thin scrollbar-thumb-neon-purple/20 scrollbar-track-transparent">

        {/* 내 대기방 섹션 */}
        {!isCollapsed && isAuthenticated && (
          <div className="mb-1">
            <SectionHeader
              title="내 대기방"
              isOpen={roomsOpen}
              onToggle={() => setRoomsOpen(!roomsOpen)}
              count={myWaitingRoomChat ? 1 : 0}
            />
            {roomsOpen && (
              <div className="px-2 pb-1 space-y-0.5">
                {!myWaitingRoomChat ? (
                  <p className="px-3 py-2 text-xs text-text-muted italic">대기방 없음</p>
                ) : (
                  <button
                    type="button"
                    onClick={handleMyWaitingRoomClick}
                    disabled={isOpeningRoomChat}
                    className="w-full text-left flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-dark-tertiary transition-all group disabled:opacity-60"
                  >
                    <div className="relative flex-shrink-0">
                      <div className="w-8 h-8 bg-dark-tertiary rounded-md flex items-center justify-center">
                        <Users size={16} className="text-neon-purple" />
                      </div>
                      <span className={`absolute -bottom-0.5 -right-0.5 w-2.5 h-2.5 rounded-full border-2 border-dark-secondary ${STATUS_COLORS.OPEN}`} />
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="text-xs font-medium text-text-primary truncate group-hover:text-neon-purple transition-colors">
                        {myWaitingRoomChat.relatedRoomTitle ?? myWaitingRoomChat.name ?? '내 대기방'}
                      </p>
                      <p className="text-xs text-text-muted">ROOM_CHAT</p>
                    </div>
                  </button>
                )}
              </div>
            )}
          </div>
        )}

        {/* 채팅방 섹션: ROOM_CHAT 제외, 남은 항목 없으면 숨김 */}
        {!isCollapsed && isAuthenticated && (() => {
          const filteredChatRooms = myChatRooms.filter(c => c.type !== 'ROOM_CHAT');
          if (filteredChatRooms.length === 0) return null;
          return (
            <div className="mb-1">
              <SectionHeader
                title="채팅방"
                isOpen={chatsOpen}
                onToggle={() => setChatsOpen(!chatsOpen)}
                count={filteredChatRooms.length}
              />
              {chatsOpen && (
                <div className="px-2 pb-1 space-y-0.5">
                  {filteredChatRooms.map((chat) => (
                    <Link
                      key={chat.id}
                      to={`/chat/${chat.id}`}
                      className="flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-dark-tertiary transition-all group"
                    >
                      <div className="w-8 h-8 bg-dark-tertiary rounded-md flex items-center justify-center flex-shrink-0">
                        {chat.type === 'DIRECT_CHAT' ? (
                          <User size={16} className="text-neon-cyan" />
                        ) : (
                          <MessageSquare size={16} className="text-neon-cyan" />
                        )}
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-text-primary truncate group-hover:text-neon-cyan transition-colors">
                          {chat.name ?? `채팅 #${chat.id}`}
                        </p>
                        <p className="text-xs text-text-muted">{CHAT_TYPE_LABELS[chat.type]}</p>
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </div>
          );
        })()}

        {/* 친구 섹션 */}
        {!isCollapsed && isAuthenticated && (
          <div className="mb-1">
            <SectionHeader
              title="친구"
              isOpen={friendsOpen}
              onToggle={() => setFriendsOpen(!friendsOpen)}
              count={friends.length}
            />
            {friendsOpen && (
              <div className="px-2 pb-1 space-y-0.5">
                {friends.length === 0 ? (
                  <p className="px-3 py-2 text-xs text-text-muted italic">친구 없음</p>
                ) : (
                  friends.map((friend) => (
                    <button
                      type="button"
                      key={friend.id}
                      onClick={() => navigate('/friends')}
                      className="w-full text-left flex items-center gap-2 px-3 py-2 rounded-lg hover:bg-dark-tertiary transition-all group"
                    >
                      <div className="w-8 h-8 bg-dark-tertiary rounded-md flex items-center justify-center flex-shrink-0">
                        <User size={16} className="text-neon-pink" />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-xs font-medium text-text-primary truncate group-hover:text-neon-pink transition-colors">
                          {friend.friendNickname}
                        </p>
                      </div>
                    </button>
                  ))
                )}
              </div>
            )}
          </div>
        )}

        {/* 구분선 */}
        {!isCollapsed && isAuthenticated && (
          <div className="mx-4 my-2 border-t border-neon-purple/10" />
        )}

        {/* 게임 카테고리 섹션 */}
        <div className="mb-2">
          {!isCollapsed && (
            <SectionHeader
              title="게임 카테고리"
              isOpen={gamesOpen}
              onToggle={() => setGamesOpen(!gamesOpen)}
            />
          )}
          {(gamesOpen || isCollapsed) && (
            <div className="space-y-0.5 px-2">
              {games.map((game) => {
                const fallback = game.name.slice(0, 3).toUpperCase();
                return (
                  <Link
                    key={game.id}
                    to={`/games/${game.id}/rooms`}
                    className="flex items-center space-x-3 px-3 py-2.5 rounded-lg hover:bg-dark-tertiary transition-all group relative overflow-hidden"
                  >
                    <div className="w-9 h-9 bg-dark-tertiary rounded-lg flex items-center justify-center overflow-hidden transform group-hover:scale-110 transition-transform duration-200 flex-shrink-0 border border-white/5">
                      {game.imageUrl ? (
                        <img
                          src={game.imageUrl}
                          alt={game.name}
                          className="w-full h-full object-cover"
                          onError={(e) => {
                            const t = e.currentTarget;
                            t.style.display = 'none';
                            (t.nextElementSibling as HTMLElement)?.style.setProperty('display', 'flex');
                          }}
                        />
                      ) : null}
                      <span
                        className="text-xs text-text-muted font-bold items-center justify-center"
                        style={{ display: game.imageUrl ? 'none' : 'flex' }}
                      >
                        {fallback}
                      </span>
                    </div>
                    {!isCollapsed && (
                      <p className="text-sm font-medium text-text-primary truncate group-hover:text-neon-purple transition-colors flex-1 min-w-0">
                        {game.name}
                      </p>
                    )}
                    <div className="absolute inset-0 bg-gradient-to-r from-neon-purple/0 via-neon-purple/5 to-neon-purple/0 opacity-0 group-hover:opacity-100 transition-opacity pointer-events-none" />
                  </Link>
                );
              })}
            </div>
          )}
        </div>
      </div>
    </aside>
  );
}
