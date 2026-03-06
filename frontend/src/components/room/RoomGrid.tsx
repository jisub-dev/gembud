import { Gamepad2 } from 'lucide-react';
import type { Room } from '@/types/room';
import { RoomCard } from './RoomCard';

interface RoomGridProps {
  rooms: Room[];
  isLoading?: boolean;
  onRoomClick?: (roomPublicId: string) => void;
}

export function RoomGrid({ rooms, isLoading, onRoomClick }: RoomGridProps) {
  if (isLoading) {
    return (
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {[...Array(6)].map((_, index) => (
          <div
            key={index}
            className="bg-[#18181b] border-2 border-gray-700 rounded-lg p-4 animate-pulse"
          >
            <div className="flex justify-between items-start mb-3">
              <div className="h-6 w-16 bg-gray-700 rounded" />
              <div className="h-4 w-12 bg-gray-700 rounded" />
            </div>
            <div className="h-6 bg-gray-700 rounded mb-2" />
            <div className="h-4 bg-gray-700 rounded mb-1" />
            <div className="h-4 bg-gray-700 rounded w-2/3" />
          </div>
        ))}
      </div>
    );
  }

  if (!rooms || rooms.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-12 text-center">
        <Gamepad2 size={56} className="text-gray-600 mb-4" />
        <h3 className="text-xl font-bold text-white mb-2">방이 없습니다</h3>
        <p className="text-gray-400">첫 번째 방을 만들어보세요!</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
      {rooms.map((room) => (
        <RoomCard key={room.id} room={room} onClick={onRoomClick} />
      ))}
    </div>
  );
}
