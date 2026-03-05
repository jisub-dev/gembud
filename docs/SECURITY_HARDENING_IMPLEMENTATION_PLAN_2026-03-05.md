# Gembud 운영 보안 하드닝 실행계획 (2026-03-05)

## 목적
- 운영 투입 전 공격/남용/이상접속 대응 체계를 최소 수준 이상으로 끌어올린다.
- 이미 확정된 제품 정책(단일세션, refresh rotation, 자동참가, 소프트삭제)을 보안적으로 안전하게 구현한다.

## 범위
- Backend: 인증/인가, CSRF, 세션, 토큰, WebSocket, 레이트리밋, 감사로그
- Frontend: 세션 만료 UX(강제 모달), CSRF 헤더, 에러 라우팅
- Ops: 환경변수, 알림, 대시보드 지표

---

## Phase 0 (D+0) - 즉시 차단 항목

### 0-1. 프리미엄 활성화 권한 잠금
- 작업:
  - `/subscriptions/activate`에 관리자 권한 강제
  - 일반 유저 요청 403 처리
- 파일:
  - `backend/src/main/java/com/gembud/controller/SubscriptionController.java`
- 완료 기준:
  - `USER` 토큰: 403
  - `ADMIN` 토큰: 정상 동작

### 0-2. 채팅 메시지 원문 로그 제거
- 작업:
  - WS 수신 로그에서 메시지 본문 제거(길이/해시만 남김 가능)
- 파일:
  - `backend/src/main/java/com/gembud/websocket/ChatWebSocketController.java`
- 완료 기준:
  - 로그에 사용자 채팅 평문이 남지 않음

### 0-3. 신고 중복정책 일치
- 작업:
  - room 기준 중복 체크 제거
  - “같은 대상 전체 기준(+기간)” 쿼리로 변경
- 파일:
  - `backend/src/main/java/com/gembud/repository/ReportRepository.java`
  - `backend/src/main/java/com/gembud/service/ReportService.java`
  - `backend/src/main/resources/db/migration/*`
- 완료 기준:
  - 다른 방이어도 동일 대상 재신고 차단

---

## Phase 1 (D+1~D+2) - 세션/토큰 보안

### 1-1. 단일세션 강제
- 작업:
  - 로그인 시 사용자 세션 버전 증가
  - JWT에 `sessionVersion` 클레임 포함
  - 필터에서 클레임 vs Redis/DB 버전 대조
  - 불일치 시 401 + 프론트 강제 만료 모달
- 파일:
  - `AuthService`, `JwtTokenProvider`, `JwtAuthenticationFilter`
  - (신규) `SessionService` / Redis 키 설계
- 완료 기준:
  - 새 로그인 즉시 기존 기기 API/WS 모두 무효화

### 1-2. Refresh Token Rotation
- 작업:
  - refresh 토큰 저장소는 Redis 단독 사용(초기 버전)
  - refresh 토큰 원문 저장 금지(해시 저장)
  - refresh 성공 시:
    - 기존 refresh 폐기
    - 신규 refresh 발급/저장
  - 폐기된 refresh 재사용 탐지 이벤트 기록
- 파일:
  - `AuthService`, `AuthController`
  - (신규) refresh session repository/entity
- 완료 기준:
  - old refresh 재사용 시 즉시 401

---

## Phase 2 (D+2~D+3) - CSRF/쿠키 정책

### 2-1. CSRF 보호 활성화
- 작업:
  - CSRF 토큰 쿠키 발급(`XSRF-TOKEN`)
  - 상태변경 요청에서 `X-XSRF-TOKEN` 강제
- 파일:
  - `SecurityConfig`
  - `frontend/src/services/api.ts`
- 완료 기준:
  - 토큰 없는 변경 요청은 403

### 2-2. 쿠키 정책 환경변수화
- 작업:
  - `COOKIE_SAMESITE`, `COOKIE_DOMAIN`, `COOKIE_SECURE` 적용
  - dev/prod 매트릭스 문서화
- 파일:
  - `AuthController`, `OAuth2SuccessHandler`, `application*.yml`
- 완료 기준:
  - 환경별 쿠키 속성 정확히 반영

---

## Phase 3 (D+3~D+4) - 레이트리밋/이상탐지

### 3-1. 레이트리밋
- 대상:
  - `/auth/login`, `/auth/refresh`, 주요 쓰기 API, WS CONNECT/SEND
- 작업:
  - IP+계정 기준 버킷 제한
  - 초과 시 429 + retry-after
- 완료 기준:
  - brute-force/폭주 요청 자동 차단

### 3-2. 감사로그(`security_events`)
- 작업:
  - 테이블 및 저장 서비스 추가
  - 이벤트 최소셋 저장:
    - LOGIN_SUCCESS/FAIL
    - REFRESH_SUCCESS/FAIL/REUSE
    - SESSION_REVOKED
    - WS_CONNECT_DENIED
    - RATE_LIMIT_HIT
- 완료 기준:
  - 보안 이벤트 조회/집계 가능

### 3-3. 운영 알림
- 작업:
  - HIGH/CRITICAL 이벤트 webhook 알림
  - 기준 임계치 설정
- 완료 기준:
  - 이상 징후 실시간 알림 수신

### 3-4. Slack 알림 세부 스펙 (운영 필수)
- 채널:
  - `#gembud-security-alerts` (기본 단일 채널)
- 전송 레벨:
  - `HIGH`, `CRITICAL`만 전송
