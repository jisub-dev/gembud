# Gembud API Documentation

## Base URL
```
http://localhost:8080/api
```

## Authentication
Most endpoints require JWT authentication. Include the token in the `Authorization` header:
```
Authorization: Bearer <your_jwt_token>
```

---

## Authentication APIs

### 1. Sign Up
회원가입

**Endpoint:** `POST /auth/signup`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123",
  "nickname": "UserNickname",
  "ageRange": "20대"
}
```

**Response:** `201 Created`
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 2. Login
로그인

**Endpoint:** `POST /auth/login`

**Request Body:**
```json
{
  "email": "user@example.com",
  "password": "password123"
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

### 3. Refresh Token
토큰 갱신

**Endpoint:** `POST /auth/refresh`

**Request Body:**
```json
{
  "refreshToken": "eyJhbGc..."
}
```

**Response:** `200 OK`
```json
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "tokenType": "Bearer",
  "expiresIn": 3600
}
```

---

## Game APIs

### 1. Get All Games
모든 게임 목록 조회

**Endpoint:** `GET /games`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "name": "리그 오브 레전드",
    "imageUrl": "https://...",
    "genre": "MOBA",
    "description": "5v5 팀 전략 게임",
    "options": [
      {
        "id": 1,
        "optionKey": "position",
        "optionType": "SELECT",
        "optionValues": "[\"탑\",\"정글\",\"미드\",\"원딜\",\"서폿\"]",
        "isCommon": false
      }
    ]
  }
]
```

### 2. Get Game by ID
게임 상세 조회

**Endpoint:** `GET /games/{gameId}`

**Response:** `200 OK`
```json
{
  "id": 1,
  "name": "리그 오브 레전드",
  "imageUrl": "https://...",
  "genre": "MOBA",
  "description": "5v5 팀 전략 게임",
  "options": [...]
}
```

---

## Room APIs

### 1. Create Room
방 생성

**Endpoint:** `POST /rooms`

**Request Body:**
```json
{
  "gameId": 1,
  "title": "골드 이상 랭크 구합니다",
  "description": "편하게 즐겨요",
  "maxParticipants": 5,
  "isPrivate": false,
  "password": null,
  "filters": {
    "tier": "골드",
    "position": "미드",
    "ageRange": "20대",
    "micRequired": "true"
  }
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "gameId": 1,
  "title": "골드 이상 랭크 구합니다",
  "description": "편하게 즐겨요",
  "maxParticipants": 5,
  "currentParticipants": 1,
  "isPrivate": false,
  "status": "OPEN",
  "createdBy": {
    "id": 1,
    "nickname": "UserNickname"
  },
  "filters": {...},
  "createdAt": "2026-02-17T10:00:00"
}
```

### 2. Get Rooms by Game
게임별 방 목록 조회

**Endpoint:** `GET /rooms/game/{gameId}`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "gameId": 1,
    "title": "골드 이상 랭크 구합니다",
    "maxParticipants": 5,
    "currentParticipants": 3,
    "status": "OPEN",
    "createdAt": "2026-02-17T10:00:00"
  }
]
```

### 3. Join Room
방 참가

**Endpoint:** `POST /rooms/{roomId}/join`

**Request Body:**
```json
{
  "password": "optional_password_if_private"
}
```

**Response:** `200 OK`
```json
{
  "id": 1,
  "title": "골드 이상 랭크 구합니다",
  "currentParticipants": 4,
  "status": "OPEN"
}
```

### 4. Leave Room
방 나가기

**Endpoint:** `DELETE /rooms/{roomId}/leave`

**Response:** `204 No Content`

### 5. Close Room
방 종료 (방장만 가능)

**Endpoint:** `PUT /rooms/{roomId}/close`

**Response:** `200 OK`

---

## Evaluation APIs

### 1. Create Evaluation
평가 작성

**Endpoint:** `POST /rooms/{roomId}/evaluations`

**Request Body:**
```json
{
  "evaluatedId": 2,
  "mannerScore": 5,
  "skillScore": 4,
  "communicationScore": 5,
  "comment": "좋은 플레이어였습니다!"
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "roomId": 1,
  "evaluatorId": 1,
  "evaluatedId": 2,
  "mannerScore": 5,
  "skillScore": 4,
  "communicationScore": 5,
  "averageScore": 4.67,
  "comment": "좋은 플레이어였습니다!",
  "createdAt": "2026-02-17T12:00:00"
}
```

### 2. Get Evaluations by Room
방별 평가 조회

