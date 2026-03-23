import type { ChatRoomInfo } from '@/types/chat';

export function selectChatRoomByPublicId(chatRooms: ChatRoomInfo[], chatPublicId?: string | null): ChatRoomInfo | null {
  if (!chatPublicId) return null;
  return (
    chatRooms.find(
      (chatRoom) => chatRoom.publicId === chatPublicId || String(chatRoom.id) === chatPublicId,
    ) ?? null
  );
}

export function selectRoomChatByPublicId(roomChats: ChatRoomInfo[], chatPublicId?: string | null): ChatRoomInfo | null {
  const roomChat = selectChatRoomByPublicId(roomChats, chatPublicId);
  return roomChat?.type === 'ROOM_CHAT' ? roomChat : null;
}

export function selectRoomChatByRoomId(roomChats: ChatRoomInfo[], roomId?: number | null): ChatRoomInfo | null {
  if (roomId == null) return null;
  return (
    roomChats.find((chatRoom) => chatRoom.type === 'ROOM_CHAT' && chatRoom.relatedRoomId === roomId) ?? null
  );
}
