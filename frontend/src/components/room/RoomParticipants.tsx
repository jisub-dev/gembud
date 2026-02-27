import { ParticipantInfo } from '@/types/room';

interface RoomParticipantsProps {
  participants?: ParticipantInfo[];
  maxParticipants: number;
}

/**
 * Room participants list component showing all current participants.
 *
 * @author Gembud Team
 * @since 2026-02-27
 */
export function RoomParticipants({ participants = [], maxParticipants }: RoomParticipantsProps) {
  // Create empty slots for UI
  const emptySlots = Math.max(0, maxParticipants - participants.length);
  const slots = [...participants, ...Array(emptySlots).fill(null)];

  return (
    <div className="space-y-3">
      {/* Participants Grid */}
      <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
        {slots.map((participant, index) => (
          <ParticipantSlot
            key={participant ? participant.userId : `empty-${index}`}
            participant={participant}
            slotNumber={index + 1}
          />
        ))}
      </div>

      {/* Summary */}
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
}

function ParticipantSlot({ participant, slotNumber }: ParticipantSlotProps) {
  if (!participant) {
    // Empty slot
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

  // Filled slot
  return (
    <div className="flex items-center gap-3 p-3 bg-dark-tertiary border-2 border-neon-purple/30 rounded-lg hover:border-neon-purple/50 transition-all group">
      {/* Avatar */}
      <div className={`w-10 h-10 rounded-full flex items-center justify-center ring-2 ${
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
          <p className="text-sm font-medium text-text-primary truncate">
            {participant.nickname}
          </p>
          {participant.isHost && (
            <span className="px-2 py-0.5 bg-gradient-to-r from-neon-purple to-neon-pink rounded text-xs font-gaming font-bold">
              HOST
            </span>
          )}
        </div>
      </div>

      {/* Slot number badge */}
      <div className="w-6 h-6 rounded-full bg-dark-secondary border border-neon-purple/30 flex items-center justify-center">
        <span className="text-xs text-text-secondary font-gaming">{slotNumber}</span>
      </div>
    </div>
  );
}