**Endpoint:** `GET /evaluations/room/{roomId}`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "evaluatorId": 1,
    "evaluatedId": 2,
    "mannerScore": 5,
    "averageScore": 4.67,
    "createdAt": "2026-02-17T12:00:00"
  }
]
```

### 3. Get My Received Evaluations
내가 받은 평가 조회

**Endpoint:** `GET /evaluations/received`

**Response:** `200 OK`

---

## Friend APIs

### 1. Send Friend Request
친구 요청 전송

**Endpoint:** `POST /friends/requests`

**Request Body:**
```json
{
  "friendId": 2
}
```

**Response:** `201 Created`
```json
{
  "id": 1,
  "userId": 1,
  "userNickname": "User1",
  "friendId": 2,
  "friendNickname": "User2",
  "status": "PENDING",
  "createdAt": "2026-02-17T10:00:00"
}
```

### 2. Accept Friend Request
친구 요청 수락

**Endpoint:** `PUT /friends/requests/{requestId}/accept`

**Response:** `200 OK`
```json
{
  "id": 1,
  "status": "ACCEPTED",
  "updatedAt": "2026-02-17T10:05:00"
}
```

### 3. Reject Friend Request
친구 요청 거절

**Endpoint:** `PUT /friends/requests/{requestId}/reject`

**Response:** `200 OK`

### 4. Unfriend
친구 삭제

**Endpoint:** `DELETE /friends/{friendId}`

**Response:** `204 No Content`

### 5. Get My Friends
친구 목록 조회

**Endpoint:** `GET /friends`

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "userId": 1,
    "friendId": 2,
    "friendNickname": "User2",
    "status": "ACCEPTED",
    "createdAt": "2026-02-17T10:00:00"
  }
]
```

### 6. Get Pending Requests
받은 친구 요청 조회

**Endpoint:** `GET /friends/requests/pending`

**Response:** `200 OK`

### 7. Get Sent Requests
보낸 친구 요청 조회

**Endpoint:** `GET /friends/requests/sent`

**Response:** `200 OK`

---

## Chat APIs

### 1. Get Recent Messages
채팅 메시지 조회

**Endpoint:** `GET /chat/rooms/{chatRoomId}/messages?limit=50`

**Query Parameters:**
- `limit` (optional): 최대 메시지 개수 (기본값: 50)

**Response:** `200 OK`
```json
[
  {
    "id": 1,
    "chatRoomId": 1,
    "userId": 1,
    "username": "User1",
    "message": "안녕하세요!",
    "createdAt": "2026-02-17T10:00:00"
  }
]
```

### 2. Create Direct Chat
1:1 채팅방 생성

**Endpoint:** `POST /chat/rooms/direct`

**Request Body:**
```json
{
  "friendId": 2
}
```

**Response:** `201 Created`
```json
{
  "chatRoomId": 10
}
```

### 3. Create Group Chat
그룹 채팅방 생성

**Endpoint:** `POST /chat/rooms/group`

**Request Body:**
```json
{
  "name": "우리 팀 채팅방"
}
```

**Response:** `201 Created`
```json
{
  "chatRoomId": 20
}
```

### 4. Add Member to Group Chat
그룹 채팅방에 멤버 추가

**Endpoint:** `POST /chat/rooms/{chatRoomId}/members`

**Request Body:**
```json
{
  "userId": 3
}
```

**Response:** `201 Created`

---

## WebSocket Chat

### Connection
```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({
  Authorization: 'Bearer ' + accessToken
}, function(frame) {
  console.log('Connected: ' + frame);
});
```

### Subscribe to Chat Room
```javascript
stompClient.subscribe('/topic/chat/' + chatRoomId, function(message) {
  const msg = JSON.parse(message.body);
  console.log('Received:', msg);
});
```

### Send Message
```javascript
stompClient.send('/app/chat.send/' + chatRoomId, {}, JSON.stringify({
  chatRoomId: chatRoomId,
  message: 'Hello!'
}));
```

### Join Chat Room
```javascript
stompClient.send('/app/chat.join/' + chatRoomId, {}, '{}');
```

### Leave Chat Room
```javascript
stompClient.send('/app/chat.leave/' + chatRoomId, {}, '{}');
```

---

## Error Responses

### Standard Error Format
```json
{
  "timestamp": "2026-02-17T10:00:00",
  "status": 400,
  "error": "Bad Request",
  "code": "VAL001",
  "message": "Invalid input",
  "path": "/api/rooms"
}
```

### Common Error Codes

#### Authentication (AUTH)
- `AUTH001` - Invalid credentials
- `AUTH002` - Authentication required
- `AUTH003` - Access denied
- `AUTH004` - Token expired
- `AUTH005` - Invalid token
- `AUTH006` - Duplicate email

#### User (USER)
- `USER001` - User not found
- `USER002` - Low temperature (< 30°C)

#### Room (ROOM)
- `ROOM001` - Room not found
- `ROOM002` - Room is full
- `ROOM003` - Room is closed
- `ROOM004` - Already in room
- `ROOM005` - Not in room
- `ROOM006` - Invalid room password
- `ROOM007` - Not host

#### Evaluation (EVAL)
- `EVAL001` - Room not closed for evaluation
- `EVAL002` - Evaluator not in room
- `EVAL003` - Evaluated user not in room
- `EVAL004` - Already evaluated
- `EVAL005` - Cannot evaluate self

#### Chat (CHAT)
- `CHAT001` - Chat room not found
- `CHAT002` - Not a chat member
- `CHAT003` - Message cannot be empty

---

## Rate Limiting

API rate limits:
- 100 requests per minute per user
- 1000 requests per hour per user

Exceeded limits return `429 Too Many Requests`.

---

## Pagination

For endpoints that return lists, pagination is supported:

**Query Parameters:**
- `page` - Page number (0-indexed, default: 0)
- `size` - Page size (default: 20, max: 100)
- `sort` - Sort field and direction (e.g., `createdAt,desc`)

**Example:**
```
GET /rooms/game/1?page=0&size=20&sort=createdAt,desc
```

---

Last updated: 2026-02-17
