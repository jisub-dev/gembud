-- Add public_id (UUID-based external identifier) and invite_code to rooms
ALTER TABLE rooms ADD COLUMN public_id VARCHAR(36) UNIQUE;
ALTER TABLE rooms ADD COLUMN invite_code VARCHAR(32) UNIQUE;
ALTER TABLE rooms ADD COLUMN invite_code_expires_at TIMESTAMP;

-- Backfill existing rows with UUID
UPDATE rooms SET public_id = gen_random_uuid()::VARCHAR WHERE public_id IS NULL;

-- Now make it NOT NULL
ALTER TABLE rooms ALTER COLUMN public_id SET NOT NULL;

-- Index for public_id lookups
CREATE INDEX idx_rooms_public_id ON rooms(public_id);

-- Index for invite_code lookups
CREATE INDEX idx_rooms_invite_code ON rooms(invite_code);
