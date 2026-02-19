# Gembud Phase 12: 보안·정책 긴급 패치 의사결정 문서

**작성일**: 2026-02-19
**기반 자료**: 리서치 팀 Phase 11 심층 검증 보고서
**목표**: 운영 전 필수 보안 취약점 및 정책 개선

---

## Executive Summary

리서치 팀 보고서(1차, 2차, 3차)에서 지적한 **5가지 긴급 이슈 + 법적 컴플라이언스 + 운영 실무 보완**에 대한 의사결정을 완료하고, 구현 우선순위와 상세 스펙을 확정했습니다.

### 핵심 결정사항 요약

| 이슈 | 심각도 | 결정 | 구현 기간 |
|------|--------|------|-----------|
| 1. ADMIN 권한 분리 부재 | 🔴 최우선 | ENUM 역할 + @EnableMethodSecurity + JWT role | 2주 |
| 2. 자동 제재 오남용 | 🟠 높음 | CRITICAL 4명 / 일반 6명 + 7일 쿨다운 | 2주 (단순) → 6주 (점수형) |
| 3. 개인정보 노출 | 🟡 중간 | 타인 이메일 전면 제거 | 즉시 |
| 4. 동시성/멱등성 | 🟡 중간 | DB 유니크 제약 + 예외 핸들러 | 2주 |
| 5. OAuth 토큰 + PII URL 노출 | 🔴 최우선 | HTTP-only Cookie + URL 파라미터 전부 제거 | 2주 |
| 6. 연령 인증 (신규) | 🟡 중간 | 생년월일 입력 → PASS 인증 (데이터 최소화) | 6주 → 12주 |
| 7. 접근 로그 보관 (PIPA) | 🟢 낮음 | 법령 기준 정확화 (1년/2년), AOP 로깅 | 12주 |
| 8. Policy Engine | 🟡 중간 | JSON 스키마 검증 + 활성 정책 단일성 | 6주 |
| 9. 계정 삭제 (Google Play) | 🟡 중간 | 인앱 + 웹 경로, 데이터 보관 고지 | 12주 |

### 3차 검증 반영 사항 (운영 실무 보완) ✨

**긴급 보안 이슈**:
- 🔴 **OAuth URL에 토큰 + 이메일 + 닉네임 노출** → Cookie 전환 + URL 파라미터 전부 제거
- 🔴 `@EnableGlobalMethodSecurity` → `@EnableMethodSecurity` (Spring Boot 3 권장)
- 🔴 `DataIntegrityViolationException` 핸들러 누락 → 친화적 메시지 추가

**운영 정책 조정**:
- 🟠 자동 제재 6명 임계치가 초기 DAU에서 너무 보수적 → CRITICAL(FRAUD/HARASSMENT) 4명으로 완화
- 🟠 Policy Engine에 JSON 스키마 검증, 활성 정책 단일성, 변경 감사 로그 추가
- 🟠 PASS 연동 시 데이터 최소화 (CI만 저장, 성명/생년월일 불저장)

**법적 컴플라이언스**:
- 🟡 **Google Play 계정 삭제 요구사항** 추가 (인앱 `DELETE /api/users/me` + 웹 페이지)
- 🟡 접근 로그 법령 기준 정확화 (기본 1년, 조건부 2년 → 우리는 2년 채택)
- 🟢 Generated Column immutable 제약 명시

### 2차 검증 반영 사항

**기술적 보완** (2차 리서치 보고서):
- ✅ PostgreSQL UNIQUE 제약 한계 → Generated Column 방식 채택
- ✅ CSRF 재활성화 필수 (Cookie 인증 시)
- ✅ `effective_for_sanction` 플래그 도입 (저온도 신고자 필터링)

**법적 컴플라이언스**:
- ✅ PIPA 접근 로그 2년 보관
- ✅ Google Play 연령 인증 (만 13세 이상)
- ✅ PASS 본인인증 단계적 도입

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
- [ ] **`reports.effective_for_sanction` 플래그 추가** ✅
- [ ] V19 마이그레이션: `user_violations` 테이블 생성
- [ ] `reports` 테이블: `is_false_report`, `retaliation_flag` 컬럼 추가
- [ ] 테스트: 6명 미만 신고 시 자동 정지 안 됨
- [ ] 테스트: 동일 신고자 7일 쿨다운
- [ ] 테스트: 온도 < 25°C 신고자의 신고는 effective_for_sanction=false

**Phase 2 (6주, 데이터 기반)**:
- [ ] 점수형 자동 제재 알고리즘 구현
- [ ] 신고자 신뢰도 계산 로직
- [ ] 정책 엔진(Policy Engine) 도입

**⚠️ 운영 개선 사항 (2차 검증)**:
- **`effective_for_sanction` 플래그 도입 권장**:
  - 온도 < 25°C 신고자의 신고는 기록은 되지만 자동 제재 카운트에서 제외
  - 관리자가 수동 검토 시 참고 가능 (완전 삭제 X)
  - 향후 신고자 신뢰도 점수 계산 시 활용 가능
  - DB 컬럼: `effective_for_sanction BOOLEAN DEFAULT TRUE`

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
- [ ] **CSRF 재활성화** (Cookie 인증 필수)
- [ ] SecurityConfig: `.csrf(csrf -> csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse()))`
- [ ] SecurityConfig: SameSite 쿠키 설정
- [ ] 프론트 OAuth2CallbackPage: 토큰 추출 로직 제거
- [ ] 프론트 axios: `withCredentials: true` 설정
- [ ] 프론트 인터셉터: 401 자동 갱신
- [ ] 프론트: CSRF 토큰 헤더 자동 포함
- [ ] 테스트: 쿠키로 인증 동작
- [ ] 테스트: 401 시 자동 갱신 후 재시도
- [ ] 테스트: CSRF 토큰 누락 시 403

