# Gembud Active Status

> Last updated: 2026-03-23 KST
> Maintainer: Codex

---

## Current Snapshot

| 항목 | 값 |
|------|-----|
| Base HEAD | `0b9abd9` |
| Active branch | `main` |
| Current focus | room/chat lifecycle stabilization + frontend active-room/query/test stabilization |
| Worktree state | modified files present, not committed |
| Verification | backend targeted test passed, frontend full suite passed |
| Default execution mode | main agent orchestrates, sub-agents used aggressively for separable work |

### Active Work Summary

- Backend:
  - `GET /rooms/my/active` active room contract added
  - room/chat invariant centralized in `ensureRoomChat()`
  - `OPEN`, `FULL`, `IN_PROGRESS` room list policy covered by test
  - `createRoom` / `joinRoom` now lock the user row before active-room checks
  - `joinRoom` now locks the room row before capacity-sensitive joins
  - CSRF cookie path now uses `/` so frontend routes can read `XSRF-TOKEN`
  - Added `GET /auth/csrf` as a lightweight CSRF bootstrap endpoint that does not depend on game catalog queries
- Frontend:
  - `방 종료` UI/API 제거 반영 유지
  - `ROOM008` 시 기존 활성 대기방 leave 후 재입장 UX 유지
  - Sidebar / RoomListPage now read active room from backend contract
  - `myRooms` / `myActiveRoom` query key and hook usage centralized
  - `myChatRooms` / `myRoomChatRooms` query key and hook usage centralized
  - API client now prefetches a CSRF cookie before the first non-GET request when needed
  - CSRF bootstrap now uses `GET /api/auth/csrf` instead of `GET /api/games`
  - App now redirects immediately to `/login` when the session-expired bridge fires
  - Chat socket session-expired events now use the same global session-expired bridge
  - Session-expiry bridge now falls back to `location.replace('/login')` even before the App-level handler is registered
  - RoomListPage create-modal query param flow extracted into a dedicated hook
  - RoomListPage invite-entry URL flow extracted into a dedicated hook
  - RoomListPage join/password/retry orchestration extracted into `useRoomJoinFlow`
  - Recommendation localStorage state and auto-join flow centralized into a shared hook/util layer
  - ROOM_CHAT mapping logic separated into shared selector helper
  - ChatPage evaluatable query now only runs for `IN_PROGRESS` / `CLOSED` rooms
  - Sidebar waiting-room navigation no longer toggles unnecessary async local state
- Tests:
  - backend `RoomServiceTest` updated
  - backend `RoomControllerTest` now covers `GET /rooms/my/active` and `POST /rooms/{publicId}/join` `ROOM008`
  - frontend `api` interceptor now has CSRF bootstrap coverage
  - frontend room/chat/sidebar targeted tests updated
  - RoomListPage test mocks aligned to the new hook layer
  - targeted room/chat/sidebar suites now run without `act(...)` warnings
  - full frontend vitest suite passes without `act(...)` warning output

### Current Modified Areas

- Backend:
  - `backend/src/main/java/com/gembud/controller/RoomController.java`
  - `backend/src/main/java/com/gembud/repository/RoomRepository.java`
  - `backend/src/main/java/com/gembud/repository/UserRepository.java`
  - `backend/src/main/java/com/gembud/service/RoomService.java`
  - `backend/src/test/java/com/gembud/controller/RoomControllerTest.java`
  - `backend/src/test/java/com/gembud/service/RoomServiceTest.java`
- Frontend:
  - `frontend/src/components/layout/Sidebar.tsx`
  - `frontend/src/hooks/queries/useChatQueries.ts`
  - `frontend/src/hooks/useRoomCreateEntry.ts`
  - `frontend/src/hooks/useRoomInviteEntry.ts`
  - `frontend/src/hooks/useRoomJoinFlow.ts`
  - `frontend/src/hooks/useRoomRecommendations.ts`
  - `frontend/src/hooks/queries/roomSelectors.ts`
  - `frontend/src/hooks/queries/useRooms.ts`
  - `frontend/src/pages/ChatPage.tsx`
  - `frontend/src/pages/RoomListPage.tsx`
  - `frontend/src/services/roomService.ts`
  - `frontend/src/test/components/layout/Sidebar.test.tsx`
  - `frontend/src/test/pages/ChatPage.test.tsx`
  - `frontend/src/test/pages/RoomListPage.test.tsx`
