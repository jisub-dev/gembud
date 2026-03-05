# Gembud 다음 스텝 실행 계획 (2026-03-05)

## 목적
- 현재 반영한 보안/인증/방 로직 수정을 안정적으로 릴리즈 가능한 상태로 만들기
- 백엔드 테스트 실패(컨텍스트/마이그레이션) 해소
- 개발 루프를 “수정 → 검증 → 문서화”로 고정
- 확정 정책(단일 세션, 자동 참가, 수동 방종료 제거)을 코드에 반영

## 현재 기준선
- 프론트 테스트: 통과 (`13/13`)
- 백엔드 테스트: 실패 (`178 tests, 62 failed`)
- 주요 실패군:
  1. `@WebMvcTest` 보안 빈 누락 (`JwtTokenProvider`)
  2. 테스트 DB(H2)와 운영 SQL(PostgreSQL) 불일치 (`V1__init_users_table.sql`의 `plpgsql`)

---

## Phase 1. 테스트 환경 복구 (P0)
목표: 백엔드 테스트가 “실행 가능한 상태”로 복귀

### 1-1) WebMvc 슬라이스 테스트 보안 의존성 정리
- 대상:
  - `backend/src/test/java/com/gembud/controller/GameControllerTest.java`
  - `backend/src/test/java/com/gembud/controller/RoomControllerTest.java`
- 작업:
  - `@MockBean JwtTokenProvider`
  - `@MockBean UserDetailsService` (필요 시 `CustomUserDetailsService`)
  - 기존 `@WithMockUser` 시나리오 유지
- 완료 기준:
  - 위 2개 테스트 클래스 단독 실행 통과
- 검증:
  - `cd backend && ./gradlew test --tests com.gembud.controller.GameControllerTest`
  - `cd backend && ./gradlew test --tests com.gembud.controller.RoomControllerTest`

### 1-2) Repository/Integration 테스트 DB 전략 확정
- 대상:
  - `backend/src/test/resources/application-test.yml`
  - `backend/src/main/resources/db/migration/V1__init_users_table.sql`
- 확정:
  - Testcontainers PostgreSQL 전환
- 완료 기준:
  - `GameRepositoryTest` 컨텍스트 로딩 성공
- 검증:
  - `cd backend && ./gradlew test --tests com.gembud.repository.GameRepositoryTest`

---

## Phase 2. 전체 회귀 안정화 (P1)
목표: `./gradlew test` 통과 또는 실패 원인 100% 분류/티켓화

### 2-1) 실패군 분류 후 순차 처리
- 분류 기준:
  - 컨텍스트 실패
  - DB 마이그레이션/스키마 실패
  - 비즈니스 assertion 실패
- 처리 순서:
  1. 컨텍스트 실패 0건
  2. DB 실패 0건
  3. 비즈니스 assertion 정리

### 2-2) 전체 테스트 게이트 복구
- 검증:
  - `cd backend && ./gradlew test`
- 완료 기준:
  - 빌드 실패 원인 “환경 문제” 제거
  - 남은 실패는 기능 회귀만 남도록 정리

---

## Phase 3. 정책 확정 반영 (P1)
목표: 확정된 제품 정책을 기능 동작으로 일치

### 3-1) 수동 방 종료 기능 제거 + 자동 종료 정리
- 대상:
  - `RoomController`, `RoomService`, 프론트 Room 상세 UI
- 작업:
  - `DELETE /rooms/{roomId}` 제거
  - 마지막 참여자 이탈 시 `deleted_at` 소프트 삭제
  - 소프트 삭제된 방은 목록/상세/매칭에서 제외
  - 방 상세의 종료/참가 UX 정비(자동 참가 정책 반영)
- 완료 기준:
  - 호스트 종료 액션 없음
  - “모든 참여자 이탈”이 유일한 종료 조건

### 3-2) 방 상세 진입 시 자동 참가
- 대상:
  - `RoomListPage`, `roomService`, 관련 훅/에러 UX
- 작업:
  - 방 상세 페이지 제거
  - 목록 선택 시 즉시 자동 `join` 시도 (`OPEN` 상태에서만)
  - 실패 UX 분기:
    - 비밀번호 오류: 토스트 + 입력창 유지
    - 정원초과/게임중: 토스트 + 목록 이동
    - 존재하지 않는 방 ID: 공통 에러 페이지
- 완료 기준:
  - 상세 페이지 없이 목록 진입 플로우 일관
  - 자동참가 조건/실패 UX 정책과 1:1 일치

### 3-3) WebSocket/REST 채팅 멤버십 강제
- 대상:
  - `backend/src/main/java/com/gembud/websocket/ChatWebSocketController.java`
  - `backend/src/main/java/com/gembud/service/ChatService.java`
