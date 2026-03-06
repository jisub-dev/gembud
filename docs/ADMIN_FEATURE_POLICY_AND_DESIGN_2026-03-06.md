# Gembud 관리자 기능 정책 및 설계서 (코드 반영용) - 2026-03-06

## 1. 문서 목적
- 현재 코드베이스(`backend`/`frontend`)에 바로 적용 가능한 관리자 기능 정책/설계를 확정한다.
- 이미 결정된 운영 정책을 기술 스펙으로 고정해 재작업을 줄인다.
- 구현, 테스트, 배포, 운영 검증까지 한 문서로 연결한다.

## 2. 확정 정책 (Owner Final)
| 항목 | 확정값 |
|---|---|
| 세션 만료 응답 | `401 + AUTH004` 통일 |
| 프리미엄 OFF | 완전 강등 (기능 비활성) |
| 잠금 해제 권한 | `ADMIN`만 |
| 로그인 실패 BURST | 10회/5분 HIGH, 30회/5분 CRITICAL |
| Slack 재시도 | 3회 (1m / 5m / 15m) |
| 보안 이벤트 삭제 | 매일 3시 하드삭제 |
| WS 세션 만료 UX | 모달 확인 후 이동 |
| `/subscriptions/**` 미인증 | `401` 유지 |
| 에러 페이지 톤 | 간단 안내 |
| Feature Flag | 환경변수만 사용 |
| 기존 잠금해제 경로 | 즉시 제거 (`/auth/admin/...` 제거) |
| Admin 설정 키 | `app.admin.email` 단일 |
| 신고 처리 추가 액션 | `warn`만 추가 |
| 보안 이벤트 조회 페이지 | 기본 20, 최대 100 |
| Slack 재시도 방식 | 앱 내부 스케줄러 |

## 3. 현재 코드 상태 요약

### 이미 있는 것
- `ADMIN` 역할 존재 (`User.UserRole.ADMIN`)
- 시작 시 관리자 승격 초기화기 존재:
  - `backend/src/main/java/com/gembud/config/AdminInitializer.java`
- 신고 관리자 API 다수 존재 (`ReportController` + `@PreAuthorize("hasRole('ADMIN')")`)
- 보안 이벤트 엔티티/저장/정리 스케줄 존재:
  - `SecurityEvent`, `SecurityEventService`, `SecurityEventRepository`
- Slack 알림 기초 구현 존재 (`SlackAlertService`)
- 로그인 잠금 해제 API 존재하나 경로가 다름:
  - 현재: `DELETE /auth/admin/users/{userId}/login-lock`

### 갭 (이번 설계로 메울 것)
1. 잠금 해제 API 경로를 최종 확정 스펙으로 정렬 필요  
- 목표: `DELETE /admin/users/{userId}/login-lock` (204)

2. 관리자 운영 API가 `AuthController`/`ReportController`에 분산  
- 목표: `/admin/*` 네임스페이스로 수렴

3. Slack 전송은 재시도/레이트리밋/임계치 규칙이 약함  
- 목표: 임계치/재시도/중복억제 명시 구현

4. 보안 이벤트 조회용 관리자 API 부재  
- 목표: 최근 이벤트 조회/필터 API 추가

5. 기존 잠금해제 경로 즉시 제거 필요
- 목표: `/auth/admin/users/{userId}/login-lock` 삭제, `/admin/users/{userId}/login-lock`만 유지

## 4. 관리자 기능 범위 (V1)

## 4-1. Admin User Operations
1. 로그인 잠금 해제
- `DELETE /admin/users/{userId}/login-lock`
- 권한: ADMIN
- 응답: `204 No Content`
- 동작:
  - `users.login_locked_until = null`
  - 보안 이벤트 기록: `SESSION_REVOKED` 대신 신규 `ADMIN_UNLOCK_LOGIN` 권장

2. 유저 보안 상태 조회
- `GET /admin/users/{userId}/security-status`
- 권한: ADMIN
- 응답 필드:
  - `userId`, `email`, `isLoginLocked`, `loginLockedUntil`
  - `failedLoginCountInWindow`, `lastLoginFailAt`

