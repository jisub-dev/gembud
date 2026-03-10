export interface ParticipantInfo {
  userId: number;
  nickname: string;
  isHost: boolean;
}

export interface Room {
  id: number;
  publicId: string;
  title: string;
  description: string;
  gameId: number;
  gameName: string;
  maxParticipants: number;
  currentParticipants: number;
  isPrivate: boolean;
  status: 'OPEN' | 'FULL' | 'IN_PROGRESS' | 'CLOSED';
  createdBy: string;  // 호스트 닉네임 (백엔드와 일치)
  createdAt: string;
  participants?: ParticipantInfo[];
  filters?: Record<string, string>;
  inviteCode?: string;
  inviteCodeExpiresAt?: string;
}

export interface JoinRoomResult {
  room: Room;
  chatRoomId: number;
}

export interface CreateRoomRequest {
  gameId: number;
  title: string;
  description: string;
  maxParticipants: number;
  isPrivate: boolean;
  password?: string;
  tierOptionIds?: number[];
  positionOptionIds?: number[];
}

export interface RecommendedRoom {
  roomId: number;
  matchingScore: number;
  hostTemperature?: number;
  reason: string;
  room: Room;
}
