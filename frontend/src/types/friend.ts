export interface Friend {
  id: number;
  userId: number;
  userNickname: string;
  friendId: number;
  friendNickname: string;
  status: 'PENDING' | 'ACCEPTED' | 'REJECTED';
  createdAt: string;
  updatedAt?: string;
}

/** Alias for pending/sent friend requests (same shape as Friend) */
export type FriendRequest = Friend;
