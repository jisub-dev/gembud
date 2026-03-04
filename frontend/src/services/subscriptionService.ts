import api from './api';
import type { SubscriptionStatusResponse } from '@/types/subscription';

export const subscriptionService = {
  getStatus(): Promise<SubscriptionStatusResponse> {
    return api.get('/subscriptions/status').then((r) => r.data.data);
  },

  activate(months = 1): Promise<SubscriptionStatusResponse> {
    return api.post('/subscriptions/activate', { months }).then((r) => r.data.data);
  },

  cancel(): Promise<SubscriptionStatusResponse> {
    return api.post('/subscriptions/cancel').then((r) => r.data.data);
  },
};
