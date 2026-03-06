# Gembud 병렬 개발 운영 플레이북 - 2026-03-06

> **Last updated:** 2026-03-06 21:58 KST (by Claude, main terminal) — 3라운드 완료

---

## STATUS BOARD

| 항목 | 값 |
|------|-----|
| `main` HEAD | `f48a43e` 3라운드 완료 (BCrypt + publicId URL + 방 정책) |
| Backend tests | ✅ 239 passed / 0 failed (2026-03-06 21:56 KST) |
| Frontend tests | ✅ 24 passed / 0 failed (2026-03-06 21:56 KST) |
| Frontend build | ✅ success (dist 455.34kB gzip 142.42kB) |

### Worktree 구조

| 경로 | 브랜치 | 담당 |
|------|--------|------|
| `/Users/gimjiseob/Projects/gembud` | `main` | Claude (지휘) |
| `gembud-t1/t2/t3` | — | 제거 완료 (4라운드에 재생성) |

### 브랜치 현황

| 브랜치 | 상태 | 담당 | PR | 마지막 업데이트 |
|--------|------|------|----|----------------|
| `main` | 🟢 최신 | Claude | — | 2026-03-06 21:58 |
| `feat/t1-password-bcrypt` | 🟢 병합 완료 | Terminal 1 | squash merged | 2026-03-06 21:58 |
| `feat/t2-publicid-url` | 🟢 병합 완료 | Terminal 2 | squash merged | 2026-03-06 21:58 |
| `feat/t3-room-policy` | 🟢 병합 완료 | Terminal 3 | squash merged | 2026-03-06 21:58 |
| `feat/t3-spec-remaining` | 🟢 PR 준비 완료 | Terminal 3 | — | 2026-03-06 22:08 (RoomServiceTest + backend test --continue 통과) |

### 공통 파일 잠금 현황 (동시 수정 금지)

| 파일 | 잠금 브랜치 | 상태 | 이유 |
|------|------------|------|------|
| 현재 없음 | — | — | 충돌 없음 |
| `docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md` | `feat/t2-publicid-url` | 해제 | Terminal 2 시작/완료 상태 업데이트 반영 완료 |
| `docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md` | `feat/t1-password-bcrypt` | 해제 | Terminal 1 시작/완료 상태 업데이트 반영 |
| `docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md` | `feat/t3-spec-remaining` | 해제 | Terminal 3 시작/완료 상태 업데이트 반영 |

---

## 1. 목적

- 여러 터미널/에이전트에서 기능을 병렬로 개발한다.
- 충돌(동일 파일 동시 수정), 누락(테스트/문서), 병합 리스크를 줄인다.
- Claude(main terminal)가 지휘 센터 역할을 하며 모든 병합 결정을 내린다.

---

## 2. 지휘 구조

```
Claude (main terminal) — 지휘 / 조율 / 병합 결정 / 문서 최종 관리
  ├── Terminal 1 — feat/admin-security-core
  ├── Terminal 2 — feat/friend-search-flow
  └── Terminal 3 — feat/chat-room-lifecycle
```

- **Claude**만 main에 직접 커밋/push할 수 있다.
- **각 터미널**은 자신의 브랜치에서만 커밋/push하고, PR을 올리면 Claude가 승인 후 병합한다.
- 병합 순서, 공통 파일 소유권, 충돌 해결 방향은 모두 Claude가 결정한다.

---

## 3. 운영 원칙

1. `main` 직접 커밋 금지 (Claude 예외)
2. 기능 단위 브랜치 1개 = 목적 1개
3. 동일 파일 동시 수정 금지 → 공통 파일 잠금 현황 확인 필수
4. 공통 파일 수정 필요 시 → Claude에게 먼저 보고 후 승인받고 수정
5. 브랜치마다 테스트 + 빌드 통과 후 PR 생성

