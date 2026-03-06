# Premium Dark Launch Plan (2026-03-05)

## 1) 목표
- 프리미엄 기능은 코드/DB 기준으로 구현 상태 유지
- 오픈 전까지 사용자 노출 0 (UI/URL/API/문구 포함)
- 개시 시 코드 수정 없이 환경변수만 변경해 오픈

## 2) 운영 원칙
- 기본값은 `OFF`
- `OFF`일 때는 “숨김”이 아니라 “접근 차단”까지 포함
- `ON` 전환은 배포 단위에서 관리하고 즉시 롤백 가능해야 함

## 3) Feature Flag 정책

### Frontend
- 키: `VITE_FEATURE_PREMIUM_ENABLED`
- 기본값: `false`
- OFF 동작:
  - 헤더/메뉴의 프리미엄 진입점 비노출
  - `/premium` 직접 접근 시 `404` 라우팅
  - 프리미엄 관련 안내/배지 문구 비노출

### Backend
- 키: `FEATURE_PREMIUM_ENABLED`
- 매핑 예시: `app.feature.premium.enabled`
- 기본값: `false`
- OFF 동작:
  - `/subscriptions/**` 엔드포인트 비활성화(404)
  - Swagger/OpenAPI에서도 비노출
  - 우회 접근(직접 API 호출) 불가

## 4) 범위 체크리스트

### 프론트 노출 차단
- [ ] 헤더 `PRO 업그레이드` 메뉴 숨김
- [ ] 프리미엄 라우트 직접 진입 차단 (`/premium -> /error/404`)
- [ ] 약관/정책 페이지의 프리미엄 사전고지 문구 처리(비노출 또는 출시 예정 문구로 대체)
- [ ] 프리미엄 배지 표기 정책 확정(OFF일 때 미표시 권장)

### 백엔드 접근 차단
- [ ] 구독 조회/활성화/취소 API 전부 플래그로 비활성화
- [ ] API 문서 노출 제거 확인
- [ ] 컨트롤러 빈 로딩 조건 확인(OFF 시 라우트 미등록)

### 환경/설정
- [ ] `frontend/.env.example`에 프리미엄 플래그 추가
- [ ] `backend/.env.example`에 프리미엄 플래그 추가
- [ ] 운영 환경변수 기본 OFF 설정 확인

## 5) 테스트 시나리오 (출시 전 필수)

### OFF 검증
- [ ] 로그인 사용자 기준, 프리미엄 진입 버튼이 보이지 않음
- [ ] `/premium` URL 직접 접근 시 404 페이지
- [ ] `/subscriptions/status|activate|cancel` 호출 시 404
- [ ] 기존 핵심 기능(로그인/방목록/채팅/신고/알림) 회귀 없음

### ON 검증
- [ ] 플래그 ON 후 프리미엄 메뉴/페이지 즉시 노출
- [ ] 구독 API 정상 응답
- [ ] 기존 유저 데이터(`isPremium`, `premiumExpiresAt`)와 UI 동기화 정상
- [ ] 광고 노출/제거 로직 충돌 없음

## 6) 배포 절차
1. Staging 배포: 두 플래그 모두 `false`
2. OFF 상태 E2E 검증 완료
3. Production 배포: 기본 `false`로 먼저 반영
4. 개시 시점: 운영 환경변수만 `true` 전환 후 재배포(또는 설정 반영 배포)
5. 사후 모니터링: 30분 집중 관찰(5xx, 4xx 급증, 구독 API 지표)

## 7) 롤백 절차
1. `VITE_FEATURE_PREMIUM_ENABLED=false`
2. `FEATURE_PREMIUM_ENABLED=false`
3. 재배포
4. `/premium` 및 `/subscriptions/**` 차단 재검증

## 8) 운영 기준선(Go/No-Go)
- Go 조건:
  - OFF/ON 양쪽 시나리오 테스트 통과
  - 에러 라우팅(404/403/500) 정상
  - 프리미엄 경로 우회 접근 불가
- No-Go 조건:
  - OFF인데 프리미엄 진입점이 하나라도 노출
  - OFF인데 구독 API 호출 가능
  - ON 전환 시 주요 기능 회귀 발생

## 9) 개시 후 단기 액션
- D+1: 전환율/오류율/문의량 확인
- D+3: 약관/환불/고객안내문 최종 문구 확정
- D+7: 프리미엄 지표 리뷰 후 정책 튜닝 항목 도출
