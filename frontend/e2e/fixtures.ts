import type { Page } from '@playwright/test';

export const API = 'http://localhost:8080/api';

export const mockUser = {
  id: 1,
  email: 'test@gembud.com',
  nickname: 'TestUser',
  temperature: 36.5,
  isPremium: false,
};

export const mockGame = {
  id: 7,
  name: 'League of Legends',
  description: 'MOBA game',
  genre: 'MOBA',
  imageUrl: '/lol.png',
};

export const mockRoom = {
  id: 33,
  publicId: 'room-public-33',
  title: '골드+ 서폿 구함',
  description: '즐겜 위주',
  gameId: 7,
  gameName: 'League of Legends',
  maxParticipants: 5,
  currentParticipants: 1,
  isPrivate: false,
  status: 'OPEN',
  createdBy: 'TestUser',
  createdAt: '2026-04-26T10:00:00',
  participants: [{ userId: 1, nickname: 'TestUser', isHost: true }],
  filters: {},
};

export const mockChatRoom = {
  id: 101,
  publicId: 'chat-public-101',
  type: 'ROOM_CHAT',
  name: '골드+ 서폿 구함',
  relatedRoomId: 33,
};

/** Intercept common API calls needed for an authenticated session. */
export async function setupAuthenticatedSession(page: Page) {
  await page.route(`${API}/auth/csrf`, (route) =>
    route.fulfill({ status: 200, body: '' }),
  );

  await page.route(`${API}/users/me`, (route) =>
    route.fulfill({ json: { data: mockUser } }),
  );

  await page.route(`${API}/auth/refresh`, (route) =>
    route.fulfill({ status: 200, body: '' }),
  );

  await page.route(`${API}/games`, (route) =>
    route.fulfill({ json: { data: [mockGame] } }),
  );

  await page.route(`${API}/notifications/unread/count`, (route) =>
    route.fulfill({ json: { data: 0 } }),
  );

  await page.route(`${API}/ads`, (route) =>
    route.fulfill({ json: { data: [] } }),
  );

  await page.route(`${API}/friends**`, (route) =>
    route.fulfill({ json: { data: [] } }),
  );

  await page.route(`${API}/chat/rooms/my**`, (route) =>
    route.fulfill({ json: { data: [] } }),
  );

  await page.route(`${API}/rooms/my**`, (route) =>
    route.fulfill({ json: { data: [] } }),
  );
}

/** Seed Zustand auth store via localStorage before the page loads. */
export async function seedAuthStore(page: Page, user = mockUser) {
  await page.addInitScript((u) => {
    localStorage.setItem(
      'auth-storage',
      JSON.stringify({ state: { user: u, isAuthenticated: true }, version: 0 }),
    );
  }, user);
}

/** Intercept common API calls for unauthenticated state (suppresses session-expired redirect). */
export async function setupUnauthenticatedSession(page: Page) {
  // Return 403 (not 401) so the refresh interceptor doesn't trigger
  await page.route(`${API}/users/me`, (route) =>
    route.fulfill({ status: 403, body: '' }),
  );

  await page.route(`${API}/auth/refresh`, (route) =>
    route.fulfill({ status: 403, body: '' }),
  );

  await page.route(`${API}/auth/csrf`, (route) =>
    route.fulfill({ status: 200, body: '' }),
  );
}