**⚠️ 보안 주의사항 (2차 검증)**:
- 현재 `SecurityConfig`에 `csrf().disable()` 설정되어 있음
- Cookie 기반 인증으로 전환 시 CSRF 공격에 취약해짐
- **즉시 CSRF 재활성화 필수** ✅
- `CookieCsrfTokenRepository` + `SameSite=Strict` 권장

---

## 전체 구현 로드맵

### 📅 Phase 12-A: 긴급 보안 패치 (2주)

**Week 1 (긴급)**:

**Day 1-2**:
- ✅ 1-1. ADMIN 권한 분리 (User entity, migration, JWT)
- 1-2. @PreAuthorize 적용
- 1-3. Admin 부트스트랩

**Day 3**:
- 3. 개인정보 노출 제거 (빠름, 법적 리스크)

**Day 4-5**:
- 5. OAuth Cookie 방식 (보안 이슈)
- **CSRF 재활성화** (Cookie 인증 필수)
- CORS 설정

**Week 2 (안정화)**:

**Day 6-7**:
- 4. DB 유니크 제약 (V18 migration, Generated Column)
- 예외 처리 (DataIntegrityViolationException)

**Day 8-10**:
- 2-1. 자동 제재 개선 (Phase 1 - 단순 차단)
- 유니크 6명, 7일 쿨다운, effective_for_sanction 플래그

**Day 11-12**:
- 테스트 작성 (단위, 통합)
- 문서화 업데이트

**Day 13-14**:
- 버그 수정
- QA
- 배포 준비

---

### 📅 Phase 12-B: 운영 안정화 (6주)

**Week 3-4: 점수형 자동 제재 + Policy Engine**
- [ ] 점수형 자동 제재 알고리즘 구현
- [ ] 신고자 신뢰도 계산 로직
- [ ] `policy_versions` 테이블 생성
- [ ] PolicyService 구현
- [ ] 자동 제재, 온도 계산에 Policy Engine 적용

**Week 5: 연령 인증 (MVP)**
- [ ] `users.birth_date`, `age_verified` 컬럼 추가
- [ ] 회원가입 시 생년월일 입력 필수
- [ ] 만 13세 미만 가입 차단
- [ ] 프론트엔드: 생년월일 입력 폼

**Week 6: 허위 신고 대응**
- [ ] 허위 신고 자동 탐지 로직
- [ ] 4단계 패널티 시스템 구현
- [ ] 관리자 허위 신고 확정 기능
- [ ] 신고 제한 기능 (30일, 영구)

---

### 📅 Phase 12-C: 법적 컴플라이언스 (12주)

**Week 7-9: PASS 본인인증 통합**
- [ ] NICE 평가정보 또는 KCB API 연동
- [ ] `users.ci`, `di` 컬럼 추가
- [ ] PASS 인증 OAuth 플로우 구현
- [ ] CI/DI 중복 가입 체크
- [ ] 외국인 예외 처리 (생년월일 입력 허용)
- [ ] 프론트엔드: PASS 인증 버튼

**Week 10-11: 접근 로그 보관 시스템 (PIPA)**
- [ ] `admin_action_logs` 테이블 생성
- [ ] Spring AOP: 자동 로깅 애스펙트
- [ ] 관리자 액션 로깅 (신고 처리, 제재)
- [ ] Spring Batch: 2년 지난 로그 자동 삭제
- [ ] 로그 조회 API (관리자 전용)

**Week 12: 최종 검증 & 배포**
- [ ] 전체 시스템 통합 테스트
- [ ] 보안 감사 (OWASP 체크리스트)
- [ ] 법적 컴플라이언스 체크
- [ ] Google Play 정책 준수 확인
- [ ] 프로덕션 배포 준비

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

-- 광고 하루 1회 (PostgreSQL 제약: DATE() 표현식 직접 사용 불가)
-- Option A: Generated Column 방식 (권장) ✅
ALTER TABLE ad_views
ADD COLUMN view_date DATE GENERATED ALWAYS AS (DATE(viewed_at)) STORED;

ALTER TABLE ad_views
ADD CONSTRAINT uk_ad_view_daily
UNIQUE (user_id, ad_id, view_date);

CREATE INDEX idx_ad_views_date ON ad_views(view_date);

-- Option B (대안): Expression-based Unique Index
-- CREATE UNIQUE INDEX uk_ad_view_daily
-- ON ad_views (user_id, ad_id, DATE(viewed_at));
```

**⚠️ 기술적 보완 사항 (2차 검증)**:
- PostgreSQL은 UNIQUE 제약에 함수 표현식 직접 사용 불가
- Generated Column 방식 채택 이유:
  - 쿼리 성능 향상 (인덱스 활용)
  - 명시적 컬럼으로 디버깅 용이
  - 향후 날짜 기반 쿼리 최적화

### V19: 허위 신고 & 자동 제재
```sql
-- reports 테이블 확장
ALTER TABLE reports
ADD COLUMN is_false_report BOOLEAN DEFAULT FALSE,
ADD COLUMN retaliation_flag BOOLEAN DEFAULT FALSE,
ADD COLUMN effective_for_sanction BOOLEAN DEFAULT TRUE;  -- ✨ 2차 검증 반영

