import { useState } from 'react';
import type { QueryClient } from '@tanstack/react-query';
import type { NavigateFunction } from 'react-router-dom';
import { chatKeys } from '@/hooks/queries/useChatQueries';
import { roomKeys } from '@/hooks/queries/useRoomQueries';
import { markRecommendedRoomActive } from '@/hooks/useRoomRecommendations';
import { chatService } from '@/services/chatService';
import { roomService } from '@/services/roomService';
import type { JoinRoomResult, Room } from '@/types/room';

interface ToastHandlers {
  error: (message: string) => void;
  success: (message: string) => void;
}

interface UseRoomJoinFlowParams {
  activeRoom: Room | null;
  gameId: number;
  myRooms: Room[];
  navigate: NavigateFunction;
  onInviteExpired: (inviteCode: string, room: Room) => void;
  onInviteMissing: (inviteCode: string, room: Room) => void;
  queryClient: QueryClient;
  toast: ToastHandlers;
}

interface JoinRoomOptions {
  markAsRecommended?: boolean;
  skipActiveRoomPrompt?: boolean;
}

export function useRoomJoinFlow({
  activeRoom,
  gameId,
  myRooms,
  navigate,
  onInviteExpired,
  onInviteMissing,
  queryClient,
  toast,
}: UseRoomJoinFlowParams) {
  const [joiningRoom, setJoiningRoom] = useState<Room | null>(null);
  const [joiningInviteCode, setJoiningInviteCode] = useState<string | undefined>(undefined);
  const [isJoining, setIsJoining] = useState(false);
  const [showPasswordModal, setShowPasswordModal] = useState(false);

  const resetJoinState = () => {
    setShowPasswordModal(false);
    setJoiningRoom(null);
    setJoiningInviteCode(undefined);
  };

  const openInviteEntry = (room: Room, inviteCode?: string) => {
    setJoiningRoom(room);
    setJoiningInviteCode(inviteCode);
    setShowPasswordModal(true);
  };

  const joinRoom = async (
    room: Room,
    password?: string,
    inviteCode?: string,
    options?: JoinRoomOptions,
  ) => {
    setIsJoining(true);

    try {
      const result = await roomService.joinRoom(room.publicId, password, inviteCode);
      handleJoinSuccess({
        gameId,
        navigate,
        options,
        result,
        room,
      });
      resetJoinState();
    } catch (err: unknown) {
      const errorCode = extractErrorCode(err);

      if (errorCode === 'ROOM006' || errorCode === 'ROOM012') {
        if (errorCode === 'ROOM012') {
          toast.error('초대 코드가 유효하지 않거나 만료되었습니다');
          if (inviteCode) {
            onInviteExpired(inviteCode, room);
            resetJoinState();
          }
        } else {
          toast.error('비밀번호가 올바르지 않습니다');
        }
      } else if (errorCode === 'ROOM001') {
        toast.error('방을 찾을 수 없습니다');
        if (inviteCode) {
          onInviteMissing(inviteCode, room);
        }
        resetJoinState();
        await queryClient.invalidateQueries({ queryKey: roomKeys.list(gameId) });
        navigate(`/games/${gameId}/rooms`);
      } else if (errorCode === 'ROOM002') {
        toast.error('방이 꽉 찼습니다');
        resetJoinState();
      } else if (errorCode === 'ROOM010') {
        toast.error('이미 게임이 시작된 방입니다');
        resetJoinState();
      } else if (errorCode === 'ROOM008') {
        const resolved = await resolveAlreadyInOtherRoom({
          activeRoom,
          gameId,
          inviteCode,
          options,
          password,
          queryClient,
          retryJoin: joinRoom,
          room,
          toast,
        });

        if (!resolved) {
          resetJoinState();
        }
      } else {
        toast.error('방 입장에 실패했습니다');
        resetJoinState();
      }
    } finally {
      setIsJoining(false);
    }
  };

  const confirmPasswordJoin = async (password?: string) => {
    if (!joiningRoom) {
      return;
    }

    await joinRoom(joiningRoom, password, joiningInviteCode);
  };

  const requestRoomEntry = async (room: Room) => {
    const alreadyIn = myRooms.find((joinedRoom) => joinedRoom.publicId === room.publicId);
    if (alreadyIn) {
      try {
        const chatPublicId = await chatService.getChatRoomByGameRoom(alreadyIn.id);
        navigate(`/chat/${chatPublicId}`);
        return;
      } catch {
        // Fall through to the regular join flow if the mapping lookup fails.
      }
    }

    if (room.isPrivate) {
      openInviteEntry(room);
      return;
    }

    await joinRoom(room);
  };

  return {
    confirmPasswordJoin,
    isJoining,
    joinRoom,
    joiningInviteCode,
    joiningRoom,
    openInviteEntry,
    requestRoomEntry,
    resetJoinState,
    showPasswordModal,
  };
}

