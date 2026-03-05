# Gembud 프로젝트 리뷰 & 기능 체크리스트 (2026-03-05)

## 1) 리뷰 범위
- 코드 기준: 현재 워크트리 기준(수정 중 파일 다수 존재)
- 점검 방식:
  - 백엔드/프론트 핵심 코드 정적 리뷰
  - 테스트 실행 확인
  - 문서와 실제 구현/실행 커맨드 일치성 확인

## 2) 실행 결과 요약 (2026-03-05 업데이트)
- 프론트엔드 테스트: 통과
  - `cd frontend && npm test -- --run`
  - 결과: 2 files, 13 tests passed
- 백엔드 테스트: **전체 통과** (2026-03-05 수정 완료)
  - `cd backend && ./gradlew test`
  - 결과: 178 tests, 0 failures, 5 skipped
  - 기존 62 failures → 0 failures (컨텍스트 실패 0건, DB/Flyway 이슈 해소)

## 3) 기능 체크리스트

### 인증/권한
- [x] JWT 필터 + 인증 체인 구성
- [x] OAuth2 로그인 성공 핸들러 존재
- [x] 로컬 개발 환경에서 쿠키 기반 로그인 안정 동작 보장
  - `COOKIE_SECURE` 환경변수로 분리 완료 (로컬 기본 false, 운영 true)
- [x] OAuth 콜백 기본 경로 일치
  - `/oauth2/callback`으로 통일 완료

### 방/매칭
- [x] 방 생성/입장/퇴장/강퇴/방장 위임/시작 API 존재
- [x] “사용자 1개 활성 방” 제약 일관성
  - `createRoom`에도 `existsActiveParticipationByUserId` 체크 추가 완료
- [x] 방 종료 시 후속 정리(참여자/채팅 멤버/실시간 갱신) 일관성
  - `closeRoom` 후 `ROOM_UPDATE` 브로드캐스트 추가 완료

### 채팅/WebSocket
- [x] REST + STOMP 채팅 구조 존재
- [x] WebSocket Origin 제한
  - `WEBSOCKET_ALLOWED_ORIGINS` 환경변수로 화이트리스트 적용 완료
- [x] WebSocket 메시지의 roomId 무결성 검증
  - path 변수와 payload `chatRoomId` 불일치 시 에러 반환 추가 완료
- [x] join/leave 이벤트 권한 검증
  - `isChatRoomMember()` 체크 후 비멤버 요청 차단 추가 완료

### 사용자 프로필/유니크 제약
- [x] `/users/me` 조회/수정 API 존재
- [x] 닉네임 중복 사전 검증
  - `existsByNickname` 체크 + `DUPLICATE_NICKNAME` 반환 추가 완료

### 테스트/문서 운영
- [x] 테스트 문서 실행 커맨드 최신화
  - `TEST_STRATEGY.md` Gradle 기준으로 통일 완료
- [x] CI 게이트에서 테스트 통과 확인
  - 178 tests, 0 failures 달성

## 4) 코드 리뷰 Findings (심각도순)

### [High] OAuth 콜백 경로 기본값 불일치로 로그인 완료 후 화면 전환 실패 가능
- 근거
  - `backend/src/main/resources/application.yml:32` → `http://localhost:5173/auth/callback`
  - `backend/src/main/java/com/gembud/security/OAuth2SuccessHandler.java:36`
  - `frontend/src/App.tsx:79` → 실제 라우트는 `/oauth2/callback`
- 영향
  - 환경변수 미설정 시 OAuth 성공 후 프론트 라우트 미스매치로 로그인 완료 플로우 실패
- 수정 방법
  - 백엔드 기본값을 `/oauth2/callback`으로 통일
  - 배포/개발 환경별 `OAUTH2_REDIRECT_URI`를 명시 관리

### [High] 쿠키 `secure=true` 고정으로 로컬 HTTP 환경에서 인증 쿠키 미저장 가능
- 근거
  - `backend/src/main/java/com/gembud/controller/AuthController.java:136-139,194-197`
  - `backend/src/main/java/com/gembud/security/OAuth2SuccessHandler.java:75-78,84-87`
- 영향
  - 로컬 개발에서 로그인 성공 응답 후에도 인증 상태 복원 실패 가능
- 수정 방법
  - `app.cookie.secure` 프로퍼티 분리(개발 false, 운영 true)
  - `sameSite`/`domain`도 환경별로 명시

### [High] 백엔드 테스트 컴파일 실패 상태
- 근거
  - `backend/src/test/java/com/gembud/service/RoomServiceTest.java:217`
  - 실제 서비스 시그니처: `ChatService.addMemberToChatRoom(Long, Long, Long)`
- 영향
  - 회귀 탐지 불가, 릴리즈 신뢰도 하락
- 수정 방법
  - 테스트를 `addMemberToChatRoomInternal` 검증으로 맞추거나, 공개 메서드 호출 구조를 서비스와 정합되게 수정
  - CI에서 `./gradlew test`를 필수 게이트로 설정

### [Medium] createRoom에 “이미 활성 방 참여 중” 체크 누락
- 근거
  - `backend/src/main/java/com/gembud/service/RoomService.java:56-118`
  - `joinRoom`에는 동일 제약 체크 존재: `:193-196`
