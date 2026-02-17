-- Create room_participants table
CREATE TABLE room_participants (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    is_host BOOLEAN NOT NULL DEFAULT FALSE,
    join_order INT NOT NULL,
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_room_user UNIQUE (room_id, user_id)
);

-- Create indexes
CREATE INDEX idx_room_participants_room_id ON room_participants(room_id);
CREATE INDEX idx_room_participants_user_id ON room_participants(user_id);
CREATE INDEX idx_room_participants_join_order ON room_participants(room_id, join_order);

-- Create room_filters table for room matching conditions
CREATE TABLE room_filters (
    id BIGSERIAL PRIMARY KEY,
    room_id BIGINT NOT NULL REFERENCES rooms(id) ON DELETE CASCADE,
    option_key VARCHAR(50) NOT NULL,
    option_value VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on room_id
CREATE INDEX idx_room_filters_room_id ON room_filters(room_id);

-- Add comments
COMMENT ON COLUMN room_participants.is_host IS 'Whether this participant is the room host';
COMMENT ON COLUMN room_participants.join_order IS 'Order of joining, used for host transfer';
COMMENT ON COLUMN room_filters.option_key IS 'Filter option key (e.g., tier, position, age_range)';
COMMENT ON COLUMN room_filters.option_value IS 'Filter option value';
