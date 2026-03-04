import api from './api';

export interface AdData {
  id: number;
  title: string;
  description: string;
  imageUrl: string | null;
  targetUrl: string | null;
}

export const adService = {
  getAds(): Promise<AdData[]> {
    return api.get('/ads').then((r) => r.data.data ?? []);
  },

  recordView(adId: number): Promise<void> {
    return api.post(`/ads/${adId}/view`).then(() => undefined);
  },
};
