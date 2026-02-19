# Gembud Phase 12: 보안·정책 긴급 패치 의사결정 문서

**작성일**: 2026-02-19
**기반 자료**: 리서치 팀 Phase 11 심층 검증 보고서
**목표**: 운영 전 필수 보안 취약점 및 정책 개선

---

## Executive Summary

리서치 팀 보고서에서 지적한 5가지 긴급 이슈에 대한 의사결정을 완료하고, 구현 우선순위와 상세 스펙을 확정했습니다.

### 핵심 결정사항 요약

| 이슈 | 심각도 | 결정 | 구현 기간 |
|------|--------|------|-----------|
| 1. ADMIN 권한 분리 부재 | 🔴 최우선 | ENUM 역할 + @PreAuthorize + JWT role | 2주 |
| 2. 자동 제재 오남용 | 🟠 높음 | 유니크 6명 + 7일 쿨다운 + 4단계 패널티 | 2주 (단순) → 6주 (점수형) |
| 3. 개인정보 노출 | 🟡 중간 | 타인 이메일 전면 제거 | 즉시 |
| 4. 동시성/멱등성 | 🟡 중간 | DB 유니크 제약 중심 | 2주 |
| 5. OAuth 토큰 URL 노출 | 🟠 높음 | HTTP-only Cookie + CORS | 2주 |

---

## 1. ADMIN 권한 분리 부재 (최우선)

### 문제점
- `SecurityConfig`: 모든 API가 `authenticated()`만 요구
- "admin only" 주석이 붙은 신고 처리 API가 모든 로그인 유저에게 열려있음
- OWASP BFLA (Broken Function Level Authorization) 취약점

### 의사결정

#### Q1. User 역할 구조
**결정**: **Option A - 단순 ENUM (MVP 추천안 채택)**

```java
// User.java
public enum UserRole {
    USER,   // 일반 사용자
    ADMIN   // 관리자
}

@Enumerated(EnumType.STRING)
@Column(nullable = false, length = 20)
private UserRole role = UserRole.USER;
```

**선택 이유**:
- 단순하고 빠른 구현
- MVP에 충분한 기능
- 나중에 다중 역할(MODERATOR 등)로 확장 가능

#### Q2. 관리자 계정 생성 방법
**결정**: **Option B - 환경변수 기반 부트스트랩**

```java
@Component
public class AdminInitializer {
    @PostConstruct
    public void initAdmin() {
        String adminEmail = env.getProperty("ADMIN_INIT_EMAIL");
        if (adminEmail != null && !userRepository.existsByEmail(adminEmail)) {
            // 최초 실행 시에만 ADMIN 생성
        }
    }
}
```

**환경변수**:
```bash
ADMIN_INIT_EMAIL=admin@gembud.com
ADMIN_INIT_PASSWORD=<secure-password>
ADMIN_INIT_NICKNAME=Admin
```

**선택 이유**:
- 환경별로 다른 관리자 설정 가능
- Git에 비밀번호 노출 방지
- 유연한 운영 가능

#### Q3. API 보호 방법
**결정**: **Option B - @PreAuthorize (메서드 레벨)**

```java
@PutMapping("/{reportId}/review")
@PreAuthorize("hasRole('ADMIN')")
public ResponseEntity<ReportResponse> markAsReviewed(@PathVariable Long reportId) {
    // ...
}
```

**선택 이유**:
- 메서드와 권한이 함께 위치 (명확성)
- URL 변경 시 권한도 함께 관리됨
- 코드 가독성 우수

**적용 대상 API**:
- `PUT /api/reports/{id}/review` (신고 검토)
- `PUT /api/reports/{id}/resolve` (신고 해결)
- `DELETE /api/reports/{id}` (신고 삭제)
- `GET /api/reports/status/{status}` (상태별 조회)
- `GET /api/reports/user/{userId}` (유저별 신고 조회)

#### Q4. JWT에 role 포함 여부
**결정**: **포함 O**