### 공통 파일 (충돌 고위험 — 수정 전 Claude 승인 필수)
- `backend/src/main/java/com/gembud/exception/ErrorCode.java`
- `backend/src/main/java/com/gembud/exception/GlobalExceptionHandler.java`
- `frontend/src/App.tsx`
- `docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md` (이 파일)

---

## 4. 살아있는 문서 프로토콜 ⭐ (필독)

이 파일은 **모든 터미널이 공유하는 실시간 상태판**이다.

### 각 터미널의 업데이트 의무

작업 단계가 바뀔 때마다 이 파일의 STATUS BOARD를 **직접 Edit 도구로** 수정한다.

| 시점 | 업데이트 항목 |
|------|-------------|
| 작업 시작 | 브랜치 현황 → `🔵 작업중` + 날짜 기재 |
| 공통 파일 수정 시작 | 잠금 현황 테이블에 행 추가 |
| 공통 파일 수정 완료 | 잠금 현황 → `해제` 로 변경 |
| 테스트/빌드 완료 | 브랜치 현황 → `🟠 PR 대기` + 테스트 결과 기재 |
| PR 생성 | PR 열 → PR 번호 기재 |
| 병합 완료 (Claude) | `🟢 병합 완료` + main HEAD 업데이트 |

### 상태 이모지 규칙

| 이모지 | 의미 |
|--------|------|
| 🟡 | 대기 (미시작) |
| 🔵 | 작업중 |
| 🟠 | PR 대기 (테스트 통과, Claude 승인 기다리는 중) |
| 🟢 | 병합 완료 / 최신 |
| 🔴 | 블록됨 (이유 명시) |

### 완료 보고 양식 (Claude에게 전달)

```
[완료 보고] Terminal N — feat/브랜치명
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과: (통과 N / 실패 N)
4) 남은 리스크:
5) 공통 파일 수정 여부: (없음 or 파일명 목록)
6) PR: (번호 또는 링크)
```

---

## 5. 커밋 / 푸시 / 머지 타이밍 전략 ⭐

### 기본 흐름

```
브랜치 내부 커밋 (자유)
  → push origin feat/브랜치명
    → PR 생성 → Claude 리뷰 → 병합 승인
      → Squash merge into main
        → 다른 터미널: git fetch && git rebase origin/main
```

### 각 단계별 규칙

#### 브랜치 내부 커밋
- 작업 단위로 자유롭게 커밋한다 (WIP 커밋 허용).
- `main`에는 절대 직접 push하지 않는다.

#### push 타이밍
- 하루 1회 이상 `git push origin feat/브랜치명` — 작업 내용 백업 및 Claude 모니터링 목적.
- PR 올릴 준비가 됐을 때 최종 push.

#### PR 생성 조건 (모두 충족 시)
```
✅ 테스트 통과 (backend ./gradlew test, frontend npx vitest run)
✅ 빌드 성공 (frontend npm run build)
✅ 공통 파일 수정 없음 or Claude 승인 완료
✅ STATUS BOARD → 🟠 PR 대기로 업데이트
```

#### 병합 순서 (Claude가 집행)

| 순서 | 브랜치 | 이유 |
|------|--------|------|
| 1st | `feat/admin-security-core` | ErrorCode 등 공통 인프라 변경 가능성 → 먼저 확정해야 후속 충돌 감소 |
| 2nd | `feat/chat-room-lifecycle` | 비교적 독립적, 백엔드 서비스 레이어만 건드림 |
| 3rd | `feat/friend-search-flow` | ErrorCode/GlobalExceptionHandler 충돌 가능성 → 마지막에 rebase해서 깨끗하게 정리 |

#### main이 앞서갈 때 (다른 브랜치가 먼저 병합됐을 때)

각 터미널이 스스로 실행:
```bash
git fetch origin
git rebase origin/main
# 충돌 발생 시 → 해결 후 git rebase --continue
# 해결 불가 시 → Claude에 보고, Claude가 소유권 결정
```

