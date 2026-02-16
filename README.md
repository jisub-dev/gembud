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
- [API 문서](docs/api/) (개발 중)
- [개발 가이드](docs/guides/) (개발 중)

## 개발 단계

현재 진행 상황:

- ✅ Phase 0: 프로젝트 셋업 및 문서화 기반 구축 (진행 중)
- ⏳ Phase 1: 코어 인증 시스템
- ⏳ Phase 2: 게임 및 방 시스템
- ⏳ Phase 3: 온도 및 평가 시스템
- ⏳ Phase 4: 실시간 채팅 시스템
- ⏳ Phase 5: 폴리싱 및 배포

자세한 내용은 [프로젝트 계획서](.claude/plans/)를 참고하세요.

## 라이선스

Copyright © 2026 Gembud Team. All rights reserved.