```java
// JwtTokenProvider.java
public String generateAccessToken(String email, String role) {
    return Jwts.builder()
        .subject(email)
        .claim("role", role)  // ← role 포함
        .issuedAt(now)
        .expiration(expiration)
        .signWith(getSigningKey())
        .compact();
}
```

**선택 이유**:
- 토큰만으로 권한 체크 가능 (성능 향상)
- 매 요청마다 DB 조회 불필요
- 역할 변경은 드물어 즉시 반영 불필요

### 구현 체크리스트

- [x] User 엔티티에 `role` 필드 추가
- [x] V17 마이그레이션: `users.role` 컬럼 추가
- [x] CustomUserDetails에 `role` 필드 추가
- [x] CustomUserDetailsService에서 `role` 전달
- [x] JwtTokenProvider: `generateAccessToken(email, role)` 시그니처 변경
- [ ] AuthService: JWT 생성 시 `user.getRole()` 전달
- [ ] OAuth2SuccessHandler: role 전달
- [ ] ReportController: `@PreAuthorize` 적용
- [ ] AdminInitializer 구현 (환경변수 기반)
- [ ] SecurityConfig: `@EnableGlobalMethodSecurity(prePostEnabled = true)` 추가
- [ ] 테스트: 일반 유저가 admin API 호출 시 403 응답

---

## 2. 자동 제재 오남용 리스크

### 문제점
- `roomId=null` 신고는 중복 체크 없음 → 1명이 3번 신고로 타인 정지 가능
- 신고자 신뢰도 미고려 → 악의적 유저 악용 가능
- 단순 count 기반 → 친구끼리 담합 가능

### 의사결정

#### Q1. 단계적 적용 전략
**결정**: **Option A - 2단계 전략**

**Phase 1 (2주, 즉시 적용)**:
- 유니크 신고자 **6명** 조건
- 7일 쿨다운 (동일 대상 7일에 1회만 신고 가능)
- 신고자 온도 25°C 미만 무시

**Phase 2 (6주, 데이터 기반 전환)**:
- 카테고리별 점수 (FRAUD 100, HARASSMENT 100, VERBAL_ABUSE 35...)
- 신고자 신뢰도 가중치 (온도, 계정 연령, 허위신고율)
- 시간 감쇠 (최근 신고에 더 높은 가중치)

**선택 이유**:
- 일단 구멍 막고 운영 시작
- 실제 데이터로 점수 튜닝 가능
- 복잡도 점진적 증가

#### Q2. Phase 1 정책 (단순 차단)
**결정**: **아래 스펙 확정**

```yaml
auto_sanction_v1:
  conditions:
    unique_reporters: 6        # ✨ 5인 파티 담합 방지
    within_days: 7
    exclude_low_temp: 25       # 온도 25°C 미만 신고자 무시

  duplicate_prevention:
    same_target_cooldown_days: 7
    applies_to_null_room: true  # roomId=null도 쿨다운 적용

  action:
    suspend_days: 7
```

**6명 선택 이유**:
- 5인 파티 게임에서 전원이 담합해도 정지 안 됨
- 실제 문제 유저는 충분히 신고될 것
- 오판 최소화

#### Q3. roomId=null 신고 정책
**결정**: **Option A - 동일 대상 중복 방지만 추가**

```java
// 7일 내 A→B 신고가 이미 있으면 차단 (room 무관)
if (reportRepository.existsByReporterAndReportedWithinDays(
    reporterId, reportedId, 7)) {
    throw new IllegalStateException("동일 사용자를 최근에 이미 신고했습니다");
}
```

**선택 이유**:
- DM/프로필에서도 신고 필요 (사기, 성희롱)
- 7일 쿨다운으로 악용 방지

#### Q4. DB 구조 변경
**결정**: **Option A - 최소 변경 (YAGNI 원칙)**

- 현재 `reports` 테이블 유지
- Phase 2를 위한 사전 준비 불필요 (필요할 때 추가)

