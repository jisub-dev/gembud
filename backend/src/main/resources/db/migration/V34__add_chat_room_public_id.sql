ALTER TABLE chat_rooms
    ADD COLUMN IF NOT EXISTS public_id VARCHAR(36);

UPDATE chat_rooms
SET public_id = gen_random_uuid()::text
WHERE public_id IS NULL;

ALTER TABLE chat_rooms
    ALTER COLUMN public_id SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_chat_rooms_public_id
    ON chat_rooms (public_id);
