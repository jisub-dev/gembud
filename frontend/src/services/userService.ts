import api from './api';
import type { User } from '@/types/user';
import { ApiResponse } from '@/types/api';

export const userService = {
  async updateProfile(data: { nickname: string }): Promise<User> {
    const response = await api.patch<ApiResponse<User>>('/users/me', data);
    return response.data.data;
  },
};

export default userService;
