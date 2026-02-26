# Gembud 테스트 가이드

## 🚀 서버 실행

### 백엔드 실행
```bash
cd /Users/gimjiseob/Projects/gembud/backend
./gradlew bootRun
```
서버 주소: http://localhost:8080/api

### 프론트엔드 실행
```bash
cd /Users/gimjiseob/Projects/gembud/frontend
npm run dev
```
프론트엔드 주소: http://localhost:3001

---

## 👤 테스트 계정

### 방법 1: 회원가입으로 새 계정 생성

1. http://localhost:3001/signup 접속
2. 다음 정보 입력:
   - **이메일**: test1@example.com (또는 원하는 이메일)
   - **비밀번호**: password123
   - **닉네임**: 테스트유저1
3. 회원가입 클릭

### 방법 2: 직접 API 호출로 계정 생성

```bash
# 테스트 계정 1 생성
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test1@example.com",
    "password": "password123",
    "nickname": "테스트유저1"
  }'

# 테스트 계정 2 생성
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test2@example.com",
    "password": "password123",
    "nickname": "테스트유저2"
  }'

# 테스트 계정 3 생성
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test3@example.com",
    "password": "password123",
    "nickname": "테스트유저3"
  }'
```

### 방법 3: SQL로 직접 데이터베이스에 삽입

PostgreSQL에 접속하여 다음 SQL 실행:

```sql
-- 테스트 사용자 생성 (비밀번호: password123)
-- BCrypt 해시: $2a$10$eN7Y8pVF7IUF1QGq9JpC/OxWQPr.r4zJZ9Q8xKZ9vF1Q9Q8xKZ9vF
INSERT INTO users (email, password, nickname, temperature, created_at, updated_at)
VALUES
  ('test1@example.com', '$2a$10$eN7Y8pVF7IUF1QGq9JpC/OxWQPr.r4zJZ9Q8xKZ9vF1Q9Q8xKZ9vF', '테스트유저1', 36.5, NOW(), NOW()),
  ('test2@example.com', '$2a$10$eN7Y8pVF7IUF1QGq9JpC/OxWQPr.r4zJZ9Q8xKZ9vF1Q9Q8xKZ9vF', '테스트유저2', 36.5, NOW(), NOW()),
  ('test3@example.com', '$2a$10$eN7Y8pVF7IUF1QGq9JpC/OxWQPr.r4zJZ9Q8xKZ9vF1Q9Q8xKZ9vF', '테스트유저3', 36.5, NOW(), NOW());
```

---

## 🔑 로그인하기

### 웹 브라우저에서 로그인

1. http://localhost:3001/login 접속
2. 이메일: `test1@example.com`
3. 비밀번호: `password123`
4. 로그인 버튼 클릭

### API로 직접 로그인 (토큰 받기)

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test1@example.com",
    "password": "password123"
  }'
```

응답 예시:
```json
{
  "timestamp": "2026-02-26T...",
  "status": 200,
  "message": "Success",
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "email": "test1@example.com",
    "nickname": "테스트유저1"
  }
}
```

---

## 🎮 테스트 데이터 준비

### 게임 데이터 추가

데이터베이스에 테스트용 게임 추가:

```sql
INSERT INTO games (name, image_url, genre, description, created_at, updated_at)
VALUES
  ('League of Legends', 'https://example.com/lol.png', 'MOBA', '5v5 전략 게임', NOW(), NOW()),
  ('PUBG', 'https://example.com/pubg.png', 'Battle Royale', '배틀로얄 게임', NOW(), NOW()),
  ('Valorant', 'https://example.com/valorant.png', 'FPS', '전술 FPS 게임', NOW(), NOW());