-- effective_for_sanction 컬럼 설명
COMMENT ON COLUMN reports.effective_for_sanction IS
  '자동 제재 카운트 포함 여부 (온도 < 25°C 신고자는 false)';

CREATE INDEX idx_reports_effective ON reports(effective_for_sanction)
  WHERE effective_for_sanction = TRUE;

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

## 6. 연령 인증 & 법적 컴플라이언스 (신규 요구사항)

### 배경

**Google Play Families 정책** 및 **개인정보보호법(PIPA)** 준수를 위해 추가 고려 필요:

1. **연령 제한 (Age Gating)**:
   - Google Play: 채팅 기능이 있는 앱은 만 13세 이상 연령 인증 필요
   - 한국: 온라인 게임은 만 18세 미만 심야 접속 제한 (셧다운제 폐지, 자율 규제)

2. **접근 로그 보관**:
   - PIPA 제75조: 개인정보 처리 시스템 접근 기록 1년 이상 보관
   - 실무 권장: 2년 보관 (소송 시효 대비)

3. **정책 유연성**:
   - 자동 제재 점수, 온도 계산 로직 등 하드코딩 시 변경 어려움
   - Policy Engine 도입으로 운영 유연성 확보

### 의사결정

#### Q1. 연령 인증 방식

**검토 옵션**:

**Option A - PASS 인증 (본인인증) 🇰🇷**:
```
장점:
- 법적 공신력 (실명, 생년월일 확인)
- 구글 플레이 정책 완벽 준수
- 중복 가입 방지 (CI/DI 활용)
- 미성년자 보호 강력

단점:
- 비용 발생 (건당 300-500원)
- 외국인 사용 불가
- PASS 앱 설치 필요
- 통신사 연동 필요

구현:
- NICE 평가정보, KCB 등 본인인증 API
- OAuth2 처럼 리다이렉트 플로우
- CI/DI 저장 (중복 가입 체크)
```

**Option B - 생년월일 입력 + 이메일 인증**:
```
장점:
- 무료
- 간단한 구현
- 외국인 가능
- MVP에 충분

단점:
- 법적 강제력 없음
- 거짓 입력 가능
- 구글 플레이 거부 가능성
```

**Option C - 하이브리드 (단계적 도입)**:
```
Phase 1 (MVP): 생년월일 입력
Phase 2 (스토어 론칭): PASS 인증 추가
```

**결정**: **Option C - 하이브리드 전략 ✅**

**근거**:
- MVP 단계에서는 생년월일 입력으로 빠른 테스트
- Google Play 제출 전 PASS 인증 통합
- 외국인 사용자는 생년월일 입력 허용 (선택적 PASS)

**구현 일정**:
- **6주 차**: 생년월일 입력 + 만 13세 미만 차단
- **12주 차 (스토어 론칭 전)**: PASS 인증 옵션 추가

#### Q2. 접근 로그 보관

**결정**: **12주 차에 구현 (PIPA 준수)**

**요구사항**:
```yaml
access_log_retention:
  scope:
    - 관리자 신고 처리 액션
    - 사용자 정지/해제 이력
    - 개인정보 조회 (이메일, 프로필)

  retention_period: 730 days  # 2년

  fields:
    - timestamp
    - user_id (행위자)
    - action_type (예: 'REPORT_REVIEWED', 'USER_SUSPENDED')
    - target_user_id
    - ip_address
    - request_path
```

**구현 방식**:
- `admin_action_logs` 테이블 생성
- Spring AOP로 자동 로깅
- 2년 후 자동 삭제 (Spring Batch)

#### Q3. Policy Engine 도입

**결정**: **6주 차에 MVP 버전 구현**

**배경**:
- 자동 제재 점수, 온도 계산 로직이 코드에 하드코딩됨
- 정책 변경 시 재배포 필요 → 운영 부담
- A/B 테스트 불가능

**Policy Engine 아키텍처**:
```sql
-- 정책 버전 관리
CREATE TABLE policy_versions (
  id BIGSERIAL PRIMARY KEY,
  policy_type VARCHAR(50),  -- 'AUTO_SANCTION', 'TEMPERATURE', 'MATCHING'
  version INT,
  config JSONB,
  effective_from TIMESTAMP,
  created_by BIGINT,
  is_active BOOLEAN DEFAULT TRUE
);

-- 예시: 자동 제재 정책
INSERT INTO policy_versions (policy_type, version, config, effective_from)
VALUES ('AUTO_SANCTION', 2, '{
  "unique_reporters": 6,
  "within_days": 7,
  "exclude_low_temp": 25,
  "category_points": {
    "FRAUD": 100,
    "HARASSMENT": 100,
    "VERBAL_ABUSE": 35
  }
}', NOW());
```

**장점**:
- 재배포 없이 정책 변경
- 정책 변경 이력 추적
- A/B 테스트 가능 (세그먼트별 다른 정책)
- 감사 로그 자동 생성

