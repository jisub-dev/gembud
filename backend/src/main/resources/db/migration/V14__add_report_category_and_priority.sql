-- V14: Add report category and priority (Phase 11)
-- Purpose: Improve report handling with categorization and auto-prioritization

-- Add category column
ALTER TABLE reports
ADD COLUMN category VARCHAR(30) NOT NULL DEFAULT 'VERBAL_ABUSE';

-- Add priority column
ALTER TABLE reports
ADD COLUMN priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM';

-- Add index for priority-based queries
CREATE INDEX IF NOT EXISTS idx_report_priority_status
ON reports(priority, status, created_at);

-- Add index for category queries
CREATE INDEX IF NOT EXISTS idx_report_category
ON reports(category, created_at);

-- Comment on columns
COMMENT ON COLUMN reports.category IS 'Report category: VERBAL_ABUSE, GAME_DISRUPTION, HARASSMENT, FRAUD, FALSE_INFO';
COMMENT ON COLUMN reports.priority IS 'Report priority: LOW, MEDIUM, HIGH, CRITICAL';