- Misc:
  - `docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md`
  - untracked local artifact: `.vite/`

### Known Risks / Follow-ups

- `RoomListPage` create/join/invite/recommendation 흐름은 대부분 훅으로 분리됐지만, 화면 레벨 배선과 invalidate 포인트는 여전히 페이지가 최종 조립을 맡는다.
- Backend now serializes `create/join` by user and locks joined rooms; controller coverage improved, but broader integration coverage for the new path is still thin.
- Worktree is dirty; changes are not yet committed or grouped into a final PR-ready unit.

### Working Preference

- From 2026-03-23 onward, default to sub-agent-heavy execution when the task can be cleanly split.
- Main agent owns:
  - task decomposition
  - file ownership boundaries
  - integration review
  - final verification
- Sub-agents own:
  - bounded backend changes
  - bounded frontend changes
  - targeted tests or review passes
- Avoid parallel edits on the same file unless explicitly intended and re-coordinated.

---

## Change Log

### 2026-03-23

#### Status

- In progress

#### Performed

- Added this status ledger at `docs/ACTIVE_STATUS.md`.
- Switched frontend active room handling to use backend `GET /rooms/my/active` instead of inferring the active room only from `myRooms + ROOM_CHAT`.
- Updated `Sidebar` and `RoomListPage` to consume `roomService.getMyActiveRoom()`.
- Consolidated `myRooms` and `myActiveRoom` onto shared room query keys/hooks in `frontend/src/hooks/queries/useRooms.ts`.
- Added shared chat query keys/hooks in `frontend/src/hooks/queries/useChatQueries.ts`.
- Extracted `RoomListPage` invite-entry URL parsing, room resolution, and invite banner state into `frontend/src/hooks/useRoomInviteEntry.ts`.
- Extracted `RoomListPage` create-modal query param handling into `frontend/src/hooks/useRoomCreateEntry.ts`.
- Extracted `RoomListPage` join/password/retry/navigation flow into `frontend/src/hooks/useRoomJoinFlow.ts`.
- Extracted recommendation storage rules and `recommend=true&exclude=...` auto-join flow into `frontend/src/hooks/useRoomRecommendations.ts`.
- Added pessimistic lock repository methods for `User` / `Room` and routed `createRoom` / `joinRoom` through them to tighten active-room uniqueness and join-capacity races.
- Added `RoomControllerTest` coverage for `GET /rooms/my/active` success and no-active-room paths to lock the controller contract around the new endpoint.
- Added `RoomControllerTest` coverage for `POST /rooms/{publicId}/join` returning `ROOM008` so the publicId join contract matches the frontend leave-then-rejoin flow.
- Updated `RoomServiceTest` so the `ALREADY_IN_OTHER_ROOM` create-room failure path also verifies `findByEmailForUpdate`, not just the happy path.
- Updated `RoomServiceTest` so representative `joinRoom` failure paths (`ROOM008`, `ROOM_FULL`, `ROOM_ALREADY_IN_PROGRESS`) also verify `findByEmailForUpdate` and `findByIdForUpdate`.
- Updated backend CSRF cookie path to `/` and frontend API bootstrap logic so login/signup can obtain and send `X-XSRF-TOKEN` from a public preflight GET instead of failing on the first POST.
- Replaced the session-expired confirm modal flow with an immediate redirect to `/login`.
- Routed chat socket `session-expired` events through the shared session-expiry bridge so HTTP and socket expiry follow the same redirect path.
- Added a bridge-level fallback redirect so early session-expired notifications still land on `/login` before the App handler mounts.
- Added `GET /auth/csrf` so login/signup CSRF bootstrap no longer depends on `GET /games`.
- Switched frontend CSRF prefetch from `GET /api/games` to `GET /api/auth/csrf`.
- Updated `Sidebar`, `RoomListPage`, and `ChatPage` to consume the shared room hooks and shared room query keys instead of inline raw keys.
- Updated `Sidebar` and `ChatPage` to consume shared chat hooks and shared chat query keys instead of inline raw keys.
- Updated `ChatPage` and `RoomListPage` to share the same recommendation localStorage contract.
- Removed legacy selector exports that inferred the active room from ROOM_CHAT mappings without the backend contract.
- Kept shared selector logic for ROOM_CHAT mapping in `frontend/src/hooks/queries/roomSelectors.ts`.
- Updated targeted frontend tests to match the new active room contract usage.
- Updated `RoomListPage` tests to mock `useMyRooms` and `useMyActiveRoom` directly.
- Gated `ChatPage` evaluatable-user fetching to evaluatable room states only.
- Removed unnecessary `RoomListPage` invite-state resets on mount and awaited join flows in event handlers.
- Simplified `Sidebar` waiting-room navigation by removing unnecessary transient opening state.
- Added async render/click/type helpers in room/chat/sidebar tests to eliminate targeted `act(...)` warning noise.
- Cleaned up `AdminPage` / `NotificationsPage` render and interaction flows so the full frontend suite no longer emits `act(...)` warning output.
- Recorded the default working preference to use sub-agents aggressively for future development.

