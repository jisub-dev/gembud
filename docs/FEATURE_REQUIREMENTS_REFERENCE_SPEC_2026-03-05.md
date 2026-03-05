# Gembud 기능 요구사항 기준서 (Research 기반) - 2026-03-05

## 문서 목적
- 1인 개발 중 발생한 “요구사항 흔들림”을 줄이기 위한 **기능별 결정 기준 문서**
- 현재 코드의 동작과, 외부 표준/권고(보안, 인증, 테스트)를 분리해서 명시
- 추후 구현/리팩터링/테스트의 “정답 기준(acceptance criteria)”로 사용

## 적용 원칙
1. 이 문서에 명시된 규칙이 코드보다 우선이다. 코드가 다르면 코드 수정 대상이다.
2. 각 기능은 `필수 규칙(MUST)` / `권장(SHOULD)` / `옵션(CAN)`으로 구분한다.
3. 예외/경계 케이스를 API 설계와 테스트 케이스에 반드시 반영한다.
4. 모든 보안 민감 기능은 “로컬(dev)과 운영(prod) 설정”을 분리한다.

---

## 1. 인증/인가 (JWT + Cookie + OAuth2)

### 1-1. 목표
- 브라우저 SPA 환경에서 안전한 세션 유지
- 로그인/로그아웃/토큰 재발급이 예측 가능하게 동작

### 1-2. 요구사항
- MUST: Access/Refresh 토큰은 `HttpOnly` 쿠키로만 전달
- MUST: `COOKIE_SECURE`를 환경별 분리 (`dev=false`, `prod=true`)
- MUST: OAuth redirect URI는 프론트 라우트와 정확히 일치
- MUST: Refresh 실패 시 클라이언트 인증 상태 정리 (`isAuthenticated=false`)
- MUST: 동시 로그인 금지 (새 로그인 시 기존 세션 강제 만료)
- MUST: Refresh 토큰 회전(Rotation) 적용
- SHOULD: 쿠키 이름 prefix (`__Host-`) 적용 검토 (도메인/경로 고정 강화)
- SHOULD: 세션 추적용 Redis 저장소 사용

### 1-3. 예외/경계 케이스
- OAuth 성공 후 `success=true`인데 `/users/me` 실패
  - 처리: 로그인 실패로 간주하고 `/login` 이동
- Refresh 쿠키 없음/만료/위조
  - 처리: 401 + 클라이언트 세션 즉시 종료
- 동일 계정 다른 기기/브라우저 로그인
  - 처리: 최신 로그인만 유효, 기존 세션 즉시 무효화

### 1-4. API 수용 기준
- `/auth/login`: 200 + 토큰은 바디가 아닌 쿠키에만 존재
- `/auth/refresh`: 200 시 access 쿠키 갱신, 실패 시 401
- `/auth/logout`: access/refresh 모두 만료 쿠키 발급
- `/users/me`: 인증 없으면 401

### 1-5. 테스트 기준
- 단위: 쿠키 속성(`HttpOnly`, `Secure`, `SameSite`) 검증
- 통합: 로그인→새로고침→세션 복원, logout 후 재요청 401
- 보안: CSRF 토큰 검증 시나리오(활성화 정책 선택 시)

---

## 2. 사용자 프로필

### 2-1. 요구사항
- MUST: 닉네임 변경 시 중복 사전 검증
- MUST: 본인 프로필만 수정 가능
- MUST: 닉네임 변경 쿨다운 30일 1회 적용

### 2-2. 예외/경계 케이스
- 공백/길이 초과/금칙어
- 기존 닉네임과 동일한 값 요청
- 경쟁 상태(동시 요청)로 unique 충돌

### 2-3. 수용 기준
- 중복 닉네임: 409 + `DUPLICATE_NICKNAME`
- validation 실패: 400 + 필드 오류 포함

---

## 3. 방(Room) 라이프사이클

### 3-1. 핵심 도메인 정책
- MUST: 한 사용자당 활성 대기방 1개
  - `createRoom`/`joinRoom` 모두 동일 규칙 적용
  - 이미 활성 방 참가 중이면 방 생성 불가
  - 프론트에서 방 생성 버튼 비활성화로 사전 차단
- MUST: 방 상태 전이 명확화
  - `OPEN -> FULL -> OPEN` (인원 변동)
  - `OPEN/FULL -> IN_PROGRESS`
