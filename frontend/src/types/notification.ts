export interface Notification {
  id: number;
  userId: number;
  type: 'FRIEND_REQUEST' | 'FRIEND_ACCEPTED' | 'ROOM_INVITATION' | 'ROOM_JOIN' | 'EVALUATION_RECEIVED' | 'ROOM_INVITE' | 'EVALUATION' | 'REPORT' | 'SYSTEM';
  title: string;
  message: string;
  isRead: boolean;
  relatedId?: number;
  relatedUrl?: string;
  createdAt: string;
}
