-- Create game_options table for game-specific matching options
CREATE TABLE game_options (
    id BIGSERIAL PRIMARY KEY,
    game_id BIGINT NOT NULL REFERENCES games(id) ON DELETE CASCADE,
    option_key VARCHAR(50) NOT NULL,
    option_type VARCHAR(20) NOT NULL,
    option_values JSONB,
    is_common BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_game_option UNIQUE (game_id, option_key)
);

-- Create index on game_id for fast lookup
CREATE INDEX idx_game_options_game_id ON game_options(game_id);

-- Create index on option_key
CREATE INDEX idx_game_options_key ON game_options(option_key);

-- Add comments
COMMENT ON COLUMN game_options.option_key IS 'Option identifier (e.g., position, tier, age_range)';
COMMENT ON COLUMN game_options.option_type IS 'Option type (SELECT, MULTI_SELECT, RANGE, BOOLEAN)';
COMMENT ON COLUMN game_options.option_values IS 'JSON array of possible values';
COMMENT ON COLUMN game_options.is_common IS 'Whether this is a common option across all games';
