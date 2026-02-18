-- V16: Add advertisement system (Phase 11)
-- Purpose: Gaming-related ads with 1-day 3x view limit

-- Create advertisements table
CREATE TABLE advertisements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    description TEXT,
    image_url VARCHAR(500),
    target_url VARCHAR(500) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_gaming_related BOOLEAN NOT NULL DEFAULT true,
    display_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

-- Create ad_views table (track daily limit)
CREATE TABLE ad_views (
    id BIGSERIAL PRIMARY KEY,
    ad_id BIGINT NOT NULL REFERENCES advertisements(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    viewed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Add indexes for performance
CREATE INDEX IF NOT EXISTS idx_ad_active_gaming
ON advertisements(is_active, is_gaming_related, display_order);

CREATE INDEX IF NOT EXISTS idx_ad_expires_at
ON advertisements(expires_at);

CREATE INDEX IF NOT EXISTS idx_ad_view_user_date
ON ad_views(user_id, viewed_at);

-- Add comments
COMMENT ON TABLE advertisements IS 'Advertisement data (Phase 11: Gaming-only, 1-day 3x limit)';
COMMENT ON TABLE ad_views IS 'Ad view tracking for daily limit enforcement';
COMMENT ON COLUMN advertisements.is_gaming_related IS 'Only gaming-related ads allowed';
COMMENT ON COLUMN advertisements.display_order IS 'Lower = higher priority';
