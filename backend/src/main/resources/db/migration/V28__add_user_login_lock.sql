-- Add login lock support to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS login_locked_until TIMESTAMP;
