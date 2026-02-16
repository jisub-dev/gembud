# ADR-0001: 프로젝트 아키텍처 개요

## 상태
Accepted

## 날짜
2026-02-16

## 컨텍스트 (Context)

게임 파티원 모집 플랫폼 "Gembud"를 엔터프라이즈급 수준으로 개발하기 위해 전체 아키텍처를 설계해야 합니다.

### 주요 요구사항
- 실시간 채팅 및 알림
- 사용자 평가 기반 온도 시스템
- 게임별 세분화된 매칭 옵션
- 확장 가능한 구조
- 높은 코드 품질 (테스트 커버리지 80% 이상)
- 전문 개발팀 수준의 문서화 및 품질 관리

### 제약사항
- 빠른 MVP 출시 필요
- 향후 모바일 앱 확장 가능성
- 트래픽 증가에 대비한 확장성

## 결정 (Decision)

**마이크로서비스가 아닌 모놀리식 아키텍처를 기반으로 백엔드와 프론트엔드를 분리한 구조를 채택합니다.**

### 아키텍처 구성

```
┌─────────────────┐         ┌─────────────────────────┐
│                 │         │                         │
│  React (SPA)    │ ◄─────► │   Spring Boot (API)     │
│  + TypeScript   │  REST   │   + WebSocket           │
│                 │  WS     │                         │
└─────────────────┘         └──────────┬──────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
              ┌─────▼─────┐      ┌────▼────┐      ┌──────▼─────┐
              │ PostgreSQL│      │  Redis  │      │  S3/CDN    │
              │  (메인 DB) │      │(캐시/세션)│      │(이미지 등)  │
              └───────────┘      └─────────┘      └────────────┘
```

### 기술 스택

**백엔드:**
- Spring Boot 3.2.x (Java 17)
- Spring Security + JWT + OAuth2
- Spring Data JPA
- PostgreSQL 15
- Redis 7
- WebSocket (STOMP)
- Flyway (DB 마이그레이션)

**프론트엔드:**
- React 18 + TypeScript 5.x
- Vite 5.x
- TanStack Query v5 (서버 상태)
- Zustand (클라이언트 상태)
- Tailwind CSS + shadcn/ui
- WebSocket (STOMP.js)

**DevOps:**
- Docker & Docker Compose
- GitHub Actions (CI/CD)

## 근거 (Rationale)

### 모놀리식 아키텍처 선택 이유

**장점:**
1. **빠른 개발 및 배포**: MVP 단계에서 마이크로서비스의 복잡도는 불필요
2. **단순한 트랜잭션 관리**: 온도 계산, 평가 등 여러 엔티티를 동시에 처리하는 로직이 많음
3. **낮은 운영 복잡도**: 초기 단계에서 여러 서비스 관리 부담 감소
4. **성능**: 네트워크 호출 없이 메서드 호출로 처리 가능

**단점 (허용 가능):**
- 향후 확장 시 모듈 분리 필요 → 계층 구조를 명확히 하여 추후 분리 용이하게 설계
- 특정 기능의 독립적 배포 불가 → MVP 단계에서는 문제 없음

### Spring Boot 선택 이유

1. **강력한 생태계**: Spring Security, Spring Data JPA 등 엔터프라이즈급 기능
2. **WebSocket 지원**: 실시간 채팅 구현에 적합
3. **테스트 지원**: Spring Test, MockMvc 등 풍부한 테스트 도구
4. **팀 경험**: 카카오 전문 개발팀의 Spring 경험 활용

### React 선택 이유

1. **풍부한 생태계**: TanStack Query, Zustand 등 검증된 라이브러리
2. **컴포넌트 재사용성**: shadcn/ui로 일관된 UI 구축
3. **TypeScript 지원**: 타입 안정성으로 런타임 에러 감소
4. **향후 React Native 확장 가능성**: 코드 재사용 용이

### PostgreSQL 선택 이유

1. **ACID 보장**: 평가, 온도 계산 등 정확성 필요
2. **복잡한 쿼리 지원**: 매칭 필터링, 추천 시스템 구현에 유리
3. **JSON 타입 지원**: 게임별 옵션 저장에 유연성 제공
4. **확장성**: 향후 Read Replica, Sharding 가능

