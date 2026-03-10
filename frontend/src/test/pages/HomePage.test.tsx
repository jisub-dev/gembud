import { beforeEach, describe, expect, it, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { createElement } from 'react';
import { MemoryRouter } from 'react-router-dom';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import HomePage from '@/pages/HomePage';
import { useGames } from '@/hooks/queries/useGames';
import { useAds } from '@/hooks/queries/useAds';
import { useAuthStore } from '@/store/authStore';

const { mockNavigate } = vi.hoisted(() => ({
  mockNavigate: vi.fn(),
}));

vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

vi.mock('@/hooks/queries/useGames', () => ({
  useGames: vi.fn(),
}));

vi.mock('@/hooks/queries/useAds', () => ({
  useAds: vi.fn(),
}));

vi.mock('@/store/authStore', () => ({
  useAuthStore: vi.fn(),
}));

vi.mock('@/components/common/AdBanner', () => ({
  default: () => null,
}));

vi.mock('@/components/game/GameGrid', () => ({
  default: ({
    games,
    onGameSelect,
  }: {
    games: Array<{ id: number; name: string }>;
    onGameSelect?: (gameId: number) => void;
  }) => (
    <div>
      {games.map((game) => (
        <button key={game.id} onClick={() => onGameSelect?.(game.id)}>
          {game.name}
        </button>
      ))}
    </div>
  ),
}));

function createWrapper() {
  const queryClient = new QueryClient({
    defaultOptions: {
      queries: { retry: false },
      mutations: { retry: false },
    },
  });

  return ({ children }: { children: React.ReactNode }) =>
    createElement(QueryClientProvider, { client: queryClient },
      createElement(
        MemoryRouter,
        { future: { v7_startTransition: true, v7_relativeSplatPath: true } },
        children,
      ),
    );
}

describe('HomePage navigation', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(useGames).mockReturnValue({
      data: [
        { id: 1, name: 'League of Legends', description: 'MOBA', genre: 'MOBA', imageUrl: '/lol.png' },
      ],
      isLoading: false,
      error: null,
    } as any);
    vi.mocked(useAds).mockReturnValue({ data: [] } as any);
    vi.mocked(useAuthStore).mockReturnValue({
      isAuthenticated: true,
      user: { isPremium: false },
    } as any);
  });

  it('navigates directly to room list when a game is selected', async () => {
    const user = userEvent.setup();

    render(<HomePage />, { wrapper: createWrapper() });
    await user.click(screen.getByRole('button', { name: 'League of Legends' }));

    expect(mockNavigate).toHaveBeenCalledWith('/games/1/rooms');
  });

  it('does not render main-page recommendation section anymore', () => {
    render(<HomePage />, { wrapper: createWrapper() });

    expect(screen.queryByText('추천 방')).not.toBeInTheDocument();
  });
});
