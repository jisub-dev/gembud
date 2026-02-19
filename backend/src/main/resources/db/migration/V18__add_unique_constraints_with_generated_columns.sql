-- V18: Phase 12 - DB 유니크 제약 (Generated Column)
-- 목적: 중복 액션 차단 (광고 보기, 평가)
-- 작성일: 2026-02-19

-- ========================================
-- 1. ad_views: 하루 한 번 광고 보기 제한
-- ========================================

-- viewed_date Generated Column 추가 (viewed_at의 날짜 부분만 추출)
ALTER TABLE ad_views
ADD COLUMN viewed_date DATE GENERATED ALWAYS AS (DATE(viewed_at)) STORED;

-- 유니크 제약: 동일 유저가 하루에 같은 광고를 두 번 볼 수 없음
CREATE UNIQUE INDEX idx_ad_views_user_ad_date
ON ad_views (user_id, ad_id, viewed_date);

COMMENT ON COLUMN ad_views.viewed_date IS 'Phase 12: Generated column for daily uniqueness constraint';
COMMENT ON INDEX idx_ad_views_user_ad_date IS 'Phase 12: Prevent duplicate ad views per day';

-- ========================================
-- 2. evaluations: 방당 1회 평가 제한
-- ========================================

-- 유니크 제약: 동일 평가자가 동일 방에서 동일 피평가자를 두 번 평가할 수 없음
CREATE UNIQUE INDEX idx_evaluations_evaluator_evaluated_room
ON evaluations (evaluator_id, evaluated_id, room_id);

COMMENT ON INDEX idx_evaluations_evaluator_evaluated_room IS 'Phase 12: Prevent duplicate evaluations per room';

-- ========================================
-- 3. reports: 7일 내 동일 대상 재신고 차단
-- ========================================

-- 참고: 7일 쿨다운은 애플리케이션 레벨에서 처리
-- (시간 기반 제약은 DB 유니크 제약으로 불가능)
-- 대신 조회 최적화를 위한 인덱스 추가

CREATE INDEX idx_reports_reporter_reported_created
ON reports (reporter_id, reported_id, created_at DESC);

COMMENT ON INDEX idx_reports_reporter_reported_created IS 'Phase 12: Optimize duplicate report cooldown check';
