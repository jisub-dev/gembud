# Gembud 병렬 개발 운영 플레이북 - 2026-03-06

> **Last updated:** 2026-03-06 13:56 KST (by Claude, main terminal)

---

## STATUS BOARD

| 항목 | 값 |
|------|-----|
| `main` HEAD | `4673daf` feat: security hardening |
| Backend tests | ✅ 218 passed / 0 failed (2026-03-06 13:55 KST) |
| Frontend tests | ✅ 19 passed / 0 failed (2026-03-06 13:55 KST) |
| Frontend build | ✅ success (dist 454.75kB gzip 142.18kB) |

### 브랜치 현황

| 브랜치 | 상태 | 담당 | 마지막 업데이트 |
|--------|------|------|----------------|
| `main` | 🟢 최신 (push 완료) | Claude | 2026-03-06 13:56 |
| `feat/admin-security-core` | 🟡 대기 (미시작) | Terminal 1 | — |
| `feat/friend-search-flow` | 🟡 대기 (미시작) | Terminal 2 | — |
| `feat/chat-room-lifecycle` | 🟡 대기 (미시작) | Terminal 3 | — |

### 공통 파일 잠금 현황 (동시 수정 금지)

| 파일 | 잠금 브랜치 | 이유 |
|------|------------|------|
| 현재 없음 | — | 충돌 없음 |

---

## 1. 목적
- 여러 터미널/여러 Codex 세션에서 기능을 병렬로 개발한다.
- 충돌(동일 파일 동시 수정), 누락(테스트/문서), 병합 리스크를 줄인다.

---

## 2. 운영 원칙
1. `main` 직접 커밋 금지
2. 기능 단위 브랜치 1개 = 목적 1개
3. 동일 파일 동시 수정 금지
4. 공통 파일 수정 시 즉시 공유
5. 브랜치마다 테스트 통과 후 PR 생성

공통 파일(충돌 고위험) 예시:
- `backend/src/main/java/com/gembud/exception/ErrorCode.java`
- `backend/src/main/java/com/gembud/exception/GlobalExceptionHandler.java`
- `frontend/src/App.tsx`
- `docs/FEATURE_REQUIREMENTS_REFERENCE_SPEC_2026-03-05.md`

---

## 2A. 문서 업데이트 프로토콜 ⭐ (필독)

이 파일(`PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md`)은 **살아있는 공유 문서**다.
모든 터미널은 작업 상태가 바뀔 때마다 이 파일의 **STATUS BOARD**를 직접 수정해야 한다.

### 지휘 구조
```
Claude (main terminal) — 지휘/조율/병합 결정
  ├── Terminal 1 (feat/admin-security-core)
  ├── Terminal 2 (feat/friend-search-flow)
  └── Terminal 3 (feat/chat-room-lifecycle)
```

- **Claude(이 터미널)**가 STATUS BOARD를 최종 관리하고 병합 순서를 결정한다.
- **각 터미널**은 자신의 작업 단계가 바뀔 때 아래 규칙에 따라 STATUS BOARD를 업데이트하고,
  Claude 터미널에 완료 보고를 남긴다.

### 각 터미널이 업데이트해야 할 시점

| 시점 | 업데이트 항목 |
|------|-------------|
| 작업 시작 | 브랜치 현황 → `🔵 작업중` |
| 공통 파일 건드릴 때 | 공통 파일 잠금 현황에 행 추가 |
| 공통 파일 수정 완료 | 잠금 현황 → 잠금 해제로 변경 |
| 테스트/빌드 완료 | 브랜치 현황 → `🟠 PR 대기` + 테스트 결과 기록 |
| PR 생성 | 브랜치 현황 → PR 번호 기재 |

### 상태 이모지 규칙
| 이모지 | 의미 |
|--------|------|
| 🟡 | 대기 (미시작) |
| 🔵 | 작업중 |
| 🟠 | PR 대기 (테스트 통과, 병합 승인 기다리는 중) |
| 🟢 | 병합 완료 |
| 🔴 | 블록됨 (이유 명시) |

### STATUS BOARD 수정 방법
터미널에서 직접 Edit 도구로 이 파일의 STATUS BOARD 테이블 해당 행만 수정한다.
커밋은 불필요 — 로컬 수정으로 충분 (Claude가 병합 전 최신 상태 확인).

### 완료 보고 양식 (Claude에게 전달)
작업 완료 시 아래 양식을 메시지로 Claude에게 전달한다:
```
[완료 보고] Terminal N — feat/브랜치명
1) 변경 파일: (목록)
2) 핵심 변경: (요약)
3) 테스트 결과: (통과/실패 수)
4) 남은 리스크: (있으면 기재)
5) 공통 파일 수정 여부: (있으면 파일명)
6) PR: (번호 또는 링크)
```