**구현 범위 (6주 차)**:
- `policy_versions` 테이블
- PolicyService (DB에서 정책 로드)
- 자동 제재, 온도 계산에 적용

### 구현 체크리스트

**6주 차**:
- [ ] `users` 테이블: `birth_date DATE`, `age_verified BOOLEAN` 추가
- [ ] 회원가입 API: 생년월일 입력 필수
- [ ] 만 13세 미만 가입 차단
- [ ] Policy Engine 테이블 생성
- [ ] PolicyService 구현
- [ ] 자동 제재, 온도 계산에 Policy Engine 적용

**12주 차**:
- [ ] PASS 인증 API 연동 (NICE 또는 KCB)
- [ ] `users` 테이블: `ci VARCHAR(88)`, `di VARCHAR(64)` 추가
- [ ] PASS 인증 후 CI/DI 중복 가입 체크
- [ ] `admin_action_logs` 테이블 생성
- [ ] AOP 기반 접근 로그 자동 수집
- [ ] Spring Batch: 2년 지난 로그 삭제

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

## 부록 A: 3차 운영 실무 검증 및 대응 계획

**검증일**: 2026-02-19
**검증 범위**: Phase 12 v2.0 문서 + 현재 코드베이스 (커밋 9ee8b4a)
**검증 초점**: 운영 시나리오, 공격 벡터, 법적 컴플라이언스 정확성

### 검증 결과 요약

문서 방향성은 **매우 우수**하나, 다음 항목들이 "운영 가능한 수준"으로 정밀화 필요:

| 분류 | 이슈 | 긴급도 | 대응 일정 |
|------|------|--------|-----------|
| 🔴 즉시 수정 | OAuth URL에 토큰 + PII 노출 | 최우선 | 2주 (Day 4-5) |
| 🔴 즉시 수정 | @EnableGlobalMethodSecurity → @EnableMethodSecurity | 최우선 | 2주 (Day 1-2) |
| 🔴 즉시 수정 | DataIntegrityViolationException 핸들러 누락 | 최우선 | 2주 (Day 6-7) |
| 🟠 운영 조정 | 자동 제재 6명 임계치 초기 DAU 현실화 | 높음 | 6주 |
| 🟠 운영 조정 | Policy Engine 운영 안전장치 (스키마 검증) | 높음 | 6주 |
| 🟡 컴플라이언스 | Google Play 계정 삭제 요구사항 | 중간 | 12주 |
| 🟡 컴플라이언스 | 접근 로그 법령 기준 정확성 (1년/2년) | 중간 | 12주 |
| 🟢 개선 | Generated Column immutable 제약 명시 | 낮음 | 2주 (문서화) |

---

### A.1 긴급 보완 사항 (2주 Sprint에 즉시 반영)

#### A.1.1 OAuth URL에 토큰 + 개인정보 노출 (🔴 최우선)

**문제**:
현재 `OAuth2SuccessHandler`가 다음과 같이 동작:
```java
String redirectUrl = String.format(
    "%s/oauth/callback?accessToken=%s&refreshToken=%s&email=%s&nickname=%s",
    frontendUrl, accessToken, refreshToken, user.getEmail(), user.getNickname()
);
```

**영향**:
- **OWASP 가이드 정면 위반**: "민감 데이터를 URL에 두지 말 것"
- **보안 토큰 노출**: 브라우저 히스토리, 서버 로그, Referrer 헤더에 토큰 기록
- **PII 노출**: 이메일까지 URL에 포함 → 개인정보보호법 위반 가능성
- **세션 하이재킹 위험**: URL 복사/공유 시 계정 탈취 가능

**대응 방안**:
1. **Cookie 전환** (문서 5번 결정과 일치):
   ```java
   // OAuth2SuccessHandler.java 수정
   Cookie accessTokenCookie = new Cookie("accessToken", accessToken);
   accessTokenCookie.setHttpOnly(true);
   accessTokenCookie.setSecure(true);
   accessTokenCookie.setSameSite("Strict");
   accessTokenCookie.setPath("/");
   accessTokenCookie.setMaxAge(3600);
   response.addCookie(accessTokenCookie);

   // 토큰/PII 없이 리다이렉트
   response.sendRedirect(frontendUrl + "/oauth/callback?success=true");
   ```

2. **프론트엔드 수정**:
   ```typescript
   // OAuth2CallbackPage.tsx
   const params = new URLSearchParams(window.location.search);
   if (params.get('success') === 'true') {
       // 토큰은 이미 쿠키에 저장됨
       // /me API 호출로 프로필 로딩
       const profile = await api.get('/api/users/me');
       navigate('/');
   }
   ```

**구현 일정**: **Day 4-5 (Week 1)**

**체크리스트**:
- [ ] OAuth2SuccessHandler: URL query 파라미터 전부 제거
- [ ] Cookie 설정: HttpOnly, Secure, SameSite=Strict
- [ ] 프론트: URL에서 토큰 추출 로직 삭제
- [ ] 프론트: `/me` API로 프로필 로딩
- [ ] 테스트: 브라우저 히스토리에 토큰/이메일 없음 확인

---

#### A.1.2 Spring Security 6 호환성 (@EnableMethodSecurity) (🔴 최우선)

