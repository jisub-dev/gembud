-- Add soft delete support to rooms table
ALTER TABLE rooms ADD COLUMN deleted_at TIMESTAMP;

-- Index for filtering out soft-deleted rooms
CREATE INDEX idx_rooms_deleted_at ON rooms(deleted_at);