- 영향
  - 사용자가 방 생성으로 제약 우회 가능(활성 방 중복 참여 데이터 발생 가능)
- 수정 방법
  - `createRoom` 초기에 `existsActiveParticipationByUserId` 체크 추가
  - 관련 서비스 테스트 케이스 추가

### [Medium] closeRoom 후 참여자/채팅 멤버 정리 및 브로드캐스트 누락
- 근거
  - `backend/src/main/java/com/gembud/service/RoomService.java:300-316`
- 영향
  - 사용자 관점에서는 종료 후 상태/멤버 데이터가 지연 또는 불일치 가능
- 수정 방법
  - close 시 참여자 정리 전략 명확화:
    - soft-close만 할지, 멤버 detach할지
  - 최소한 `ROOM_UPDATE` 브로드캐스트는 추가

### [Medium] WebSocket send에서 path roomId와 payload roomId 무결성 검증 없음
- 근거
  - `backend/src/main/java/com/gembud/websocket/ChatWebSocketController.java:49-67`
- 영향
  - 악의적/오류 payload로 다른 채팅방 메시지 저장/검증 혼선 가능
- 수정 방법
  - 컨트롤러에서 `request.chatRoomId == path chatRoomId` 강제 검증
  - 불일치 시 즉시 예외 반환

### [Medium] WebSocket endpoint Origin 전체 허용
- 근거
  - `backend/src/main/java/com/gembud/config/WebSocketConfig.java:44-46`
- 영향
  - 교차 출처 연결 허용 범위 과도
- 수정 방법
  - 환경변수 기반 허용 Origin 화이트리스트 적용
  - 최소 `localhost` 개발 도메인 + 운영 도메인만 허용

### [Low] 사용자 프로필 수정 시 닉네임 중복 사전 검증 없음
- 근거
  - `backend/src/main/java/com/gembud/controller/UserController.java:89-90`
- 영향
  - DB unique 제약 충돌 시 비즈니스 오류 대신 500 가능
- 수정 방법
  - 저장 전 `existsByNickname` 검사 + `DUPLICATE_NICKNAME` 반환

### [Low] 인증 API 응답 타입 정의와 실제 응답 필드 불일치
- 근거
  - 프론트 타입: `frontend/src/services/authService.ts:16-22` (token 필드 포함)
  - 백엔드 실제 응답: `backend/src/main/java/com/gembud/controller/AuthController.java:65-69,97-101` (email/nickname만 반환)
- 영향
  - 타입 신뢰도 하락, 추후 리팩터링 시 런타임 혼선 위험
- 수정 방법
  - 프론트 `AuthResponse`를 실제 응답 스키마에 맞게 축소
  - 필요 시 `LoginResult`/`SessionUser` 타입 분리

## 5) 우선순위별 수정 계획 (추천)

### P0 (오늘 바로)
1. OAuth redirect 경로 통일 (`/oauth2/callback`)
2. 쿠키 `secure` 환경 분리
3. 깨진 테스트 컴파일 복구 (`RoomServiceTest`)

### P1 (이번 주)
1. `createRoom`에도 활성 방 중복 체크 추가
2. `closeRoom` 후 브로드캐스트/정리 정책 반영
3. WebSocket roomId 무결성 및 Origin 화이트리스트 적용

### P2 (다음 스프린트)
1. 닉네임 중복 검증 에러코드 정규화
2. Auth API 응답 타입 정리
3. `TEST_STRATEGY.md` 실행 커맨드 Gradle 기준으로 최신화

## 6) 바로 적용 가능한 수정 체크리스트 (완료)
- [x] `application.yml`의 OAuth redirect 기본값 수정 — `ab4136d`
- [x] `AuthController`/`OAuth2SuccessHandler` 쿠키 옵션 환경 변수화 — `ab4136d`
- [x] `RoomServiceTest` 시그니처 정합성 수정 후 `./gradlew test` 재확인 — `ab4136d`
- [x] `RoomService#createRoom`에 active participation 체크 추가 + 테스트 추가 — `ab4136d`
- [x] `RoomService#closeRoom`에 실시간 업데이트 반영 — `ab4136d`
- [x] `ChatWebSocketController` roomId 검증 추가 — `ab4136d`
- [x] `WebSocketConfig` Origin 제한 적용 — `ab4136d`
- [x] `UserController#updateCurrentUser` 닉네임 중복 검증 추가 — `ab4136d`
- [x] `TEST_STRATEGY.md`의 `mvnw` 표기 제거, `gradlew`로 통일 — (이번 커밋)

## 7) 테스트 환경 복구 내역 (2026-03-05)
- `@WebMvcTest` 슬라이스: `JwtTokenProvider`, `UserDetailsService` MockBean 추가 + `@AutoConfigureMockMvc(addFilters=false)` 적용
- Repository 테스트: `application-test.yml` Flyway 비활성화 + `ddl-auto: create-drop`, `@ActiveProfiles("test")` 추가
- `GlobalExceptionHandlerTest`, `ReleaseGateTest`: `@ActiveProfiles("test")` 추가
- `ReleaseGateTest`: context-path 중복(`/api/...`) 수정
- `User.temperature` precision `4→5` 수정 (100.00 저장 가능)
- `WebSocket join/leave`: `isChatRoomMember()` 멤버십 검증 추가
- `ChatService.isChatRoomMember()` 신규 메서드 추가