- 작업:
  - `chat.join/{chatRoomId}` / `chat.leave/{chatRoomId}` 멤버십 검증
  - 비멤버 요청 시 에러 큐 응답
  - 메시지 조회 REST도 멤버만 허용 재확인
- 완료 기준:
  - 비멤버 join/leave 이벤트 브로드캐스트 차단 확인

### 3-4) 단일 세션 + Refresh Rotation
- 대상:
  - `AuthService`, `JwtTokenProvider`, Redis/session 저장 구조
- 작업:
  - 로그인 시 기존 세션 전부 즉시 무효화
  - 기존 기기에는 "다른 기기에서 로그인됨" 모달 강제 확인
  - refresh 시 토큰 회전(기존 refresh 폐기)
- 완료 기준:
  - 동일 계정 동시 로그인 완전 차단
  - 탈취 refresh 재사용 방지

### 3-6) 비공개 방 식별자/참가 보안 강화
- 대상:
  - Room 엔티티/DTO/컨트롤러/프론트 라우팅
- 작업:
  - 순차 PK 직접 노출 제거 (UUID 또는 랜덤 초대코드)
  - 비공개 방은 목록에서 자물쇠 아이콘만 노출
  - 초대코드 검증 성공 시 참가 허용 (비밀번호는 별도 수단)
  - `invite_code_expires_at` 도입 + TTL(`INVITE_CODE_TTL_HOURS`, 기본 24) 검증
  - 재발급 시 기존 코드 즉시 무효 + TTL 재시작
  - `public_id`/`invite_code` 재사용 영구 금지
- 완료 기준:
  - 예측 가능한 순차 식별자 의존 제거
  - 비공개 방 접근 제어 시나리오 테스트 통과

### 3-5) 환경 변수 운영값 검증
- 대상:
  - `application.yml` + 배포 환경 변수
- 점검 항목:
  - `COOKIE_SECURE=true` (prod)
  - `COOKIE_SAMESITE=Lax/None` 정책 반영
  - `WEBSOCKET_ALLOWED_ORIGINS` 운영 도메인만 허용
  - `OAUTH2_REDIRECT_URI` 실제 프론트 경로 일치
  - `MATCHING_MAX_LIMIT_FREE/PREMIUM`, `ADS_DAILY_VIEW_LIMIT`,
    `REPORT_DUPLICATE_BLOCK_DAYS`, `NICKNAME_CHANGE_COOLDOWN_DAYS`
- 완료 기준:
  - dev/prod 설정값 매트릭스 문서화

---

## Phase 4. 문서/운영 정리 (P2)
목표: 다음 작업자가 바로 이어받을 수 있는 상태

### 4-1) 테스트 문서 최신화
- 대상: `TEST_STRATEGY.md`
- 작업:
  - `mvnw` 표기 제거, `gradlew` 기준으로 통일
  - 단일 테스트 실행 예시 업데이트

### 4-2) 리뷰 문서 상태 업데이트
- 대상: `docs/PROJECT_REVIEW_CHECKLIST_2026-03-05.md`
- 작업:
  - 항목별 상태를 `완료/진행중/미착수`로 갱신
  - 남은 이슈는 티켓 링크 또는 TODO ID 연결

### 4-3) 에러 페이지 도입
- 대상:
  - 프론트 라우팅/공통 에러 컴포넌트
- 작업:
  - 최소 `401/403/404/500` 에러 페이지 구현
  - `404/403/500`은 공통 에러 페이지 라우팅
  - 비밀번호 오류는 에러 페이지로 보내지 않고 토스트 유지
- 완료 기준:
  - 주요 오류 상황에서 빈 화면 없이 일관된 안내 제공

---

## 오늘 바로 실행 순서 (추천)
1. `GameControllerTest`, `RoomControllerTest` 보안 빈 누락 해결
2. 테스트 DB를 Testcontainers PostgreSQL로 전환
3. `./gradlew test` 전체 실행 후 실패군 재분류
4. 수동 방 종료 제거 + 자동 참가 정책(OPEN only) 반영
5. 단일 세션/Refresh rotation + 기존기기 알림 구현
6. 비공개 방 식별자 보안(UUID/초대코드) + 에러 페이지 구현

## Definition of Done
- 백엔드: `./gradlew test`에서 컨텍스트 실패 0건
- 프론트: `npm test -- --run` 통과 유지
- 문서: 테스트 실행법/환경변수 정책 최신화 완료
- 릴리즈 전 체크: 인증(OAuth/쿠키), 방 생성/입장, 채팅 송수신 스모크 테스트 완료