### 허위 신고 패널티 (추가 결정)

#### 허위 신고 판정
**결정**: **Option C - 혼합 (자동 탐지 + 수동 확정)**

**자동 탐지 조건**:
1. 신고가 "RESOLVED" 되었는데 피신고자에게 조치 없음
2. 동일 신고자가 7일 내 5건 이상 신고 (무분별)
3. 동일 타겟에게 반복 신고 (stalking)

→ 관리자가 최종 확정 후 패널티 부여

#### 허위 신고 패널티
**결정**: **Option E - 단계별 혼합**

```yaml
false_report_penalty:
  first_offense:
    temperature: -10.0
    warning: "허위 신고는 제재 대상입니다"

  second_offense:
    temperature: -15.0
    suspend_days: 3
    report_restriction_days: 30  # 30일간 신고 불가

  third_offense:
    temperature: -20.0
    suspend_days: 30
    report_permanently_disabled: true

  fourth_offense:
    permanent_ban: true
```

**선택 이유**:
- 1회: 실수 가능성 (경고)
- 2~3회: 의도적 → 강력 제재
- 4회: 악질 → 영구 제거

#### 보복 신고 대책
**결정**: **정보 차단 방식 (ADMIN 권한 분리로 해결)**

- 피신고자는 "내가 신고당했다"는 사실을 모름
- `GET /api/reports/user/{userId}`는 ADMIN 전용 → 일반 유저 접근 불가
- 보복할 방법 자체가 없음 ✅

### 구현 체크리스트

**Phase 1 (2주)**:
- [ ] ReportRepository: `existsByReporterAndReportedWithinDays()` 추가
- [ ] ReportRepository: `countUniqueReportersWithinDays()` 추가
- [ ] ReportService: 7일 쿨다운 체크 로직 추가
- [ ] ReportService: 유니크 신고자 6명 조건으로 변경
- [ ] ReportService: 온도 25°C 미만 신고자 필터링
- [ ] V19 마이그레이션: `user_violations` 테이블 생성
- [ ] `reports` 테이블: `is_false_report`, `retaliation_flag` 컬럼 추가
- [ ] 테스트: 6명 미만 신고 시 자동 정지 안 됨
- [ ] 테스트: 동일 신고자 7일 쿨다운

**Phase 2 (6주, 데이터 기반)**:
- [ ] 점수형 자동 제재 알고리즘 구현
- [ ] 신고자 신뢰도 계산 로직
- [ ] 정책 엔진(Policy Engine) 도입

---

## 3. 개인정보 노출 (Email)

### 문제점
- `ReportResponse.UserSummary`에 타인 이메일 포함
- 개인정보보호법 위반 가능성
- 스팸/피싱 악용 위험

### 의사결정

#### Q1. 이메일 노출 범위
**결정**: **Option A - 전면 제거 (타인 이메일 노출 금지)**

```java
// ReportResponse.java
public static class UserSummary {
    private Long id;
    private String nickname;
    // email 제거 ✂️
}
```

**선택 이유**:
- 게이밍 서비스에서 타인 이메일 볼 이유 없음
- 법적 리스크 최소화
- 프론트엔드 미구현 상태 → 지금 제거가 최적

#### Q2. 제거 범위
**결정**: **모든 "타인 정보" DTO에서 email 제거**

**제거 대상**:
- ✅ `ReportResponse.UserSummary.email`
- ✅ `RoomResponse.createdBy.email` (있다면)
- ✅ `FriendResponse.email` (있다면)
- ✅ 기타 모든 타인 정보 DTO

**유지 대상**:
- ✅ 본인 프로필: `GET /api/users/me`
- ✅ 관리자 전용 DTO (나중에 분리)

#### Q3. DB 변경 필요성
**결정**: **DB 변경 없음 (DTO만 수정)**

### 구현 체크리스트

