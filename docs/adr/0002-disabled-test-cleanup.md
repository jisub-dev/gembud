# ADR-0002: @Disabled 테스트 정리 (PR 1.5)

## 상태
Accepted

## 날짜
2026-04-25

## 컨텍스트 (Context)

백엔드 테스트 스위트에는 `@Disabled` 표시가 붙은 테스트가 다섯 개 남아 있었고, 어떤 것은 비활성화 사유가 더 이상 정확하지 않았으며 어떤 것은 검증 대상이 사라진 상태였다. PR 1.5의 목표는 각 테스트를 분류해 (a) 다시 켜거나, (b) 작은 보강을 거쳐 켜거나, (c) Testcontainers 패턴으로 전환하거나, (d) 사유와 함께 삭제하는 것이다.

대상 파일:

- `backend/src/test/java/com/gembud/ReleaseGateTest.java`
- `backend/src/test/java/com/gembud/controller/RoomControllerTest.java`

## 결정 (Decision)

| # | 테스트 | 결정 | 사유 |
|---|--------|------|------|
| 1 | `ReleaseGateTest#test03_CSRF토큰_없으면_403` | 재활성화 (사유 없음으로 통과) | Phase 2 보안 강화로 `CookieCsrfTokenRepository` 가 다시 활성화되었으므로 비활성화 사유("CSRF is disabled ...")는 stale. `@WithMockUser` 만으로 충분하다. |
| 2 | `ReleaseGateTest#test03_2_CSRF토큰_있으면_성공` | 수정 후 재활성화 | `@WithMockUser` 의 기본 principal 은 `CustomUserDetails` 가 아니어서 컨트롤러의 `@AuthenticationPrincipal CustomUserDetails` 가 null 이 되어 실패한다. 영속 사용자에 맞춘 `CustomUserDetails` 를 `.with(authentication(...))` 로 주입해 해결. |
| 3 | `ReleaseGateTest#test05_신고쿨다운_7일` | 수정 후 재활성화 | 위와 동일한 principal 문제. 추가로 기대값을 `400 + "최근"` 메시지에서 현행 구현(`ErrorCode.DUPLICATE_REPORT` → HTTP 409 / `REPORT003`)에 맞게 갱신. |
| 4 | `ReleaseGateTest#test06_타인이메일_노출없음` | 삭제 | 검증 대상인 `GET /reports/{id}` 엔드포인트가 구현된 적이 없고 의도적으로 범위 외로 남겨졌다. 이메일 미노출은 `ReportResponse` DTO 에 email 필드가 없는 것으로 이미 보장되며 별도 단위 테스트가 존재한다. 동일 이름의 빈 placeholder 를 유지하면 잘못된 안전감을 준다. |
| 5 | `RoomControllerTest#createRoom_Unauthorized` | 삭제 | `@WebMvcTest(RoomController.class)` + `@AutoConfigureMockMvc(addFilters = false)` 슬라이스에서 보안 필터가 비활성이므로 미인증 401 시나리오를 재현할 수 없다. 해당 행위는 `JwtAuthenticationFilter` 단위 테스트와 `CustomAuthenticationEntryPoint` 통합 테스트로 이미 커버된다. 슬라이스 한 개를 위해 `@SpringBootTest` 로 다시 짜는 것은 중복 비용이 크다. |

전환(`(C) Testcontainers`) 결정은 없음 — 위 다섯 건 중 H2/Flyway 비호환이 원인인 항목은 없었다.

## 근거 (Rationale)

- 비활성화된 테스트는 "지나가면 다시 보겠다" 는 메모로 누적되기 쉬워 회귀 신호가 점차 사라진다. 각 항목을 분류해 사유를 남기면 미래의 작업자가 같은 분석을 반복하지 않아도 된다.
- 삭제는 두 건 모두 (a) 검증 대상이 부재하거나 (b) 슬라이스 구조상 의미 있는 어서션이 불가능한 경우로, 보존 가치보다 잡음 비용이 더 크다.

## 결과 (Consequences)

### 긍정적 영향
- @Disabled 카운트 0 (PR 1.5 완료 기준).
- CSRF / principal 주입 패턴 (`.with(authentication(authFor(user)))`) 이 통합 테스트 예시로 남는다.

### 부정적 영향
- 추후 `GET /reports/{id}` 가 구현될 때 `test06` 의 이메일 미노출 시나리오는 새로 작성해야 한다. (PR 3.2 의 커버리지 추가 범위에서 다루는 것이 자연스럽다.)

## 관련 ADR
- ADR-0001: 프로젝트 아키텍처 개요