rebase 완료 후 STATUS BOARD의 "마지막 업데이트" 날짜만 갱신.

#### 병합 전략
- **Squash and merge** 고정 (커밋 히스토리 깔끔하게 유지).
- 병합 후 Claude가 STATUS BOARD `main` HEAD 업데이트 + 해당 브랜치 → `🟢 병합 완료`.

### 긴급 충돌 처리 절차

1. 터미널이 충돌 발생 시 즉시 Claude에 보고
2. Claude가 소유 브랜치 결정 (어느 쪽 변경을 살릴지)
3. Claude가 결정 내용을 STATUS BOARD 잠금 현황에 기재
4. 소유 브랜치 터미널이 변경 적용, 다른 브랜치는 cherry-pick 또는 rebase

---

## 6. 브랜치 시작 명령

```bash
# Terminal 1
git switch main && git pull
git switch -c feat/admin-security-core

# Terminal 2
git switch main && git pull
git switch -c feat/friend-search-flow

# Terminal 3
git switch main && git pull
git switch -c feat/chat-room-lifecycle
```

---

## 7. 현재 작업 범위

### A. Terminal 1 — `feat/admin-security-core`
**목표**: 관리자 컨트롤러 테스트 NPE 픽스 + RateLimitService 통합 테스트 보완

- Admin 컨트롤러(`@PreAuthorize("hasRole('ADMIN')")`) 테스트에서 `CustomUserDetails` NPE → `@WithMockUser` 대신 `CustomUserDetails` 직접 생성 방식으로 픽스
- `RateLimitService` WS CONNECT 레이트리밋 통합 테스트 보완
- `SecurityEventService` `@Async` 동작 검증

핵심 파일:
```
backend/src/main/java/com/gembud/controller/Admin*Controller.java
backend/src/test/java/com/gembud/controller/Admin*ControllerTest.java
backend/src/main/java/com/gembud/service/RateLimitService.java
backend/src/main/java/com/gembud/config/SecurityConfig.java
```

검증:
```bash
cd backend && ./gradlew test --tests "com.gembud.controller.Admin*" --continue
cd backend && ./gradlew test --tests "com.gembud.service.RateLimitServiceTest"
```

### B. Terminal 2 — `feat/friend-search-flow`
**목표**: 친구 검색 기반 요청/수락/거절 UX + FriendListPage 탭 분리

- 닉네임 검색 → 친구 요청 전송 UI
- 친구 요청 상태 배지 (PENDING/ACCEPTED/REJECTED) 정렬/표시
- `FriendListPage.tsx` 탭 분리 (받은 요청 / 보낸 요청)
- E2E 시나리오 테스트 추가

핵심 파일:
```
frontend/src/pages/FriendListPage.tsx
frontend/src/hooks/queries/useFriends.ts
frontend/src/test/hooks/useFriends.test.ts
backend/src/main/java/com/gembud/service/FriendService.java
```

검증:
```bash
cd frontend && npx vitest run --reporter=verbose
cd frontend && npm run build
cd backend && ./gradlew test --tests "com.gembud.service.FriendServiceTest"
```

### C. Terminal 3 — `feat/chat-room-lifecycle`
**목표**: 채팅방 라이프사이클 정합 + Sidebar 회귀 테스트

- 참여자 0명 시 chat room 자동 정리 로직 검증
- `ChatWebSocketController` join/leave 이벤트 처리 정합
- `Sidebar.tsx` 채팅방 링크 이동 회귀 테스트

핵심 파일:
```
frontend/src/components/layout/Sidebar.tsx
frontend/src/test/components/layout/Sidebar.test.tsx
backend/src/main/java/com/gembud/service/ChatService.java
backend/src/main/java/com/gembud/websocket/ChatWebSocketController.java
```

검증:
```bash
cd frontend && npx vitest run --reporter=verbose
cd frontend && npm run build
cd backend && ./gradlew test --tests "com.gembud.service.ChatServiceTest"
```

