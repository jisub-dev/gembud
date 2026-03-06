# ADMIN 구현 실행 명세서 (No-Ambiguity) - 2026-03-06

## 0. 적용 범위
- 대상: `backend` 관리자 API/서비스/리포지토리/스케줄러, 관련 테스트
- 제외: 관리자 웹 UI(별도), 영구 제재(ban), 임시 정지 정책 고도화

## 1. 절대 규칙 (변경 금지)
1. 세션 만료 응답은 `401 + AUTH004`만 사용
2. 잠금 해제 권한은 `ADMIN`만 허용
3. 기존 경로 `DELETE /auth/admin/users/{userId}/login-lock`는 즉시 제거
4. 공식 설정 키는 `app.admin.email`만 사용
5. 신고 관리자 추가 액션은 `warn`만 구현
6. 보안 이벤트 조회는 `size` 기본 20, 최대 100
7. Slack 재시도는 앱 내부 스케줄러 방식(1m/5m/15m)
8. 보안 이벤트는 90일 보존, 매일 03:00 하드삭제

## 2. API 최종 스펙

## 2-1. Admin User API
1. 로그인 잠금 해제
- Method/Path: `DELETE /admin/users/{userId}/login-lock`
- Auth: `ROLE_ADMIN`
- Success: `204 No Content`
- Error:
  - `401 AUTH002` 미인증
  - `403 AUTH003` 권한 없음
  - `404 USER001` 유저 없음

2. 유저 보안 상태 조회
- Method/Path: `GET /admin/users/{userId}/security-status`
- Auth: `ROLE_ADMIN`
- Query: 없음
- Success 200 body:
```json
{
  "status": 200,
  "message": "Success",
  "data": {
    "userId": 1,
    "email": "u@example.com",
    "isLoginLocked": false,
    "loginLockedUntil": null,
    "failedLoginCountInWindow": 0,
    "windowMinutes": 10
  }
}
```

## 2-2. Admin Security Event API
1. 이벤트 목록
- Method/Path: `GET /admin/security-events`
- Auth: `ROLE_ADMIN`
- Query:
  - `eventType` optional (enum)
  - `riskScore` optional (`LOW|MEDIUM|HIGH|CRITICAL`)
  - `from` optional (ISO-8601 datetime)
  - `to` optional (ISO-8601 datetime)
  - `page` default `0`, min `0`
  - `size` default `20`, max `100`
- Validation:
  - `from > to`면 `400 VAL001`

2. 이벤트 요약
- Method/Path: `GET /admin/security-events/summary`
- Auth: `ROLE_ADMIN`
- Query:
  - `windowMinutes` default `60`, min `1`, max `1440`
- Success:
  - `loginFailCount`, `loginLockedCount`, `refreshReuseCount`, `rateLimitHitCount`

## 2-3. Admin Report API
1. 경고(warn) 발송
- Method/Path: `POST /admin/reports/{reportId}/warn`
- Auth: `ROLE_ADMIN`
- Request JSON:
```json
{
  "warningMessage": "욕설 신고가 접수되어 경고합니다. 재발 시 제재됩니다."
}
```
- Validation:
  - `warningMessage`: `@NotBlank`, `@Size(max=500)`
- Success 200:
  - `ReportResponse` + `warningIssued=true` 확장 필드
- Error:
  - `404 REPORT001`
  - `409 REPORT005`(이미 RESOLVED이고 재경고 금지 정책일 때)

2. 기존 review/resolve/delete는 유지
- 기존 엔드포인트 유지, 권한은 ADMIN 고정

## 3. Report 상태 전이 규칙
1. `warn` 호출 시
- `PENDING` -> `REVIEWED`로 변경
- `reviewedAt` 설정
- `adminComment`에 warningMessage 저장
- 경고 이력 테이블에 1건 생성

2. `resolve` 호출 시
- `PENDING|REVIEWED` -> `RESOLVED`
- `resolvedAt` 설정

3. `RESOLVED`에서 `warn` 재호출
- 금지, `409 REPORT005`

## 4. 데이터 모델 변경

## 4-1. 신규 테이블: `user_warnings`
- 마이그레이션 파일: `V30__add_user_warnings.sql`
- DDL:
```sql
CREATE TABLE user_warnings (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL REFERENCES users(id),
  report_id BIGINT NOT NULL REFERENCES reports(id),
  admin_user_id BIGINT NOT NULL REFERENCES users(id),
  message VARCHAR(500) NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX ux_user_warnings_report_id ON user_warnings(report_id);
CREATE INDEX idx_user_warnings_user_id_created_at ON user_warnings(user_id, created_at DESC);
```

규칙:
1. report 당 warning 1회만 허용 (`ux_user_warnings_report_id`)
2. 경고 내역은 삭제하지 않음

## 4-2. 기존 테이블 유지
- `security_events` purge 정책 유지 (03:00, 90일)

