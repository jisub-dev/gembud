import { test, expect } from '@playwright/test';
import { API, mockUser, mockGame, mockRoom, setupAuthenticatedSession, seedAuthStore } from './fixtures';

async function setupRoomListPage(page: import('@playwright/test').Page) {
  await setupAuthenticatedSession(page);

  await page.route(`${API}/rooms**`, (route) => {
    const url = new URL(route.request().url());
    if (url.pathname.endsWith('/my')) {
      return route.fulfill({ json: { data: [] } });
    }
    if (url.pathname.endsWith('/active')) {
      return route.fulfill({ status: 404, json: { data: null } });
    }
    return route.fulfill({ json: { data: [mockRoom] } });
  });

  await page.route(`${API}/games/${mockGame.id}`, (route) =>
    route.fulfill({ json: { data: { ...mockGame, options: [] } } }),
  );

  await page.route(`${API}/matching/recommendations/game/${mockGame.id}**`, (route) =>
    route.fulfill({ json: { data: [] } }),
  );

  await seedAuthStore(page);
}

test.describe('Room list page', () => {
  test('displays rooms for a game', async ({ page }) => {
    await setupRoomListPage(page);

    await page.goto(`/games/${mockGame.id}/rooms`);

    await expect(page.getByText('골드+ 서폿 구함')).toBeVisible();
  });

  test('shows error banner when room fetch fails', async ({ page }) => {
    await setupAuthenticatedSession(page);

    await page.route(`${API}/rooms**`, (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith('/my') || url.pathname.endsWith('/active')) {
        return route.fulfill({ json: { data: [] } });
      }
      return route.fulfill({ status: 500, json: { message: 'Server error' } });
    });

    await page.route(`${API}/games/${mockGame.id}`, (route) =>
      route.fulfill({ json: { data: { ...mockGame, options: [] } } }),
    );

    await page.route(`${API}/matching/recommendations/game/${mockGame.id}**`, (route) =>
      route.fulfill({ json: { data: [] } }),
    );

    await seedAuthStore(page);
    await page.goto(`/games/${mockGame.id}/rooms`);

    await expect(page.getByText('방 목록을 불러오는 중 오류가 발생했습니다.')).toBeVisible();
  });

  test('opens create room modal when "방 만들기" button is clicked', async ({ page }) => {
    await setupRoomListPage(page);

    await page.goto(`/games/${mockGame.id}/rooms`);

    const createBtn = page.getByRole('button', { name: '방 만들기' });
    await expect(createBtn).toBeVisible();
    await createBtn.click();

    await expect(page.getByRole('heading', { name: '방 만들기' })).toBeVisible();
    await expect(page.getByPlaceholder('예: 골드 이상만 | 랭크 같이 가요')).toBeVisible();
  });

  test('creates a room and shows success toast', async ({ page }) => {
    await setupRoomListPage(page);

    await page.route(`${API}/rooms`, async (route) => {
      if (route.request().method() === 'POST') {
        return route.fulfill({ json: { data: mockRoom } });
      }
      return route.fallback();
    });

    await page.goto(`/games/${mockGame.id}/rooms`);

    await page.getByRole('button', { name: '방 만들기' }).first().click();
    await page.getByPlaceholder('예: 골드 이상만 | 랭크 같이 가요').fill('골드+ 서폿 구함');
    await page.getByRole('button', { name: '방 만들기' }).last().click();

    await expect(page.getByText('방이 생성되었습니다!')).toBeVisible({ timeout: 5_000 });
    // Modal closes after success
    await expect(page.getByRole('heading', { name: '방 만들기' })).not.toBeVisible();
  });

  test('shows "방 없음" when no rooms match filter', async ({ page }) => {
    await setupAuthenticatedSession(page);

    await page.route(`${API}/rooms**`, (route) => {
      const url = new URL(route.request().url());
      if (url.pathname.endsWith('/my') || url.pathname.endsWith('/active')) {
        return route.fulfill({ json: { data: [] } });
      }
      return route.fulfill({ json: { data: [] } });
    });

    await page.route(`${API}/games/${mockGame.id}`, (route) =>
      route.fulfill({ json: { data: { ...mockGame, options: [] } } }),
    );

    await page.route(`${API}/matching/recommendations/game/${mockGame.id}**`, (route) =>
      route.fulfill({ json: { data: [] } }),
    );

    await seedAuthStore(page);
    await page.goto(`/games/${mockGame.id}/rooms`);

    await expect(page.getByText('방이 없습니다')).toBeVisible();
  });
});
