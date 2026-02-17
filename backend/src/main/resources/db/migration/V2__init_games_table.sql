-- Create games table
CREATE TABLE games (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    image_url VARCHAR(500),
    genre VARCHAR(50),
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create index on name for fast lookup
CREATE INDEX idx_games_name ON games(name);

-- Create index on genre for filtering
CREATE INDEX idx_games_genre ON games(genre);