**문제**:
문서 체크리스트에 `@EnableGlobalMethodSecurity(prePostEnabled = true)`로 명시되어 있으나, Spring Boot 3 / Spring Security 6에서는 **deprecated**.

**영향**:
- 향후 Spring 버전 업그레이드 시 제거될 API
- 최신 보안 패치 미적용 가능성

**대응 방안**:
```java
// SecurityConfig.java
@Configuration
@EnableWebSecurity
@EnableMethodSecurity  // ← @EnableGlobalMethodSecurity 대신
public class SecurityConfig {
    // ...
}
```

**문서 수정**:
```markdown
- [ ] SecurityConfig: @EnableMethodSecurity 추가 (Spring Boot 3 권장)
```

**구현 일정**: **Day 1-2 (Week 1)** - ADMIN 권한 분리와 동시 적용

**체크리스트**:
- [ ] SecurityConfig: `@EnableMethodSecurity` 적용
- [ ] ReportController: `@PreAuthorize("hasRole('ADMIN')")` 테스트
- [ ] 일반 유저 403 응답 확인

---

#### A.1.3 DB 제약 위반 예외 처리 누락 (🔴 최우선)

**문제**:
V18 마이그레이션으로 `evaluations`, `ad_views`에 UNIQUE 제약을 추가하지만, 현재 `GlobalExceptionHandler`에 `DataIntegrityViolationException` 처리 로직 없음.

**영향**:
- 중복 평가/광고 시도 시 500 Internal Server Error 응답
- 사용자에게 "이미 처리됨" 안내 불가능
- 운영 로그에 불필요한 에러 스택 누적

**대응 방안**:
```java
// GlobalExceptionHandler.java
@ExceptionHandler(DataIntegrityViolationException.class)
public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
    DataIntegrityViolationException ex
) {
    String message = "요청을 처리할 수 없습니다.";

    // 제약 조건별 메시지 커스터마이징
    if (ex.getMessage().contains("uk_evaluation_per_room")) {
        message = "이미 이 방에서 해당 사용자를 평가하셨습니다.";
    } else if (ex.getMessage().contains("uk_ad_view_daily")) {
        message = "오늘 이 광고를 이미 보셨습니다.";
    }

    return ResponseEntity
        .status(HttpStatus.CONFLICT)
        .body(new ErrorResponse("DUPLICATE_ACTION", message));
}
```

**구현 일정**: **Day 6-7 (Week 2)** - V18 마이그레이션과 동시

**체크리스트**:
- [ ] GlobalExceptionHandler: DataIntegrityViolationException 핸들러 추가
- [ ] 평가 중복 시: "이미 평가하셨습니다" (409 Conflict)
- [ ] 광고 중복 시: "오늘 이 광고를 이미 보셨습니다" (409)
- [ ] 프론트: 409 응답에 대한 친화적 토스트 표시

---

### A.2 운영 정책 조정 (6주 내 반영)

#### A.2.1 자동 제재 임계치의 초기 DAU 현실화 (🟠 높음)

**문제**:
문서 결정: "유니크 신고자 6명"은 악용 방지에는 효과적이나, **초기 서비스 (유저 풀 작음)**에서는 다음 리스크 존재:

- 실제 악성 유저가 6명 신고에 도달하지 못해 오래 남음
- 피해자가 누적되어도 자동 제재 발동 안 됨
- 관리자 수동 개입 부담 증가

**영향**:
- 초기 커뮤니티 신뢰도 하락
- 악성 유저 유입으로 평판 시스템 왜곡

**대응 방안**:

**Option A - 카테고리별 차등 임계치** ✅ **권장**:
```yaml
auto_sanction_v1_refined:
  default:
    unique_reporters: 6
    within_days: 7

  critical_categories:  # FRAUD, HARASSMENT만
    unique_reporters: 4  # 낮춤
    within_days: 7
    action: auto_suspend_7d

  high_categories:  # VERBAL_ABUSE 등
    unique_reporters: 6
    action: admin_review_priority  # 자동 정지 대신 우선 검토
```

**Option B - 자동 검토 큐 우선순위**:
```yaml
auto_triage_system:
  condition: unique_reporters >= 3 AND category IN ['FRAUD', 'HARASSMENT']
  action: move_to_admin_queue_top  # 정지 대신 관리자 검토 상단 이동
  notification: admin_slack_alert
```

**권장 전략**: **Option A + B 혼합**
- CRITICAL(FRAUD, HARASSMENT): 4명 → 즉시 7일 정지
- 나머지: 6명 → 정지 OR 3명 → 관리자 우선 검토

**구현 일정**: **Week 3-4 (Phase 12-B)**

**체크리스트**:
- [ ] ReportCategory enum에 `severity` 필드 추가
- [ ] AutoSanctionPolicy: 카테고리별 임계치 설정
- [ ] 관리자 검토 큐 우선순위 정렬
- [ ] Slack/Discord 웹훅 알림 (optional)

---

#### A.2.2 Policy Engine 운영 안전장치 (🟠 높음)

**문제**:
문서의 Policy Engine은 "DB JSON으로 정책 관리"라는 강력한 아이디어이나, **운영 리스크**가 큼:

- 잘못된 JSON으로 서비스 장애 가능
- 정책 변경 승인 프로세스 없음
- 변경 이력 추적 불가능

