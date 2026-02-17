# Gembud 실행 가이드

## 🚀 빠른 시작

### 1. 사전 요구사항

다음 프로그램들이 설치되어 있어야 합니다:
- **Docker & Docker Compose** (PostgreSQL, Redis용)
- **Java 17** 이상
- **Gradle 8.x** (프로젝트에 포함된 gradlew 사용 가능)

설치 확인:
```bash
docker --version
docker-compose --version
java -version
```

---

## 📦 1단계: 데이터베이스 및 Redis 실행

프로젝트 루트 디렉토리에서 Docker Compose를 실행합니다:

```bash
# 프로젝트 루트로 이동
cd /Users/gimjiseob/Projects/gembud

# PostgreSQL과 Redis 컨테이너 실행
docker-compose up -d
```

실행 확인:
```bash
# 컨테이너 상태 확인
docker-compose ps

# PostgreSQL 접속 테스트
docker exec -it gembud-postgres psql -U gembud -d gembud

# Redis 접속 테스트
docker exec -it gembud-redis redis-cli ping
```

컨테이너 정보:
- **PostgreSQL**: `localhost:5432`
  - Database: `gembud`
  - Username: `gembud`
  - Password: `gembud_dev_password`

- **Redis**: `localhost:6379`

---

## 🏃 2단계: 백엔드 애플리케이션 실행

### 옵션 A: Gradle Wrapper 사용 (권장)

```bash
# backend 디렉토리로 이동
cd backend

# 빌드 및 실행
./gradlew bootRun
```

### 옵션 B: IDE에서 실행

IntelliJ IDEA 또는 Eclipse에서:
1. `backend` 폴더를 프로젝트로 열기
2. `GembudApplication.java` 찾기
3. 메인 메서드 우클릭 → Run

### 옵션 C: JAR 빌드 후 실행

```bash
cd backend

# JAR 빌드
./gradlew clean build

# JAR 실행
java -jar build/libs/gembud-backend-0.0.1-SNAPSHOT.jar
```

---

## ✅ 3단계: 실행 확인

애플리케이션이 정상적으로 실행되면 다음과 같은 로그가 나타납니다:

```
Started GembudApplication in X.XXX seconds
```

### API 엔드포인트 테스트

**기본 URL**: `http://localhost:8080/api`

#### 1. 회원가입 테스트
```bash
curl -X POST http://localhost:8080/api/auth/signup \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123",
    "nickname": "테스트유저",
    "ageRange": "20대"
  }'
```

#### 2. 로그인 테스트
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "test@example.com",
    "password": "password123"
  }'
```

#### 3. 게임 목록 조회 (인증 필요)
```bash
# 먼저 로그인으로 받은 accessToken을 사용
curl -X GET http://localhost:8080/api/games \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

---

## 🗄️ 데이터베이스 마이그레이션

Flyway가 자동으로 마이그레이션을 실행합니다:
- 총 12개의 마이그레이션 파일
- 애플리케이션 시작 시 자동 실행
- 마이그레이션 이력은 `flyway_schema_history` 테이블에서 확인

마이그레이션 확인:
```bash
docker exec -it gembud-postgres psql -U gembud -d gembud -c "SELECT * FROM flyway_schema_history;"
```

---

## 🔧 환경 변수 설정 (선택사항)

실제 서비스 운영 시 다음 환경 변수를 설정해야 합니다:

### JWT 설정
```bash
export JWT_SECRET="your-secure-secret-key-at-least-256-bits-long"
```

### OAuth2 설정 (Google)
```bash
export GOOGLE_CLIENT_ID="your-google-client-id"
export GOOGLE_CLIENT_SECRET="your-google-client-secret"
```

### OAuth2 설정 (Discord)
```bash
export DISCORD_CLIENT_ID="your-discord-client-id"
export DISCORD_CLIENT_SECRET="your-discord-client-secret"
```

### 데이터베이스 (필요시)
```bash
export DATABASE_USERNAME="gembud"
export DATABASE_PASSWORD="gembud_dev_password"
```

**개발 환경에서는 환경 변수 없이도 기본값으로 실행됩니다.**

---

## 📊 테스트 실행

전체 테스트 실행:
```bash
cd backend
./gradlew test
```

특정 테스트만 실행:
```bash
./gradlew test --tests "com.gembud.service.AuthServiceTest"
```

테스트 리포트 확인:
```
backend/build/reports/tests/test/index.html
```

---

## 🛑 종료 방법

### 백엔드 애플리케이션 종료
- Gradle: `Ctrl + C`
- IDE: Stop 버튼 클릭

### Docker 컨테이너 종료
```bash
# 컨테이너 중지
docker-compose stop

# 컨테이너 중지 및 삭제
docker-compose down

# 볼륨까지 삭제 (데이터 초기화)
docker-compose down -v
```

---

## 🐛 트러블슈팅

### 1. 포트 충돌
```
Error: Port 8080 is already in use
```
**해결**: `application-dev.yml`에서 `server.port`를 변경하거나 8080 포트 사용 중인 프로세스 종료

### 2. PostgreSQL 연결 실패
```
org.postgresql.util.PSQLException: Connection refused
```
**해결**:
```bash
# PostgreSQL 컨테이너 상태 확인
docker-compose ps

# 컨테이너 재시작
docker-compose restart postgres
```

### 3. Redis 연결 실패
```
Unable to connect to Redis
```
**해결**:
```bash
# Redis 컨테이너 상태 확인
docker-compose logs redis

# 컨테이너 재시작
docker-compose restart redis
```

### 4. Flyway 마이그레이션 실패
```
FlywayException: Validate failed
```
**해결**:
```bash
# 데이터베이스 초기화
docker-compose down -v
docker-compose up -d

# 애플리케이션 재시작
```

### 5. Gradle 빌드 실패
```
Permission denied: ./gradlew
```
**해결**:
```bash
chmod +x gradlew
```

---

## 📝 주요 엔드포인트

전체 API 문서는 `docs/api/API.md` 참고

### 인증
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/refresh` - 토큰 갱신

### 게임
- `GET /api/games` - 게임 목록

### 방
- `GET /api/rooms/game/{gameId}` - 게임별 방 목록
- `POST /api/rooms` - 방 생성
- `POST /api/rooms/{roomId}/join` - 방 참가

### 매칭 추천
- `GET /api/matching/recommendations/game/{gameId}` - 추천 방 목록

### 친구
- `POST /api/friends/requests` - 친구 요청
- `GET /api/friends` - 친구 목록

### 신고
- `POST /api/reports` - 신고 생성
- `GET /api/reports/my` - 내 신고 목록

### 알림
- `GET /api/notifications` - 알림 목록
- `GET /api/notifications/unread/count` - 읽지 않은 알림 개수

---

## 🎯 다음 단계

1. **API 테스트**: Postman 또는 Insomnia로 API 테스트
2. **WebSocket 테스트**: 채팅 및 실시간 알림 테스트
3. **프론트엔드 연동**: React 앱과 연동

---

## 💡 팁

### 개발 중 자동 재시작
Spring Boot DevTools를 사용하면 코드 변경 시 자동으로 재시작됩니다.

```gradle
// build.gradle에 추가 (이미 포함됨)
developmentOnly 'org.springframework.boot:spring-boot-devtools'
```

### 로그 레벨 조정
```yaml
# application-dev.yml
logging:
  level:
    com.gembud: DEBUG  # 상세 로그
    org.springframework: INFO  # 기본 로그
```

### API 문서화
전체 API 명세는 `docs/api/API.md` 참고

---

문제가 발생하면 GitHub Issues에 등록해주세요!
