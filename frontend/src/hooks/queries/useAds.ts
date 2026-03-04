import { useQuery } from '@tanstack/react-query';
import { adService } from '@/services/adService';
import { useAuthStore } from '@/store/authStore';

export function useAds() {
  const { isAuthenticated, user } = useAuthStore();

  return useQuery({
    queryKey: ['ads'],
    queryFn: () => adService.getAds(),
    enabled: isAuthenticated && !user?.isPremium,
    staleTime: 5 * 60 * 1000,
  });
}