**영향**:
- 관리자 실수로 자동 제재 오작동
- 감사 시 "누가 언제 왜 변경했는지" 증명 불가

**대응 방안**:

**1. JSON 스키마 검증**:
```java
@Service
public class PolicyService {

    private final ObjectMapper objectMapper;
    private final JsonSchema autoSanctionSchema;

    public void savePolicy(String policyType, String configJson) {
        // 1. 스키마 검증
        JsonNode config = objectMapper.readTree(configJson);
        Set<ValidationMessage> errors = getSchema(policyType).validate(config);

        if (!errors.isEmpty()) {
            throw new InvalidPolicyException(
                "정책 JSON이 유효하지 않습니다: " + errors
            );
        }

        // 2. 활성 정책 단일성 보장
        policyRepository.deactivateAllByType(policyType);

        // 3. 새 버전 저장
        PolicyVersion newVersion = PolicyVersion.builder()
            .policyType(policyType)
            .config(configJson)
            .createdBy(getCurrentAdminId())
            .isActive(true)
            .build();

        policyRepository.save(newVersion);

        // 4. 변경 로그
        auditLogger.log("POLICY_CHANGED", policyType, newVersion.getId());
    }
}
```

**2. 정책 변경 승인 워크플로우** (선택, 12주 이후):
```sql
CREATE TABLE policy_change_requests (
  id BIGSERIAL PRIMARY KEY,
  policy_type VARCHAR(50),
  new_config JSONB,
  requested_by BIGINT,
  approved_by BIGINT,
  status VARCHAR(20),  -- 'PENDING', 'APPROVED', 'REJECTED'
  created_at TIMESTAMP,
  approved_at TIMESTAMP
);
```

**구현 일정**: **Week 5 (Phase 12-B)**

**체크리스트**:
- [ ] JSON Schema 정의 (auto_sanction, temperature, matching)
- [ ] PolicyService: 스키마 검증 로직
- [ ] 활성 정책 단일성 보장 (트랜잭션)
- [ ] 정책 변경 감사 로그
- [ ] 관리자 API: 정책 조회/변경 (ADMIN only)

---

#### A.2.3 연령 인증 데이터 최소화 (🟠 높음)

**문제**:
PASS 본인인증 연동 시 **성명, 생년월일, CI, DI, 성별, 통신사** 등 다양한 정보를 제공받을 수 있으나, "받을 수 있다"와 "저장해야 한다"는 별개.

**영향**:
- 개인정보보호법: "목적에 필요한 최소한의 정보만 수집"
- 불필요한 정보 저장 시 법적 리스크 증가
- 개인정보 유출 시 피해 범위 확대

**대응 방안**:

**저장 최소화 전략**:
```yaml
pass_authentication_minimal:
  save_to_db:
    - ci: VARCHAR(88)      # 중복 가입 방지용 (필수)
    - age_verified: BOOLEAN  # 만 13세 이상 확인 (필수)
    - birth_year: INT        # 연령대 분석용 (선택)

  do_not_save:
    - 성명 (실명)             # 서비스에 불필요
    - 생년월일 전체           # birth_year만 저장
    - 성별                   # 서비스에 불필요
    - 통신사                 # 서비스에 불필요
    - DI                     # CI만으로 충분
```

**접근 통제**:
```java
// User.java
@Column(name = "ci", length = 88)
@JsonIgnore  // API 응답에 절대 노출 금지
private String ci;

// UserRepository.java
@Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END " +
       "FROM User u WHERE u.ci = :ci")
boolean existsByCi(@Param("ci") String ci);
// → CI는 중복 체크에만 사용, 조회 금지
```

**구현 일정**: **Week 7-9 (Phase 12-C)**

**체크리스트**:
- [ ] PASS 인증 후 필요한 최소 정보만 추출
- [ ] CI 컬럼: @JsonIgnore, 접근 제어
- [ ] 성명/생년월일/성별 DB 저장 안 함
- [ ] 개인정보 처리방침 업데이트

---

### A.3 법적 컴플라이언스 정밀화 (12주 내 반영)

#### A.3.1 Google Play 계정 삭제 요구사항 (🟡 중간)

**문제**:
현재 문서에 **계정 삭제 기능이 누락**되어 있으나, Google Play User Data 정책은 다음을 명시:

> "앱에서 계정을 만들 수 있으면, **앱 안과 앱 밖(웹)에서 계정 삭제 요청 경로**를 제공해야 함"

**영향**:
- Google Play 심사 거부 가능성
- 개인정보보호법: 이용자의 개인정보 삭제 요구권 보장

**대응 방안**:

**1. 인앱 계정 삭제**:
```java
// UserController.java
@DeleteMapping("/api/users/me")
@PreAuthorize("isAuthenticated()")
public ResponseEntity<Void> deleteAccount(
    @AuthenticationPrincipal CustomUserDetails userDetails
) {
    userService.deleteAccount(userDetails.getUserId());
    return ResponseEntity.noContent().build();
}

// UserService.java
@Transactional
public void deleteAccount(Long userId) {
    User user = userRepository.findById(userId)
        .orElseThrow(() -> new NotFoundException("사용자를 찾을 수 없습니다"));

    // 1. 연관 데이터 처리
    chatMessageRepository.deleteByUserId(userId);
    evaluationRepository.deleteByUserId(userId);

    // 2. 보안/사기 방지용 데이터는 익명화 후 보관
    reportRepository.anonymizeReporterOrReported(userId);

    // 3. 계정 삭제 (soft delete)
    user.markAsDeleted();
    userRepository.save(user);

    // 4. 삭제 로그
    auditLogger.log("ACCOUNT_DELETED", userId);
}
```

