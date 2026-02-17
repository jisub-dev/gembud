-- Create chat_rooms table
CREATE TABLE chat_rooms (
    id BIGSERIAL PRIMARY KEY,
    type VARCHAR(20) NOT NULL,
    related_room_id BIGINT REFERENCES rooms(id) ON DELETE CASCADE,
    name VARCHAR(100),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_chat_room_type CHECK (type IN ('ROOM_CHAT', 'GROUP_CHAT', 'DIRECT_CHAT'))
);

-- Create index on related_room_id
CREATE INDEX idx_chat_rooms_related_room_id ON chat_rooms(related_room_id);

-- Create index on type
CREATE INDEX idx_chat_rooms_type ON chat_rooms(type);

-- Add comments
COMMENT ON COLUMN chat_rooms.type IS 'Chat room type: ROOM_CHAT (not saved), GROUP_CHAT (last 100), DIRECT_CHAT (all saved)';
COMMENT ON COLUMN chat_rooms.related_room_id IS 'Related room ID for ROOM_CHAT type';
COMMENT ON COLUMN chat_rooms.name IS 'Chat room name for GROUP_CHAT';