- [ ] ReportResponse: email 제거
- [ ] 전체 DTO 검색: `email` 필드 찾기
- [ ] RoomResponse, FriendResponse 등 확인 후 제거
- [ ] 본인 프로필 API는 유지 확인
- [ ] 테스트: 타인 정보 조회 시 email=null

---

## 4. 동시성/멱등성 문제 (레이스 컨디션)

### 문제점
- 광고 노출: "COUNT 후 INSERT" → 동시 요청 시 3회 초과 가능
- 월 3회 평가: 2회 상태에서 2건 동시 요청 시 4회 가능
- 중복 신고: `existsBy...` 체크 후 INSERT → 동시 신고 취약

### 의사결정

#### Q1. 전반적 전략
**결정**: **Option A - DB 유니크 제약 중심**

**선택 이유**:
- Redis 없이도 동작 (인프라 단순)
- 데이터 무결성 보장
- 나중에 성능 문제 시 Redis 추가 가능

#### Q2. 월 3회 평가 제한 동시성
**결정**: **Option A - 무시 (실무적으로 문제 없음)**

**선택 이유**:
- 유저가 동시에 평가할 이유 없음 (실제론 거의 안 일어남)
- 설령 3~4회 되어도 큰 문제 아님
- 복잡도 대비 효과 낮음

#### Q3. 신고 중복 방지
**결정**: **Option B - 7일 쿨다운만 (영구 차단 X)**

```sql
-- roomId null 포함 처리
CREATE UNIQUE INDEX uk_report_unique
ON reports (reporter_id, reported_id, COALESCE(room_id, -1));
```

**하지만**: 7일 쿨다운 정책과 충돌하므로, **애플리케이션 레벨 체크**로 처리

**선택 이유**:
- 신고 사유가 시간에 따라 다를 수 있음 (1월 욕설, 2월 사기)
- 유니크 제약은 너무 강함

#### Q4. 광고 노출 제한
**결정**: **Option A - 하루 동일 광고 1회 (DB 유니크)**

```sql
ALTER TABLE ad_views
ADD CONSTRAINT uk_ad_view_daily
UNIQUE (user_id, ad_id, DATE(viewed_at));
```

**효과**:
- 같은 광고를 하루에 1번만
- 사용자는 최대 N개 광고 볼 수 있음 (광고 개수만큼)
- "3회 제한"보다 합리적 (같은 광고 반복 방지)

**선택 이유**:
- UX 개선 (광고 피로도 감소)
- Redis 불필요
- Google Play "disruptive ads" 정책 준수

### 구현 체크리스트

**V18 마이그레이션**:
- [ ] `evaluations`: UNIQUE (room_id, evaluator_id, evaluated_id)
- [ ] `ad_views`: UNIQUE (user_id, ad_id, DATE(viewed_at))

**예외 처리**:
- [ ] DataIntegrityViolationException → 사용자 친화적 메시지
- [ ] 평가: "이미 평가하셨습니다"
- [ ] 광고: "오늘 이 광고를 이미 보셨습니다"

**테스트**:
- [ ] 동시 평가 요청 → 1건만 성공
- [ ] 동일 광고 중복 노출 → 2번째 실패

---

## 5. OAuth 토큰 URL 전달 방식

### 문제점
- OAuth 성공 시 `access/refresh token`을 **URL query**로 리다이렉트
- 브라우저 히스토리, 서버 로그, Referrer 헤더에 토큰 노출
- OWASP 권고 위반, 세션 하이재킹 위험

### 의사결정

#### Q1. 토큰 전달 방식
**결정**: **Option A - HTTP-only Cookie**

```java
// OAuth2SuccessHandler.java
Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
accessTokenCookie.setHttpOnly(true);  // JS 접근 불가
accessTokenCookie.setSecure(true);    // HTTPS only
accessTokenCookie.setPath("/");
accessTokenCookie.setMaxAge(3600);    // 1시간
response.addCookie(accessTokenCookie);

Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
refreshTokenCookie.setHttpOnly(true);
refreshTokenCookie.setSecure(true);
refreshTokenCookie.setPath("/api/auth/refresh");
refreshTokenCookie.setMaxAge(604800);  // 7일
response.addCookie(refreshTokenCookie);

// 프론트로 리다이렉트 (토큰 없이)
response.sendRedirect(frontendUrl + "/oauth/callback?success=true");
```

