# Gembud API Error Codes

Complete catalog of all error codes used in the Gembud backend API.

## Error Response Format

All error responses follow this standard format:

```json
{
  "timestamp": "2026-02-25T10:30:00",
  "status": 404,
  "error": "Not Found",
  "code": "USER001",
  "message": "User not found",
  "path": "/api/users/123"
}
```

---

## Authentication & Authorization (AUTH)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `AUTH001` | 401 | Invalid email or password | 잘못된 이메일 또는 비밀번호 |
| `AUTH002` | 401 | Authentication required | 인증 필요 |
| `AUTH003` | 403 | Access denied | 접근 거부 |
| `AUTH004` | 401 | Token has expired | 토큰 만료 |
| `AUTH005` | 401 | Invalid token | 유효하지 않은 토큰 |
| `AUTH006` | 409 | Email already exists | 이메일 중복 |
| `AUTH007` | 401 | Invalid refresh token | 유효하지 않은 리프레시 토큰 |

### Usage Examples

```java
// Signup with duplicate email
throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);

// Invalid login credentials
throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);

// Expired JWT token
throw new BusinessException(ErrorCode.TOKEN_EXPIRED);
```

---

## User (USER)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `USER001` | 404 | User not found | 사용자를 찾을 수 없음 |
| `USER002` | 403 | Temperature too low to create room | 온도가 낮아 방 생성 불가 |
| `USER003` | 409 | Nickname already exists | 닉네임 중복 |

### Usage Examples

```java
// User not found by ID
User user = userRepository.findById(userId)
    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

// Temperature check before room creation
if (user.getTemperature() < 36.5) {
    throw new BusinessException(ErrorCode.LOW_TEMPERATURE);
}
```

---

## Game (GAME)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `GAME001` | 404 | Game not found | 게임을 찾을 수 없음 |

---

## Advertisement (AD)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `AD001` | 404 | Advertisement not found | 광고를 찾을 수 없음 |
| `AD002` | 400 | Advertisement is not active or expired | 광고가 비활성화되었거나 만료됨 |
| `AD003` | 429 | Daily ad view limit exceeded | 일일 광고 시청 제한 초과 |

### Usage Examples

```java
// Check ad validity before viewing
if (!ad.isValid()) {
    throw new BusinessException(ErrorCode.AD_NOT_ACTIVE);
}

// Daily view limit check
if (viewCount >= MAX_ADS_PER_DAY) {
    throw new BusinessException(ErrorCode.AD_VIEW_LIMIT_EXCEEDED);
}
```

---

## Room (ROOM)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `ROOM001` | 404 | Room not found | 방을 찾을 수 없음 |
| `ROOM002` | 409 | Room is full | 방이 가득 참 |
| `ROOM003` | 409 | Room is closed | 방이 종료됨 |
| `ROOM004` | 409 | Already in this room | 이미 이 방에 참여 중 |
| `ROOM005` | 400 | Not in this room | 이 방에 참여하지 않음 |
| `ROOM006` | 401 | Invalid room password | 잘못된 방 비밀번호 |
| `ROOM007` | 403 | Only host can perform this action | 방장만 이 작업 수행 가능 |

### Usage Examples

```java
// Room capacity check
if (room.getCurrentParticipants() >= room.getMaxParticipants()) {
    throw new BusinessException(ErrorCode.ROOM_FULL);
}

// Host authorization check
if (!room.getHostId().equals(userId)) {
    throw new BusinessException(ErrorCode.NOT_HOST);
}
```

---

## Evaluation (EVAL)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `EVAL001` | 400 | Can only evaluate after room is closed | 방 종료 후에만 평가 가능 |
| `EVAL002` | 400 | Evaluator was not in this room | 평가자가 이 방에 참여하지 않았음 |
| `EVAL003` | 400 | Evaluated user was not in this room | 평가 대상자가 이 방에 참여하지 않았음 |
| `EVAL004` | 409 | Already evaluated this user for this room | 이미 이 사용자를 평가함 |
| `EVAL005` | 400 | Cannot evaluate yourself | 자기 자신을 평가할 수 없음 |
| `EVAL006` | 400 | Evaluated user does not match request | 평가 대상자가 요청과 일치하지 않음 |

### Usage Examples

```java
// Room status check before evaluation
if (room.getStatus() != RoomStatus.CLOSED) {
    throw new BusinessException(ErrorCode.ROOM_NOT_CLOSED_FOR_EVALUATION);
}

// Self-evaluation prevention
if (evaluatorId.equals(evaluatedId)) {
    throw new BusinessException(ErrorCode.CANNOT_EVALUATE_SELF);
}
```

---

## Friend (FRIEND)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `FRIEND001` | 404 | Friend request not found | 친구 요청을 찾을 수 없음 |
| `FRIEND002` | 409 | Friend request already exists | 친구 요청이 이미 존재함 |
| `FRIEND003` | 400 | Cannot send friend request to yourself | 자기 자신에게 친구 요청 불가 |
| `FRIEND004` | 403 | Only the receiver can perform this action | 수신자만 이 작업 수행 가능 |
| `FRIEND005` | 409 | Friend request already accepted | 친구 요청이 이미 수락됨 |
| `FRIEND006` | 400 | Not friends with this user | 이 사용자와 친구가 아님 |