```

또는 Flyway 마이그레이션이 실행되면 자동으로 게임 데이터가 추가됩니다.

---

## 📋 주요 기능 테스트 시나리오

### 1. 회원가입 및 로그인
1. ✅ 회원가입 페이지에서 새 계정 생성
2. ✅ 로그인 페이지에서 로그인
3. ✅ 홈 페이지로 리디렉션 확인

### 2. 게임 탐색
1. ✅ 홈 페이지에서 게임 목록 확인
2. ✅ 게임 카드 클릭하여 게임 상세 페이지 이동
3. ✅ "방 목록 보기" 버튼 클릭

### 3. 방 생성 및 관리
1. ✅ 방 목록 페이지에서 "방 만들기" 버튼 클릭
2. ✅ 방 정보 입력 (제목, 설명, 최대 인원)
3. ✅ 방 생성 후 목록에 표시 확인
4. ✅ 방 클릭하여 상세 페이지 이동
5. ✅ 방 입장/퇴장 기능 테스트

### 4. 친구 기능
1. ✅ 친구 목록 페이지 (`/friends`) 이동
2. ✅ 다른 사용자 이메일로 친구 요청 보내기
3. ✅ 받은 친구 요청 확인 및 수락/거절
4. ✅ 친구 목록 확인

### 5. 알림 기능
1. ✅ 알림 페이지 (`/notifications`) 이동
2. ✅ 알림 목록 확인
3. ✅ 알림 읽음 처리
4. ✅ 알림 삭제

### 6. 프로필
1. ✅ 프로필 페이지 (`/profile/:userId`) 이동
2. ✅ 매너 온도, 게임 수, 받은 평가 확인
3. ✅ 평가 태그 및 최근 평가 내역 확인

---

## 🐛 문제 해결

### 백엔드 연결 실패
- 백엔드가 실행 중인지 확인: http://localhost:8080/api/games
- PostgreSQL 데이터베이스가 실행 중인지 확인
- `application.yml`의 데이터베이스 설정 확인

### 프론트엔드 빌드 오류
```bash
cd /Users/gimjiseob/Projects/gembud/frontend
rm -rf node_modules .vite
npm install
npm run dev
```

### CORS 오류
- 백엔드 `SecurityConfig.java`에서 CORS 설정 확인
- 프론트엔드 URL이 허용 목록에 있는지 확인

### 로그인 후 리디렉션 안됨
- 브라우저 콘솔에서 에러 확인
- 쿠키가 제대로 설정되었는지 확인 (개발자 도구 > Application > Cookies)

---

## 📡 API 엔드포인트

### 인증 API
- POST `/api/auth/signup` - 회원가입
- POST `/api/auth/login` - 로그인
- POST `/api/auth/logout` - 로그아웃
- POST `/api/auth/refresh` - 토큰 갱신

### 게임 API (공개)
- GET `/api/games` - 모든 게임 조회
- GET `/api/games/{id}` - 게임 상세 조회
- GET `/api/games?genre=FPS` - 장르별 게임 조회

### 방 API (인증 필요)
- POST `/api/rooms` - 방 생성
- GET `/api/rooms?gameId=1` - 게임별 방 목록
- GET `/api/rooms/{id}` - 방 상세 조회
- POST `/api/rooms/{id}/join` - 방 입장
- POST `/api/rooms/{id}/leave` - 방 퇴장
- DELETE `/api/rooms/{id}` - 방 닫기 (호스트만)

### 친구 API (인증 필요)
- GET `/api/friends` - 친구 목록
- GET `/api/friends/requests` - 받은 친구 요청
- GET `/api/friends/sent` - 보낸 친구 요청
- POST `/api/friends/request` - 친구 요청 보내기
- POST `/api/friends/requests/{id}/accept` - 친구 요청 수락
- POST `/api/friends/requests/{id}/reject` - 친구 요청 거절
- DELETE `/api/friends/{id}` - 친구 삭제

### 알림 API (인증 필요)
- GET `/api/notifications` - 알림 목록
- GET `/api/notifications/unread-count` - 읽지 않은 알림 수
- PUT `/api/notifications/{id}/read` - 알림 읽음 처리
- PUT `/api/notifications/read-all` - 모든 알림 읽음 처리
- DELETE `/api/notifications/{id}` - 알림 삭제

---

## 🔍 Swagger UI

API 문서 확인: http://localhost:8080/swagger-ui.html

Swagger UI에서 직접 API를 테스트할 수 있습니다.

---

## 📝 개발 노트

### 구현 완료된 기능
- ✅ 회원가입/로그인 (JWT + Cookie)
- ✅ 게임 목록/상세
- ✅ 방 생성/조회/입장/퇴장
- ✅ 친구 관리 (요청/수락/거절/삭제)
- ✅ 알림 시스템
- ✅ 프로필 페이지
- ✅ API 응답 표준화 (ApiResponse<T>)
- ✅ 에러 코드 체계 (48개 ErrorCode)

### 구현 예정 기능
- ⏳ 실시간 채팅 (WebSocket)
- ⏳ 평가 시스템 (방 종료 후)
- ⏳ 매칭 알고리즘
- ⏳ 광고 시스템

---

**즐거운 테스트 되세요! 🎮**
