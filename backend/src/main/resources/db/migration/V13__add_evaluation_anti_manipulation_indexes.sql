-- V13: Add indexes for evaluation anti-manipulation (Phase 11)
-- Purpose: Optimize monthly evaluation limit check query

-- Index for monthly evaluation count query
-- Speeds up: countByEvaluatorAndEvaluatedInCurrentMonth
CREATE INDEX IF NOT EXISTS idx_evaluation_monthly_limit
ON evaluations(evaluator_id, evaluated_id, created_at);

-- Index for evaluations by creation date (for general queries)
CREATE INDEX IF NOT EXISTS idx_evaluation_created_at
ON evaluations(created_at);

-- Comment on indexes
COMMENT ON INDEX idx_evaluation_monthly_limit IS 'Optimizes monthly evaluation limit check for anti-manipulation';
COMMENT ON INDEX idx_evaluation_created_at IS 'Optimizes evaluations queries by creation date';