function handleJoinSuccess({
  gameId,
  navigate,
  options,
  result,
  room,
}: {
  gameId: number;
  navigate: NavigateFunction;
  options?: JoinRoomOptions;
  result: JoinRoomResult;
  room: Room;
}) {
  if (options?.markAsRecommended) {
    markRecommendedRoomActive(room.publicId, gameId);
  }

  navigate(`/chat/${result.chatRoomId}`);
}

async function resolveAlreadyInOtherRoom({
  activeRoom,
  gameId,
  inviteCode,
  options,
  password,
  queryClient,
  retryJoin,
  room,
  toast,
}: {
  activeRoom: Room | null;
  gameId: number;
  inviteCode?: string;
  options?: JoinRoomOptions;
  password?: string;
  queryClient: QueryClient;
  retryJoin: (
    room: Room,
    password?: string,
    inviteCode?: string,
    options?: JoinRoomOptions,
  ) => Promise<void>;
  room: Room;
  toast: ToastHandlers;
}) {
  const shouldOfferMove = Boolean(
    activeRoom &&
    activeRoom.publicId !== room.publicId &&
    !options?.skipActiveRoomPrompt,
  );

  if (!shouldOfferMove) {
    toast.error('이미 다른 대기방에 참가 중입니다');
    return false;
  }

  const currentActiveRoom = activeRoom;
  if (!currentActiveRoom) {
    toast.error('이미 다른 대기방에 참가 중입니다');
    return false;
  }

  const confirmed = window.confirm(
    `현재 "${currentActiveRoom.title}"에 참가 중입니다. 해당 방을 나가고 "${room.title}"에 입장할까요?`,
  );

  if (!confirmed) {
    return false;
  }

  try {
    await roomService.leaveRoom(currentActiveRoom.id);
    await invalidateActiveRoomQueries(queryClient, gameId);
    toast.success('이전 대기방을 나가고 새 방에 입장합니다');
    await retryJoin(room, password, inviteCode, {
      ...options,
      skipActiveRoomPrompt: true,
    });
    return true;
  } catch {
    toast.error('기존 대기방 정리에 실패했습니다');
    return false;
  }
}

async function invalidateActiveRoomQueries(queryClient: QueryClient, gameId: number) {
  await queryClient.invalidateQueries({ queryKey: roomKeys.list(gameId) });
  await queryClient.invalidateQueries({ queryKey: roomKeys.myList() });
  await queryClient.invalidateQueries({ queryKey: roomKeys.myActive() });
  await queryClient.invalidateQueries({ queryKey: chatKeys.myList() });
  await queryClient.invalidateQueries({ queryKey: chatKeys.myRoomChats() });
}

function extractErrorCode(err: unknown): string | null {
  if (
    err &&
    typeof err === 'object' &&
    'response' in err
  ) {
    const response = (err as { response?: { data?: { code?: string } } }).response;
    return response?.data?.code ?? null;
  }

  return null;
}