- MUST: 수동 방 종료 기능 제거
  - 방은 “모든 참여자 이탈 시” 자동 종료/정리
- MUST: 호스트 권한 액션(강퇴/이양/시작/종료) 검증

### 3-2. 예외/경계 케이스
- 비공개 방 비밀번호 누락/오류
- 시작된 방(IN_PROGRESS)에 신규 참가 요청
- 호스트 이탈 시 차기 호스트 선출 실패
- 마지막 참여자 이탈과 동시 참가 요청 경합
- 자동 참가 실패 UX 분기:
  - 비밀번호 오류: 토스트 + 비밀번호 입력 유지
  - 정원초과/게임중: 토스트 + 방 목록으로 이동
  - 존재하지 않는 방 ID: 404 처리 + 방 목록 이동

### 3-3. 수용 기준
- 활성방 중복 참여 시 409 (`ALREADY_IN_OTHER_ROOM`)
- 종료된 방 join 시 409 (`ROOM_CLOSED`)
- 비밀번호 오류 시 401 (`INVALID_ROOM_PASSWORD`)
- 자동참가는 `OPEN` 상태에서만 수행
- `IN_PROGRESS` 방 진입 시 토스트 + 목록 이동

### 3-4. 구현 권고
- MUST: `closeRoom` API/버튼 제거
- MUST: 마지막 참여자 이탈 시 소프트 삭제(`deleted_at`) 처리
- MUST: 소프트 삭제된 방은 목록/상세/매칭에서 제외
- MUST: 소프트 삭제 방은 영구 보관(초기 정책)
- SHOULD: 배치 물리삭제는 트래픽 관측 후 별도 도입
- SHOULD: 동시성 보호(낙관락/비관락) 도입 검토

---

## 4. 매칭 추천

### 4-1. 요구사항
- MUST: 추천 점수 산식과 가중치가 문서/코드에서 일치
- MUST: 추천 사유(reason) 텍스트 생성 규칙 고정
- SHOULD: 프리미엄/무료 limit 정책을 API 레벨에서 강제

### 4-2. 예외/경계 케이스
- 후보 방 0개
- 사용자 데이터 결손(온도, 과거 평가)
- limit 비정상 값(음수, 과대)

### 4-3. 수용 기준
- 빈 결과는 200 + 빈 배열
- limit 상한 초과 시 정책값으로 clamp

---

## 5. 실시간 채팅(WebSocket + REST)

### 5-1. 요구사항
- MUST: 메시지 send 시 path roomId와 payload roomId 일치 검증
- MUST: Origin allowlist 사용 (와일드카드 금지)
- MUST: 채팅 멤버만 send/join/leave 가능
- MUST: 채팅 멤버가 아니면 메시지 조회(REST)도 불가
- MUST: 방 상세 진입 시 자동 참가 정책 적용(명시적 참가 버튼 제거)
- MUST: 자동참가 실패 시 상황별 UX를 고정 규칙으로 처리
- SHOULD: 사용자 logout 시 활성 소켓 정리
- SHOULD: 메시지 크기/빈도 제한 (DoS 완화)

### 5-2. 예외/경계 케이스
- 연결은 살아있는데 서버 세션 만료
- 멤버십 없는 사용자의 join/leave 이벤트 전송
- 중복 메시지/재전송
- 자동 참가 실패(비밀번호/정원/상태) 시 상세 페이지 진입 처리

### 5-3. 수용 기준
- 비멤버 send/join/leave는 거부 + 에러 큐 응답
- `ROOM_UPDATE` 이벤트 수신 시 클라이언트 room detail 재조회

### 5-4. 운영 권고
- SHOULD: `wss://` only (prod)
- SHOULD: 비정상 접속/메시지 패턴 로깅

---

## 5A. 비공개 방 비밀번호 + URL 초대 (확정 스펙)

### 5A-1. 목표
- 비공개 방 접근을 “예측 불가능한 URL + 비밀번호/초대코드 검증”으로 보호
- 순차 ID 노출 없이 공유 가능한 초대 링크 제공

### 5A-2. 데이터 모델
- MUST: `rooms`에 외부 노출용 식별자 추가
  - `public_id` (UUID 또는 랜덤 문자열, unique, not null)
- MUST: 비공개 방 초대코드 컬럼 추가
  - `invite_code` (랜덤 문자열, unique, nullable)
  - `invite_code_expires_at` (timestamp, nullable)
