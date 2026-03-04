import { useQuery } from '@tanstack/react-query';
import { useMemo } from 'react';
import { gameService } from '@/services/gameService';

/**
 * TanStack Query hooks for game-related API calls.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */

export const gameKeys = {
  all: ['games'] as const,
  lists: () => [...gameKeys.all, 'list'] as const,
  list: () => [...gameKeys.lists()] as const,
  details: () => [...gameKeys.all, 'detail'] as const,
  detail: (id: number) => [...gameKeys.details(), id] as const,
};

/**
 * Hook to fetch all games.
 */
export function useGames() {
  return useQuery({
    queryKey: gameKeys.list(),
    queryFn: () => gameService.getGames(),
    staleTime: 5 * 60 * 1000, // 5 minutes (games don't change often)
  });
}

/**
 * Hook to fetch a single game by ID.
 */
export function useGame(id: number) {
  return useQuery({
    queryKey: gameKeys.detail(id),
    queryFn: () => gameService.getGameById(id),
    staleTime: 5 * 60 * 1000, // 5 minutes
  });
}

/**
 * Hook to get game options filtered by type.
 * Useful for room filtering (tier, position).
 */
export function useGameOptions(gameId: number) {
  const { data: game, ...rest } = useGame(gameId);

  const tierOptions = useMemo(() =>
    game?.options.filter(opt => opt.optionType === 'TIER') ?? [],
    [game]
  );

  const positionOptions = useMemo(() =>
    game?.options.filter(opt => opt.optionType === 'POSITION') ?? [],
    [game]
  );

  return {
    game,
    tierOptions,
    positionOptions,
    allOptions: game?.options ?? [],
    ...rest,
  };
}
