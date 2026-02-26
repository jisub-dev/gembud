import { useMutation, useQueryClient } from '@tanstack/react-query';
import evaluationService from '@/services/evaluationService';

/**
 * React Query hooks for evaluation
 *
 * @author Gembud Team
 * @since 2026-02-26
 */

interface EvaluateUserParams {
  roomId: number;
  evaluatedUserId: number;
  score: number;
  tags: string[];
  comment?: string;
}

// Submit evaluation
export function useEvaluateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roomId, ...data }: EvaluateUserParams) =>
      evaluationService.evaluateUser(roomId, data),
    onSuccess: () => {
      // Invalidate user profile queries if needed
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });
}

// Get evaluations received (for profile page)
export function useGetEvaluations(userId: number) {
  // This would be implemented when profile page needs it
  // For now, we'll implement evaluation submission only
  return null;
}