---

## 3. 브랜치 네이밍
- 관리자: `feat/admin-*`
- 친구: `feat/friend-*`
- 보안/세션: `feat/security-*`
- 채팅/대기방: `feat/chat-*`

예시:
- `feat/admin-security-core`
- `feat/friend-search-flow`
- `feat/chat-room-lifecycle`

---

## 4. 터미널 병렬 시작 템플릿
권장: Claude는 설계/리뷰 전담, Codex는 구현/테스트 전담으로 분리

### Terminal 0 - Claude (설계/리뷰 전담)
```bash
git switch main && git pull
# 코드 수정 없이 문서/요구사항/리뷰 코멘트 정리 중심
```

### Terminal 1 - 관리자+보안 코어
```bash
git switch main && git pull
git switch -c feat/admin-security-core
```

### Terminal 2 - 친구
```bash
git switch main && git pull
git switch -c feat/friend-search-flow
```

### Terminal 3 - 채팅/대기방
```bash
git switch main && git pull
git switch -c feat/chat-room-lifecycle
```

---

## 4A. 에이전트 역할 분리 (Claude vs Codex)
## Claude에 맡길 작업
- 요구사항 정리/정책 확정
- 설계 옵션 비교, 트레이드오프 문서화
- PR 리뷰(리스크/회귀 가능성 점검)
- 릴리즈 체크리스트/운영 문서 작성

## Codex에 맡길 작업
- 실제 코드 구현(백엔드/프론트)
- 테스트 추가/수정 + 실행
- 빌드/타입 오류 복구
- 리뷰 반영 패치

## 권장 운영 루프
1. Claude: 스펙/수용기준 확정
2. Codex: 구현 + 테스트 + 빌드
3. Claude: 변경 리뷰/피드백
4. Codex: 피드백 반영 후 PR 마무리

---

## 5. 현재 권장 병렬 작업 분할
## A. 관리자+보안 코어 (`feat/admin-security-core`)
- admin/rate-limit 관련 컴파일 이슈 해결
- 관리자 API 권한 체크 정리
- 보안/세션 정책(단일세션/락/레이트리밋) 정합
- 관리자 화면/엔드포인트 스모크 테스트

핵심 대상 파일(예시):
- `backend/src/main/java/com/gembud/controller/Admin*`
- `backend/src/main/java/com/gembud/service/Admin*`
- `backend/src/main/java/com/gembud/security/*`
- `backend/src/main/java/com/gembud/service/RateLimitService.java`
- `backend/src/test/java/com/gembud/controller/Admin*Test.java`

## B. 친구 기능 (`feat/friend-search-flow`)
- 사용자 검색 기반 친구 요청 UX 고도화
- 요청/수락/거절 E2E 시나리오 추가
- 친구 요청 상태 배지/정렬 회귀 테스트

핵심 대상 파일(예시):
- `frontend/src/pages/FriendListPage.tsx`
- `frontend/src/hooks/queries/useFriends.ts`
- `frontend/src/test/hooks/useFriends.test.ts`
- `backend/src/main/java/com/gembud/service/FriendService.java`

## C. 채팅/대기방 정합 (`feat/chat-room-lifecycle`)
- ROOM_CHAT 라이프사이클(참여자 0명 시 정리) 검증
- 사이드바 이동/도메인 분리 회귀 테스트 강화

핵심 대상 파일(예시):
- `frontend/src/components/layout/Sidebar.tsx`
- `frontend/src/test/components/layout/Sidebar.test.tsx`
- `backend/src/main/java/com/gembud/service/ChatService.java`
- `backend/src/main/java/com/gembud/service/RoomService.java`

---

## 6. 작업중 체크리스트 (브랜치 공통)
1. 기능 요구사항 문서 반영 여부 확인
2. 테스트 추가/수정
3. 로컬 검증 실행
4. 변경 파일 범위 점검
5. PR 템플릿 작성

권장 검증 명령:
```bash
# frontend
cd frontend && npx vitest run
cd frontend && npm run build

# backend
cd backend && ./gradlew test
```

---

## 7. PR 규칙
PR 제목 예시:
- `[friend] 검색 기반 친구요청 + 상태배지`
- `[admin] rate-limit 컴파일 이슈 수정`

PR 본문 최소 포함:
1. 변경 목적
2. 변경 파일
3. 테스트 결과
4. 리스크/롤백 포인트

머지 전략:
- `Squash and merge`
- 머지 전 `main` rebase 또는 최신화 후 충돌 해결

---

