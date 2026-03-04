import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import evaluationService from '@/services/evaluationService';
import type { EvaluationRequest } from '@/types/evaluation';

/**
 * React Query hooks for evaluation
 *
 * @author Gembud Team
 * @since 2026-02-26
 */

// Submit evaluation
export function useEvaluateUser() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: ({ roomId, ...data }: { roomId: number } & EvaluationRequest) =>
      evaluationService.createEvaluation(roomId, data),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['profile'] });
    },
  });
}

// Get evaluations received by a user (for profile page)
export function useGetEvaluations(userId: number) {
  return useQuery({
    queryKey: ['evaluations', 'user', userId],
    queryFn: () => evaluationService.getUserEvaluations(userId),
    enabled: !!userId,
  });
}

// Get temperature stats for a user (for profile page)
export function useUserTemperature(userId: number) {
  return useQuery({
    queryKey: ['temperature', userId],
    queryFn: () => evaluationService.getUserTemperature(userId),
    enabled: !!userId,
  });
}
