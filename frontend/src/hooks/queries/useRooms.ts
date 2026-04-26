import { useQuery, useMutation, useQueryClient, type QueryClient } from '@tanstack/react-query';
import { roomService } from '@/services/roomService';
import { chatKeys } from './useChatQueries';
import { roomKeys } from './useRoomQueries';
import type { ChatRoomInfo } from '@/types/chat';
import type { CreateRoomRequest, JoinRoomResult, Room } from '@/types/room';

/**
 * TanStack Query hooks for room-related API calls.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */

/**
 * Hook to fetch rooms by game ID.
 * Auto-refetches every 5 seconds for real-time updates.
 */
export function useRooms(gameId: number) {
  return useQuery({
    queryKey: roomKeys.list(gameId),
    queryFn: () => roomService.getRoomsByGame(gameId),
    refetchInterval: 5000, // 5초마다 자동 갱신 (실시간 느낌)
    staleTime: 3000,
  });
}

/**
 * Hook to fetch a single room by public ID.
 * Auto-refetches every 3 seconds for real-time participant updates.
 */
export function useRoom(roomPublicId: string) {
  return useQuery({
    queryKey: roomKeys.detail(roomPublicId),
    queryFn: () => roomService.getRoom(roomPublicId),
    refetchInterval: 3000,
    staleTime: 2000,
  });
}

export function useMyRooms(options?: {
  enabled?: boolean;
  refetchInterval?: number | false;
  staleTime?: number;
}) {
  return useQuery({
    queryKey: roomKeys.myList(),
    queryFn: roomService.getMyRooms,
    ...options,
  });
}

export function useMyActiveRoom(options?: {
  enabled?: boolean;
  refetchInterval?: number | false;
  staleTime?: number;
}) {
  return useQuery<Room | null>({
    queryKey: roomKeys.myActive(),
    queryFn: roomService.getMyActiveRoom,
    ...options,
  });
}

/**
 * Hook to create a new room.
 * Invalidates room list cache on success.
 */
export function useCreateRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (data: CreateRoomRequest) => roomService.createRoom(data),
    onSuccess: async (newRoom) => {
      queryClient.setQueryData<Room | null>(roomKeys.myActive(), newRoom);
      seedJoinedRoomCaches(queryClient, newRoom);
      await Promise.all([
        queryClient.invalidateQueries({ queryKey: roomKeys.list(newRoom.gameId) }),
        queryClient.invalidateQueries({ queryKey: roomKeys.myList() }),
        queryClient.invalidateQueries({ queryKey: roomKeys.myActive() }),
        queryClient.invalidateQueries({ queryKey: chatKeys.myList() }),
        queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() }),
      ]);
    },
  });
}

/**
 * Hook to join a room.
 * Invalidates room detail cache on success.
 */
export function useJoinRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({
      inviteCode,
      password,
      roomPublicId,
    }: {
      inviteCode?: string;
      password?: string;
      roomPublicId: string;
    }) => roomService.joinRoom(roomPublicId, password, inviteCode),
    onSuccess: async (result: JoinRoomResult) => {
      queryClient.setQueryData<Room | null>(roomKeys.myActive(), result.room);
      seedJoinedRoomCaches(queryClient, result.room);
      await invalidateRoomMutationQueries(queryClient, result.room, {
        includeChatQueries: true,
      });
    },
  });
}

/**
 * Hook to leave a room.
 * Invalidates room detail cache on success.
 */
export function useLeaveRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ room }: { room: RoomCacheTarget }) => roomService.leaveRoom(room.id),
    onSuccess: async (_, { room }) => {
      await syncClientAfterLeavingRoom(queryClient, room);
    },
  });
}

/**
 * Hook to kick a participant (host only).
 */
export function useKickParticipant() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ room, userId }: { room: RoomCacheTarget; userId: number }) =>
      roomService.kickParticipant(room.id, userId),
    onSuccess: async (_, { room, userId }) => {
      patchRoomCaches(queryClient, room, (currentRoom) => ({
        ...currentRoom,
        currentParticipants: Math.max(0, currentRoom.currentParticipants - 1),
        participants: currentRoom.participants?.filter((participant) => participant.userId !== userId),
      }));
      await invalidateRoomMutationQueries(queryClient, room);
    },
  });
}

