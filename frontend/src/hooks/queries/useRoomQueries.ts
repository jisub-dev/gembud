/**
 * Query key factory for room-related queries.
 * Provides hierarchical query keys for efficient cache management.
 *
 * @author Gembud Team
 * @since 2026-02-22
 */
export const roomKeys = {
  all: ['rooms'] as const,
  lists: () => [...roomKeys.all, 'list'] as const,
  list: (gameId: number) => [...roomKeys.lists(), gameId] as const,
  details: () => [...roomKeys.all, 'detail'] as const,
  detail: (roomId: number) => [...roomKeys.details(), roomId] as const,
  recommended: (gameId: number) => [...roomKeys.all, 'recommended', gameId] as const,
};
