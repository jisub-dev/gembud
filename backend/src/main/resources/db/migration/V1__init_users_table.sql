-- Users table for authentication and profile
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE,
    password VARCHAR(255),
    nickname VARCHAR(50) NOT NULL,
    profile_image_url VARCHAR(500),
    age_range VARCHAR(20),
    temperature DECIMAL(4, 2) NOT NULL DEFAULT 36.5,
    oauth_provider VARCHAR(20),
    oauth_id VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    CONSTRAINT chk_temperature CHECK (temperature >= 0 AND temperature <= 100),
    CONSTRAINT chk_oauth CHECK (
        (oauth_provider IS NULL AND oauth_id IS NULL) OR
        (oauth_provider IS NOT NULL AND oauth_id IS NOT NULL)
    ),
    CONSTRAINT chk_auth CHECK (
        (email IS NOT NULL AND password IS NOT NULL) OR
        (oauth_provider IS NOT NULL AND oauth_id IS NOT NULL)
    )
);

-- Index for email lookup
CREATE INDEX idx_users_email ON users(email);

-- Index for OAuth lookup
CREATE INDEX idx_users_oauth ON users(oauth_provider, oauth_id);

-- Index for nickname search
CREATE INDEX idx_users_nickname ON users(nickname);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Trigger to automatically update updated_at
CREATE TRIGGER update_users_updated_at
    BEFORE UPDATE ON users
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Comments for documentation
COMMENT ON TABLE users IS 'User accounts with authentication and profile information';
COMMENT ON COLUMN users.temperature IS 'User reputation score (0-100), default 36.5 like Karrot Market';
COMMENT ON COLUMN users.oauth_provider IS 'OAuth provider: google, discord, or null for email/password';
COMMENT ON COLUMN users.age_range IS 'Age range: 10대, 20대, 30대, etc.';
