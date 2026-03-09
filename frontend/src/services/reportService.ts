import api from './api';
import type { ApiResponse } from '@/types/api';

type ReportReason = 'SPAM' | 'ABUSIVE' | 'CHEATING' | 'OTHER';
type ReportCategory = 'FALSE_INFO' | 'VERBAL_ABUSE' | 'GAME_DISRUPTION' | 'HARASSMENT';

interface CreateReportPayload {
  reportedId: number;
  roomId: number | null;
  category: ReportCategory;
  reason: string;
  description?: string;
}

export interface MyReport {
  id: number;
  reason: string;
  description?: string;
  status: 'PENDING' | 'RESOLVED' | 'REJECTED' | 'REVIEWED' | string;
  category: string;
  priority: string;
  createdAt: string;
  roomTitle?: string | null;
  reported?: {
    id: number;
    nickname: string;
  };
}

function toCategory(reason: ReportReason): ReportCategory {
  switch (reason) {
    case 'ABUSIVE':
      return 'VERBAL_ABUSE';
    case 'CHEATING':
      return 'GAME_DISRUPTION';
    case 'OTHER':
      return 'HARASSMENT';
    case 'SPAM':
    default:
      return 'FALSE_INFO';
  }
}

export const reportService = {
  async createReport(reportedUserId: number, reason: ReportReason, chatMessageId?: number): Promise<void> {
    const payload: CreateReportPayload = {
      reportedId: reportedUserId,
      roomId: null,
      category: toCategory(reason),
      reason,
      description: chatMessageId ? `chatMessageId:${chatMessageId}` : undefined,
    };

    await api.post<ApiResponse<unknown>>('/reports', payload);
  },

  async getMyReports(): Promise<MyReport[]> {
    const response = await api.get<ApiResponse<MyReport[]>>('/reports/my');
    return response.data.data;
  },
};

export default reportService;
