import { UserX, Crown } from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { ParticipantInfo } from '@/types/room';

interface RoomParticipantsProps {
  participants?: ParticipantInfo[];
  maxParticipants: number;
  currentUserId?: number;
  isCurrentUserHost?: boolean;
  onKick?: (userId: number, nickname: string) => void;
  onTransferHost?: (userId: number, nickname: string) => void;
}

export function RoomParticipants({
  participants = [],
  maxParticipants,
  currentUserId,
  isCurrentUserHost = false,
  onKick,
  onTransferHost,
}: RoomParticipantsProps) {
  const emptySlots = Math.max(0, maxParticipants - participants.length);
  const slots = [...participants, ...Array(emptySlots).fill(null)];

  return (
    <div className="space-y-3">
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {slots.map((participant, index) => (
          <ParticipantSlot
            key={participant ? participant.userId : `empty-${index}`}
            participant={participant}
            slotNumber={index + 1}
            currentUserId={currentUserId}
            isCurrentUserHost={isCurrentUserHost}
            onKick={onKick}
            onTransferHost={onTransferHost}
          />
        ))}
      </div>

      <div className="pt-3 border-t border-gray-700 text-center">
        <p className="text-sm text-text-secondary">
          <span className="text-neon-cyan font-gaming">{participants.length}</span>
          {' / '}
          <span className="text-text-primary font-gaming">{maxParticipants}</span>
          {' 명'}
        </p>
      </div>
    </div>
  );
}

interface ParticipantSlotProps {
  participant: ParticipantInfo | null;
  slotNumber: number;
  currentUserId?: number;
  isCurrentUserHost?: boolean;
  onKick?: (userId: number, nickname: string) => void;
  onTransferHost?: (userId: number, nickname: string) => void;
}

function ParticipantSlot({ participant, slotNumber, currentUserId, isCurrentUserHost, onKick, onTransferHost }: ParticipantSlotProps) {
  const navigate = useNavigate();

  if (!participant) {
    return (
      <div className="flex items-center gap-3 p-3 bg-dark-tertiary border border-gray-700 rounded-lg opacity-40">
        <div className="w-10 h-10 rounded-full bg-dark-secondary border-2 border-gray-700 flex items-center justify-center">
          <span className="text-lg text-text-muted">{slotNumber}</span>
        </div>
        <div className="flex-1">
          <p className="text-sm text-text-muted">빈 자리</p>
        </div>
      </div>
    );
  }

  const isMe = currentUserId === participant.userId;
  const canKick = isCurrentUserHost && !participant.isHost && !isMe && onKick;
  const canTransfer = isCurrentUserHost && !participant.isHost && !isMe && onTransferHost;

  return (
    <div className="flex items-center gap-3 p-3 bg-dark-tertiary border-2 border-neon-purple/30 rounded-lg hover:border-neon-purple/50 transition-all group">
      {/* Avatar */}
      <div className={`w-10 h-10 rounded-full flex items-center justify-center ring-2 flex-shrink-0 ${
        participant.isHost
          ? 'bg-gradient-to-br from-neon-purple to-neon-pink ring-neon-purple'
          : 'bg-gradient-to-br from-neon-cyan to-neon-purple ring-neon-cyan/50'
      }`}>
        <span className="text-sm font-bold text-white">
          {participant.nickname[0].toUpperCase()}
        </span>
      </div>

      {/* Info */}
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2">
          <button
            type="button"
            onClick={() => navigate(`/profile/${participant.userId}`)}
            className="text-sm font-medium text-text-primary truncate hover:text-neon-cyan transition-colors"
          >
            {participant.nickname}
            {isMe && <span className="text-xs text-gray-400 ml-1">(나)</span>}
          </button>
          {participant.isHost && (
            <span className="px-2 py-0.5 bg-gradient-to-r from-neon-purple to-neon-pink rounded text-xs font-gaming font-bold flex-shrink-0">
              HOST
            </span>
          )}
        </div>
      </div>

      {/* Action buttons (방장에게만, 자신 제외) */}
      {(canKick || canTransfer) ? (
        <div className="flex gap-1 opacity-0 group-hover:opacity-100 transition">
          {canTransfer && (
            <button
              onClick={() => onTransferHost(participant.userId, participant.nickname)}
              className="p-1.5 text-gray-500 hover:text-yellow-400 hover:bg-yellow-400/10 rounded transition"
              title="방장 넘기기"
            >
              <Crown size={15} />
            </button>
          )}
          {canKick && (
            <button
              onClick={() => onKick(participant.userId, participant.nickname)}
              className="p-1.5 text-gray-500 hover:text-red-400 hover:bg-red-400/10 rounded transition"
              title="강퇴"
            >
              <UserX size={15} />
            </button>
          )}
        </div>
      ) : (
        <div className="w-6 h-6 rounded-full bg-dark-secondary border border-neon-purple/30 flex items-center justify-center flex-shrink-0">
          <span className="text-xs text-text-secondary font-gaming">{slotNumber}</span>
        </div>
      )}
    </div>
  );
}
