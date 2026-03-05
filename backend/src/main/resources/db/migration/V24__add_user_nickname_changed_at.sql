-- Add nickname_changed_at to track 30-day cooldown on nickname changes
ALTER TABLE users ADD COLUMN nickname_changed_at TIMESTAMP;
