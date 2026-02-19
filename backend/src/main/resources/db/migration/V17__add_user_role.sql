-- V17: Add user role column (Phase 12)
-- Purpose: ADMIN permission separation

-- Add role column
ALTER TABLE users
ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';

-- Add index for role-based queries
CREATE INDEX IF NOT EXISTS idx_user_role
ON users(role);

-- Comment
COMMENT ON COLUMN users.role IS 'User role: USER (default), ADMIN';
