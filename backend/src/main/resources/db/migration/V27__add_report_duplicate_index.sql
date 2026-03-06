-- Add index to support global duplicate report check within a time window
CREATE INDEX IF NOT EXISTS idx_reports_reporter_reported_created
    ON reports(reporter_id, reported_id, created_at);