#### Verification

- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/pages/ChatPage.test.tsx src/test/pages/RoomListPage.test.tsx src/test/components/layout/Sidebar.test.tsx --reporter=verbose`
  - Result:
    - `Test Files 3 passed (3), Tests 29 passed (29)`
  - Notes:
    - Targeted `act(...)` warnings are no longer reproduced in these suites.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run --reporter=basic`
  - Result:
    - `Test Files 10 passed (10), Tests 54 passed (54)`
  - Notes:
    - `AdminPage` / `NotificationsPage`까지 포함한 전체 프론트 suite에서 `act(...)` warning 출력이 재현되지 않음.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/pages/RoomListPage.test.tsx --reporter=verbose`
  - Result:
    - `Test Files 1 passed (1), Tests 16 passed (16)`
  - Notes:
    - invite-entry/create-entry hook 추출 후 `RoomListPage` 회귀 통과.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/pages/RoomListPage.test.tsx src/test/pages/ChatPage.test.tsx --reporter=verbose`
  - Result:
    - `Test Files 2 passed (2), Tests 19 passed (19)`
  - Notes:
    - recommendation hook/shared storage 추출 후 `RoomListPage` / `ChatPage` 추천 흐름 회귀 통과.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/pages/RoomListPage.test.tsx src/test/pages/ChatPage.test.tsx src/test/components/layout/Sidebar.test.tsx --reporter=basic`
  - Result:
    - `Test Files 3 passed (3), Tests 29 passed (29)`
  - Notes:
    - `useRoomJoinFlow` 추출 후 room/chat/sidebar 타깃 회귀 통과.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin npm run build`
  - Result:
    - `BUILD SUCCESSFUL`
  - Notes:
    - `useRoomJoinFlow` 추출 후 타입체크와 프로덕션 번들 빌드까지 통과.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/services/api.test.ts --reporter=verbose`
  - Result:
    - `Test Files 1 passed (1), Tests 2 passed (2)`
  - Notes:
    - 비-GET 요청 전 CSRF 쿠키 시드와 기존 토큰 재사용 로직을 고정.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/pages/ChatPage.test.tsx --reporter=verbose`
  - Result:
    - `Test Files 1 passed (1), Tests 5 passed (5)`
  - Notes:
    - 소켓 경유 session-expired 브리지 정리 후 `ChatPage` 타깃 회귀 통과.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/lib/sessionExpiryBridge.test.ts src/test/pages/ChatPage.test.tsx --reporter=verbose`
  - Result:
    - `Test Files 2 passed (2), Tests 7 passed (7)`
  - Notes:
    - 브리지 핸들러 경유와 handler 미등록 fallback redirect를 함께 고정.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/services/api.test.ts --reporter=verbose`
  - Result:
    - `Test Files 1 passed (1), Tests 2 passed (2)`
  - Notes:
    - 로그인 전 CSRF bootstrap 경로를 `GET /api/auth/csrf`로 바꾼 뒤 인터셉터 회귀 통과.
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin npm run build`
  - Result:
    - `BUILD SUCCESSFUL`
  - Notes:
    - 세션 만료 시 즉시 로그인 리다이렉트로 바꾼 뒤 프론트 타입체크와 번들 빌드 통과.
- Backend:
  - Command:
    - `./gradlew test --tests "com.gembud.controller.AuthControllerTest"`
  - Result:
    - `BUILD SUCCESSFUL`
  - Notes:
    - `GET /auth/csrf` bootstrap endpoint의 컨트롤러 계약을 고정.
- Backend:
  - Command:
    - `./gradlew compileJava`
  - Result:
    - `BUILD SUCCESSFUL`
  - Notes:
    - `SecurityConfig`의 CSRF cookie path 변경이 컴파일 기준으로 유효함을 확인.
- Backend:
  - Command:
    - `./gradlew test --tests "com.gembud.service.RoomServiceTest"`
  - Result:
    - `BUILD SUCCESSFUL`
  - Notes:
    - user-row / room-row lock 경로 추가 후 room lifecycle unit test 통과.
    - `createRoom`의 `ALREADY_IN_OTHER_ROOM` 실패 경로도 `findByEmailForUpdate`를 검증하도록 고정.
- Backend:
  - Command:
    - `./gradlew test --tests "com.gembud.controller.RoomControllerTest"`
  - Result:
    - `BUILD SUCCESSFUL`
  - Notes:
    - `GET /rooms/my/active` 정상/미존재와 `POST /rooms/{publicId}/join`의 `ROOM008` 매핑을 컨트롤러 테스트로 고정.

### 2026-03-20

#### Status

- In progress

#### Performed

- Added backend active room endpoint `GET /rooms/my/active`.
- Centralized room/chat invariant handling with `ensureRoomChat()` in the backend service.
- Added shared frontend selector logic for active room / ROOM_CHAT derivation.
- Removed `방 종료` UI/API path and aligned tests with room lifecycle changes.

#### Verification

- Backend:
  - Command:
    - `./gradlew test --tests "com.gembud.service.RoomServiceTest"`
  - Result:
    - `BUILD SUCCESSFUL`
- Frontend:
  - Command:
    - `PATH=/Users/gimjiseob/.nvm/versions/node/v22.17.1/bin:/usr/bin:/bin ./node_modules/.bin/vitest run src/test/pages/RoomListPage.test.tsx src/test/pages/ChatPage.test.tsx src/test/components/layout/Sidebar.test.tsx --reporter=verbose`
  - Result:
    - `Test Files 3 passed (3), Tests 29 passed (29)`

### 2026-03-19

#### Status

- In progress

#### Performed

- Applied room lifecycle hotfixes around waiting room and chat flow.
- Removed room close action from UI/API flow.
- Added `ROOM008` recovery UX: leave active waiting room, then retry joining the target room.
- Hid stale `내 대기방` ROOM_CHAT mappings and tightened room status handling across `OPEN`, `FULL`, `IN_PROGRESS`.

#### Reference

- See `docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md` hotfix memo for the earlier summary.

---

## Update Rules

When updating this file:

1. Refresh `Last updated`.
2. Update `Current Snapshot` to reflect the latest truth.
3. Add a new dated entry at the top of `Change Log`.
4. Record:
   - what changed
   - why it changed
   - how it was verified
   - what remains risky or unfinished
5. Keep commands and results concrete.

This file is intended to be the single running memory for current status and recent work history.