- MUST: 비밀번호는 평문 저장 금지
  - `password_hash`만 저장(BCrypt/Argon2)
- MUST: 소프트 삭제 컬럼 유지
  - `deleted_at` (nullable)

### 5A-3. URL 정책
- MUST: 프론트 라우트는 순차 PK 기반 URL 금지
  - 예: `/rooms/{publicId}`
- MUST: 초대 URL은 외부 공유용 파라미터 포함
  - 예: `/rooms/{publicId}?invite={inviteCode}`
- SHOULD: 초대코드는 일정 길이 이상(예: 12~20자), URL-safe 문자만 사용

### 5A-4. 접근/참가 규칙
- 공개 방:
  - 목록에서 선택 즉시 자동참가 (`OPEN` 상태에서만)
- 비공개 방:
  - 목록에서 선택 시 비밀번호 입력 UI 노출
  - `OPEN` 상태에서만 참가 시도
  - 참가 조건:
    1. 직접 입장: 비밀번호 검증 성공
    2. 초대코드 입장: inviteCode 검증 성공 시 비밀번호 스킵
- `IN_PROGRESS`/삭제된 방:
  - 참가 불가 (토스트 + 목록 이동)

### 5A-5. API 스펙 (권장)
- `POST /rooms`
  - 비공개 방 생성 시 `publicId`, `inviteCode` 생성
- `GET /rooms/{publicId}`
  - 방 상세 조회 (비공개 여부, 상태 포함)
- `POST /rooms/{publicId}/join`
  - body: `{ password?: string, inviteCode?: string }`
  - 검증 우선순위:
    - inviteCode가 유효하면 비밀번호 없이 입장
    - inviteCode가 없으면 비밀번호 검증으로 입장
- `POST /rooms/{publicId}/invite/regenerate` (호스트만)
  - inviteCode 재발급 (기존 코드 즉시 무효)

### 5A-6. 예외 처리
- 비밀번호 오류:
  - 401 + `INVALID_ROOM_PASSWORD`
  - UX: 토스트 + 입력 유지
- inviteCode 오류/만료:
  - 401 + `INVALID_INVITE_CODE` (신규 에러코드)
  - UX: 토스트 + 입력 유지
- 정원초과/게임중:
  - 409 + 기존 에러코드
  - UX: 토스트 + 목록 이동
- 존재하지 않는 `publicId`:
  - 404 + `ROOM_NOT_FOUND`
  - UX: 공통 에러 페이지 라우팅

### 5A-7. 보안 규칙
- MUST: inviteCode는 서버 로그에 원문 출력 금지(마스킹)
- MUST: inviteCode 재발급 시 이전 링크 즉시 무효화
- MUST: 방 삭제 시 해당 방 inviteCode는 FK/참조 무효화로 즉시 만료
- MUST: 초대코드 TTL 적용 (`INVITE_CODE_TTL_HOURS`, 기본 24시간)
- MUST: 만료 검증은 요청 시점 비교(`invite_code_expires_at < now`)로 처리
- MUST: 브루트포스 방어
  - 비밀번호/초대코드 실패 횟수 제한 + 지연 또는 rate limit
- SHOULD: 초대코드 TTL 옵션 제공(운영 정책으로 on/off)
- MUST: `public_id`, `invite_code` 재사용 영구 금지

### 5A-8. 수용 기준
- 순차 PK를 알더라도 비공개 방 참여 불가
- invite URL만으로 OPEN 비공개 방 참가 가능
- invite 재발급 후 기존 링크는 즉시 실패
- 자동참가 UX 규칙(비밀번호/정원/진행중/404) 일관

---

## 6. 친구 시스템

### 6-1. 요구사항
- MUST: 자기 자신에게 친구 요청 금지
- MUST: 중복 요청/이미 친구 상태 차단
- MUST: 수신자만 수락/거절 가능

### 6-2. 예외/경계 케이스
- 이미 거절된 요청 재요청 정책
- 친구 삭제 시 양방향 정합성

### 6-3. 수용 기준
- 중복 요청 409
- 권한 없는 수락/거절 403

---

## 7. 평가/온도 시스템

### 7-1. 요구사항
- MUST: 평가 가능 조건(방 종료, 참여자만, 자기평가 금지)
- MUST: 온도 범위 제한(0~100) 유지
- SHOULD: 월간 평가 제한 정책 고정 및 응답코드 표준화

