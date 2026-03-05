# Gembud 운영 보안 점검 보고서 (2026-03-05)

## 범위
- 백엔드 인증/인가, WebSocket, 신고/제재, 운영 로깅/탐지 관점 점검
- 기준: 현재 `main` 워크스페이스 코드 상태

## 요약
- 즉시 조치가 필요한 고위험 이슈 5건
- 이상 유저/이상 접속 탐지 체계는 현재 거의 미구현
- 운영 투입 전 최소 “인증 보호 + 레이트리밋 + 감사로그 + 알림” 4축 구축 필요

---

## Findings (심각도순)

### [Critical] 프리미엄 활성화 API 권한 통제 부재 (비즈니스 남용)
- 근거: [SubscriptionController.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/controller/SubscriptionController.java:42)
- 문제:
  - 주석은 “관리자/테스트 전용”인데 실제로는 인증 사용자면 누구나 `/subscriptions/activate` 호출 가능
- 공격 시나리오:
  - 일반 유저가 반복 호출해 유료 기능 무제한 사용
- 조치:
  - `@PreAuthorize("hasRole('ADMIN')")` 또는 운영 결제 이벤트 전용 엔드포인트로 분리
  - 일반 사용자용 활성화 엔드포인트는 결제 검증 완료 후 내부 호출만 허용

### [Critical] 쿠키 인증 기반인데 CSRF 전면 비활성
- 근거: [SecurityConfig.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/config/SecurityConfig.java:51)
- 문제:
  - 상태 변경 API가 쿠키 인증을 사용하지만 CSRF 보호가 꺼져 있음
  - 정책상 `SameSite=Lax/None`으로 갈 가능성이 있어 위험 증가
- 공격 시나리오:
  - 악성 사이트 유도 후 사용자 권한으로 상태 변경 요청 유발
- 조치:
  - CSRF 토큰 도입(`CookieCsrfTokenRepository`) + 프론트 헤더 전송
  - 최소한 민감 엔드포인트(`POST/PUT/PATCH/DELETE`)에 CSRF 강제

### [High] 단일세션/Refresh Rotation 미구현
- 근거: [AuthService.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/service/AuthService.java:80), [AuthService.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/service/AuthService.java:110)
- 문제:
  - 로그인 시 기존 세션 무효화 로직 없음
  - refresh 시 기존 refresh 토큰 재사용 가능(회전 미적용)
- 공격 시나리오:
  - 탈취된 refresh 토큰 장기 재사용
  - 동시 로그인 금지 정책 위반
- 조치:
  - Redis에 `sessionVersion` 또는 `activeRefreshTokenHash` 저장
  - refresh 성공 시 토큰 회전 + 이전 토큰 즉시 폐기
  - JWT 검증 시 버전/세션 키 대조

### [High] 인증/리프레시/소켓에 레이트리밋 부재
- 근거: 인증/리프레시/WS 관련 코드 전반(제한 로직 없음)
- 문제:
  - 비밀번호 대입, refresh endpoint 폭주, WS 연결/메시지 스팸 방어 없음
- 공격 시나리오:
  - Credential stuffing
  - Refresh token brute-force
  - WS connection flood / message flood
- 조치:
  - 로그인/refresh/IP+계정 기준 토큰 버킷
  - WS CONNECT/메시지 초당 제한
  - 초과 시 429 + 지수 백오프 + 잠금 정책

### [High] 이상 접속/이상 행위 탐지를 위한 감사 로그 모델 부재
- 근거: 보안 이벤트 저장 엔티티/테이블 부재 (`activity`, `audit`, `login_history` 검색 결과 없음)
- 문제:
  - 사고 발생 시 포렌식 불가
  - 정책 위반 탐지(다중 지역 로그인, 실패 급증, 신고 남용) 불가
- 조치:
  - `security_events` 테이블 도입:
    - `event_type`, `user_id`, `ip`, `user_agent`, `path`, `result`, `risk_score`, `created_at`
  - 최소 이벤트 기록:
    - LOGIN_SUCCESS/FAIL, REFRESH_SUCCESS/FAIL, SESSION_REVOKED, WS_CONNECT_DENIED, RATE_LIMIT_HIT

### [Medium] `/ws` permitAll + 별도 CONNECT 인증 인터셉터 부재
- 근거: [SecurityConfig.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/config/SecurityConfig.java:66), [WebSocketConfig.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/config/WebSocketConfig.java:46)
- 문제:
  - 핸드셰이크 진입 자체는 공개
  - CONNECT 단계 인증/권한 검사 채널 인터셉터 없음
