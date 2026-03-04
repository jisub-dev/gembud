import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { subscriptionService } from '@/services/subscriptionService';
import { useAuthStore } from '@/store/authStore';

export function useSubscriptionStatus() {
  const { isAuthenticated } = useAuthStore();

  return useQuery({
    queryKey: ['subscriptionStatus'],
    queryFn: () => subscriptionService.getStatus(),
    enabled: isAuthenticated,
    staleTime: 5 * 60 * 1000,
  });
}

export function useActivatePremium() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (months: number) => subscriptionService.activate(months),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptionStatus'] });
    },
  });
}

export function useCancelPremium() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => subscriptionService.cancel(),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['subscriptionStatus'] });
    },
  });
}