### 7-2. 예외/경계 케이스
- 동일 대상 중복 평가
- 평가 대상 불일치(요청 userId vs 실제 참여자)

### 7-3. 수용 기준
- 중복 평가 409
- 자기 평가 400
- 제한 초과 429

---

## 8. 신고/제재

### 8-1. 요구사항
- MUST: 자기 자신 신고 금지
- MUST: 중복 신고 차단(정책 기간 명시 필요)
- MUST: 관리자 전용 API는 메서드 보안으로 강제
- SHOULD: 신고 증거(채팅 메시지) 보존 기간 명시
- MUST: 중복 신고 차단 기간은 우선 고정값 사용, 추후 환경변수화
- MUST: 중복 신고는 "같은 대상 사용자 전체 기준(방 무관)"으로 판정

### 8-2. 예외/경계 케이스
- 처리 중 신고 재오픈 필요 여부
- 피신고자 탈퇴/정지 상태와의 연계

### 8-3. 수용 기준
- 일반 사용자 관리자 API 접근 시 403
- 상태 전이 불가한 요청은 400/409로 명확히 분리

---

## 9. 알림

### 9-1. 요구사항
- MUST: 본인 알림만 조회/수정/삭제 가능
- MUST: 읽음 처리(idempotent)
- SHOULD: 정리 스케줄러(만료 알림 삭제) 운영 기준 명시

### 9-2. 예외/경계 케이스
- 동일 알림 중복 생성 방지 규칙
- WebSocket 전송 실패 시 fallback(저장만)

---

## 10. 구독/광고

### 10-1. 요구사항
- MUST: 프리미엄 만료 시 상태 자동 정리
- MUST: 프리미엄 사용자는 광고 노출 0
- MUST: 무료 사용자 광고 조회 제한(일일 캡) 고정
- MUST: 광고/추천 제한값은 환경변수로 관리
- SHOULD: 결제 이벤트 기반 상태 변경으로 확장 가능하게 구조화

### 10-2. 예외/경계 케이스
- 중복 활성화 요청(연장 vs 재시작)
- 취소와 만료가 동시에 발생하는 경합

---

## 11. 에러 응답 표준

### 11-1. 요구사항
- MUST: 비즈니스 에러는 `ErrorCode`로 통일
- MUST: 4xx/5xx 기준 일관성 유지
- SHOULD: 사용자 메시지와 내부 로깅 메시지 분리

### 11-2. 수용 기준
- DB unique 충돌 등 인프라 예외도 사용자 친화 메시지로 매핑
- 테스트에서 상태코드 + 에러코드 + 메시지 동시 검증

---

## 12. 테스트/품질 게이트

### 12-1. 최소 게이트
- MUST: `frontend` 단위 테스트 통과
- MUST: `backend` 테스트에서 컨텍스트 실패 0
- MUST: 인증/방/채팅 스모크 테스트 자동화

### 12-2. DB 테스트 전략 (중요)
- 현재 이슈:
  - H2에서 PostgreSQL `plpgsql` 함수 마이그레이션 실패
- 확정:
  - MUST: 테스트 DB를 Testcontainers PostgreSQL로 전환
  - 선택 이유:
    - 운영 DB(PostgreSQL)와 SQL/DDL/Flyway 동작 일치
    - H2 전용 우회 마이그레이션 유지보수 비용 제거

### 12-3. CI 규칙
- MUST: PR 단계에서 `./gradlew test` 강제
- SHOULD: 실패군 리포트 자동 업로드

---

## 13. 운영/관측성

### 13-1. 로그
- MUST: 인증 실패, 권한 거부, WebSocket 보안 이벤트 로깅
- MUST: 토큰/개인정보 마스킹

### 13-2. 메트릭
- SHOULD: 로그인 성공률, refresh 실패율, WebSocket 연결 수, 메시지 reject 수

---

## 14. 지금 코드 기준 우선 수정 백로그
1. `@WebMvcTest` 보안 빈 누락 해결 (`JwtTokenProvider` 등)
2. 테스트 DB를 Testcontainers PostgreSQL로 전환
3. WebSocket join/leave + 메시지 조회 멤버십 검증 추가
4. 수동 방 종료 기능 제거 + 마지막 참여자 이탈 자동 정리 반영
5. 방 상세 진입 시 자동 참가 UX/API 흐름 반영
6. 단일 세션 + Refresh Rotation 구현
7. 테스트 전략 문서의 실행 명령 `gradlew`로 통일

---