- 이벤트 매핑:
  - `CRITICAL`: `REFRESH_REUSE_DETECTED`, 반복 계정탈취 의심, 세션변조 탐지
  - `HIGH`: `LOGIN_FAIL_BURST`, `WS_CONNECT_FLOOD`, `RATE_LIMIT_HIT_BURST`
- 중복 억제:
  - dedupe key = `event_type + user_id(or ip) + 10m window`
  - 동일 key는 10분 내 1회만 전송
- 재알림:
  - 동일 이슈 지속 시 30분 간격 요약 알림
- 메시지 포맷(Block Kit 단순형):
  - 제목: `[SEVERITY] event_type`
  - 본문: `userId`, `ip`, `endpoint`, `count`, `window`, `firstSeen`, `lastSeen`
  - 링크: Kibana/Grafana/내부 대시보드 URL
  - 액션: `Ack`, `Create Incident` 링크
- 장애 대비:
  - Slack 전송 실패 시 앱 처리 실패로 전파하지 않음
  - 실패 로그는 `WARN` + 재시도 큐(최대 3회)
- 보안:
  - webhook URL은 시크릿으로만 주입, 로그 출력 금지
  - 메시지에 토큰/비밀번호/초대코드 원문 포함 금지
- 온콜 운영:
  - HIGH: 30분 내 확인
  - CRITICAL: 10분 내 확인 + 임시 차단 룰 적용

---

## Phase 4 (D+4~D+5) - 비공개 방/초대코드 보안

### 4-1. 식별자/초대코드 체계
- 작업:
  - `public_id`, `invite_code`, `invite_code_expires_at`, `deleted_at` 반영
  - 재발급 시 기존 코드 즉시 무효
  - 재사용 영구 금지
  - 기본 TTL: 24h (`INVITE_CODE_TTL_HOURS`)
- 완료 기준:
  - 순차 PK 노출 없이 참여 제어 가능

### 4-2. 참여 UX 보안 정렬
- 작업:
  - 방 상세 제거, 목록에서 즉시 자동참가
  - 비공개 방은 비밀번호 입력 UI + 초대코드 검증 흐름
  - 404/403/500 공통 에러 페이지
- 완료 기준:
  - 정책 문서와 동작 완전 일치

---

## 검증 계획

## 자동 테스트
1. 단위 테스트:
- sessionVersion 검증
- refresh rotation/reuse 탐지
- inviteCode TTL 만료 처리

2. 통합 테스트:
- 로그인 후 다른 기기 로그인 시 기존 세션 차단
- CSRF 누락 403
- rate limit 429

3. 보안 회귀 테스트:
- refresh 재사용 공격
- WS 무인증/비멤버 메시지
- 신고 중복 우회

## 수동 스모크
1. 정상 로그인/로그아웃
2. 기존 세션 강제 만료 모달 노출
3. 초대코드 만료/재발급 동작
4. 운영 알림 수신 확인

---

## 운영 환경변수(최소)
- `COOKIE_SECURE`
- `COOKIE_SAMESITE`
- `COOKIE_DOMAIN`
- `SESSION_SINGLE_LOGIN_ENABLED`
- `REFRESH_TOKEN_ROTATION_ENABLED`
- `INVITE_CODE_TTL_HOURS`
- `LOGIN_RATE_LIMIT_PER_MIN`
- `REFRESH_RATE_LIMIT_PER_MIN`
- `WS_CONNECT_RATE_LIMIT_PER_MIN`
- `SECURITY_ALERT_WEBHOOK_URL`
- `REPORT_DUPLICATE_BLOCK_DAYS` (default: `7`)
- `LOGIN_ACCOUNT_LOCK_THRESHOLD` (계정 기준 잠금 임계치)
- `LOGIN_LOCK_DURATION_MINUTES` (default: `10`)
- `SECURITY_EVENT_RETENTION_DAYS` (default: `90`)
- `LOGIN_LOCK_THRESHOLD` (default: `10`)
- `LOGIN_LOCK_WINDOW_MINUTES` (default: `10`)

---

## Go/No-Go 게이트
- [ ] Phase 0/1 완료
- [ ] `./gradlew test` 컨텍스트 실패 0
- [ ] 인증/세션/CSRF E2E 통과
- [ ] 보안 이벤트 저장 + 알림 동작 확인
- [ ] 롤백 절차 문서화 완료

---

## 확정값 (Owner Decision)
1. 신고 중복 차단 기간: `7일` 디폴트 (`REPORT_DUPLICATE_BLOCK_DAYS=7`)
2. 로그인 실패 잠금: 계정 기준 우선 적용 (IP 기준은 추후 확장)
3. 단일세션 강제 시 WS: 세션 만료 즉시 강제 종료
4. 보안 이벤트 보존기간: `90일`
5. 운영 알림 채널: Slack webhook(무료 플랜) 고정
6. Slack 알림 레벨: `HIGH + CRITICAL`
7. 보안 이벤트 삭제: 매일 새벽 3시 스케줄러
8. 로그인 잠금 기본값: `10회 실패 / 10분 잠금`
9. 세션 만료 모달:
   - 문구: "다른 기기에서 로그인되어 세션이 종료되었습니다"
   - 버튼: 로그인 페이지로 이동(1개)
10. 로그인 잠금 해제 API:
   - `DELETE /admin/users/{userId}/login-lock`
   - 응답 `204 No Content`
11. 잠금 저장소: Redis only
12. 로그인 실패 카운트: 존재하지 않는 이메일도 동일 카운트
13. Slack 알림: 스레드 규칙 없이 단순 메시지 전송
