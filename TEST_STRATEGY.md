# Gembud Test Strategy

## Test Stack

### Frontend
- **Vitest** + **React Testing Library** + **MSW** (mock service worker)
- Setup: `frontend/src/test/setup.ts`
- Run: `cd frontend && npm test`

### Backend
- **JUnit 5** + **Mockito** (unit tests)
- **Spring Boot Test** (integration tests)
- Run: `cd backend && ./mvnw test`

---

## Feature Test Scenarios

### 1. Friend System

| Scenario | Expected |
|---|---|
| User A sends request to User B (by email) | PENDING request created |
| User A sends request to themselves | Error: Cannot send to yourself |
| Duplicate request | Error: Friend request already exists |
| User B accepts request | Status → ACCEPTED, bidirectional |
| User B rejects request | Status → REJECTED |
| User A removes friend | Friend record deleted |
| View pending requests | Only PENDING requests for receiver |

**Test files:**
- `backend/src/test/java/com/gembud/service/FriendServiceTest.java`
- `frontend/src/test/hooks/useFriends.test.ts`

---

### 2. Room System

| Scenario | Expected |
|---|---|
| Create room with filters | Room saved with filter map |
| Join open room | Participant added, count incremented |
| Join same room twice | Error: Already a participant |
| Join full room | Error: Room is full |
| Leave room (non-host) | Participant removed |
| Leave room (host) | Room closed or host transferred |
| Join private room without password | Error: Wrong password |
| Join private room with correct password | Success |

---

### 3. Premium System

| Scenario | Expected |
|---|---|
| Activate premium (1 month) | isPremium=true, expiresAt=+1 month |
| Re-activate (extends) | Old subscription cancelled, new created |
| Cancel premium | isPremium=false immediately |
| Premium expiry (scheduler) | isPremium=false, subscription EXPIRED |
| getStatus with stale DB | Auto-deactivate if expiresAt in past |
| Free user requests limit=20 | Returns max 10 recommendations |
| Premium user requests limit=20 | Returns up to 20 recommendations |
| Ad fetch for premium user | Returns empty list |
| Ad fetch for free user | Returns ad list |

**Test files:**
- `backend/src/test/java/com/gembud/service/SubscriptionServiceTest.java`
- `frontend/src/test/hooks/useSubscription.test.ts`

---

### 4. Matching/Recommendations

| Scenario | Expected |
|---|---|
| User with no preferences | Score based on host temperature only |
| Room matching user's tier | Higher score than non-matching |
| Room matching user's position | Higher score than non-matching |
| Empty room list | Returns empty recommendations |
| limit=5 request | Returns at most 5 rooms |

**Test files:**
- `backend/src/test/java/com/gembud/service/MatchingServiceTest.java`

---

### 5. Chat System

| Scenario | Expected |
|---|---|
| User joins chat room | System message broadcast |
| User sends message | Message broadcast to all subscribers |
| User leaves chat room | System message broadcast |
| Load message history | Returns last N messages |
| getChatRoomByGameRoom | Returns correct chat room ID |

---

## Running Tests

```bash
# Frontend unit tests
cd frontend && npm test

# Frontend with coverage
cd frontend && npm run test:coverage

# Backend unit tests
cd backend && ./mvnw test

# Backend single class
cd backend && ./mvnw test -Dtest=SubscriptionServiceTest
```

## Test Coverage Goals
- Service layer: 80%+ branch coverage
- Frontend hooks: all success/error paths
- Controllers: basic smoke tests via MockMvc
