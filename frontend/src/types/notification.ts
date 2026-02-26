export interface Notification {
  id: number;
  userId: number;
  type: 'FRIEND_REQUEST' | 'ROOM_INVITE' | 'EVALUATION' | 'REPORT' | 'SYSTEM';
  title: string;
  message: string;
  isRead: boolean;
  relatedId?: number;
  createdAt: string;
}
