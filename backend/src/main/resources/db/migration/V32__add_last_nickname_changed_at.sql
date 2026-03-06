-- Add last_nickname_changed_at for 30-day nickname cooldown policy.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS last_nickname_changed_at TIMESTAMP;

-- Backfill from legacy nickname_changed_at when present.
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'users'
          AND column_name = 'nickname_changed_at'
    ) THEN
        UPDATE users
        SET last_nickname_changed_at = nickname_changed_at
        WHERE last_nickname_changed_at IS NULL
          AND nickname_changed_at IS NOT NULL;
    END IF;
END $$;