## 4-2. Admin Report Moderation
- 기존 `ReportController` 관리자 API 유지, 경로만 `/admin/reports/*`로 확장/정리
- 필수 엔드포인트:
  - `GET /admin/reports?status=PENDING&page=...`
  - `GET /admin/reports/users/{userId}`
  - `POST /admin/reports/{reportId}/warn`
  - `PUT /admin/reports/{reportId}/review`
  - `PUT /admin/reports/{reportId}/resolve`
  - `DELETE /admin/reports/{reportId}`

warn 정책:
- 목적: 즉시 강한 제재 대신 1차 경고
- 동작:
  - 신고 상태 변경 없이 관리자 코멘트/경고이력 기록
  - 대상 유저 알림 발송
- 보류:
  - `temporary_suspend`, `permanent_ban` (V2)

## 4-3. Admin Security Monitoring
1. 보안 이벤트 조회
- `GET /admin/security-events?risk=HIGH&type=LOGIN_FAIL&from=...&to=...&page=...`
- 권한: ADMIN

2. 요약 대시보드용 집계
- `GET /admin/security-events/summary?windowMinutes=60`
- 응답 예:
  - `loginFailCount`, `loginLockedCount`, `refreshReuseCount`, `rateLimitHitCount`

## 5. 권한 매트릭스
| API | USER | ADMIN |
|---|---:|---:|
| `/admin/**` | 403 | 허용 |
| 신고 생성 `/reports` | 허용 | 허용 |
| 신고 처리 `/admin/reports/*` | 403 | 허용 |
| 잠금 해제 `/admin/users/*/login-lock` | 403 | 허용 |

권장 구현:
1. `SecurityConfig`에서 `/admin/**`는 ADMIN 강제
2. 컨트롤러 메서드에 `@PreAuthorize("hasRole('ADMIN')")` 이중 적용

## 6. 세션/잠금/이상탐지 정책 상세

## 6-1. 세션 만료
- 모든 만료/무효 세션 응답은 `401 + AUTH004` 통일
- 프론트는 해당 코드 수신 시 세션만료 모달 노출 후 로그인 이동

## 6-2. 로그인 잠금
- 기준: 계정 기준 `10회/10분`
- 미존재 이메일도 동일 카운트
- 자동 해제: 10분
- 수동 해제: ADMIN API

## 6-3. BURST 룰
- `LOGIN_FAIL >= 10 within 5m` -> HIGH
- `LOGIN_FAIL >= 30 within 5m` -> CRITICAL
- 이벤트 계산 기준:
  - 기본: `IP + email(normalized)` 키
  - 보조: `IP only` 보조 카운터

## 7. Slack 알림 설계 (최종)

## 7-1. 전송 대상
- 위험도 `HIGH`, `CRITICAL`만 전송

## 7-2. dedupe
- 키: `slack:security:{eventType}:{scope}:{5mBucket}`
- TTL: 10분
- scope: `userId` 우선, 없으면 `ip`

## 7-3. 재시도
- 1차 실패 후 1분, 2차 실패 후 5분, 3차 실패 후 15분
- 총 3회 실패 시 포기, 앱 요청 흐름은 실패시키지 않음

## 7-4. 메시지 형식
- 단순 텍스트(스레드 규칙 없음)
- 포함:
  - severity, eventType, userId, ip, endpoint, count/window
- 금지:
  - 토큰/비밀번호/쿠키/초대코드 원문

## 8. 데이터 설계

## 8-1. RDB (existing + extension)
1. `users`
- 사용 컬럼: `role`, `login_locked_until`

2. `security_events`
- 사용 컬럼: `event_type`, `user_id`, `ip`, `endpoint`, `result`, `risk_score`, `created_at`
- 보존: 90일
- 삭제: 매일 03:00 하드삭제

## 8-2. Redis 키 설계
1. 로그인 카운트
- `ratelimit:login:account:{email}` (TTL 10m)
- `ratelimit:login:ip:{ip}` (TTL 1m)

2. Slack dedupe
- `ratelimit:slack:{dedupeKey}` (TTL 10m)

3. Slack 재시도 스케줄러 상태
- 앱 내부 스케줄러 사용
- 영속 큐(별도 워커/ZSET)는 V2로 보류

## 9. API 계약 (초안)

## 9-1. 잠금 해제
- Request:
  - `DELETE /admin/users/{userId}/login-lock`
- Response:
  - `204`
