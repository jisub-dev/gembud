export interface User {
  id: number;
  email: string;
  nickname: string;
  temperature: number;
  profileImageUrl?: string;
  ageRange?: string;
  isPremium?: boolean;
  premiumExpiresAt?: string | null;
}

export interface UserSearchResult {
  id: number;
  email: string;
  nickname: string;
}
