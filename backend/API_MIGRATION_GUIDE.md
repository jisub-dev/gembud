# API Migration Guide - Response Format Standardization

## 📋 Overview

**Date**: 2026-02-22
**Version**: Backend v2.0.0
**Breaking Change**: ✅ YES - All API response formats have changed

All Gembud API endpoints now return responses wrapped in a standardized `ApiResponse<T>` format. This guide helps frontend developers migrate from the old format to the new one.

---

## 🔄 Response Format Changes

### Old Format (Before)

```json
{
  "id": 1,
  "title": "League of Legends Room",
  "hostId": 123,
  "maxParticipants": 5,
  "currentParticipants": 2
}
```

### New Format (After)

```json
{
  "timestamp": "2026-02-22T10:30:45.123",
  "status": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "title": "League of Legends Room",
    "hostId": 123,
    "maxParticipants": 5,
    "currentParticipants": 2
  }
}
```

### Key Changes

1. **Response Wrapper**: All successful responses are wrapped in `ApiResponse<T>`
2. **Data Access**: Response data is now accessed via `response.data.data` instead of `response.data`
3. **Metadata**: Every response includes `timestamp`, `status`, and `message` fields
4. **Error Format**: Error responses remain unchanged (already using `ErrorResponse`)

---

## 📊 Response Structure

### Success Response (200, 201)

```typescript
interface ApiResponse<T> {
  timestamp: string;      // ISO 8601 datetime
  status: number;         // HTTP status code (200, 201, etc.)
  message: string;        // "Success" or "Created"
  data: T;               // Actual response data
}
```

### No Content Response (204)

```json
{
  "timestamp": "2026-02-22T10:30:45.123",
  "status": 204,
  "message": "No Content",
  "data": null
}
```

### Error Response (Unchanged)

```json
{
  "timestamp": "2026-02-22T10:30:45.123",
  "status": 404,
  "error": "Not Found",
  "code": "ROOM001",
  "message": "Room not found",
  "path": "/api/rooms/999"
}
```

---

## 🛠️ Frontend Migration Steps

### Step 1: Update TypeScript Interfaces

Create a new `ApiResponse<T>` interface:

```typescript
// src/types/api.ts
export interface ApiResponse<T> {
  timestamp: string;
  status: number;
  message: string;
  data: T;
}
```

### Step 2: Update Service Files

**Pattern**: Change `response.data` to `response.data.data`

#### Example: Room Service

**Before:**
```typescript
// src/services/roomService.ts
export const getRoomsByGame = async (gameId: number): Promise<RoomResponse[]> => {
  const response = await api.get<RoomResponse[]>(`/rooms?gameId=${gameId}`);
  return response.data;  // ❌ Old
};
```

**After:**
```typescript
// src/services/roomService.ts
import { ApiResponse } from '../types/api';

export const getRoomsByGame = async (gameId: number): Promise<RoomResponse[]> => {
  const response = await api.get<ApiResponse<RoomResponse[]>>(`/rooms?gameId=${gameId}`);
  return response.data.data;  // ✅ New
};
```

### Step 3: Update Error Handling (Optional Enhancement)

You can now access additional metadata:

```typescript
try {
  const response = await api.get<ApiResponse<RoomResponse[]>>(`/rooms?gameId=${gameId}`);

  // Access metadata
  console.log('Response timestamp:', response.data.timestamp);
  console.log('HTTP status:', response.data.status);
  console.log('Message:', response.data.message);

  return response.data.data;
} catch (error) {
  // Error format unchanged
  if (axios.isAxiosError(error) && error.response) {
    console.error('Error code:', error.response.data.code);
    console.error('Error message:', error.response.data.message);
  }
  throw error;
}
```

---

## 📝 Service File Migration Checklist

Update all service files to use the new response format:

### Required Updates (8 files)

- [ ] `/frontend/src/services/authService.ts` (4 endpoints)
  - `signup()`, `login()`, `refreshToken()`, `logout()`

- [ ] `/frontend/src/services/gameService.ts` (2 endpoints)
  - `getAllGames()`, `getGamesByGenre()`

- [ ] `/frontend/src/services/roomService.ts` (6 endpoints)
  - `createRoom()`, `getRoomsByGame()`, `getRoomById()`, `joinRoom()`, `leaveRoom()`, `closeRoom()`