**선택 이유**:
- XSS 공격 방지 (httpOnly)
- 로그/히스토리 안전
- 웹 표준 방식

#### Q2. 프론트엔드 수정
**결정**: **OK - 프론트 미구현 상태라 지금 적용 최적**

**변경 사항**:
```typescript
// OAuth2CallbackPage.tsx
const params = new URLSearchParams(window.location.search);
if (params.get('success') === 'true') {
    // 토큰은 이미 쿠키에 저장됨
    navigate('/');
}

// API 호출 시
axios.defaults.withCredentials = true;  // 쿠키 자동 포함
```

#### Q3. 토큰 갱신 플로우
**결정**: **OK - 자동 갱신 인터셉터**

```java
// AuthController.java
@PostMapping("/api/auth/refresh")
public ResponseEntity<Void> refresh(
    @CookieValue("refreshToken") String refreshToken,
    HttpServletResponse response
) {
    // 1. refresh 토큰 검증
    // 2. 새 access 토큰 발급
    // 3. 새 쿠키 설정
    Cookie newAccess = new Cookie("accessToken", newAccessToken);
    response.addCookie(newAccess);
    return ResponseEntity.ok().build();
}
```

**프론트 자동 갱신**:
```typescript
axios.interceptors.response.use(
    response => response,
    async error => {
        if (error.response?.status === 401) {
            await axios.post('/api/auth/refresh');
            return axios.request(error.config);  // 재시도
        }
    }
);
```

#### Q4. 로그아웃
**결정**: **OK - 쿠키 삭제**

```java
@PostMapping("/api/auth/logout")
public ResponseEntity<Void> logout(HttpServletResponse response) {
    Cookie access = new Cookie("accessToken", "");
    access.setMaxAge(0);
    response.addCookie(access);

    Cookie refresh = new Cookie("refreshToken", "");
    refresh.setMaxAge(0);
    response.addCookie(refresh);

    return ResponseEntity.ok().build();
}
```

### 구현 체크리스트

- [ ] OAuth2SuccessHandler: Cookie 방식 변경
- [ ] AuthController: `/api/auth/refresh` 구현
- [ ] AuthController: `/api/auth/logout` 구현
- [ ] CorsConfig: `allowCredentials(true)` 설정
- [ ] 프론트 OAuth2CallbackPage: 토큰 추출 로직 제거
- [ ] 프론트 axios: `withCredentials: true` 설정
- [ ] 프론트 인터셉터: 401 자동 갱신
- [ ] 테스트: 쿠키로 인증 동작
- [ ] 테스트: 401 시 자동 갱신 후 재시도

---

## 구현 우선순위 (2주 Sprint)

### Week 1 (긴급)

**Day 1-2**:
- ✅ 1-1. ADMIN 권한 분리 (User entity, migration, JWT)
- 1-2. @PreAuthorize 적용
- 1-3. Admin 부트스트랩

**Day 3**:
- 3. 개인정보 노출 제거 (빠름, 법적 리스크)

**Day 4-5**:
- 5. OAuth Cookie 방식 (보안 이슈)
- CORS 설정

### Week 2 (안정화)

**Day 6-7**:
- 4. DB 유니크 제약 (V18 migration)
- 예외 처리

**Day 8-10**:
- 2-1. 자동 제재 개선 (Phase 1 - 단순 차단)
- 유니크 6명, 7일 쿨다운

**Day 11-12**:
- 테스트 작성
- 통합 테스트
- 문서화

**Day 13-14**:
- 버그 수정
- QA
- 배포 준비

---

## DB 마이그레이션 계획

