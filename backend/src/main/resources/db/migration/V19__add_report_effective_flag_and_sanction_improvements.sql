-- V19: Phase 12 - 자동 제재 개선 (신고자 신뢰도 + Sybil 방어)
-- 목적: 저품질 신고 필터링, 다계정 공격 방어
-- 작성일: 2026-02-19

-- ========================================
-- 1. reports: effective_for_sanction 필드 추가
-- ========================================

-- 신고가 자동 제재 집계에 포함되는지 여부
ALTER TABLE reports
ADD COLUMN effective_for_sanction BOOLEAN DEFAULT TRUE NOT NULL;

COMMENT ON COLUMN reports.effective_for_sanction IS 'Phase 12: Whether this report counts toward auto-sanction (filters low-trust reporters)';

-- ========================================
-- 2. 인덱스: effective_for_sanction 조회 최적화
-- ========================================

-- 자동 제재 집계 쿼리 최적화 (effective=true인 신고만 집계)
CREATE INDEX idx_reports_effective_reported_category
ON reports (reported_id, category, effective_for_sanction, created_at DESC)
WHERE effective_for_sanction = TRUE;

COMMENT ON INDEX idx_reports_effective_reported_category IS 'Phase 12: Optimize auto-sanction aggregation (only effective reports)';

-- ========================================
-- 3. 기존 데이터 마이그레이션
-- ========================================

-- 기존 신고 데이터: 신고자 신뢰도 기반으로 effective 플래그 설정
-- (온도 25°C 미만 또는 가입 7일 미만인 신고자의 신고는 FALSE)

UPDATE reports r
SET effective_for_sanction = FALSE
FROM users u
WHERE r.reporter_id = u.id
  AND (
    u.temperature < 25
    OR u.created_at > NOW() - INTERVAL '7 days'
  );

-- 통계 로깅 (변경된 레코드 수 확인용)
DO $$
DECLARE
    ineffective_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO ineffective_count
    FROM reports
    WHERE effective_for_sanction = FALSE;

    RAISE NOTICE 'Phase 12 Migration V19: % reports marked as ineffective', ineffective_count;
END $$;

-- ========================================
-- 4. 임계치 조정 가이드 (애플리케이션 레벨)
-- ========================================

-- Phase 12 자동 제재 임계치:
-- - CRITICAL 카테고리 (FRAUD, SEXUAL_HARASSMENT, THREAT): 4명
-- - 일반 카테고리 (VERBAL_ABUSE, INAPPROPRIATE_CONTENT 등): 6명
-- - 조건: effective_for_sanction=TRUE인 유니크 신고자만 집계
-- - 기간: 7일 이내
-- - 신고자 제외: 온도 25°C 미만 또는 가입 7일 미만

COMMENT ON TABLE reports IS 'Phase 12: Auto-sanction thresholds - CRITICAL: 4 unique reporters, General: 6 unique reporters (effective_for_sanction=TRUE only)';
