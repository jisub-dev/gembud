-- V15: Add user suspension (Phase 11)
-- Purpose: Auto-sanction users with 3+ pending reports

-- Add suspended_until column
ALTER TABLE users
ADD COLUMN suspended_until TIMESTAMP;

-- Add index for suspension queries
CREATE INDEX IF NOT EXISTS idx_user_suspended_until
ON users(suspended_until);

-- Comment on column
COMMENT ON COLUMN users.suspended_until IS 'User suspension expiration time (null if not suspended)';
