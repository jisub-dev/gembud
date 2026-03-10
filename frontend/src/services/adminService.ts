import api from '@/services/api';
import type { ApiResponse } from '@/types/api';

export interface SecuritySummary {
  loginFailCount: number;
  loginLockedCount: number;
  refreshReuseCount: number;
  rateLimitHitCount: number;
}

export interface UserSecurityStatus {
  userId: number;
  email: string;
  loginLocked: boolean;
  loginLockedUntil: string | null;
  failedLoginCountInWindow: number;
  windowMinutes: number;
}

export interface SecurityEventItem {
  id: number;
  eventType: string;
  userId: number | null;
  ip: string | null;
  endpoint: string | null;
  result: string | null;
  riskScore: string | null;
  createdAt: string;
}

export interface SecurityEventListParams {
  eventType?: string;
  riskScore?: string;
  from?: string;
  to?: string;
  page?: number;
  size?: number;
}

export interface SecurityEventListResponse {
  content: SecurityEventItem[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export const adminService = {
  async getSecuritySummary(windowMinutes: number = 60): Promise<SecuritySummary> {
    const response = await api.get<ApiResponse<SecuritySummary>>('/admin/security-events/summary', {
      params: { windowMinutes },
    });
    return response.data.data;
  },

  async getSecurityEvents(params: SecurityEventListParams): Promise<SecurityEventListResponse> {
    const response = await api.get<ApiResponse<SecurityEventListResponse>>('/admin/security-events', {
      params,
    });
    return response.data.data;
  },

  async getUserSecurityStatus(userId: number): Promise<UserSecurityStatus> {
    const response = await api.get<ApiResponse<UserSecurityStatus>>(`/admin/users/${userId}/security-status`);
    return response.data.data;
  },

  async unlockUserLogin(userId: number): Promise<void> {
    await api.delete(`/admin/users/${userId}/login-lock`);
  },
};

export default adminService;
