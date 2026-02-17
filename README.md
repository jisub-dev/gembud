# Gembud - 게임 파티원 모집 플랫폼

게임 파티원을 모집하고 매칭할 수 있는 웹 플랫폼입니다. 롤(LoL), 배그(PUBG), 발로란트 등 다양한 게임을 지원합니다.

## 주요 기능

- 🎮 **게임별 파티 모집**: 롤, 배그, 발로란트 등 게임별 파티 생성
- 🌡️ **온도 시스템**: 당근마켓 스타일의 사용자 평가 시스템
- 💬 **실시간 채팅**: WebSocket 기반 실시간 채팅
- 🔐 **OAuth + 자체 로그인**: Google, Discord OAuth 지원
- 🎯 **하이브리드 매칭**: 공통 옵션 + 게임별 세부 옵션
- ⭐ **평가 시스템**: 게임 종료 후 파티원 평가

## 기술 스택

### Backend
- Java 17
- Spring Boot 3.2.x
- Spring Security (JWT + OAuth2)
- PostgreSQL 15
- Redis 7
- WebSocket (STOMP)
- Flyway

### Frontend
- React 18
- TypeScript 5.x
- Vite 5.x
- TanStack Query v5
- Zustand
- Tailwind CSS + shadcn/ui

### DevOps
- Docker & Docker Compose
- GitHub Actions

## 프로젝트 구조

```
gembud/
├── docs/                   # 프로젝트 문서
│   ├── adr/                # Architecture Decision Records
│   ├── api/                # API 문서
│   ├── erd/                # ERD 다이어그램
│   └── guides/             # 개발 가이드
├── backend/                # Spring Boot 백엔드
├── frontend/               # React 프론트엔드
└── docker-compose.yml      # Docker 설정
```

## 시작하기

### 필수 요구사항

- Java 17+
- Node.js 18+
- Docker & Docker Compose

### 개발 환경 설정

```bash
# 저장소 클론
git clone git@github.com:jisub-dev/gembud.git
cd gembud

# Docker 서비스 실행 (PostgreSQL, Redis)
docker-compose up -d

# 백엔드 실행
cd backend
./gradlew bootRun

# 프론트엔드 실행 (새 터미널)
cd frontend
npm install
npm run dev
```

## 문서

- [Architecture Decision Records](docs/adr/README.md)
- [API 문서](docs/api/API.md)
- [개발 가이드](docs/guides/) (개발 중)

## 개발 단계

현재 진행 상황:

### ✅ 완료된 Phase (MVP 백엔드)

- ✅ **Phase 0**: 프로젝트 셋업 및 문서화 기반 구축
- ✅ **Phase 1**: 코어 인증 시스템 (JWT + OAuth2)
  - JWT 토큰 기반 인증 (Access Token 1h, Refresh Token 7d)
  - Google, Discord OAuth2 연동
  - 자체 회원가입/로그인 (BCrypt 암호화)
- ✅ **Phase 2**: 게임 및 방 시스템
  - 게임 도메인 (롤, 배그, 발로란트)
  - 하이브리드 매칭 옵션 (공통 + 게임별 옵션)
  - 방 생성/참가/나가기
  - 방장 자동 이양 시스템
- ✅ **Phase 3**: 온도 및 평가 시스템
  - 온도 시스템 (0~100°C, 기본 36.5°C)
  - 평가 기반 온도 증감 (-0.5°C ~ +0.5°C)
  - 온도 30°C 미만 시 방 생성 제한
  - 다차원 평가 (매너, 실력, 소통)
- ✅ **Phase 4**: 실시간 채팅 시스템
  - WebSocket (STOMP) 기반 실시간 채팅
  - 차등 메시지 저장 전략
    - ROOM_CHAT: 저장 안 함 (실시간만)
    - GROUP_CHAT: 최근 100개만 저장
    - DIRECT_CHAT: 전부 저장
- ✅ **Phase 5**: 폴리싱 및 배포 준비
  - GlobalExceptionHandler (통합 예외 처리)
  - CORS 설정 및 XSS 방지 (HtmlSanitizer)
  - Redis 캐싱 (게임 목록, 옵션)
  - 46개의 서비스 테스트 케이스

- ✅ **Phase 6**: 친구 시스템
  - 양방향 친구 관계 관리
  - 친구 요청/수락/거절/삭제
  - 친구 목록 조회 (받은/보낸 요청 포함)
  - FriendService: 8개 비즈니스 로직
  - FriendController: 7개 REST API
  - 23개 테스트 케이스

- ✅ **Phase 7**: 그룹 및 1:1 채팅
  - CustomUserDetails 개선 (userId 포함)
  - 1:1 직접 채팅 생성
  - 그룹 채팅 생성 및 멤버 관리
  - ChatController: 5개 REST API
  - WebSocket과 REST API 통합
  - 5개 추가 테스트 케이스

