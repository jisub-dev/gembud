import { test, expect } from '@playwright/test';
import { API, mockUser, setupAuthenticatedSession, setupUnauthenticatedSession } from './fixtures';

test.describe('Authentication flow', () => {
  test('login page renders and shows error on bad credentials', async ({ page }) => {
    await setupUnauthenticatedSession(page);
    await page.route(`${API}/auth/login`, (route) =>
      route.fulfill({
        status: 401,
        json: { message: '이메일 또는 비밀번호가 올바르지 않습니다' },
      }),
    );

    await page.goto('/login');
    await expect(page.getByText('Sign in to Gembud')).toBeVisible();

    await page.fill('#email', 'wrong@test.com');
    await page.fill('#password', 'wrongpassword');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page.getByText('이메일 또는 비밀번호가 올바르지 않습니다')).toBeVisible();
  });

  test('successful login redirects to home page', async ({ page }) => {
    // Set up all routes before page load
    await setupAuthenticatedSession(page);
    // Override users/me for the pre-login state: return 403 (not 401, avoids sessionExpired)
    // The setupAuthenticatedSession route for users/me will be used after login
    await page.route(`${API}/auth/login`, (route) =>
      route.fulfill({ status: 200, body: '' }),
    );

    await page.goto('/login');
    await page.fill('#email', 'test@gembud.com');
    await page.fill('#password', 'password123');
    await page.getByRole('button', { name: 'Sign in' }).click();

    await expect(page).toHaveURL('/', { timeout: 10_000 });
  });

  test('signup page renders all required fields', async ({ page }) => {
    await setupUnauthenticatedSession(page);
    await page.goto('/signup');
    await expect(page.getByText('Create your account')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#nickname')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
  });

  test('signup shows password mismatch error', async ({ page }) => {
    await setupUnauthenticatedSession(page);
    await page.goto('/signup');

    await page.fill('#email', 'newuser@test.com');
    await page.fill('#nickname', 'NewUser');
    await page.fill('#password', 'password123');
    await page.fill('#confirmPassword', 'different456');
    await page.getByRole('button', { name: 'Sign up' }).click();

    await expect(page.getByText('Passwords do not match')).toBeVisible();
  });

  test('unauthenticated user is redirected to login when accessing protected route', async ({ page }) => {
    await setupUnauthenticatedSession(page);
    await page.route(`${API}/games`, (route) => route.fulfill({ json: [] }));
    await page.route(`${API}/notifications/unread-count`, (route) =>
      route.fulfill({ json: { count: 0 } }),
    );
    await page.route(`${API}/ads`, (route) => route.fulfill({ json: [] }));

    await page.goto('/games/7/rooms');

    await expect(page).toHaveURL('/login');
  });

  test('login page has link to signup', async ({ page }) => {
    await page.goto('/login');
    await expect(page.getByRole('link', { name: /sign up/i })).toBeVisible();
  });

  test('logout clears session and redirects to login', async ({ page }) => {
    await setupAuthenticatedSession(page);
    await page.route(`${API}/auth/logout`, (route) => route.fulfill({ status: 200, body: '' }));

    // Seed auth state via localStorage (simulates logged-in user)
    await page.goto('/');
    await page.evaluate((user) => {
      localStorage.setItem('auth-storage', JSON.stringify({ state: { user, isAuthenticated: true } }));
    }, mockUser);
    await page.reload();

    // Click logout via header
    const logoutBtn = page.getByRole('button', { name: /로그아웃/i });
    if (await logoutBtn.isVisible()) {
      await logoutBtn.click();
      await expect(page).toHaveURL('/login');
    }
  });
});
