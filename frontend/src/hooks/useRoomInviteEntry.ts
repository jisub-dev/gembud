import { useEffect, useRef, useState } from 'react';
import type { SetURLSearchParams } from 'react-router-dom';
import { roomService } from '@/services/roomService';
import type { Room } from '@/types/room';

export type InviteEntryStatus = 'ready' | 'expired' | 'missing';

export interface InviteEntryState {
  inviteCode: string;
  roomPublicId?: string;
  targetRoom?: Room;
  status: InviteEntryStatus;
}

interface InviteModalRequest {
  inviteCode: string;
  room: Room;
}

interface UseRoomInviteEntryParams {
  rooms?: Room[];
  roomsLoading: boolean;
  searchParams: URLSearchParams;
  setSearchParams: SetURLSearchParams;
  onMissingInvite: () => void;
}

export function useRoomInviteEntry({
  rooms,
  roomsLoading,
  searchParams,
  setSearchParams,
  onMissingInvite,
}: UseRoomInviteEntryParams) {
  const [inviteEntryState, setInviteEntryState] = useState<InviteEntryState | null>(null);
  const [inviteModalRequest, setInviteModalRequest] = useState<InviteModalRequest | null>(null);
  const inviteHandledRef = useRef<string | null>(null);
  const onMissingInviteRef = useRef(onMissingInvite);

  useEffect(() => {
    onMissingInviteRef.current = onMissingInvite;
  }, [onMissingInvite]);

  useEffect(() => {
    const inviteCode = searchParams.get('invite')?.trim();
    const roomPublicId = searchParams.get('room')?.trim();

    if (!inviteCode) {
      inviteHandledRef.current = null;
      setInviteEntryState((prev) => (prev === null ? prev : null));
      return;
    }

    if (roomsLoading) {
      return;
    }

    const inviteKey = `${roomPublicId ?? 'none'}:${inviteCode}`;
    if (inviteHandledRef.current === inviteKey) {
      return;
    }
    inviteHandledRef.current = inviteKey;

    let cancelled = false;

    const openInviteModal = async () => {
      const targetRoom = await resolveInviteTarget(rooms, roomPublicId, inviteCode);
      if (cancelled) {
        return;
      }

      if (!targetRoom) {
        setInviteEntryState({
          inviteCode,
          roomPublicId,
          status: 'missing',
        });
        onMissingInviteRef.current();
        return;
      }

      setInviteEntryState({
        inviteCode,
        roomPublicId: targetRoom.publicId,
        targetRoom,
        status: 'ready',
      });
      setInviteModalRequest({
        inviteCode,
        room: targetRoom,
      });
    };

    void openInviteModal();

    return () => {
      cancelled = true;
    };
  }, [rooms, roomsLoading, searchParams]);

  const clearInviteEntry = () => {
    inviteHandledRef.current = null;
    setInviteModalRequest(null);
    setInviteEntryState(null);
    setSearchParams({}, { replace: true });
  };

  const consumeInviteModalRequest = () => {
    setInviteModalRequest(null);
  };

  const markInviteExpired = (inviteCode: string, room: Room) => {
    setInviteModalRequest(null);
    setInviteEntryState((prev) => ({
      inviteCode,
      roomPublicId: room.publicId,
      targetRoom: prev?.targetRoom ?? room,
      status: 'expired',
    }));
  };

  const markInviteMissing = (inviteCode: string, room: Room) => {
    setInviteModalRequest(null);
    setInviteEntryState({
      inviteCode,
      roomPublicId: room.publicId,
      targetRoom: room,
      status: 'missing',
    });
  };

  return {
    inviteEntryState,
    inviteModalRequest,
    consumeInviteModalRequest,
    clearInviteEntry,
    markInviteExpired,
    markInviteMissing,
  };
}

async function resolveInviteTarget(
  rooms: Room[] | undefined,
  roomPublicId: string | undefined,
  inviteCode: string,
) {
  let targetRoom: Room | undefined;

  if (roomPublicId) {
    targetRoom = rooms?.find((room) => room.publicId === roomPublicId);
    if (!targetRoom) {
      try {
        targetRoom = await roomService.getRoom(roomPublicId);
      } catch {
        targetRoom = undefined;
      }
    }
  } else {
    targetRoom = rooms?.find((room) => room.inviteCode === inviteCode);
  }

  return targetRoom;
}