/**
 * Hook to start a room (host only).
 */
export function useStartRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ room }: { room: RoomCacheTarget }) => roomService.startRoom(room.id),
    onSuccess: async (_, { room }) => {
      patchRoomCaches(queryClient, room, (currentRoom) => ({
        ...currentRoom,
        status: 'IN_PROGRESS',
      }));
      await invalidateRoomMutationQueries(queryClient, room);
    },
  });
}

/**
 * Hook to transfer host to another participant (host only).
 */
export function useTransferHost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ room, userId }: { room: RoomCacheTarget; userId: number }) =>
      roomService.transferHost(room.id, userId),
    onSuccess: async (_, { room, userId }) => {
      patchRoomCaches(queryClient, room, (currentRoom) => ({
        ...currentRoom,
        participants: currentRoom.participants?.map((participant) => ({
          ...participant,
          isHost: participant.userId === userId,
        })),
      }));
      await invalidateRoomMutationQueries(queryClient, room);
    },
  });
}

export function useResetRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ room }: { room: RoomCacheTarget }) => roomService.resetRoom(room.publicId),
    onSuccess: async (_, { room }) => {
      patchRoomCaches(queryClient, room, (currentRoom) => ({
        ...currentRoom,
        status: 'OPEN',
      }));
      await invalidateRoomMutationQueries(queryClient, room);
    },
  });
}

export function useRegenerateInviteCode() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ room }: { room: RoomCacheTarget }) => roomService.regenerateInviteCode(room.publicId),
    onSuccess: async (updatedRoom) => {
      queryClient.setQueryData<Room | null>(roomKeys.myActive(), (currentRoom) =>
        currentRoom && isSameRoom(currentRoom, updatedRoom) ? updatedRoom : currentRoom,
      );
      seedJoinedRoomCaches(queryClient, updatedRoom);
      await invalidateRoomMutationQueries(queryClient, updatedRoom);
    },
  });
}

export async function syncClientAfterLeavingRoom(queryClient: QueryClient, room: RoomCacheTarget) {
  patchRoomCaches(queryClient, room, () => null);
  queryClient.setQueryData<ChatRoomInfo[] | undefined>(chatKeys.myRoomChats(), (currentChats: ChatRoomInfo[] | undefined) => {
    if (!currentChats) return currentChats;
    return currentChats.filter((chat) => chat.relatedRoomId !== room.id);
  });
  queryClient.setQueryData<ChatRoomInfo[] | undefined>(chatKeys.myList(), (currentChats: ChatRoomInfo[] | undefined) => {
    if (!currentChats) return currentChats;
    return currentChats.filter(
      (chat) => !(chat.type === 'ROOM_CHAT' && chat.relatedRoomId === room.id),
    );
  });

  const invalidations: Promise<unknown>[] = [
    queryClient.invalidateQueries({ queryKey: roomKeys.lists() }),
    queryClient.invalidateQueries({ queryKey: roomKeys.myList() }),
    queryClient.invalidateQueries({ queryKey: roomKeys.myActive() }),
    queryClient.invalidateQueries({ queryKey: chatKeys.myList() }),
    queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() }),
    queryClient.invalidateQueries({ queryKey: roomKeys.detail(room.id) }),
    queryClient.invalidateQueries({ queryKey: roomKeys.detail(room.publicId) }),
    queryClient.invalidateQueries({ queryKey: roomKeys.list(room.gameId) }),
  ];

  await Promise.all(invalidations);
}

type RoomCacheTarget = Pick<Room, 'gameId' | 'id' | 'publicId'>;

