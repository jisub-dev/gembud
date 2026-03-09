import { Lock, Users, Gamepad2 } from 'lucide-react';
import type { MouseEvent } from 'react';
import type { Room } from '@/types/room';

interface RoomCardProps {
  room: Room;
  onClick?: (roomPublicId: string) => void;
  showRegenerateInviteButton?: boolean;
  onRegenerateInviteCode?: (roomPublicId: string) => void;
  isMyRoom?: boolean;
}

export function RoomCard({
  room,
  onClick,
  showRegenerateInviteButton,
  onRegenerateInviteCode,
  isMyRoom = false,
}: RoomCardProps) {
  const statusStyles = {
    OPEN: 'border-green-500 hover:shadow-green-500/50',
    FULL: 'border-orange-500 opacity-75 cursor-not-allowed',
    IN_PROGRESS: 'border-purple-500 opacity-75 cursor-not-allowed',
    CLOSED: 'border-gray-500 opacity-50 cursor-not-allowed',
  };

  const statusLabels = {
    OPEN: '모집중',
    FULL: '인원 가득',
    IN_PROGRESS: '게임중',
    CLOSED: '종료',
  };

  const handleClick = () => {
    if (room.status === 'OPEN' && onClick) {
      onClick(room.publicId);
    }
  };

  const handleRegenerateClick = (e: MouseEvent<HTMLButtonElement>) => {
    e.stopPropagation();
    if (onRegenerateInviteCode) {
      onRegenerateInviteCode(room.publicId);
    }
  };

  return (
    <div
      onClick={handleClick}
      className={`
        relative group bg-[#18181b] border-2 rounded-lg p-4 transition overflow-hidden
        ${statusStyles[room.status]}
        ${room.status === 'OPEN' ? 'cursor-pointer hover:scale-105' : ''}
      `}
    >
      {room.status === 'FULL' && (
        <div className="absolute inset-0 bg-black/35 backdrop-blur-[1px] pointer-events-none flex items-center justify-center">
          <span className="rounded-full border border-orange-300/50 bg-orange-500/20 px-3 py-1 text-xs font-bold text-orange-200">
            인원 가득
          </span>
        </div>
      )}

      {room.status === 'OPEN' && isMyRoom && (
        <div className="absolute left-3 bottom-3 z-10 pointer-events-none opacity-0 group-hover:opacity-100 transition-opacity">
          <span className="rounded-full border border-cyan-300/50 bg-cyan-500/20 px-2.5 py-1 text-xs font-semibold text-cyan-200">
            이미 참여 중
          </span>
        </div>
      )}

      {/* Header: Status Badge + Participants */}
      <div className="flex justify-between items-start mb-3">
        <span className={`
          px-2 py-1 rounded text-xs font-bold
          ${room.status === 'OPEN' ? 'bg-green-500/20 text-green-400' : ''}
          ${room.status === 'FULL' ? 'bg-orange-500/20 text-orange-400' : ''}
          ${room.status === 'IN_PROGRESS' ? 'bg-purple-500/20 text-purple-400' : ''}
          ${room.status === 'CLOSED' ? 'bg-gray-500/20 text-gray-400' : ''}
        `}>
          {statusLabels[room.status]}
        </span>
        <span className="flex items-center gap-1 text-sm text-gray-400">
          <Users size={14} />
          {room.currentParticipants}/{room.maxParticipants}
        </span>
      </div>

      {/* Title */}
      <h3 className="text-lg font-bold text-white mb-2 truncate">
        {room.title}
      </h3>

      {/* Game Name */}
      <p className="flex items-center gap-1.5 text-sm text-gray-400 mb-1">
        <Gamepad2 size={14} />
        {room.gameName}
      </p>

      {/* Host */}
      <p className="flex items-center gap-1.5 text-sm text-gray-400 mb-1">
        <Users size={14} />
        {room.createdBy}
      </p>

      {/* Private Badge */}
      {room.isPrivate && (
        <div className="mt-2 flex items-center gap-2">
          <span className="inline-flex items-center gap-1 px-2 py-1 bg-red-500/20 text-red-400 rounded text-xs">
            <Lock size={11} />
            비공개
          </span>
          {showRegenerateInviteButton && (
            <button
              type="button"
              onClick={handleRegenerateClick}
              className="px-2 py-1 text-xs rounded bg-blue-500/20 text-blue-300 hover:bg-blue-500/30 transition"
            >
              초대코드 재발급
            </button>
          )}
        </div>
      )}
    </div>
  );
}
