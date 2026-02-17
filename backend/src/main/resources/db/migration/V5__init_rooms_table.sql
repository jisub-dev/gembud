-- Create rooms table
CREATE TABLE rooms (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    max_participants INT NOT NULL DEFAULT 5,
    current_participants INT NOT NULL DEFAULT 0,
    is_private BOOLEAN NOT NULL DEFAULT FALSE,
    password VARCHAR(255),
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    created_by BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_max_participants CHECK (max_participants > 0 AND max_participants <= 100),
    CONSTRAINT chk_current_participants CHECK (current_participants >= 0 AND current_participants <= max_participants),
    CONSTRAINT chk_status CHECK (status IN ('OPEN', 'FULL', 'IN_PROGRESS', 'CLOSED'))
);

-- Create index on game_id for fast lookup
CREATE INDEX idx_rooms_game_id ON rooms(game_id);

-- Create index on status for filtering
CREATE INDEX idx_rooms_status ON rooms(status);

-- Create index on created_by for user's rooms
CREATE INDEX idx_rooms_created_by ON rooms(created_by);

-- Create trigger to update updated_at
CREATE OR REPLACE FUNCTION update_rooms_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trigger_update_rooms_updated_at
    BEFORE UPDATE ON rooms
    FOR EACH ROW
    EXECUTE FUNCTION update_rooms_updated_at();

-- Add comments
COMMENT ON COLUMN rooms.status IS 'Room status: OPEN, FULL, IN_PROGRESS, CLOSED';
COMMENT ON COLUMN rooms.is_private IS 'Whether room requires password or invitation';
