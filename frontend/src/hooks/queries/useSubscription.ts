import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { subscriptionService } from '@/services/subscriptionService';
import { useAuthStore } from '@/store/authStore';
import type { SubscriptionStatusResponse } from '@/types/subscription';
import { featureFlags, isPremiumActive } from '@/config/features';

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
    onSuccess: (data: SubscriptionStatusResponse) => {
      queryClient.invalidateQueries({ queryKey: ['subscriptionStatus'] });
      // Sync isPremium into authStore so UI reflects change immediately
      const { user } = useAuthStore.getState();
      if (user) {
        useAuthStore.setState({
          user: {
            ...user,
            isPremium: isPremiumActive(data.isPremium),
            premiumExpiresAt: featureFlags.premium ? data.premiumExpiresAt ?? undefined : null,
          },
        });
      }
    },
  });
}

export function useCancelPremium() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: () => subscriptionService.cancel(),
    onSuccess: (data: SubscriptionStatusResponse) => {
      queryClient.invalidateQueries({ queryKey: ['subscriptionStatus'] });
      // Sync isPremium into authStore so UI reflects change immediately
      const { user } = useAuthStore.getState();
      if (user) {
        useAuthStore.setState({
          user: {
            ...user,
            isPremium: isPremiumActive(data.isPremium),
            premiumExpiresAt: featureFlags.premium ? data.premiumExpiresAt ?? undefined : null,
          },
        });
      }
    },
  });
}
