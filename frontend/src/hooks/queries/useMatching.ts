import { useQuery } from '@tanstack/react-query';
import { matchingService } from '@/services/matchingService';
import type { RecommendedRoom } from '@/types/room';
import { useAuthStore } from '@/store/authStore';

export function useRecommendedRooms(gameId: number | undefined, limit = 3) {
  const { isAuthenticated } = useAuthStore();

  return useQuery<RecommendedRoom[]>({
    queryKey: ['recommendations', gameId, limit],
    queryFn: () => matchingService.getRecommendedRooms(gameId!, limit),
    enabled: isAuthenticated && !!gameId,
    staleTime: 60 * 1000,
  });
}
