# Premium Feature Toggle Runbook (2026-03-05)

## 목적
- 프리미엄 기능을 코드 수정 없이 환경변수만으로 ON/OFF 전환한다.
- 기본 상태는 OFF(비노출/비활성)로 운영한다.

## 현재 구현 상태
- Frontend: `VITE_FEATURE_PREMIUM_ENABLED` 플래그 적용 완료
- Backend: `FEATURE_PREMIUM_ENABLED` 플래그 적용 완료
- OFF 시:
  - 프리미엄 메뉴/배지/약관 조항 비노출
  - `/premium` 직접 접근 시 404 라우팅
  - `/subscriptions/**` API 미등록(404)

## 환경변수

### Frontend
- 키: `VITE_FEATURE_PREMIUM_ENABLED`
- 기본값: `false`

### Backend
- 키: `FEATURE_PREMIUM_ENABLED`
- 기본값: `false`
- 매핑: `app.feature.premium.enabled`

## ON/OFF 전환 절차

### OFF 유지(기본)
1. Frontend env: `VITE_FEATURE_PREMIUM_ENABLED=false`
2. Backend env: `FEATURE_PREMIUM_ENABLED=false`
3. 배포

### ON 개시
1. Frontend env: `VITE_FEATURE_PREMIUM_ENABLED=true`
2. Backend env: `FEATURE_PREMIUM_ENABLED=true`
3. 배포
4. 아래 검증 체크리스트 수행

### 롤백
1. 두 env를 모두 `false`로 변경
2. 재배포
3. `/premium`, `/subscriptions/**` 차단 재확인

## 배포 후 검증 체크리스트

### OFF 검증
- [ ] 로그인 후 헤더에 PRO 메뉴가 보이지 않는다.
- [ ] `/premium` 접근 시 404 페이지로 이동한다.
- [ ] 인증 사용자 기준 `/api/subscriptions/status` 호출 시 404다.
- [ ] 비인증 호출은 보안 설정에 따라 401이 나올 수 있다.
- [ ] 기존 기능(로그인/방/채팅/알림) 회귀가 없다.

### ON 검증
- [ ] 헤더 PRO 메뉴 노출
- [ ] `/premium` 페이지 진입 가능
- [ ] `/api/subscriptions/status` 정상 응답
- [ ] 구독 활성화/취소 API 정상 동작

## 관련 변경 파일
- `frontend/src/config/features.ts`
- `frontend/src/App.tsx`
- `frontend/src/components/layout/Header.tsx`
- `frontend/src/pages/TermsPage.tsx`
- `frontend/src/vite-env.d.ts`
- `frontend/.env.example`
- `backend/src/main/java/com/gembud/controller/SubscriptionController.java`
- `backend/src/main/resources/application.yml`
- `backend/.env.example`

## 로컬 검증 명령(참고)
```bash
# frontend
cd frontend
npm run build
npm test -- --run

# backend
cd ../backend
./gradlew compileJava
```
