import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import friendService from '@/services/friendService';

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
  });
}

// Get friend requests sent
export function useSentFriendRequests() {
  return useQuery({
    queryKey: friendKeys.sent(),
    queryFn: friendService.getSentRequests,
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
