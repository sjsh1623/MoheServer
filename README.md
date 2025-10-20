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

### 문서 & 빠른 링크
- [API 가이드 (한글)](API_GUIDE.md) - 컨트롤러별 엔드포인트와 권한 요약
- [BATCH_GUIDE.md](BATCH_GUIDE.md) - 배치 작업 설정 및 운영 팁
- [REGION_GUIDE.md](REGION_GUIDE.md) - 행정구역 데이터 수집과 캐시 정책

### 대표 API 그룹
- **인증/온보딩**: `POST /api/auth/login`, `POST /api/auth/signup`, `POST /api/auth/verify-email`
- **사용자 & 활동** *(Bearer)*: `GET /api/user/profile`, `GET /api/user/recent-places`, `POST /api/bookmarks/toggle`
- **장소 탐색**: `GET /api/places`, `GET /api/places/search`, `GET /api/places/vector-search` *(Bearer)*
- **추천 서비스**: `GET /api/recommendations/enhanced` *(Bearer)*, `GET /api/recommendations/contextual`, `GET /api/keyword-recommendations/by-keyword` *(Bearer)*
- **관리자/데이터 관리** *(Bearer ADMIN)*: `POST /api/admin/place-management/check-availability`, `POST /api/place-enhancement/batch-enhance`, `POST /api/admin/similarity/calculate`
- **배치/동기화**: `POST /api/batch/jobs/place-collection`, `POST /api/batch/jobs/update-crawled-data`

## 🔄 배치 작업

### Spring Batch Job 실행

```bash
# API를 통한 배치 실행
curl -X POST http://localhost:8080/api/batch/jobs/place-collection
curl -X POST http://localhost:8080/api/batch/jobs/update-crawled-data
curl -X POST http://localhost:8080/api/batch/jobs/vector-embedding

# 실행 중인 배치 작업 조회
curl http://localhost:8080/api/batch/jobs/running

# 특정 배치 작업 중지 (executionId는 /running에서 조회)
curl -X POST http://localhost:8080/api/batch/jobs/stop/123

# 모든 실행 중인 배치 작업 중지
curl -X POST http://localhost:8080/api/batch/jobs/stop-all
```

### 배치 작업 제어

| 엔드포인트 | 메서드 | 설명 |
|-----------|--------|------|
| `/api/batch/jobs/running` | GET | 실행 중인 배치 작업 목록 조회 (executionId, 상태, Step 진행률 포함) |
| `/api/batch/jobs/stop/{executionId}` | POST | 특정 배치 작업 중지 (현재 청크 완료 후 안전하게 종료) |
| `/api/batch/jobs/stop-all` | POST | 모든 실행 중인 배치 작업 중지 |

### 배치 작업 상태 플래그

배치 작업(`UpdateCrawledDataJob`)은 각 장소의 처리 상태를 두 가지 플래그로 관리합니다:

| 상황 | `crawler_found` | `ready` | 설명 |
|------|----------------|---------|------|
| **크롤링 실패** | `false` | `false` | 크롤러가 네이버 지도에서 장소를 찾지 못함 (404, 타임아웃 등) |
| **정보 부족** | `true` | `false` | 크롤링은 성공했지만 설명 데이터가 없음 |
| **AI 처리 실패** | `true` | `false` | 크롤링은 성공했지만 AI 키워드/벡터 생성 실패 |
| **처리 완료** | `true` | `true` | 모든 처리 성공, API에서 사용 가능 |

**재시도 정책:**
- `crawler_found = false` → 크롤러 개선 후 재시도 권장
- `crawler_found = true, ready = false` → AI 모델 개선 후 재시도 권장

### 배치 처리 최적화

배치 작업은 메모리 효율성과 안정성을 위해 다음과 같이 최적화되었습니다:

- **Two-Step Query**: ID만 먼저 로드 후 엔티티 조회 (Hibernate pagination 이슈 해결)
- **Page-by-Page Loading**: 한 번에 10개씩만 메모리에 로드 (메모리 효율)
- **Collection Lazy Loading**: 여러 컬렉션을 개별 쿼리로 로드하여 MultipleBagFetchException 방지
- **Graceful Shutdown**: 현재 청크 완료 후 안전하게 종료

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
- [API_GUIDE.md](API_GUIDE.md) - REST API 개요 및 권한 체계
- [BATCH_GUIDE.md](BATCH_GUIDE.md) - Spring Batch 가이드
- [REGION_GUIDE.md](REGION_GUIDE.md) - 한국 행정구역 데이터 처리 가이드
- [CLAUDE.md](CLAUDE.md) - Claude Code 사용 가이드

## 👤 작성자

**Andrew Lim (임석현)**
- Email: sjsh1623@gmail.com

---

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!
