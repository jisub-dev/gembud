-- V11__init_reports_table.sql
-- 신고 시스템 테이블

CREATE TABLE reports (
    id BIGSERIAL PRIMARY KEY,
    reporter_id BIGINT NOT NULL,
    reported_id BIGINT NOT NULL,
    room_id BIGINT,
    reason VARCHAR(50) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP,
    resolved_at TIMESTAMP,
    admin_comment TEXT,

    CONSTRAINT fk_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_reported FOREIGN KEY (reported_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_reports_room FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE SET NULL,
    CONSTRAINT chk_reports_not_self CHECK (reporter_id != reported_id)
);

-- 인덱스 생성
CREATE INDEX idx_reports_reporter ON reports(reporter_id);
CREATE INDEX idx_reports_reported ON reports(reported_id);
CREATE INDEX idx_reports_status ON reports(status);
CREATE INDEX idx_reports_created_at ON reports(created_at DESC);

-- 코멘트 추가
COMMENT ON TABLE reports IS '신고 정보';
COMMENT ON COLUMN reports.id IS '신고 ID';
COMMENT ON COLUMN reports.reporter_id IS '신고자 ID';
COMMENT ON COLUMN reports.reported_id IS '신고 받은 사용자 ID';
COMMENT ON COLUMN reports.room_id IS '신고 발생 방 ID (nullable)';
COMMENT ON COLUMN reports.reason IS '신고 사유';
COMMENT ON COLUMN reports.description IS '상세 설명';
COMMENT ON COLUMN reports.status IS '신고 상태 (PENDING, REVIEWED, RESOLVED)';
COMMENT ON COLUMN reports.created_at IS '신고 생성 시간';
COMMENT ON COLUMN reports.reviewed_at IS '검토 시작 시간';
COMMENT ON COLUMN reports.resolved_at IS '처리 완료 시간';
COMMENT ON COLUMN reports.admin_comment IS '관리자 코멘트';