### V17: User Role
```sql
ALTER TABLE users ADD COLUMN role VARCHAR(20) NOT NULL DEFAULT 'USER';
CREATE INDEX idx_user_role ON users(role);
```

### V18: 동시성 제약
```sql
-- 평가 중복 방지
ALTER TABLE evaluations
ADD CONSTRAINT uk_evaluation_per_room
UNIQUE (room_id, evaluator_id, evaluated_id);

-- 광고 하루 1회
ALTER TABLE ad_views
ADD CONSTRAINT uk_ad_view_daily
UNIQUE (user_id, ad_id, DATE(viewed_at));
```

### V19: 허위 신고 & 자동 제재
```sql
-- reports 테이블 확장
ALTER TABLE reports
ADD COLUMN is_false_report BOOLEAN DEFAULT FALSE,
ADD COLUMN retaliation_flag BOOLEAN DEFAULT FALSE;

-- 사용자 위반 이력
CREATE TABLE user_violations (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT REFERENCES users(id),
  violation_type VARCHAR(50),  -- 'FALSE_REPORT', 'RETALIATION'
  report_id BIGINT REFERENCES reports(id),
  confirmed_at TIMESTAMP,
  confirmed_by BIGINT,  -- admin user_id
  penalty_applied VARCHAR(100)
);

-- 허위 신고 카운트 (캐시)
ALTER TABLE users ADD COLUMN false_report_count INT DEFAULT 0;
```

---

## 테스트 체크리스트

### 1. ADMIN 권한
- [ ] 일반 유저가 `PUT /api/reports/{id}/review` 호출 → 403
- [ ] ADMIN 유저가 신고 처리 → 200
- [ ] JWT에 role 포함 확인
- [ ] 환경변수로 ADMIN 생성 확인

### 2. 자동 제재
- [ ] 유니크 신고자 5명 → 정지 안 됨
- [ ] 유니크 신고자 6명 → 7일 정지
- [ ] 동일 신고자 7일 내 재신고 → 차단
- [ ] 온도 24°C 신고자 → 카운트 제외

### 3. 개인정보
- [ ] `GET /api/reports/my` → email null (타인 정보)
- [ ] `GET /api/users/me` → email 포함 (본인)

### 4. 동시성
- [ ] 동시 평가 → 1건만 성공
- [ ] 동일 광고 하루 2회 노출 → 2번째 실패

### 5. OAuth Cookie
- [ ] OAuth 성공 → 쿠키에 토큰 저장
- [ ] URL에 토큰 없음 확인
- [ ] API 호출 → 쿠키 자동 전송
- [ ] 401 → 자동 refresh → 재시도

---

## 다음 단계 (6주 후 - Phase 2)

### 자동 제재 점수형 전환
```yaml
category_points:
  FRAUD: 100
  HARASSMENT: 100
  VERBAL_ABUSE: 35
  GAME_DISRUPTION: 25
  FALSE_INFO: 10

reporter_weight:
  formula: |
    0.5
    + 0.5 * sigmoid((reporter_temp - 36.5)/5)
    + 0.3 * min(1, account_age_days/30)
    - 0.5 * false_report_rate_90d

auto_suspend:
  condition: score >= 120 AND unique_reporters >= 3
```

### 정책 엔진 (Policy Engine)
- DB 기반 정책 버전 관리
- 재배포 없이 규칙 변경
- 정책 의사결정 로그

### 어드민 대시보드
- 신고 큐 (우선순위별)
- 증거 뷰어 (채팅 스냅샷)
- 제재 조작 (수동 정지/해제)
- 감사 로그

---

## 참고 자료

- 리서치 팀 보고서: Phase 11 심층 검증 보고서 (2026-02-19)
- OWASP API Security Top 10 2023
- 개인정보보호법 (PIPA)
- Google Play 정책 (Families, Data Safety, Ads)

---

**문서 버전**: 1.0
**최종 수정**: 2026-02-19
**작성자**: Gembud 개발팀 + Claude Code
