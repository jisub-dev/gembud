# 병렬 실행 계획서 (Claude Code vs Codex) - 2026-03-06

## 1. 목표
- 운영 직전 안정화를 빠르게 끝내기 위해 작업을 2개 스트림으로 병렬화한다.
- 충돌 없이 머지 가능하도록 파일 소유권, 인터페이스, 머지 순서를 고정한다.
- 완료 기준은 “코드 반영 + 테스트 통과 + 문서 갱신” 3가지 모두 충족이다.

## 2. 공통 작업 규칙
1. 브랜치
- Claude Code: `feat/security-backend-hardening`
- Codex: `feat/frontend-hardening-and-runbook`

2. 커밋 규칙
- 한 커밋은 하나의 목적만 포함한다.
- 커밋 메시지 형식: `type(scope): summary`
- 예시: `feat(auth): enforce single-session revocation`

3. 파일 소유권
- 같은 파일을 두 에이전트가 동시에 수정하지 않는다.
- 공용 파일(`application.yml`, 문서)은 머지 순서에 따라 한쪽만 최종 수정한다.

4. 인터페이스 고정
- 에러 응답 필드: `status`, `code`, `message`, `path`, `timestamp`
- 세션 만료 코드: `AUTH004`(만료), `AUTH007`(refresh invalid) 유지
- 프리미엄 OFF 동작: API 미노출(404), UI 비노출

5. 검증 공통 명령
- Frontend: `cd frontend && npm test -- --run && npm run build`
- Backend: `cd backend && ./gradlew test && ./gradlew compileJava`

## 3. 작업 분할 (최종)

## 3-1. Claude Code 담당 (Backend 중심, 고위험 로직)
1. 인증/세션 보안
- 범위:
  - 단일세션 강제(새 로그인 시 기존 세션 즉시 무효)
  - refresh token rotation/reuse 탐지
  - WS 세션 만료 즉시 강제 종료
- 대상 파일(예상):
  - `backend/src/main/java/com/gembud/service/AuthService.java`
  - `backend/src/main/java/com/gembud/security/JwtTokenProvider.java`
  - `backend/src/main/java/com/gembud/security/JwtAuthenticationFilter.java`
  - `backend/src/main/java/com/gembud/config/WebSocketConfig.java`
  - `backend/src/main/java/com/gembud/websocket/ChatWebSocketController.java`
- 완료 기준:
  - 새 로그인 이후 기존 access/refresh 사용 불가
  - WS 연결이 세션 만료 시 즉시 종료
  - 재사용 refresh 탐지 이벤트 저장

2. 로그인 잠금/레이트리밋
- 범위:
  - 계정 기준 `10회/10분` 잠금
  - 존재하지 않는 이메일도 동일 카운트
  - 관리자 수동 해제 API
- 대상 파일(예상):
  - `backend/src/main/java/com/gembud/service/RateLimitService.java`
  - `backend/src/main/java/com/gembud/controller/AuthController.java`
  - `backend/src/main/java/com/gembud/controller/AdminController.java` (없으면 신규)
  - `backend/src/main/java/com/gembud/entity/User.java`
- 완료 기준:
  - `DELETE /admin/users/{userId}/login-lock` 204 동작
  - 잠금 10분 자동 해제

3. 보안 이벤트 + Slack 알림
- 범위:
  - `security_events` 90일 보존
  - HIGH/CRITICAL Slack webhook 전송
  - 429/전송실패 재시도(backoff) + 중복 억제
- 대상 파일(예상):
  - `backend/src/main/java/com/gembud/service/SecurityEventService.java`
  - `backend/src/main/java/com/gembud/service/SlackAlertService.java`
  - `backend/src/main/java/com/gembud/repository/SecurityEventRepository.java`
  - `backend/src/main/resources/application.yml`
- 완료 기준:
  - HIGH/CRITICAL 이벤트 발생 시 Slack 수신
  - 실패해도 서비스 본 요청은 정상 지속

4. API 테스트 보강
- 범위:
  - 보안 정책 회귀 테스트
  - 프리미엄 OFF 시 `/subscriptions/**` 404 유지
- 대상 파일(예상):
  - `backend/src/test/java/com/gembud/controller/*`
  - `backend/src/test/java/com/gembud/service/*`
- 완료 기준:
  - 신규 테스트 통과
  - 기존 테스트 회귀 없음

## 3-2. Codex 담당 (Frontend + 통합 정책 + 문서/운영)
1. 프론트 세션 만료 UX 고정
- 범위:
  - “다른 기기에서 로그인됨” 강제 모달(확인 1버튼)
  - 보호 라우트에서 만료 상태 일관 처리
- 대상 파일:
  - `frontend/src/components/common/SessionExpiredModal.tsx`
  - `frontend/src/services/api.ts`
  - `frontend/src/App.tsx`
  - `frontend/src/store/authStore.ts`