## 15. 확정 정책 (Owner Decision)
1. 활성 대기방 1개 정책은 방 생성에도 동일 적용
2. 수동 방 종료 기능 제거, 마지막 참여자 이탈 시 소프트 삭제(`deleted_at`)
3. 채팅은 멤버만 조회/송신/입퇴장 가능
4. 방 상세 진입 시 자동 참가(별도 참가 버튼 제거)
5. 동시 로그인 금지, 새 로그인 시 기존 세션 전부 즉시 만료
   - 기존 기기에는 "다른 기기에서 로그인됨" 메시지 제공
6. Refresh 토큰 회전 도입
7. 테스트 DB는 Testcontainers PostgreSQL 채택
8. 광고/추천/신고 정책값은 환경변수화(초기값 고정, 추후 변경 가능)
9. 닉네임 변경 쿨다운 30일 1회
10. SameSite는 `Strict` 고정 대신 `Lax/None` 환경별 조정
11. 자동 참가 실패 UX:
  - 비밀번호 오류: 토스트 + 입력 유지
  - 정원초과/게임중: 토스트 + 목록 이동
  - 존재하지 않는 방 ID: 404 + 목록 이동
12. 신고 중복 차단 기준: 같은 대상 전체 기준(방 무관)
13. 자동참가는 `OPEN`에서만 허용 (`IN_PROGRESS`는 차단)
14. 비공개 방 보안:
  - 순차 PK 노출 대신 UUID/랜덤 초대코드 기반 식별자 사용
  - 목록에는 자물쇠 아이콘만 노출
  - 초대코드 검증으로 참가 (비밀번호는 별도 수단)
  - 초대코드 기본 TTL 24시간, 재발급 시 TTL 리셋
15. 방 상세 페이지 제거, 목록에서 즉시 자동참가
16. 에러 라우팅: `404/403/500` 공통 에러 페이지, 비밀번호 오류는 토스트 유지
17. 단일세션 만료 알림은 모달 강제 확인

## 16. 환경변수 표준(초안)
- `COOKIE_SECURE` (dev: `false`, prod: `true`)
- `COOKIE_SAMESITE` (기본 `Lax`, cross-site 필요 시 `None`)
- `WEBSOCKET_ALLOWED_ORIGINS`
- `MATCHING_MAX_LIMIT_FREE` (예: `10`)
- `MATCHING_MAX_LIMIT_PREMIUM` (예: `20`)
- `ADS_DAILY_VIEW_LIMIT` (예: `5`)
- `REPORT_DUPLICATE_BLOCK_DAYS` (예: `7`)
- `NICKNAME_CHANGE_COOLDOWN_DAYS` (고정값 `30`에서 시작)
- `SESSION_SINGLE_LOGIN_ENABLED` (`true`)
- `REFRESH_TOKEN_ROTATION_ENABLED` (`true`)
- `SOFT_DELETED_ROOM_RETENTION_DAYS` (초기 미사용/영구보관 정책)
- `PRIVATE_ROOM_CODE_ENABLED` (`true`)
- `INVITE_CODE_TTL_HOURS` (기본 `24`)
- `LOGIN_LOCK_THRESHOLD` (기본 `10`)
- `LOGIN_LOCK_DURATION_MINUTES` (기본 `10`)

---

## Research References
- MDN Set-Cookie: https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Set-Cookie
- MDN Cookies: https://developer.mozilla.org/en-US/docs/Web/HTTP/Cookies
- Spring Security CORS: https://docs.spring.io/spring-security/reference/servlet/integrations/cors.html
- Spring Security CSRF: https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html
- Spring Framework WebSocket Allowed Origins: https://docs.enterprise.spring.io/spring-framework/reference/web/websocket/server.html
- OWASP WebSocket Security Cheat Sheet: https://cheatsheetseries.owasp.org/cheatsheets/WebSocket_Security_Cheat_Sheet.html
- RFC 9700 (OAuth 2.0 Security BCP): https://www.rfc-editor.org/rfc/rfc9700
- RFC 7519 (JWT): https://www.rfc-editor.org/rfc/rfc7519.html
- RFC 6455 (WebSocket Protocol): https://www.rfc-editor.org/info/rfc6455
- Spring Boot Testcontainers: https://docs.spring.io/spring-boot/reference/testing/testcontainers.html
- Testcontainers PostgreSQL Module: https://java.testcontainers.org/modules/databases/postgres/