### Redis 선택 이유

1. **캐싱**: 게임 목록, 방 목록 등 자주 조회되는 데이터 캐싱
2. **세션 관리**: JWT Refresh Token 저장
3. **WebSocket 메시지 브로커**: 실시간 채팅 메시지 브로드캐스트
4. **고성능**: 인메모리 데이터 스토어로 빠른 응답 속도

## 고려한 대안 (Alternatives Considered)

### 대안 1: 마이크로서비스 아키텍처

**설명**: 인증, 매칭, 채팅, 평가를 각각 독립 서비스로 분리

**장점:**
- 각 서비스 독립 배포 가능
- 서비스별 기술 스택 선택 자유
- 특정 서비스 장애 시 전체 시스템 영향 최소화

**단점:**
- 높은 개발 복잡도 (API Gateway, Service Discovery 등)
- 분산 트랜잭션 처리 복잡 (온도 계산, 평가 등)
- 초기 인프라 비용 증가
- MVP 단계에서 과도한 복잡도

**선택하지 않은 이유**: MVP 단계에서 불필요한 복잡도. 향후 필요 시 모듈 분리로 전환 가능.

### 대안 2: Next.js SSR

**설명**: React 대신 Next.js를 사용하여 서버사이드 렌더링

**장점:**
- SEO 최적화
- 초기 로딩 속도 향상
- API Routes로 간단한 백엔드 로직 처리 가능

**단점:**
- 실시간 채팅, WebSocket 등 복잡한 백엔드 로직은 별도 서버 필요
- SSR이 필수적이지 않음 (로그인 후 사용하는 서비스)
- 배포 복잡도 증가

**선택하지 않은 이유**: 이 플랫폼은 SEO보다 실시간 기능이 중요하며, SPA가 적합.

### 대안 3: MongoDB (NoSQL)

**설명**: PostgreSQL 대신 MongoDB 사용

**장점:**
- 스키마 유연성
- 수평 확장 용이
- JSON 데이터 자연스럽게 저장

**단점:**
- 트랜잭션 처리 제한적 (평가-온도 업데이트 등에서 ACID 필요)
- 복잡한 조인 쿼리 어려움
- 일관성보다 가용성 우선 (AP)

**선택하지 않은 이유**: 평가 시스템, 온도 계산 등 정확성이 중요한 기능이 많아 RDBMS가 적합.

## 결과 (Consequences)

### 긍정적 영향

1. **빠른 MVP 출시**: 단순한 구조로 개발 속도 향상
2. **낮은 학습 곡선**: 팀이 익숙한 기술 스택 활용
3. **강력한 타입 안정성**: TypeScript + Java로 런타임 에러 감소
4. **테스트 용이성**: Spring Test, Vitest로 높은 커버리지 달성 가능
5. **명확한 책임 분리**: 백엔드(비즈니스 로직), 프론트엔드(UI)

### 부정적 영향

1. **초기 부하 시 백엔드 병목 가능성**: Redis 캐싱으로 완화
2. **프론트엔드 번들 크기**: Code splitting으로 완화
3. **향후 마이크로서비스 전환 시 리팩토링 비용**: 계층 구조 명확히 하여 최소화

### 중립적 영향

1. **Docker 필수**: 로컬 개발 환경 구축 시 Docker 필수
2. **CI/CD 파이프라인 필요**: GitHub Actions로 자동화
3. **문서화 부담**: ADR, API 문서, ERD 등 지속적 관리 필요

## 참고 자료 (References)

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [React Documentation](https://react.dev/)
- [PostgreSQL Documentation](https://www.postgresql.org/docs/)
- [Redis Documentation](https://redis.io/documentation)
- [Martin Fowler - Microservices](https://martinfowler.com/articles/microservices.html)

## 관련 ADR

- ADR-0002: Spring Boot 3 채택
- ADR-0003: JWT 인증 전략
- ADR-0004: 온도 시스템 설계
- ADR-0005: WebSocket을 통한 실시간 통신
