export type SubscriptionStatus = 'ACTIVE' | 'CANCELLED' | 'EXPIRED';

export interface SubscriptionStatusResponse {
  isPremium: boolean;
  premiumExpiresAt: string | null;
  subscriptionStatus: SubscriptionStatus | null;
  startedAt: string | null;
  amount: number;
}
