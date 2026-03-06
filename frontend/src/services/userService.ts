import api from './api';
import type { User, UserSearchResult } from '@/types/user';
import { ApiResponse } from '@/types/api';

export const userService = {
  async updateProfile(data: { nickname: string }): Promise<User> {
    const response = await api.patch<ApiResponse<User>>('/users/me', data);
    return response.data.data;
  },

  async searchUsers(query: string, limit: number = 10): Promise<UserSearchResult[]> {
    const response = await api.get<ApiResponse<UserSearchResult[]>>('/users/search', {
      params: { q: query, limit },
    });
    return response.data.data;
  },
};

export default userService;