---

## 8. 작업중 체크리스트 (브랜치 공통)

```
[ ] STATUS BOARD → 🔵 작업중으로 업데이트
[ ] 공통 파일 수정 필요 여부 확인 → 필요 시 Claude에 먼저 보고
[ ] 기능 구현
[ ] 테스트 추가/수정
[ ] 로컬 검증 실행 (테스트 + 빌드)
[ ] STATUS BOARD → 🟠 PR 대기로 업데이트 + 테스트 결과 기재
[ ] git push origin feat/브랜치명
[ ] PR 생성
[ ] Claude에 완료 보고 전달
```

---

## 9. PR 규칙

제목 형식: `[범위] 핵심 변경 요약`
```
[admin] 관리자 컨트롤러 테스트 NPE 픽스 + RateLimitService 통합 테스트
[friend] 검색 기반 친구 요청 + FriendListPage 탭 분리
[chat] 채팅방 라이프사이클 정합 + Sidebar 회귀 테스트
```

PR 본문 필수 포함:
1. 변경 목적
2. 변경 파일 목록
3. 테스트 결과 (통과 N / 실패 N)
4. 리스크 / 롤백 포인트
5. 공통 파일 수정 여부

병합 전략: **Squash and merge** 고정

---

## 10. 즉시 실행 프롬프트 템플릿

> **중요**: 각 터미널은 반드시 지정된 디렉토리에서만 작업한다.
> 디렉토리가 다르면 브랜치가 섞이지 않는다 (git worktree 구조).

---

### Terminal 1

```
작업 디렉토리: /Users/gimjiseob/Projects/gembud-t1
브랜치: feat/t1-admin-security-core

## 시작 명령
cd /Users/gimjiseob/Projects/gembud-t1

## 필수: 살아있는 공유 문서 확인
작업 시작 전 이 파일을 Read 도구로 반드시 읽어라:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md

STATUS BOARD를 확인하고 공통 파일 잠금 현황을 체크한 후 작업을 시작한다.

## 문서 업데이트 의무
작업 단계가 바뀔 때마다 아래 파일의 STATUS BOARD를 Edit 도구로 직접 수정한다:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md
- 시작 시: feat/t1-admin-security-core 행 → 🔵 작업중 + 날짜
- 공통 파일 건드릴 때: 잠금 현황 테이블에 행 추가 (수정 전 Claude에 보고)
- 완료 시: → 🟠 PR 대기 + 테스트 결과 기재

## 커밋/푸시 규칙
- /Users/gimjiseob/Projects/gembud-t1 에서만 작업한다.
- 완료 시: git push origin feat/t1-admin-security-core → PR 생성
- main이 앞서간 경우: git fetch && git rebase origin/main 후 계속

## 작업 범위 (섹션 7. A 참조)
관리자 컨트롤러 테스트 NPE 픽스 + RateLimitService 통합 테스트 보완

## 검증 명령
cd /Users/gimjiseob/Projects/gembud-t1/backend && ./gradlew test --tests "com.gembud.controller.Admin*" --continue
cd /Users/gimjiseob/Projects/gembud-t1/backend && ./gradlew test --continue

## 완료 후 Claude에 전달
[완료 보고] Terminal 1 — feat/t1-admin-security-core
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과:
4) 남은 리스크:
5) 공통 파일 수정 여부:
6) PR:
```

---

### Terminal 2