function invalidateRoomMutationQueries(
  queryClient: QueryClient,
  room: RoomCacheTarget,
  options?: { includeChatQueries?: boolean },
) {
  const invalidations: Promise<unknown>[] = [
    queryClient.invalidateQueries({ queryKey: roomKeys.detail(room.id) }),
    queryClient.invalidateQueries({ queryKey: roomKeys.detail(room.publicId) }),
    queryClient.invalidateQueries({ queryKey: roomKeys.list(room.gameId) }),
    queryClient.invalidateQueries({ queryKey: roomKeys.lists() }),
    queryClient.invalidateQueries({ queryKey: roomKeys.myList() }),
    queryClient.invalidateQueries({ queryKey: roomKeys.myActive() }),
  ];

  if (options?.includeChatQueries) {
    invalidations.push(
      queryClient.invalidateQueries({ queryKey: chatKeys.myList() }),
      queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() }),
    );
  }

  return Promise.all(invalidations);
}

function seedJoinedRoomCaches(queryClient: QueryClient, room: Room) {
  queryClient.setQueryData<Room | undefined>(roomKeys.detail(room.id), room);
  queryClient.setQueryData<Room | undefined>(roomKeys.detail(room.publicId), room);
  queryClient.setQueryData<Room[] | undefined>(roomKeys.myList(), (currentRooms) =>
    upsertRoom(currentRooms, room),
  );
  queryClient.setQueryData<Room[] | undefined>(roomKeys.list(room.gameId), (currentRooms) =>
    upsertRoom(currentRooms, room),
  );
}

function patchRoomCaches(
  queryClient: QueryClient,
  room: RoomCacheTarget,
  updater: (currentRoom: Room) => Room | null,
) {
  queryClient.setQueryData<Room | null>(roomKeys.myActive(), (currentRoom) => {
    if (!currentRoom || !isSameRoom(currentRoom, room)) {
      return currentRoom;
    }
    return updater(currentRoom);
  });

  queryClient.setQueryData<Room[] | undefined>(roomKeys.myList(), (currentRooms) =>
    updateRoomCollection(currentRooms, room, updater),
  );
  queryClient.setQueryData<Room[] | undefined>(roomKeys.list(room.gameId), (currentRooms) =>
    updateRoomCollection(currentRooms, room, updater),
  );
  queryClient.setQueryData<Room | undefined>(roomKeys.detail(room.id), (currentRoom) => {
    if (!currentRoom || !isSameRoom(currentRoom, room)) {
      return currentRoom;
    }
    return updater(currentRoom) ?? undefined;
  });
  queryClient.setQueryData<Room | undefined>(roomKeys.detail(room.publicId), (currentRoom) => {
    if (!currentRoom || !isSameRoom(currentRoom, room)) {
      return currentRoom;
    }
    return updater(currentRoom) ?? undefined;
  });
}

function updateRoomCollection(
  currentRooms: Room[] | undefined,
  room: RoomCacheTarget,
  updater: (currentRoom: Room) => Room | null,
) {
  if (!currentRooms) {
    return currentRooms;
  }

  let didUpdate = false;
  const nextRooms = currentRooms.flatMap((currentRoom) => {
    if (!isSameRoom(currentRoom, room)) {
      return [currentRoom];
    }

    didUpdate = true;
    const updatedRoom = updater(currentRoom);
    return updatedRoom ? [updatedRoom] : [];
  });

  return didUpdate ? nextRooms : currentRooms;
}

function upsertRoom(currentRooms: Room[] | undefined, nextRoom: Room) {
  if (!currentRooms) {
    return currentRooms;
  }

  const roomIndex = currentRooms.findIndex((currentRoom) => isSameRoom(currentRoom, nextRoom));
  if (roomIndex === -1) {
    return [nextRoom, ...currentRooms];
  }

  return currentRooms.map((currentRoom, index) =>
    index === roomIndex ? { ...currentRoom, ...nextRoom } : currentRoom,
  );
}

function isSameRoom(currentRoom: Pick<Room, 'id' | 'publicId'>, room: Pick<Room, 'id' | 'publicId'>) {
  return currentRoom.id === room.id || currentRoom.publicId === room.publicId;
}
