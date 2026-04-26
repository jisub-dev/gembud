import { useRegenerateInviteCode } from '@/hooks/queries/useRooms';
import { roomService } from '@/services/roomService';
import type { Room } from '@/types/room';
import { copyTextToClipboard } from '@/utils/clipboard';

interface ToastHandlers {
  error: (message: string) => void;
  success: (message: string) => void;
}

interface CopyInviteMessages {
  error?: string;
  missing?: string;
  success?: string;
}

interface RegenerateInviteLinkOptions {
  room: Room;
  confirmMessage?: string;
  copyAfterRegenerate?: boolean;
  copyMessages?: CopyInviteMessages;
  onRegenerated?: (result: { inviteLink: string; room: Room }) => void;
  regenerateErrorMessage?: string;
  regenerateSuccessMessage?: string;
}

export function useRoomInviteActions({ toast }: { toast: ToastHandlers }) {
  const regenerateInviteCode = useRegenerateInviteCode();

  const copyInviteLink = async (
    room: Pick<Room, 'gameId' | 'publicId' | 'inviteCode'> | null | undefined,
    messages?: CopyInviteMessages,
  ) => {
    if (!room?.inviteCode) {
      toast.error(messages?.missing ?? '현재 사용할 수 있는 초대 링크가 없습니다');
      return false;
    }

    try {
      await copyTextToClipboard(roomService.buildInviteLink(room));
      toast.success(messages?.success ?? '초대 링크가 복사되었습니다');
      return true;
    } catch {
      toast.error(messages?.error ?? '초대 링크 복사에 실패했습니다');
      return false;
    }
  };

  const regenerateInviteLink = async ({
    room,
    confirmMessage,
    copyAfterRegenerate = false,
    copyMessages,
    onRegenerated,
    regenerateErrorMessage = '초대 링크 재발급에 실패했습니다',
    regenerateSuccessMessage = '초대 링크를 재발급했습니다',
  }: RegenerateInviteLinkOptions) => {
    if (confirmMessage && !window.confirm(confirmMessage)) {
      return null;
    }

    try {
      const updatedRoom = await regenerateInviteCode.mutateAsync({ room });
      if (!updatedRoom.inviteCode) {
        toast.error(regenerateErrorMessage);
        return null;
      }

      const inviteLink = roomService.buildInviteLink(updatedRoom);
      onRegenerated?.({ inviteLink, room: updatedRoom });

      if (copyAfterRegenerate) {
        await copyInviteLink(updatedRoom, copyMessages);
      } else {
        toast.success(regenerateSuccessMessage);
      }

      return { inviteLink, room: updatedRoom };
    } catch {
      toast.error(regenerateErrorMessage);
      return null;
    }
  };

  return {
    copyInviteLink,
    isRegeneratingInvite: regenerateInviteCode.isPending,
    regenerateInviteLink,
  };
}