**2. 웹 계정 삭제 페이지** (인앱 없이도 접근 가능):
```
https://gembud.com/account-deletion
- 로그인 필요
- 삭제 확인 절차 (비밀번호 재입력)
- 삭제 후 복구 불가 고지
```

**3. 데이터 보관 고지**:
```
"계정 삭제 후에도 다음 정보는 보안/법적 사유로 익명화하여 보관됩니다:
- 신고 이력 (익명 처리)
- 제재 이력 (익명 처리)
- 접근 로그 (2년 보관 후 자동 삭제)

이 정보는 사기/어뷰징 방지를 위해 필요하며,
개인식별 정보는 모두 제거됩니다."
```

**구현 일정**: **Week 10-11 (Phase 12-C)**

**체크리스트**:
- [ ] `DELETE /api/users/me` API 구현
- [ ] 연관 데이터 삭제/익명화 로직
- [ ] Soft delete (deleted_at 컬럼)
- [ ] 웹 계정 삭제 페이지 (Next.js 정적 페이지)
- [ ] 개인정보 처리방침: 삭제 절차 및 보관 정책 명시
- [ ] Google Play Console: Data Safety → Account deletion 경로 등록

---

#### A.3.2 접근 로그 법령 기준 정확성 (🟡 중간)

**문제**:
문서에 "PIPA 접근 로그 2년 보관"으로 명시되어 있으나, **법령 문구는 다름**:

- **기본**: 개인정보 처리 시스템 접근 기록 **1년 이상** 보관
- **2년**: 5만명 이상 처리, 민감정보/고유식별 처리, 기간통신사업자 등 **조건부**

**영향**:
- 법령 근거 오해 가능성
- 불필요하게 긴 보관 기간 설정 시 저장 비용 증가

**대응 방안**:

**1. 법령 기준 정확화**:
```markdown
### 접근 로그 보관 기간 결정

**개인정보보호법 시행령 제30조 (접근기록의 보관 및 점검)**:
- 기본: 개인정보취급자의 접근 기록을 **1년 이상** 보관·관리
- 2년 조건:
  - 개인정보 5만명 이상 처리
  - 민감정보 또는 고유식별정보 처리
  - 정보통신서비스 제공자 등

**우리 서비스 결정**: **2년 보관**
**근거**:
- 향후 5만명 이상 예상 (MVP 단계에서는 1년도 OK)
- 소송 시효 대비 (민사 3년, 형사 5~7년)
- 운영 안전 마진 확보

**주의**: 초기 1년으로 시작 후, DAU 5만 도달 시 2년으로 전환 가능
```

**2. 보관 기간 자동 관리**:
```java
// AdminActionLogCleanupJob.java
@Scheduled(cron = "0 0 3 * * *")  // 매일 새벽 3시
public void deleteOldLogs() {
    LocalDateTime cutoff = LocalDateTime.now()
        .minusDays(policyService.getLogRetentionDays());  // 365 or 730

    int deleted = adminActionLogRepository.deleteOlderThan(cutoff);
    log.info("접근 로그 {} 건 삭제 (기준일: {})", deleted, cutoff);
}
```

**구현 일정**: **Week 10-11 (Phase 12-C)**

**체크리스트**:
- [ ] 문서: 접근 로그 법령 근거 정확화
- [ ] PolicyService: log_retention_days 설정 (기본 365)
- [ ] Spring Batch: 보관 기간 지난 로그 자동 삭제
- [ ] 관리자 대시보드: 로그 보관 정책 표시

---

#### A.3.3 Generated Column 제약 조건 명시 (🟢 낮음)

**문제**:
문서에 "Generated Column 방식 채택"이라고 명시되어 있으나, **PostgreSQL의 제약 사항**이 누락:

- Generated Column은 **immutable 함수**만 사용 가능
- `DATE(viewed_at)`는 immutable이므로 OK
- 하지만 `CURRENT_TIMESTAMP`, `random()` 등은 불가

**대응 방안**:

**문서 V18 마이그레이션 섹션에 주석 추가**:
```sql
-- V18: 동시성 제약
-- ⚠️ Generated Column 제약: immutable 함수만 사용 가능
--    DATE()는 immutable이므로 OK
--    CURRENT_TIMESTAMP, random() 등은 불가

ALTER TABLE ad_views
ADD COLUMN view_date DATE GENERATED ALWAYS AS (DATE(viewed_at)) STORED;
```

**구현 일정**: **즉시 (문서화만)**

---

### A.4 체크리스트 업데이트 (문서 반영)

위 보완사항을 반영하여 기존 체크리스트를 다음과 같이 업데이트:

#### 2주 Sprint (긴급)

**Day 1-2**:
- [x] User 엔티티 role 필드 추가
- [ ] SecurityConfig: `@EnableMethodSecurity` 적용 ✅ **업데이트**
- [ ] ReportController: `@PreAuthorize("hasRole('ADMIN')")` 적용
- [ ] AdminInitializer: 환경변수 기반 admin 생성