## 8. 병합 순서 권장
1. 관리자+보안 코어 (`feat/admin-security-core`)
2. 채팅/대기방 (`feat/chat-*`)
3. 친구 (`feat/friend-*`)

이유:
- 공통 인프라/보안/에러코드 변경을 먼저 확정하면 이후 기능 브랜치 충돌이 줄어든다.

---

## 9. 운영 팁
- 긴 작업은 하루 1회 `main` 기준으로 재정렬(rebase 또는 merge)한다.
- 같은 파일을 건드려야 하면 "소유 브랜치"를 먼저 정하고 후속 브랜치는 해당 커밋을 cherry-pick 한다.
- 브랜치가 커지면 리뷰가 느려지므로 1 PR 300~500 LOC 내를 권장한다.
- Claude 터미널은 기본적으로 `main` 읽기 전용처럼 운영하고, 코드 커밋은 Codex 브랜치에서만 수행한다.
- 공통 파일 수정이 필요하면 먼저 Claude가 변경 우선순위를 정하고, Codex는 소유 브랜치에서 선반영 후 다른 브랜치가 따라간다.

---

## 10. 즉시 실행 프롬프트 템플릿

> **중요**: 아래 프롬프트를 각 터미널에 그대로 전달한다.
> 각 터미널은 STATUS BOARD를 직접 수정할 의무가 있다.
> 모든 결정(병합 순서, 공통 파일 소유권, PR 승인)은 **Claude(main terminal)**가 내린다.

---

### Terminal 1 (feat/admin-security-core)

```text
너는 Terminal 1이야. 브랜치: feat/admin-security-core

## 살아있는 공유 문서
이 파일을 반드시 읽어라:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md

## 문서 업데이트 의무
작업 단계가 바뀔 때마다 위 문서의 STATUS BOARD를 직접 Edit 도구로 수정해야 한다:
- 시작 시: feat/admin-security-core 행 → 🔵 작업중
- 공통 파일 건드릴 때: 잠금 현황 테이블에 행 추가
- 완료 시: → 🟠 PR 대기 + 테스트 결과 기재

## 작업 범위
관리자 컨트롤러 테스트 NPE 픽스 + RateLimitService 통합 테스트 보완
(섹션 5. A 참조)

## 완료 후
아래 양식을 Claude(main terminal)에 전달:
[완료 보고] Terminal 1 — feat/admin-security-core
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과:
4) 남은 리스크:
5) 공통 파일 수정 여부:
6) PR:
```

---

### Terminal 2 (feat/friend-search-flow)

```text
너는 Terminal 2야. 브랜치: feat/friend-search-flow

## 살아있는 공유 문서
이 파일을 반드시 읽어라:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md

## 문서 업데이트 의무
작업 단계가 바뀔 때마다 위 문서의 STATUS BOARD를 직접 Edit 도구로 수정해야 한다:
- 시작 시: feat/friend-search-flow 행 → 🔵 작업중
- 공통 파일 건드릴 때: 잠금 현황 테이블에 행 추가 + Claude에게 먼저 보고
- 완료 시: → 🟠 PR 대기 + 테스트 결과 기재

## 작업 범위
친구 검색/요청/수락/거절 UX + FriendListPage 탭 분리
(섹션 5. B 참조)

## 완료 후
아래 양식을 Claude(main terminal)에 전달:
[완료 보고] Terminal 2 — feat/friend-search-flow
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과:
4) 남은 리스크:
5) 공통 파일 수정 여부:
6) PR:
```

---

### Terminal 3 (feat/chat-room-lifecycle)

```text
너는 Terminal 3이야. 브랜치: feat/chat-room-lifecycle

## 살아있는 공유 문서
이 파일을 반드시 읽어라:
/Users/gimjiseob/Projects/gembud/docs/PARALLEL_DEVELOPMENT_PLAYBOOK_2026-03-06.md

## 문서 업데이트 의무
작업 단계가 바뀔 때마다 위 문서의 STATUS BOARD를 직접 Edit 도구로 수정해야 한다:
- 시작 시: feat/chat-room-lifecycle 행 → 🔵 작업중
- 공통 파일 건드릴 때: 잠금 현황 테이블에 행 추가 + Claude에게 먼저 보고
- 완료 시: → 🟠 PR 대기 + 테스트 결과 기재

## 작업 범위
채팅방 라이프사이클(참여자 0명 정리) + Sidebar 회귀 테스트
(섹션 5. C 참조)

## 완료 후
아래 양식을 Claude(main terminal)에 전달:
[완료 보고] Terminal 3 — feat/chat-room-lifecycle
1) 변경 파일:
2) 핵심 변경:
3) 테스트 결과:
4) 남은 리스크:
5) 공통 파일 수정 여부:
6) PR:
```
