export interface Friend {
  id: number;
  userId: number;
  friendId: number;
  friendNickname: string;
  createdAt: string;
}

export interface FriendRequest {
  id: number;
  requesterId: number;
  requesterNickname: string;
  recipientId: number;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
}
