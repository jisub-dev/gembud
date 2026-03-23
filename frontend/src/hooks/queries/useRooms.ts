import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { roomService } from '@/services/roomService';
import { chatKeys } from './useChatQueries';
import { roomKeys } from './useRoomQueries';
import type { CreateRoomRequest, Room } from '@/types/room';

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
    onSuccess: (newRoom) => {
      queryClient.invalidateQueries({ queryKey: roomKeys.list(newRoom.gameId) });
      queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
      queryClient.invalidateQueries({ queryKey: chatKeys.myList() });
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
    mutationFn: ({ roomPublicId, password }: { roomPublicId: string; password?: string }) =>
      roomService.joinRoom(roomPublicId, password),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: roomKeys.detail(variables.roomPublicId) });
      queryClient.invalidateQueries({ queryKey: roomKeys.lists() });
      queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
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
    mutationFn: (roomId: number) => roomService.leaveRoom(roomId),
    onSuccess: (_, roomId) => {
      queryClient.invalidateQueries({ queryKey: roomKeys.detail(roomId) });
      queryClient.invalidateQueries({ queryKey: roomKeys.lists() });
      queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
      queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
    },
  });
}

/**
 * Hook to kick a participant (host only).
 */
export function useKickParticipant() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roomId, userId }: { roomId: number; userId: number }) =>
      roomService.kickParticipant(roomId, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: roomKeys.detail(variables.roomId) });
      queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
    },
  });
}

/**
 * Hook to start a room (host only).
 */
export function useStartRoom() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (roomId: number) => roomService.startRoom(roomId),
    onSuccess: (_, roomId) => {
      queryClient.invalidateQueries({ queryKey: roomKeys.detail(roomId) });
      queryClient.invalidateQueries({ queryKey: roomKeys.lists() });
      queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
    },
  });
}

/**
 * Hook to transfer host to another participant (host only).
 */
export function useTransferHost() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roomId, userId }: { roomId: number; userId: number }) =>
      roomService.transferHost(roomId, userId),
    onSuccess: (_, variables) => {
      queryClient.invalidateQueries({ queryKey: roomKeys.detail(variables.roomId) });
      queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
    },
  });
}