## 5. Redis 키/TTL 명세
1. 로그인 카운트
- `ratelimit:login:account:{emailNormalized}` TTL `10m`
- `ratelimit:login:ip:{ip}` TTL `60s`

2. BURST 카운트
- `security:burst:login_fail:{scope}:{5mBucket}` TTL `10m`
- HIGH 기준 `>=10`, CRITICAL 기준 `>=30`

3. Slack dedupe
- `ratelimit:slack:{eventType}:{scope}:{5mBucket}` TTL `10m`

4. Slack retry 상태(앱 메모리)
- 큐는 메모리 우선순위 큐 사용
- 앱 재시작 시 유실 허용 (운영 규모상 허용)

## 6. 서비스/클래스 구현 명세

## 6-1. 컨트롤러
1. 신규 파일
- `backend/src/main/java/com/gembud/controller/AdminUserController.java`
- `backend/src/main/java/com/gembud/controller/AdminSecurityController.java`

2. 기존 파일 수정
- `AuthController`:
  - `@DeleteMapping("/admin/users/{userId}/login-lock")` 메서드 삭제
- `ReportController`:
  - `POST /reports/{reportId}/warn` 추가 또는 `/admin/reports/{reportId}/warn`로 분리

## 6-2. 서비스
1. `AdminUserService` 신규
- `unlockLoginLock(Long userId)`
- `getSecurityStatus(Long userId)`

2. `ReportService` 확장
- `warnReport(Long reportId, Long adminUserId, String warningMessage)`

3. `SecurityEventService` 확장
- `search(...)`, `summary(windowMinutes)` 추가

4. `SlackAlertService` 확장
- `sendAlertWithRetry(...)`
- 재시도 정책: `1m -> 5m -> 15m`, 총 3회

## 6-3. 리포지토리
1. 신규
- `UserWarningRepository`

2. 확장
- `SecurityEventRepository`
  - 검색용 pageable query 추가
  - 기간/위험도/이벤트타입 필터

## 7. SecurityConfig 명세
1. `authorizeHttpRequests`에 명시 추가
- `/admin/**` -> `hasRole("ADMIN")`

2. 메서드 보안 유지
- 각 admin 메서드에 `@PreAuthorize("hasRole('ADMIN')")`

## 8. 에러 코드/응답 정책
1. 그대로 사용
- `AUTH002`, `AUTH003`, `AUTH004`, `USER001`, `REPORT001`, `REPORT005`, `VAL001`

2. 신규 코드 추가 금지 (V1)
- 기존 코드만 재사용해서 프론트 파싱 복잡도 증가 방지

## 9. 구현 순서 (정확한 작업 순서)
1. `AuthController` 기존 잠금해제 경로 삭제
2. `AdminUserController` + `AdminUserService` 추가
3. `SecurityConfig` `/admin/**` 규칙 추가
4. `V30__add_user_warnings.sql` 추가
5. `UserWarning` 엔티티/리포지토리 추가
6. `ReportService.warnReport` + 컨트롤러 endpoint 추가
7. `AdminSecurityController` + `SecurityEventRepository` 검색/요약 구현
8. `SlackAlertService` 재시도 스케줄러 반영
9. 테스트 추가/수정
10. 문서/런북 업데이트

## 10. 테스트 케이스 (필수)

## 10-1. Controller Test
1. `AdminUserControllerTest`
- ADMIN unlock 204
- USER unlock 403
- unknown user 404

2. `AdminSecurityControllerTest`
- default page/size 동작 확인 (`size=20`)
- `size=101` 요청시 400

3. `ReportControllerTest` (warn)
- warn 성공
- resolved report warn 시 409

## 10-2. Service Test
1. `ReportServiceTest`
- warn 시 상태/시각/코멘트/경고이력 검증

2. `SlackAlertServiceTest`
- 재시도 스케줄 1m/5m/15m 검증
- dedupe 키 충돌시 중복 전송 방지

## 10-3. Integration
1. 로그인 10회 실패 -> 잠금
2. 관리자 unlock -> 즉시 로그인 가능
3. HIGH/CRITICAL 이벤트 발생 시 Slack 전송 시도

## 11. 배포/롤백
1. 배포 전
- `./gradlew test`
- `./gradlew compileJava`

2. 롤백 기준
- `/admin/**` 권한 우회 발생
- warn API가 status 전이를 잘못 유발
- Slack 실패가 요청 지연/실패를 유발

3. 롤백 방법
- 마지막 정상 배포 태그로 즉시 롤백
- DB `V30`은 forward-only 유지 (코드 롤백 시 read-only 영향 없음)

## 12. 완료 정의 (DoD)
1. 기존 잠금해제 경로 완전 제거
2. 신규 관리자 경로 정상 동작
3. warn 액션 동작 + 이력 저장
4. 이벤트 조회 pagination/필터 정상
5. Slack 재시도 3회 정책 적용
6. 테스트/빌드 통과
