# Architecture Decision Records (ADR)

이 디렉토리는 Gembud 프로젝트의 주요 아키텍처 결정 사항을 문서화합니다.

## ADR이란?

Architecture Decision Record (ADR)는 소프트웨어 아키텍처에서 중요한 결정을 문서화하는 방법입니다.

## 목적

- 아키텍처 결정의 배경과 근거를 명확히 기록
- 팀 내 지식 공유 및 온보딩 지원
- 미래의 개발자들이 왜 특정 결정을 내렸는지 이해할 수 있도록 지원

## ADR 목록

| 번호 | 제목 | 상태 | 날짜 |
|------|------|------|------|
| [ADR-0001](0001-project-architecture-overview.md) | 프로젝트 아키텍처 개요 | Accepted | 2026-02-16 |
| [ADR-0002](0002-disabled-test-cleanup.md) | @Disabled 테스트 정리 (PR 1.5) | Accepted | 2026-04-25 |

## ADR 작성 가이드

1. 새로운 ADR 파일 생성: `XXXX-title-in-kebab-case.md`
2. 템플릿 사용: `template.md`를 참고
3. 상태: Proposed → Accepted/Rejected/Deprecated

## 참고 자료

- [ADR GitHub](https://adr.github.io/)
- [Documenting Architecture Decisions](https://cognitect.com/blog/2011/11/15/documenting-architecture-decisions)