- Error:
  - `401` (미인증), `403` (권한없음), `404` (유저없음)

## 9-2. 유저 보안 상태 조회
- Request:
  - `GET /admin/users/{userId}/security-status`
- Response 200:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 123,
    "email": "user@example.com",
    "isLoginLocked": true,
    "loginLockedUntil": "2026-03-06T10:10:00",
    "failedLoginCountInWindow": 7,
    "lastLoginFailAt": "2026-03-06T10:01:33"
  }
}
```

## 9-3. 보안 이벤트 목록
- Request:
  - `GET /admin/security-events?risk=HIGH&eventType=LOGIN_FAIL&from=...&to=...&page=0&size=20`
  - `size` 제약: `@RequestParam(defaultValue = "20") @Max(100) int size`
- Response 200:
  - 페이지네이션된 event list

## 10. 구현 단계 (코드 적용 순서)

1. Admin API 네임스페이스 정리
- 신규 `AdminUserController` 생성
- `unlockAccount` 이동:
  - from `AuthController`
  - to `/admin/users/{userId}/login-lock`
- 기존 경로 즉시 제거:
  - `/auth/admin/users/{userId}/login-lock` 삭제

2. SecurityConfig 강화
- `/admin/**` ADMIN 강제
- `@PreAuthorize` 유지

3. 보안 이벤트 조회 API 추가
- `AdminSecurityController` + pageable query
- `SecurityEventRepository` 조회 메서드 확장

4. Slack 재시도 스케줄러 적용
- `SlackAlertService`에 재시도 정책 추가
- 실패 시 앱 내부 스케줄러로 1m/5m/15m 재시도

5. BURST 집계 구현
- `RateLimitService` 또는 `SecurityEventService`에 5분 집계 로직 추가
- 임계치 넘으면 HIGH/CRITICAL 이벤트 기록

## 11. 테스트 설계

## 11-1. 단위 테스트
1. `AdminUserControllerTest`
- ADMIN이면 204
- USER면 403
- 없는 userId면 404

2. `SlackAlertServiceTest`
- dedupe 동작
- 1m/5m/15m 재시도 스케줄 계산

3. `RateLimitServiceTest`
- 로그인 실패 카운트, TTL, 임계치

## 11-2. 통합 테스트
1. `DELETE /admin/users/{userId}/login-lock` E2E
2. `GET /admin/security-events` 필터/페이지
3. 보안 이벤트 90일 purge 스케줄

## 11-3. 수동 운영 점검
1. 10회 실패 후 잠금 발생
2. 관리자 수동 해제
3. HIGH/CRITICAL Slack 수신
4. WS 세션 만료 모달 정상 노출

## 12. 환경변수 표준
| 키 | 기본값 | 설명 |
|---|---|---|
| `app.admin.email` | 빈값 | 시작 시 ADMIN 승격 대상 |
| `LOGIN_LOCK_THRESHOLD` | 10 | 계정 잠금 임계치 |
| `LOGIN_LOCK_WINDOW_MINUTES` | 10 | 카운트 윈도우 |
| `LOGIN_LOCK_DURATION_MINUTES` | 10 | 잠금 시간 |
| `SECURITY_EVENT_RETENTION_DAYS` | 90 | 이벤트 보존 |
| `SECURITY_ALERT_WEBHOOK_URL` | 빈값 | Slack webhook |

## 13. 리스크 및 대응
1. 관리자 API 오남용
- 대응: ADMIN 강제 + 감사로그 + 향후 IP allowlist

2. Slack 장애로 알림 누락
- 대응: 재시도 큐 + 실패 WARN 로그 + 주기적 대시보드 점검

3. 경로 변경으로 운영 스크립트 실패
- 대응: 문서/스크립트 즉시 업데이트 (deprecate 없이 교체)

## 14. 이번 스프린트 구현 대상 (권장)
1. `DELETE /admin/users/{userId}/login-lock` 경로 정렬
2. `/admin/security-events` 조회 API
3. 신고 `warn` 액션 추가
4. Slack 재시도(1m/5m/15m) + dedupe 강화 (앱 내부 스케줄러)
5. BURST 임계치 이벤트화(10/5m HIGH, 30/5m CRITICAL)
6. 테스트 4종(권한/재시도/임계치/warn) 추가