```
작업 디렉토리: /Users/gimjiseob/Projects/gembud-t2
브랜치: feat/t2-friend-search-flow

## 시작 명령
cd /Users/gimjiseob/Projects/gembud-t2

## 필수: 살아있는 공유 문서 확인
작업 시작 전 이 파일을 Read 도구로 반드시 읽어라:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md

STATUS BOARD를 확인하고 공통 파일 잠금 현황을 체크한 후 작업을 시작한다.

## 문서 업데이트 의무
작업 단계가 바뀔 때마다 아래 파일의 STATUS BOARD를 Edit 도구로 직접 수정한다:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md
- 시작 시: feat/t2-friend-search-flow 행 → 🔵 작업중 + 날짜
- 공통 파일 건드릴 때: 잠금 현황 테이블에 행 추가 (수정 전 Claude에 보고)
- 완료 시: → 🟠 PR 대기 + 테스트 결과 기재

## 커밋/푸시 규칙
- /Users/gimjiseob/Projects/gembud-t2 에서만 작업한다.
- 완료 시: git push origin feat/t2-friend-search-flow → PR 생성
- main이 앞서간 경우: git fetch && git rebase origin/main 후 계속

## 작업 범위 (섹션 7. B 참조)
친구 검색/요청/수락/거절 UX 고도화 + FriendListPage 탭 분리
(닉네임 검색 UI, 상태 배지, sent/received 탭)

## 검증 명령
cd /Users/gimjiseob/Projects/gembud-t2/frontend && npx vitest run --reporter=verbose
cd /Users/gimjiseob/Projects/gembud-t2/frontend && npm run build
cd /Users/gimjiseob/Projects/gembud-t2/backend && ./gradlew test --tests "com.gembud.service.FriendServiceTest"

## 완료 후 Claude에 전달
[완료 보고] Terminal 2 — feat/t2-friend-search-flow
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과:
4) 남은 리스크:
5) 공통 파일 수정 여부:
6) PR:
```

---

### Terminal 3

```
작업 디렉토리: /Users/gimjiseob/Projects/gembud-t3
브랜치: feat/t3-chat-room-lifecycle

## 시작 명령
cd /Users/gimjiseob/Projects/gembud-t3

## 필수: 살아있는 공유 문서 확인
작업 시작 전 이 파일을 Read 도구로 반드시 읽어라:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md

STATUS BOARD를 확인하고 공통 파일 잠금 현황을 체크한 후 작업을 시작한다.

## 문서 업데이트 의무
작업 단계가 바뀔 때마다 아래 파일의 STATUS BOARD를 Edit 도구로 직접 수정한다:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md
- 시작 시: feat/t3-chat-room-lifecycle 행 → 🔵 작업중 + 날짜
- 공통 파일 건드릴 때: 잠금 현황 테이블에 행 추가 (수정 전 Claude에 보고)
- 완료 시: → 🟠 PR 대기 + 테스트 결과 기재

## 커밋/푸시 규칙
- /Users/gimjiseob/Projects/gembud-t3 에서만 작업한다.
- 완료 시: git push origin feat/t3-chat-room-lifecycle → PR 생성
- main이 앞서간 경우: git fetch && git rebase origin/main 후 계속

## 작업 범위 (섹션 7. C 참조)
채팅방 라이프사이클 정합 + Sidebar 회귀 테스트 보강

## 검증 명령
cd /Users/gimjiseob/Projects/gembud-t3/frontend && npx vitest run --reporter=verbose
cd /Users/gimjiseob/Projects/gembud-t3/frontend && npm run build
cd /Users/gimjiseob/Projects/gembud-t3/backend && ./gradlew test --tests "com.gembud.service.ChatServiceTest"

## 완료 후 Claude에 전달
[완료 보고] Terminal 3 — feat/t3-chat-room-lifecycle
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과:
4) 남은 리스크:
5) 공통 파일 수정 여부:
6) PR:
```

---

## 11. 운영 팁

- 같은 파일을 건드려야 하면 "소유 브랜치"를 먼저 정하고, 후속 브랜치는 병합 후 rebase해서 따라간다.
- 브랜치가 커지면 리뷰가 느려지므로 1 PR 300~500 LOC 내를 권장한다.
- 긴 작업은 하루 1회 `main` 기준으로 rebase해서 드리프트를 줄인다.
- WIP 커밋은 허용. PR 올리기 전 `git rebase -i`로 스쿼시해서 정리하면 더 좋다.