- ✅ **Phase 8**: 매칭 추천 시스템
  - 다차원 점수 계산 알고리즘 (총 100점)
    - 필터 매칭: 40점
    - 온도 호환성: 30점
    - 과거 평가: 20점
    - 방장 온도 보너스: 10점
  - 사용자 맞춤 방 추천 API
  - 점수 기반 한글 추천 사유 생성
  - MatchingService: 핵심 추천 로직
  - MatchingController: GET /matching/recommendations/game/{gameId}
  - EvaluationRepository 확장 (과거 평가 조회)
  - 6개 테스트 케이스

- ✅ **Phase 9**: 신고 시스템
  - Report 엔티티 및 ReportStatus 열거형 (PENDING, REVIEWED, RESOLVED)
  - ReportRepository: 8개 복잡한 쿼리 메서드
    - 신고자/피신고자/상태/방별 조회
    - 중복 신고 체크 (동일 방 내)
    - 대기 중인 신고 개수 조회
  - ReportService: 9개 비즈니스 로직
    - 신고 생성 (자기 자신 신고 방지, 중복 체크)
    - 신고 상태 관리 (검토/처리 완료)
    - 관리자 코멘트 추가
  - ReportController: 8개 REST API
    - POST /reports - 신고 생성
    - GET /reports/my - 내 신고 목록
    - GET /reports/status/{status} - 상태별 조회 (관리자)
    - GET /reports/user/{userId} - 사용자별 신고 조회 (관리자)
    - PUT /reports/{id}/review - 검토 시작 (관리자)
    - PUT /reports/{id}/resolve - 처리 완료 (관리자)
    - DELETE /reports/{id} - 신고 삭제 (관리자)
  - Flyway V11 마이그레이션 (reports 테이블)
  - 20개 포괄적인 테스트 케이스

- ✅ **Phase 10**: 실시간 알림 시스템
  - Notification 엔티티 및 NotificationType 열거형
    - FRIEND_REQUEST, FRIEND_ACCEPTED, ROOM_INVITE
    - ROOM_JOIN, EVALUATION_RECEIVED, REPORT_RESOLVED
  - NotificationRepository: 6개 쿼리 메서드
    - 사용자별/읽지 않은 알림 조회
    - 읽지 않은 알림 개수 카운트
    - 일괄 읽음 처리 (UPDATE 쿼리)
    - 30일 이상 된 읽은 알림 자동 삭제
  - NotificationService: 9개 비즈니스 로직
    - createNotification(): 알림 생성 및 WebSocket 전송
    - getMyNotifications(): 내 알림 목록
    - getUnreadNotifications(): 읽지 않은 알림
    - getUnreadCount(): 읽지 않은 개수
    - markAsRead(): 개별 읽음 처리
    - markAllAsRead(): 일괄 읽음 처리
    - deleteNotification(): 알림 삭제
    - cleanupOldNotifications(): 오래된 알림 정리
  - NotificationController: 6개 REST API
    - GET /notifications - 내 알림 목록
    - GET /notifications/unread - 읽지 않은 알림
    - GET /notifications/unread/count - 읽지 않은 개수
    - PUT /notifications/{id}/read - 읽음 처리
    - PUT /notifications/read-all - 모두 읽음
    - DELETE /notifications/{id} - 알림 삭제
  - WebSocket 실시간 알림 전송
    - SimpMessagingTemplate 활용
    - /user/queue/notifications 구독
    - 전송 실패 시 graceful 처리
  - Flyway V12 마이그레이션 (notifications 테이블)
  - 15개 포괄적인 테스트 케이스
    - 알림 생성 및 전송 검증
    - WebSocket 실패 처리 테스트
    - 권한 검증 (다른 사용자 알림 접근 방지)
    - 일괄 처리 및 자동 정리 테스트

### 📊 통계
- **114개 파일, 13,491줄** 추가
- **Backend**: 89개 파일 (엔티티, 서비스, 컨트롤러, 테스트)
- **Frontend**: 8개 파일 (인증 UI, API 서비스, 상태 관리)
- **Database**: 12개 Flyway 마이그레이션
- **Tests**: 총 136개 테스트 케이스
- **API Documentation**: 완전한 REST API 문서 (실시간 알림 포함)

### 🚀 다음 단계

- ⏳ Phase 11: 스팀 게임 확장
- ⏳ Phase 12: 게임사 API 연동

자세한 내용은 [프로젝트 계획서](.claude/plans/)와 [API 문서](docs/api/API.md)를 참고하세요.

## 라이선스

Copyright © 2026 Gembud Team. All rights reserved.
