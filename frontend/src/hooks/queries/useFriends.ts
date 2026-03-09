import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import friendService from '@/services/friendService';
import type { FriendRequest } from '@/types/friend';

/**
 * React Query hooks for friend management
 *
 * @author Gembud Team
 * @since 2026-02-26
 */

// Query keys
export const friendKeys = {
  all: ['friends'] as const,
  list: () => [...friendKeys.all, 'list'] as const,
  requests: () => [...friendKeys.all, 'requests'] as const,
  sent: () => [...friendKeys.all, 'sent'] as const,
};

const statusPriority: Record<FriendRequest['status'], number> = {
  PENDING: 0,
  ACCEPTED: 1,
  REJECTED: 2,
};

function sortRequests(requests: FriendRequest[]) {
  return [...requests].sort((a, b) => {
    const statusDiff = statusPriority[a.status] - statusPriority[b.status];
    if (statusDiff !== 0) return statusDiff;

    const updatedA = new Date(a.updatedAt ?? a.createdAt).getTime();
    const updatedB = new Date(b.updatedAt ?? b.createdAt).getTime();
    return updatedB - updatedA;
  });
}

// Get friends list
export function useFriends() {
  return useQuery({
    queryKey: friendKeys.list(),
    queryFn: friendService.getFriends,
  });
}

// Get friend requests received
export function useFriendRequests() {
  return useQuery({
    queryKey: friendKeys.requests(),
    queryFn: friendService.getPendingRequests,
    select: sortRequests,
  });
}

// Get friend requests sent
export function useSentFriendRequests() {
  return useQuery({
    queryKey: friendKeys.sent(),
    queryFn: friendService.getSentRequests,
    select: sortRequests,
  });
}

// Send friend request
export function useSendFriendRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (friendId: number) => friendService.sendFriendRequest(friendId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: friendKeys.sent() });
    },
  });
}

// Accept friend request
export function useAcceptFriendRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (requestId: number) => friendService.acceptFriendRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: friendKeys.requests() });
      queryClient.invalidateQueries({ queryKey: friendKeys.list() });
    },
  });
}

// Reject friend request
export function useRejectFriendRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (requestId: number) => friendService.rejectFriendRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: friendKeys.requests() });
    },
  });
}

// Cancel sent friend request
export function useCancelSentFriendRequest() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (requestId: number) => friendService.cancelSentFriendRequest(requestId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: friendKeys.sent() });
    },
  });
}

// Remove friend
export function useRemoveFriend() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (friendId: number) => friendService.removeFriend(friendId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: friendKeys.list() });
    },
  });
}