- [ ] `/frontend/src/services/evaluationService.ts` (5 endpoints)
  - `createEvaluation()`, `getEvaluationsByRoom()`, `getEvaluatableParticipants()`, `getEvaluationsReceived()`, `getTemperatureStats()`

- [ ] `/frontend/src/services/friendService.ts` (7 endpoints)
  - `sendFriendRequest()`, `acceptFriendRequest()`, `rejectFriendRequest()`, `unfriend()`, `getMyFriends()`, `getPendingRequests()`, `getSentRequests()`

- [ ] `/frontend/src/services/notificationService.ts` (6 endpoints)
  - `getMyNotifications()`, `getUnreadNotifications()`, `getUnreadCount()`, `markAsRead()`, `markAllAsRead()`, `deleteNotification()`

- [ ] `/frontend/src/services/chatService.ts` (5 endpoints)
  - `getChatRooms()`, `getChatMessages()`, `sendMessage()`, `getChatRoomMembers()`, `leaveChatRoom()`

- [ ] `/frontend/src/services/matchingService.ts` (1 endpoint)
  - `getRecommendedRooms()`

### Optional Updates

- [ ] Update user service if it exists
- [ ] Update admin/report services if they exist
- [ ] Update ad service if it exists

---

## 🔍 Migration Examples by Endpoint Type

### GET Request (List)

```typescript
// OLD
const getRooms = async (): Promise<RoomResponse[]> => {
  const response = await api.get<RoomResponse[]>('/rooms');
  return response.data;
};

// NEW
const getRooms = async (): Promise<RoomResponse[]> => {
  const response = await api.get<ApiResponse<RoomResponse[]>>('/rooms');
  return response.data.data;
};
```

### GET Request (Single Object)

```typescript
// OLD
const getRoom = async (id: number): Promise<RoomResponse> => {
  const response = await api.get<RoomResponse>(`/rooms/${id}`);
  return response.data;
};

// NEW
const getRoom = async (id: number): Promise<RoomResponse> => {
  const response = await api.get<ApiResponse<RoomResponse>>(`/rooms/${id}`);
  return response.data.data;
};
```

### POST Request (Create)

```typescript
// OLD
const createRoom = async (request: CreateRoomRequest): Promise<RoomResponse> => {
  const response = await api.post<RoomResponse>('/rooms', request);
  return response.data;
};

// NEW
const createRoom = async (request: CreateRoomRequest): Promise<RoomResponse> => {
  const response = await api.post<ApiResponse<RoomResponse>>('/rooms', request);
  return response.data.data;
};
```

### DELETE Request (No Content)

```typescript
// OLD
const deleteRoom = async (id: number): Promise<void> => {
  await api.delete(`/rooms/${id}`);
};

// NEW - Now returns ApiResponse with data: null
const deleteRoom = async (id: number): Promise<void> => {
  await api.delete<ApiResponse<null>>(`/rooms/${id}`);
  // No need to return response.data.data since it's null
};
```

### PUT Request (Update)

```typescript
// OLD
const updateProfile = async (data: UpdateProfileRequest): Promise<UserResponse> => {
  const response = await api.put<UserResponse>('/users/me', data);
  return response.data;
};

// NEW
const updateProfile = async (data: UpdateProfileRequest): Promise<UserResponse> => {
  const response = await api.put<ApiResponse<UserResponse>>('/users/me', data);
  return response.data.data;
};
```

---

## 🚨 Common Pitfalls

### 1. Forgetting `.data` Access

```typescript
// ❌ WRONG - This returns the wrapper, not the actual data
const rooms = response.data;

// ✅ CORRECT - Access nested data
const rooms = response.data.data;
```

### 2. Incorrect TypeScript Types

```typescript
// ❌ WRONG - Missing ApiResponse wrapper
const response = await api.get<RoomResponse[]>('/rooms');

// ✅ CORRECT - Wrapped with ApiResponse<T>
const response = await api.get<ApiResponse<RoomResponse[]>>('/rooms');
```

### 3. Error Handling Confusion

