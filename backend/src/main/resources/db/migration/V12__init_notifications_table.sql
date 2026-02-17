-- V12__init_notifications_table.sql
-- 실시간 알림 시스템 테이블

CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    related_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- 인덱스 생성
CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_created_at ON notifications(created_at DESC);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
CREATE INDEX idx_notifications_user_unread ON notifications(user_id, is_read) WHERE is_read = false;

-- 코멘트 추가
COMMENT ON TABLE notifications IS '사용자 알림';
COMMENT ON COLUMN notifications.id IS '알림 ID';
COMMENT ON COLUMN notifications.user_id IS '수신자 ID';
COMMENT ON COLUMN notifications.type IS '알림 타입 (FRIEND_REQUEST, ROOM_INVITE, EVALUATION 등)';
COMMENT ON COLUMN notifications.content IS '알림 내용';
COMMENT ON COLUMN notifications.related_id IS '관련 엔티티 ID (친구 요청 ID, 방 ID 등)';
COMMENT ON COLUMN notifications.is_read IS '읽음 여부';
COMMENT ON COLUMN notifications.created_at IS '생성 시간';
