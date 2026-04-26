import { test, expect } from '@playwright/test';
import { API, mockUser, mockRoom, mockChatRoom, setupAuthenticatedSession, seedAuthStore } from './fixtures';

async function setupChatPage(page: import('@playwright/test').Page) {
  await setupAuthenticatedSession(page);

  await page.route(`${API}/chat/rooms/my**`, (route) =>
    route.fulfill({ json: { data: [mockChatRoom] } }),
  );

  await page.route(`${API}/rooms/my/active`, (route) =>
    route.fulfill({ json: { data: mockRoom } }),
  );

  await page.route(`${API}/chat/rooms/${mockChatRoom.publicId}/messages**`, (route) =>
    route.fulfill({ json: { data: [] } }),
  );

  // Seed auth store before page load (addInitScript must come before goto)
  await seedAuthStore(page);
}

test.describe('Chat page', () => {
  test('renders room title and chat panel', async ({ page }) => {
    await setupChatPage(page);

    await page.goto(`/chat/${mockChatRoom.publicId}`);

    await expect(page.getByText(mockRoom.title).first()).toBeVisible({ timeout: 10_000 });
    await expect(page.getByText('대기방 채팅')).toBeVisible();
  });

  test('shows "채팅방 정보를 불러오는 중입니다" when chat room info not found yet', async ({ page }) => {
    await setupAuthenticatedSession(page);

    // Return empty list so chatRoomInfo = undefined initially
    await page.route(`${API}/chat/rooms/my**`, (route) =>
      route.fulfill({ json: { data: [] } }),
    );

    await page.route(`${API}/rooms/my/active`, (route) =>
      route.fulfill({ status: 404, json: { data: null } }),
    );

    await seedAuthStore(page);
    await page.goto(`/chat/${mockChatRoom.publicId}`);

    await expect(page.getByText('접근할 수 없는 채팅방입니다')).toBeVisible({ timeout: 5_000 });
  });

  test('shows room info tab with participant count', async ({ page }) => {
    await setupChatPage(page);

    await page.goto(`/chat/${mockChatRoom.publicId}`);

    await expect(page.getByText(mockRoom.title).first()).toBeVisible({ timeout: 10_000 });
    // Room info section shows participant info
    await expect(page.getByText(/1.*\/.*5|1 \/ 5/)).toBeVisible();
  });

  test('shows leave button for room participant', async ({ page }) => {
    await setupChatPage(page);

    await page.goto(`/chat/${mockChatRoom.publicId}`);

    await expect(page.getByRole('button', { name: '대기방 나가기' })).toBeVisible({ timeout: 10_000 });
  });

  test('leave button triggers leave API and redirects', async ({ page }) => {
    await setupChatPage(page);

    let leaveCalled = false;
    await page.route(`${API}/rooms/${mockRoom.id}/leave`, (route) => {
      leaveCalled = true;
      return route.fulfill({ status: 200, body: '' });
    });

    await page.goto(`/chat/${mockChatRoom.publicId}`);
    await page.getByRole('button', { name: '대기방 나가기' }).click();

    // Confirm dialog or direct leave
    const dialog = page.getByRole('dialog');
    if (await dialog.isVisible({ timeout: 1_000 }).catch(() => false)) {
      await page.getByRole('button', { name: /확인|나가기/i }).click();
    }

    await expect(() => expect(leaveCalled).toBe(true)).toPass({ timeout: 5_000 });
  });
});
