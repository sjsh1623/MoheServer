# MoheSpring 🌸

> 한국의 숨은 장소를 발견하고 MBTI 기반 개인화 추천을 제공하는 Spring Boot 애플리케이션

[![Java](https://img.shields.io/badge/Java-21-orange)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.0-brightgreen)](https://spring.io/projects/spring-boot)
[![Spring Batch](https://img.shields.io/badge/Spring%20Batch-5.x-blue)](https://spring.io/projects/spring-batch)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-Latest-blue)](https://www.postgresql.org/)

## 📋 목차

- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [프로젝트 구조](#프로젝트-구조)
- [시작하기](#시작하기)
- [API 문서](#api-문서)
- [배치 작업](#배치-작업)

## ✨ 주요 기능

### 🎯 핵심 기능
- **장소 추천**: MBTI 기반 개인화 추천 알고리즘
- **벡터 검색**: pgvector를 활용한 유사도 검색
- **실시간 데이터 수집**: Naver/Google API 통합
- **Spring Batch**: 대용량 장소 데이터 자동 수집
- **JWT 인증**: Stateless 보안 아키텍처

### 🔐 사용자 관리
- 이메일 기반 회원가입 (OTP 인증)
- JWT Access/Refresh Token
- MBTI 프로필 설정
- 사용자 선호도 관리

### 📍 장소 기능
- 장소 검색 및 상세 정보
- 북마크 및 최근 본 장소
- 카테고리별 분류
- 평점 및 리뷰 집계

### 🤖 추천 시스템
- MBTI 기반 장소 추천
- 벡터 유사도 기반 추천
- 날씨 기반 추천
- 시간대별 맞춤 추천

## 🛠️ 기술 스택

### Backend
- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Build Tool**: Gradle 8.5
- **Batch**: Spring Batch 5.x

### Database
- **Production**: PostgreSQL (with pgvector extension)
- **Test**: H2 In-Memory Database
- **Connection Pool**: HikariCP

### Security
- **Authentication**: Spring Security + JWT
- **Token Storage**: Redis (optional)
- **Password**: BCrypt

### External APIs
- **Naver Local Search API**: 장소 데이터 수집
- **Google Places API**: 평점 및 상세 정보
- **Korean Government API**: 행정구역 정보
- **OpenAI API**: AI 기반 설명 생성
- **Ollama**: 로컬 AI 벡터 생성

## 📦 프로젝트 구조

```
src/main/java/com/mohe/spring/
├── batch/              # Spring Batch (장소 데이터 수집)
├── config/             # 설정 (Security, Batch, OpenAPI)
├── controller/         # REST API Controllers
├── dto/                # Data Transfer Objects
├── entity/             # JPA Entities (Domain Models)
├── repository/         # Spring Data JPA Repositories
├── service/            # Business Logic Services
├── security/           # JWT, UserDetails, Filters
└── exception/          # Global Exception Handling
```

자세한 구조는 [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) 참고

## 🚀 시작하기

### 필수 요구사항
- Java 21+
- Docker & Docker Compose
- Gradle 8.5+

### 환경 변수 설정

`.env` 파일 생성:

```bash
# Database
DB_USERNAME=mohe_user
DB_PASSWORD=your_password

# Naver API (필수)
NAVER_CLIENT_ID=your_client_id
NAVER_CLIENT_SECRET=your_client_secret

# Google Places API (선택사항)
GOOGLE_PLACES_API_KEY=your_api_key

# JWT Secret
JWT_SECRET=your_secret_key_minimum_64_characters
```

### Docker로 실행

```bash
# PostgreSQL + Spring Boot 실행
docker-compose up --build

# 백그라운드 실행
docker-compose up -d

# 종료
docker-compose down
```

### 로컬 개발 환경

```bash
# PostgreSQL만 Docker로 실행
docker-compose up postgres -d

# Gradle로 애플리케이션 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/MoheSpring-0.0.1-SNAPSHOT.jar
```

## 📚 API 문서

애플리케이션 실행 후:

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/health

### 주요 API 엔드포인트

#### 인증
- `POST /api/auth/signup` - 회원가입
- `POST /api/auth/login` - 로그인
- `POST /api/auth/refresh` - 토큰 갱신

#### 장소
- `GET /api/places` - 장소 목록
- `GET /api/places/{id}` - 장소 상세
- `GET /api/places/search` - 장소 검색

#### 추천
- `GET /api/recommendations` - MBTI 기반 추천
- `GET /api/recommendations/vector-similar` - 벡터 유사도 추천

#### 배치
- `POST /api/batch/jobs/place-collection` - 장소 수집 배치 실행

## 🔄 배치 작업

### Spring Batch Job 실행

```bash
# API를 통한 배치 실행
curl -X POST http://localhost:8080/api/batch/jobs/place-collection
```

자세한 내용은 [BATCH_GUIDE.md](BATCH_GUIDE.md) 참고

## 💻 개발 가이드

### 코드 스타일
- **EditorConfig**: `.editorconfig` 파일 참고
- **Indentation**: 4 spaces (Java), 2 spaces (YAML/JSON)
- **Line Length**: 120 characters max

### 테스트 실행

```bash
# 전체 테스트
./gradlew test

# 빌드
./gradlew clean build
```

## 📖 문서

- [PROJECT_STRUCTURE.md](PROJECT_STRUCTURE.md) - 상세 프로젝트 구조
- [BATCH_GUIDE.md](BATCH_GUIDE.md) - Spring Batch 가이드
- [CLAUDE.md](CLAUDE.md) - Claude Code 사용 가이드

## 👤 작성자

**Andrew Lim (임석현)**
- Email: sjsh1623@gmail.com

---

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!