**Day 3**:
- [ ] DTO email 제거 (ReportResponse, 기타 타인 DTO)

**Day 4-5**:
- [ ] **OAuth2SuccessHandler: URL query 파라미터 전부 제거** ✅ **신규**
- [ ] OAuth Cookie 전환 (HttpOnly, Secure, SameSite)
- [ ] CSRF 재활성화 (CookieCsrfTokenRepository)
- [ ] 프론트: URL 토큰 추출 로직 삭제, `/me` API로 전환

**Day 6-7**:
- [ ] V18 마이그레이션 (evaluations, ad_views UNIQUE)
- [ ] **GlobalExceptionHandler: DataIntegrityViolationException 핸들러** ✅ **신규**

**Day 8-10**:
- [ ] 자동 제재 v1 (6명, 7일 쿨다운, effective_for_sanction)

#### 6주 차 (운영 안정화)

- [ ] **자동 제재 임계치 조정 (CRITICAL 4명)** ✅ **신규**
- [ ] Policy Engine: JSON 스키마 검증 ✅ **신규**
- [ ] Policy Engine: 활성 정책 단일성 보장 ✅ **신규**
- [ ] Policy Engine: 변경 감사 로그 ✅ **신규**

#### 12주 차 (컴플라이언스)

- [ ] **계정 삭제 API (`DELETE /api/users/me`)** ✅ **신규**
- [ ] **웹 계정 삭제 페이지** ✅ **신규**
- [ ] **개인정보 처리방침: 계정 삭제 절차** ✅ **신규**
- [ ] PASS 인증: 데이터 최소화 (CI만 저장) ✅ **신규**
- [ ] 접근 로그: 법령 기준 정확화 (1년/2년) ✅ **신규**

---

## 추가 검토 사항

### Steam 게임 카탈로그 통합 (Phase 13 이후)

**요구사항**:
- Steam Web API `IStoreService/GetAppList`를 통한 게임 목록 동기화
- 주기적 업데이트 (Spring @Scheduled, 매일 1회)
- 인기도 기반 정렬 (플레이어 수, 리뷰 점수)
- 게임 검색 기능 (PostgreSQL Full-Text Search 또는 Elasticsearch)

**구현 계획**:
```java
@Scheduled(cron = "0 0 2 * * *")  // 매일 새벽 2시
public void syncSteamGames() {
    // 1. Steam API 호출
    // 2. 신규 게임 DB 저장
    // 3. 인기도 메타데이터 업데이트
}
```

**우선순위**: Phase 13 (보안 패치 완료 후)

---

## 버전 히스토리

### v3.0 (2026-02-19) ✨ **운영 실무 검증 완료**
**3차 리서치 보고서 반영 - 운영 시나리오 & 공격 벡터 정밀화**:

**긴급 보완 사항 (2주 내)**:
- ✅ **OAuth URL PII 노출 심각성** 명시 (토큰 + 이메일 + 닉네임 query 제거)
- ✅ `@EnableGlobalMethodSecurity` → `@EnableMethodSecurity` (Spring Boot 3)
- ✅ `DataIntegrityViolationException` 핸들러 추가 (DB 제약 위반 시 친화적 메시지)

**운영 정책 조정 (6주 내)**:
- ✅ 자동 제재 임계치 초기 DAU 현실화 (CRITICAL 카테고리 4명으로 완화)
- ✅ Policy Engine 운영 안전장치 (JSON 스키마 검증, 활성 정책 단일성, 변경 감사 로그)
- ✅ 연령 인증 데이터 최소화 (PASS 연동 시 CI만 저장, 성명/생년월일 불저장)

**법적 컴플라이언스 정밀화 (12주 내)**:
- ✅ **Google Play 계정 삭제 요구사항** 추가 (인앱 + 웹 경로, 데이터 보관 고지)
- ✅ 접근 로그 법령 기준 정확화 (기본 1년, 조건부 2년 → 우리는 2년 채택)
- ✅ Generated Column immutable 제약 명시

**체크리스트 업데이트**:
- 2주 Sprint: OAuth URL 제거, DataIntegrity 핸들러 추가
- 6주: 자동 제재 임계치 조정, Policy Engine 검증 로직
- 12주: 계정 삭제 API/웹 페이지, PASS 최소 데이터

### v2.0 (2026-02-19)
**2차 리서치 보고서 반영**:
- PostgreSQL UNIQUE 제약 한계 → Generated Column 방식
- CSRF 재활성화 필수 추가
- `effective_for_sanction` 플래그 도입
- PASS 본인인증 및 연령 인증 계획 추가
- PIPA 접근 로그 보관 요구사항 추가
- Policy Engine 아키텍처 상세화
- 12주 로드맵 확정

### v1.0 (2026-02-19)
**초기 의사결정 문서**:
- 5가지 긴급 보안 이슈 결정
- ADMIN 권한 분리, 자동 제재, 개인정보 보호
- 동시성 해결, OAuth Cookie 전환
- 2주 Sprint 계획

---

**문서 버전**: 3.0
**최종 수정**: 2026-02-19
**작성자**: Gembud 개발팀 + Claude Code
**리뷰**: 리서치 팀 1차, 2차, 3차 보고서 반영 완료 ✅
