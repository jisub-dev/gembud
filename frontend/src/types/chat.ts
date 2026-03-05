export interface ChatMessage {
  id: number;
  chatRoomId: number;
  userId: number;
  username: string;
  message: string;
  createdAt: string;
}

export interface CreateDirectChatRequest {
  userId: number;
}

export interface CreateGroupChatRequest {
  name: string;
  memberIds: number[];
}

export interface ChatRoom {
  id: number;
  type: 'ROOM_CHAT' | 'DIRECT_CHAT' | 'GROUP_CHAT';
  name?: string;
  memberCount: number;
}

export interface ChatRoomInfo {
  id: number;
  type: 'ROOM_CHAT' | 'DIRECT_CHAT' | 'GROUP_CHAT';
  name?: string;
  relatedRoomId?: number;
  relatedRoomTitle?: string;
}