- 공격 시나리오:
  - 비인증 연결 남발로 자원 소모
- 조치:
  - `configureClientInboundChannel` 인터셉터로 CONNECT 시 JWT/세션 검증
  - 비인증 CONNECT 즉시 차단

### [Medium] 채팅 메시지 원문이 sanitize 전 로그에 기록됨
- 근거: [ChatWebSocketController.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/websocket/ChatWebSocketController.java:64)
- 문제:
  - 악성 문자열/개인정보가 로그로 남을 수 있음
  - 로그 인젝션 가능성
- 조치:
  - 메시지 본문 로깅 금지 또는 길이/문자 제한 후 마스킹

### [Medium] 신고 중복 차단이 room 단위(정책과 불일치)
- 근거: [ReportService.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/service/ReportService.java:75)
- 문제:
  - 확정 정책은 “같은 대상 전체 기준”인데 현재는 같은 room에서만 차단
- 조치:
  - 리포지토리 쿼리를 reporter+reported(+기간) 기준으로 변경

### [Medium] 쿠키 SameSite/Domain 환경분리 미반영
- 근거: [AuthController.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/controller/AuthController.java:145), [AuthController.java](/Users/gimjiseob/Projects/gembud/backend/src/main/java/com/gembud/controller/AuthController.java:203)
- 문제:
  - `SameSite("Strict")` 하드코딩
  - 운영 정책(`Lax/None`)과 코드 불일치 가능
- 조치:
  - `COOKIE_SAMESITE`, `COOKIE_DOMAIN` 환경변수화

---

## 공격/이상 행위 탐지 설계 (운영용)

## 1) 이벤트 수집
- 필수 공통 필드:
  - `ts`, `event_type`, `user_id`, `ip`, `ua`, `endpoint`, `result`, `latency_ms`
- 필수 이벤트:
  - `LOGIN_FAIL`, `LOGIN_SUCCESS`, `REFRESH_FAIL`, `REFRESH_REUSE_DETECTED`
  - `SESSION_REVOKED`, `WS_CONNECT_DENIED`, `WS_RATE_LIMIT_HIT`
  - `REPORT_CREATED`, `REPORT_DUPLICATE_BLOCKED`, `AUTO_SANCTION_TRIGGERED`

## 2) 이상 탐지 규칙 (초기값)
- 로그인 실패 급증:
  - 동일 IP 5분 내 실패 20회 이상 -> `HIGH`
  - 동일 계정 10분 내 실패 10회 이상 -> `HIGH`
- Refresh 재사용 탐지:
  - 폐기된 refresh 재사용 즉시 `CRITICAL`
- WebSocket 이상:
  - 동일 IP 동시 연결 50 초과 -> `HIGH`
  - 사용자당 초당 메시지 20 초과 -> `MEDIUM`
- 신고 남용:
  - 동일 user가 1시간 내 신고 30회 이상 -> `MEDIUM`

## 3) 자동 대응
- `MEDIUM`: 경고 로그 + 알림 채널 전송(Slack/Discord/Webhook)
- `HIGH`: 일시 차단(예: 10~30분) + 보안팀/운영자 알림
- `CRITICAL`: 세션 강제 폐기 + 계정 잠금 + 긴급 알림

## 4) 대시보드 최소 지표
- 인증:
  - 로그인 성공률, 실패율, 상위 실패 IP, refresh 실패율
- 세션:
  - 강제 로그아웃 횟수, 동시세션 차단 횟수
- WS:
  - 연결 수, 연결 실패율, 초당 메시지 수, 차단 이벤트 수
- 신고/제재:
  - 일별 신고량, 중복 차단 수, 자동 제재 수

---

## 운영 투입 전 필수 체크리스트 (Go/No-Go)
- [ ] `/subscriptions/activate` 권한 통제 완료
- [ ] CSRF 보호 적용(또는 쿠키 전략과 함께 명시적 위험 수용 문서화)
- [ ] 단일세션 + refresh rotation 완료
- [ ] 로그인/refresh/WS rate limit 적용
- [ ] 보안 이벤트 감사로그 저장 및 알림 연동
- [ ] `SameSite`, `Secure`, `Domain` 환경변수 분리
- [ ] 비밀값( JWT_SECRET 등 ) 회전 절차(runbook) 문서화

---

## 권장 구현 순서 (1주)
1. 인증 보호: 단일세션/토큰회전/CSRF
2. 남용 방어: rate limit + lockout
3. 탐지: security_events 저장 + 알림
4. WS 하드닝: CONNECT 인터셉터 + flood 제한
5. 운영 대시보드/런북 정리
