export interface ChatMessage {
  id: number;
  chatRoomId: number;
  senderId: number;
  senderNickname: string;
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
