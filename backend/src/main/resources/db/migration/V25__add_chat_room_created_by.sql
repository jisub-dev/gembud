-- Add created_by column to chat_rooms (was missing from V8 migration)
ALTER TABLE chat_rooms ADD COLUMN IF NOT EXISTS created_by BIGINT REFERENCES users(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_chat_rooms_created_by ON chat_rooms(created_by);