```typescript
// ⚠️ REMEMBER: Error responses are NOT wrapped
try {
  const response = await api.get<ApiResponse<RoomResponse[]>>('/rooms');
  return response.data.data;
} catch (error) {
  // Error response is still ErrorResponse, not ApiResponse
  if (axios.isAxiosError(error) && error.response) {
    // ✅ Access error.response.data.code directly
    const errorCode = error.response.data.code;
    // ❌ NOT error.response.data.data.code
  }
}
```

---

## 🧪 Testing Strategy

### 1. Unit Tests

Update mock responses in your tests:

```typescript
// OLD mock
jest.mock('axios');
mockedAxios.get.mockResolvedValue({
  data: [{ id: 1, title: 'Room 1' }]
});

// NEW mock
jest.mock('axios');
mockedAxios.get.mockResolvedValue({
  data: {
    timestamp: '2026-02-22T10:30:00',
    status: 200,
    message: 'Success',
    data: [{ id: 1, title: 'Room 1' }]
  }
});
```

### 2. Integration Tests

Verify actual API responses:

```typescript
it('should fetch rooms with new response format', async () => {
  const response = await getRoomsByGame(1);

  // Response should be the unwrapped data
  expect(Array.isArray(response)).toBe(true);
  expect(response[0]).toHaveProperty('id');

  // Not the wrapper
  expect(response).not.toHaveProperty('timestamp');
  expect(response).not.toHaveProperty('status');
});
```

### 3. Manual Testing

1. Open browser DevTools Network tab
2. Verify all API calls return new format
3. Check that UI still renders correctly
4. Test error scenarios (404, 403, etc.)

---

## 🔄 Rollback Plan

If issues arise, you can temporarily revert to old format:

### Option A: Backend Rollback

```bash
cd backend
git revert <commit-hash>
./gradlew clean build
# Restart server
```

### Option B: Frontend Adapter (Temporary)

Create an adapter to unwrap responses:

```typescript
// src/utils/apiAdapter.ts
import axios, { AxiosResponse } from 'axios';
import { ApiResponse } from '../types/api';

export const unwrapResponse = <T>(response: AxiosResponse<ApiResponse<T>>): T => {
  return response.data.data;
};

// Usage
const response = await api.get<ApiResponse<RoomResponse[]>>('/rooms');
const rooms = unwrapResponse(response);
```

---

## 📊 Affected Endpoints Summary

### Total Endpoints: 49 across 11 controllers

| Controller | Endpoints | Status Change |
|-----------|-----------|---------------|
| AuthController | 4 | 200/201 |
| GameController | 2 | 200 |
| RoomController | 6 | 200/201/204 |
| EvaluationController | 5 | 200/201 |
| FriendController | 7 | 200/201/204 |
| NotificationController | 6 | 200/204 |
| ChatController | 5 | 200/201/204 |
| ReportController | 9 | 200/201/204 |
| MatchingController | 1 | 200 |
| AdController | 3 | 200/201 |
| UserController | 1 | 200 |

**All endpoints now return `ApiResponse<T>` wrapper.**

---

## ✅ Migration Completion Checklist

- [ ] Review this migration guide
- [ ] Create `ApiResponse<T>` interface in frontend
- [ ] Update all 8 service files
- [ ] Update unit test mocks
- [ ] Run integration tests
- [ ] Manual testing in browser
- [ ] Update API documentation (if separate)
- [ ] Deploy frontend and backend simultaneously
- [ ] Monitor error rates for 24 hours

---

## 📞 Support

If you encounter issues during migration:

1. **Check Swagger UI**: http://localhost:8080/api/swagger-ui.html
2. **Verify response format**: Use browser DevTools Network tab
3. **Review error codes**: See `ERROR_CODES.md` (coming in Phase 3)
4. **Contact backend team**: Report discrepancies in response format

---

## 🎯 Benefits After Migration

1. **Consistency**: All responses follow same structure
2. **Metadata**: Access to timestamp, status, message for every request
3. **Debugging**: Easier to log and trace requests
4. **Type Safety**: Better TypeScript support with generic ApiResponse<T>
5. **Future-Proof**: Easier to add new metadata fields (e.g., pagination, version)

---

**Migration Timeline**: Recommended to complete within 1-2 days
**Deployment**: Coordinate backend + frontend deployment
**Rollback Window**: 24 hours with monitoring

Good luck with the migration! 🚀
