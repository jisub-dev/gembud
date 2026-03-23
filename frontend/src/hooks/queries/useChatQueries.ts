import { useQuery } from '@tanstack/react-query';
import { chatService } from '@/services/chatService';
import type { ChatRoomInfo } from '@/types/chat';

export const chatKeys = {
  all: ['chat'] as const,
  my: () => [...chatKeys.all, 'my'] as const,
  myList: () => [...chatKeys.my(), 'list'] as const,
  myRoomChats: () => [...chatKeys.my(), 'roomChats'] as const,
};

export function useMyChatRooms(options?: {
  enabled?: boolean;
  refetchInterval?: number | false;
  staleTime?: number;
}) {
  return useQuery<ChatRoomInfo[]>({
    queryKey: chatKeys.myList(),
    queryFn: () => chatService.getMyChatRooms(),
    ...options,
  });
}

export function useMyRoomChatRooms(options?: {
  enabled?: boolean;
  refetchInterval?: number | false;
  staleTime?: number;
}) {
  return useQuery<ChatRoomInfo[]>({
    queryKey: chatKeys.myRoomChats(),
    queryFn: () => chatService.getMyChatRooms('ROOM_CHAT'),
    ...options,
  });
}