### Usage Examples

```java
// Self-friend prevention
if (userId.equals(friendId)) {
    throw new BusinessException(ErrorCode.CANNOT_ADD_SELF_AS_FRIEND);
}

// Duplicate request check
if (friendRepository.requestExists(userId, friendId)) {
    throw new BusinessException(ErrorCode.FRIEND_REQUEST_ALREADY_EXISTS);
}
```

---

## Notification (NOTIF)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `NOTIF001` | 404 | Notification not found | 알림을 찾을 수 없음 |
| `NOTIF002` | 403 | Notification does not belong to user | 알림이 사용자 소유가 아님 |

---

## Report (REPORT)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `REPORT001` | 404 | Report not found | 신고를 찾을 수 없음 |
| `REPORT002` | 400 | Cannot report yourself | 자기 자신을 신고할 수 없음 |
| `REPORT003` | 409 | Already reported this user in this room | 이 방에서 이미 이 사용자를 신고함 |
| `REPORT004` | 400 | Report is not in PENDING status | 신고가 PENDING 상태가 아님 |
| `REPORT005` | 409 | Report is already resolved | 신고가 이미 처리됨 |

### Usage Examples

```java
// Self-report prevention
if (reporterId.equals(reportedId)) {
    throw new BusinessException(ErrorCode.CANNOT_REPORT_SELF);
}

// Duplicate report check
if (reportRepository.existsByReporterAndReportedAndRoom(reporterId, reportedId, roomId)) {
    throw new BusinessException(ErrorCode.DUPLICATE_REPORT);
}
```

---

## Chat (CHAT)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `CHAT001` | 404 | Chat room not found | 채팅방을 찾을 수 없음 |
| `CHAT002` | 403 | Not a member of this chat room | 이 채팅방의 멤버가 아님 |
| `CHAT003` | 400 | Message cannot be empty | 메시지는 비어있을 수 없음 |
| `CHAT004` | 500 | Unknown chat room type | 알 수 없는 채팅방 타입 |

---

## Validation (VAL)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `VAL001` | 400 | Invalid input | 잘못된 입력 |
| `VAL002` | 400 | Missing required field | 필수 필드 누락 |

### Usage Examples

```java
// MethodArgumentNotValidException 처리 시 자동으로 VAL001 반환
@PostMapping("/rooms")
public ResponseEntity<RoomResponse> createRoom(@Valid @RequestBody CreateRoomRequest request) {
    // Validation 실패 시 GlobalExceptionHandler가 VAL001 에러 반환
}
```

---

## Server (SRV)

| Code | HTTP Status | Message | Description |
|------|------------|---------|-------------|
| `SRV001` | 500 | Internal server error | 내부 서버 오류 |
| `SRV002` | 503 | Service temporarily unavailable | 서비스 일시적으로 사용 불가 |

---

## Best Practices

### 1. Always Use BusinessException with ErrorCode

**✅ Good:**
```java
if (user == null) {
    throw new BusinessException(ErrorCode.USER_NOT_FOUND);
}
```

**❌ Bad:**
```java
if (user == null) {
    throw new IllegalArgumentException("User not found");  // Don't use generic exceptions
}
```

### 2. Let GlobalExceptionHandler Handle Errors

All `BusinessException` exceptions are automatically caught and formatted by `GlobalExceptionHandler`:

```java
@ExceptionHandler(BusinessException.class)
public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, WebRequest request) {
    // Automatically formats error response with ErrorCode
}
```

### 3. Use Appropriate HTTP Status Codes

Each `ErrorCode` has a predefined HTTP status:

- **400 (Bad Request)**: Invalid input, validation errors
- **401 (Unauthorized)**: Authentication failures
- **403 (Forbidden)**: Authorization failures
- **404 (Not Found)**: Resource not found
- **409 (Conflict)**: State conflicts (already exists, already processed)
- **429 (Too Many Requests)**: Rate limiting
- **500 (Internal Server Error)**: Unexpected errors

### 4. Monitoring and Logging

All errors are logged with their error codes:

```
WARN: Business exception: USER001 - User not found
WARN: Business exception: ROOM002 - Room is full
ERROR: Unexpected exception (SRV001)
```

Use error codes to:
- Track error frequency in monitoring dashboards
- Set up alerts for critical errors (AUTH*, SRV*)
- Analyze user experience issues (ROOM*, EVAL*)

---

## Total Error Codes: 48

- Authentication & Authorization: 7
- User: 3
- Game: 1
- Advertisement: 3
- Room: 7
- Evaluation: 6
- Friend: 6
- Notification: 2
- Report: 5
- Chat: 4
- Validation: 2
- Server: 2

---

## Version History

- **2026-02-25 (Phase 3)**: Complete error code unification
  - Added 24 new error codes (AD, FRIEND, NOTIF, REPORT, etc.)
  - Replaced all `IllegalArgumentException` → `BusinessException`
  - Replaced all `IllegalStateException` → `BusinessException`
  - Removed hardcoded error codes from `GlobalExceptionHandler`

- **2026-02-17**: Initial error code system with 24 codes