- 완료 기준:
  - 세션 만료 이벤트 시 항상 모달 노출
  - 확인 전 핵심 동작 차단

2. 프리미엄 다크런치 정책 마감
- 범위:
  - 현재 반영된 OFF 정책 회귀 방지
  - 메뉴/라우트/문구/API 시나리오 E2E 체크리스트 고정
- 대상 파일:
  - `frontend/src/config/features.ts`
  - `frontend/src/components/layout/Header.tsx`
  - `frontend/src/pages/TermsPage.tsx`
  - `docs/PREMIUM_FEATURE_TOGGLE_RUNBOOK_2026-03-05.md`
- 완료 기준:
  - OFF에서 노출 0, ON에서 즉시 복구

3. 에러 페이지/라우팅 품질
- 범위:
  - 404/403/500 공통 UX 점검
  - API 에러 매핑 표준 문서화
- 대상 파일:
  - `frontend/src/pages/ErrorPage.tsx`
  - `frontend/src/App.tsx`
  - `docs/` 내 운영 가이드
- 완료 기준:
  - 정책표와 실제 화면 동작 일치

4. 최종 통합 문서/릴리즈 게이트
- 범위:
  - 운영 점검표, 롤백 절차, QA 시나리오 문서 최종판
- 대상 파일:
  - `docs/PREMIUM_FEATURE_TOGGLE_RUNBOOK_2026-03-05.md`
  - `docs/OPERATIONS_SECURITY_REVIEW_2026-03-05.md`
  - `docs/SECURITY_HARDENING_IMPLEMENTATION_PLAN_2026-03-05.md`
- 완료 기준:
  - 운영자가 문서만 보고 배포/롤백 수행 가능

## 4. 병렬 순서도 (의존성)
1. Claude Code 1차 완료
- 세션/토큰/잠금 API 스펙 확정

2. Codex 연동 반영
- 프론트 만료 UX, 에러 매핑, 운영 문서 반영

3. Claude Code 2차
- 프론트 연동 피드백 반영(상태 코드/메시지 미세조정)

4. Codex 최종
- 통합 테스트, 최종 런북 업데이트, Go/No-Go 판정표 작성

## 5. 머지 전략 (충돌 방지)
1. 1차 머지: Claude Code 브랜치
- 이유: 백엔드 API/보안 정책이 인터페이스 기준점

2. 2차 리베이스: Codex 브랜치
- Claude 머지 후 `main` 기준 rebase
- 프론트 에러 처리/문서를 최신 API 기준으로 정합화

3. 3차 머지: Codex 브랜치
- 문서 포함 최종 반영

## 6. 검증 플랜 (상세)
1. 자동 테스트
- Backend:
  - `./gradlew test --tests com.gembud.controller.SubscriptionControllerToggleTest`
  - `./gradlew test --tests com.gembud.service.AuthServiceTest`
  - `./gradlew test --tests com.gembud.service.ReportServiceTest`
  - `./gradlew test`
- Frontend:
  - `npm test -- --run`
  - `npm run build`

2. 수동 시나리오
- 시나리오 A: 단일세션
  - 디바이스 A 로그인
  - 디바이스 B 로그인
  - 디바이스 A API/WS 즉시 만료 + 모달 확인
- 시나리오 B: 프리미엄 OFF
  - `/premium` 접근 -> 404
  - `/api/subscriptions/status` 인증 사용자 -> 404
  - 광고/추천 제한이 무료 기준 유지
- 시나리오 C: 잠금
  - 10회 실패 -> 잠금
  - 10분 경과 -> 자동 해제
  - 관리자 API -> 즉시 해제
- 시나리오 D: Slack
  - HIGH 이벤트 발생 -> Slack 메시지 수신
  - 전송 실패 시 앱 요청은 성공 유지

## 7. 완료 정의 (DoD)
1. 기능 DoD
- 정책 문서와 실제 동작 1:1 일치
- 단일세션/잠금/프리미엄 OFF/에러 라우팅 모두 확인

2. 품질 DoD
- 테스트 전부 통과
- 신규 회귀 테스트 추가 완료

3. 운영 DoD
- env 키/기본값/롤백 절차 문서화 완료
- Slack webhook/보존/스케줄러 운영 체크 완료

## 8. 오늘 바로 실행할 체크리스트
1. Claude Code에게 아래 묶음 전달
- 인증/세션/잠금/Slack/백엔드 테스트만 담당
- 프론트/문서 파일 수정 금지

2. Codex는 아래 묶음 즉시 수행
- 프론트 만료 UX + 에러 라우팅 + 다크런치 회귀 테스트 + 런북 보강

3. 저녁 통합 회의(짧게)
- 상태코드/에러코드/응답스키마만 점검
- 충돌 파일 있으면 담당 재배정
