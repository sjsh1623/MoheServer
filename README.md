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
- **Naver Reverse Geocoding API**: 좌표를 주소로 변환
- **Korean Meteorological Administration API**: 날씨 정보 (단기예보)
- **Google Places API**: 평점 및 상세 정보
- **Korean Government API**: 행정구역 정보
- **OpenAI API**: AI 기반 설명 생성
- **OpenMeteo API**: 날씨 정보 (fallback)
- **Embedding Service**: 한국어 벡터 임베딩 (kanana-nano-2.1b-embedding)

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

# 기상청 단기예보 API (선택사항)
KMA_SERVICE_KEY=your_kma_service_key

# Google Places API (선택사항)
GOOGLE_PLACES_API_KEY=your_api_key

# Embedding Service (필수 - 벡터 검색용)
EMBEDDING_SERVICE_URL=http://localhost:6000

# JWT Secret
JWT_SECRET=your_secret_key_minimum_64_characters

# Mock 위치 설정 (개발/테스트 환경 전용)
# 설정되어 있으면 API 파라미터를 무시하고 이 값을 강제로 사용
MOCK_LATITUDE=37.5636   # 서울 중구 (기본값)
MOCK_LONGITUDE=126.9976
```

### Docker로 실행

#### 🔥 개발 모드 (Hot Reload)

코드 수정 시 자동으로 재컴파일 및 재시작됩니다. Spring Boot DevTools를 활용한 빠른 개발이 가능합니다.

```bash
# 개발 모드로 실행 (Hot Reload 활성화)
docker compose --profile dev up --build app-dev

# 백그라운드 실행
docker compose --profile dev up --build -d app-dev

# 로그 확인
docker compose logs -f app-dev

# 종료
docker compose --profile dev down
```

**개발 모드 특징:**
- ✅ 소스 코드 변경 시 자동 재시작 (약 5-15초)
- ✅ 컨테이너 재빌드 불필요 - `src/` 디렉토리가 volume으로 마운트됨
- ✅ Gradle 캐시 보존으로 빠른 재시작
- ✅ LiveReload 지원 (브라우저 자동 새로고침)

**주의사항:**
- `build.gradle` 또는 `settings.gradle` 수정 시 컨테이너 재시작 필요
- 의존성 추가 시 `--build` 플래그로 재빌드 필요

#### 🚀 프로덕션 모드

최적화된 JAR 파일을 사용하는 프로덕션 배포용 모드입니다.

```bash
# 프로덕션 모드로 실행
docker compose --profile production up --build app

# 백그라운드 실행
docker compose --profile production up -d app

# 종료
docker compose --profile production down
```

### 로컬 개발 환경 (Docker 없이)

```bash
# PostgreSQL만 Docker로 실행
docker compose up postgres -d

# Gradle로 애플리케이션 실행
./gradlew bootRun

# 또는 빌드 후 실행
./gradlew build
java -jar build/libs/MoheSpring-0.0.1-SNAPSHOT.jar
```

## 📚 API 문서

애플리케이션 실행 후:

- 장소 추천 계열 API(`/api/places/recommendations`, `/api/places/new`, `/api/places/popular`, `/api/places/current-time`, `/api/recommendations/contextual`)는 위도/경도 파라미터가 필수이며, 좌표 기준 15km 이내 데이터 70% + 30km 이내 데이터 30%를 혼합 후 거리/평점/리뷰 가중치를 적용합니다.
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/health

### 문서 & 빠른 링크
- [API 가이드 (한글)](API_GUIDE.md) - 컨트롤러별 엔드포인트와 권한 요약
- [BATCH_GUIDE.md](BATCH_GUIDE.md) - 배치 작업 설정 및 운영 팁
- [CLAUDE.md](CLAUDE.md) - Claude Code 사용 가이드 및 troubleshooting

### 대표 API 그룹
- **인증/온보딩**: `POST /api/auth/login`, `POST /api/auth/signup`, `POST /api/auth/verify-email`
- **사용자 & 활동** *(Bearer)*: `GET /api/user/profile`, `GET /api/user/recent-places`, `POST /api/bookmarks/toggle`
- **장소 탐색**: `GET /api/places`, `GET /api/places/search`, `GET /api/places/vector-search` *(Bearer)*
- **추천 서비스**:
  - `GET /api/recommendations/good-to-visit?lat={lat}&lon={lon}&limit={limit}` - 지금 가기 좋은 장소 (게스트 가능)
  - `GET /api/recommendations/enhanced` *(Bearer)* - MBTI 기반 개인화 추천
  - `GET /api/recommendations/contextual?lat={lat}&lon={lon}` - 컨텍스트 기반 추천
  - `GET /api/keyword-recommendations/by-keyword` *(Bearer)* - 키워드 기반 추천
- **날씨 정보**: `GET /api/weather/current?lat=37.5665&lon=126.9780` - 좌표 기반 현재 날씨 조회 (기상청 API)
- **주소 정보**: `GET /api/address/reverse?lat=37.5665&lon=126.9780` - 좌표를 주소로 변환 (Naver API)
- **관리자/데이터 관리** *(Bearer ADMIN)*: `POST /api/admin/place-management/check-availability`, `POST /api/place-enhancement/batch-enhance`, `POST /api/admin/similarity/calculate`
- **배치/동기화**: `POST /api/batch/jobs/place-collection`, `POST /api/batch/jobs/update-crawled-data`
- **한국 행정구역 API**: `GET /api/korean-regions/all`, `GET /api/korean-regions/dong-level`, `GET /api/korean-regions/search-locations`

### 배치 작업 지역 데이터 소스 (2025-11-14 업데이트)

배치 작업(`PlaceCollectionJob`)은 이제 **정부 API 기반 동적 지역 로딩**을 지원합니다:

**데이터 소스 우선순위:**
1. **정부 표준지역코드 API** (권장) - 실시간 행정구역 데이터 (3600+ 동/읍/면)
2. **FallbackRegionService** - API 장애 시 하드코딩된 1000+ 지역 데이터
3. **Legacy Enum** - `SeoulLocation`, `JejuLocation`, `YonginLocation` (Deprecated)

**설정 방법 (.env):**
```bash
# 정부 API 사용 (권장)
BATCH_LOCATION_USE_GOVERNMENT_API=true
BATCH_LOCATION_USE_LEGACY_ENUMS=false

# 정부 API 키 (공공데이터포털에서 발급)
GOVT_API_KEY=your_govt_api_key_here

# 긴급 폴백 모드 (API 서버 화재 등)
IS_GOV_SERVER_DOWN=N
```

**특징:**
- 24시간 자동 캐싱으로 API 호출 최소화
- 행정구역 변경 시 자동 반영
- 서울/경기/제주 외 전국 17개 시도 지원
- 배치 작업 시 지역 필터링 지원: `region=seoul`, `region=gyeonggi`, `region=jeju` 등

## 📍 Mock 위치 설정 (선택 사항)

개발/테스트 중 좌표 파라미터를 매번 넘기기 번거롭다면 환경 변수로 기본 좌표를 지정할 수 있습니다.

### 동작 방식
1. **API 요청에 lat/lon이 포함되어 있으면** → 항상 요청 값을 사용합니다.
2. **파라미터가 비어 있고 ENV 기본값이 설정되어 있으면** → ENV 값을 기본값으로 사용합니다.
3. **둘 다 없으면** → API가 좌표 파라미터를 요구합니다.

### 설정 방법

#### 방법 1: .env 파일 수정 (권장)
```bash
# 기본 좌표 지정 (요청에 좌표가 없을 때만 사용)
MOCK_LATITUDE=37.4979   # 강남역
MOCK_LONGITUDE=127.0276

# 기본 좌표 사용해제 (프로덕션 권장)
# MOCK_LATITUDE=
# MOCK_LONGITUDE=
```

#### 방법 2: application-docker.yml 수정
```yaml
mohe:
  location:
    default-latitude: ${MOCK_LATITUDE:#{null}}
    default-longitude: ${MOCK_LONGITUDE:#{null}}
```

### 주요 위치 좌표 참고

| 위치 | 위도 (latitude) | 경도 (longitude) | 설명 |
|------|----------------|-----------------|------|
| **서울 중구** | `37.5636` | `126.9976` | 명동, 시청 일대 |
| 강남역 | `37.4979` | `127.0276` | 강남 상권 중심지 |
| 홍대입구역 | `37.5563` | `126.9227` | 홍대 상권 중심지 |
| 서울역 | `37.5547` | `126.9707` | 서울역 일대 |
| 광화문 | `37.5760` | `126.9769` | 광화문, 종로 일대 |
| 여의도 | `37.5219` | `126.9245` | 여의도 금융가 |
| 잠실역 | `37.5133` | `127.1000` | 잠실 롯데월드 일대 |
| 신촌역 | `37.5559` | `126.9366` | 신촌 상권 중심지 |
| 이태원역 | `37.5344` | `126.9944` | 이태원 상권 중심지 |
| 건대입구역 | `37.5403` | `127.0695` | 건대 상권 중심지 |

### 위치 기반 API 동작 방식

위치 파라미터를 지원하는 API:
- `/api/recommendations/contextual` - 컨텍스트 기반 추천 (날씨, 시간, 위치)
- `/api/places/recommendations` - 일반 장소 추천
- `/api/places/popular` - 인기 장소
- `/api/places/new` - 신규 장소
- `/api/recommendations/current-time` - 현재 시간 기반 추천

**거리 기반 혼합 전략:**
- 15km 이내 데이터: 70%
- 30km 이내 데이터: 30%
- 벡터 검색 결과와 교집합하여 최종 추천

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
| `/api/batch/jobs/running` | GET | 실행 중인 배치 작업 목록 조회 (executi`onId, 상태, Step 진행률 포함) |
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
- [CLAUDE.md](CLAUDE.md) - Claude Code 사용 가이드 및 troubleshooting

## 📝 주요 변경사항

### 지금 가기 좋은 장소 추천 API (Good-to-Visit)
- **엔드포인트**: `GET /api/recommendations/good-to-visit`
- **필수 파라미터**: `lat` (위도), `lon` (경도), `limit` (기본값: 10, 최대: 20)
- **응답 구조 간소화**: 불필요한 `context`, `weatherCondition`, `timeContext`, `recommendation` 필드 제거
- **주소 필드 필수화**: 주소가 없는 장소는 자동으로 필터링
  - `shortAddress`: 구 + 동 (예: "강남구 역삼동")
  - `fullAddress`: 전체 도로명 주소
  - `location`: shortAddress와 동일 (하위 호환성)
- **거리 기반 혼합 전략**: 15km 이내 70% + 30km 이내 30%
- **Fallback 로직**:
  - 벡터 서버 다운 시 geo-weighted 후보를 그대로 사용
  - 지역 데이터 부족 시 전국 추천 장소 반환
- **영업시간 필터 제거**: 영업시간 정보가 없는 장소도 포함하여 더 많은 추천 제공

**응답 예시:**
```json
{
  "success": true,
  "data": [
    {
      "id": 123,
      "name": "카페 이름",
      "imageUrl": "/images/...",
      "images": ["..."],
      "rating": 4.5,
      "category": "카페",
      "distance": 1.2,
      "shortAddress": "강남구 역삼동",
      "fullAddress": "서울특별시 강남구 역삼동 123-45",
      "location": "강남구 역삼동"
    }
  ]
}
```

### Embedding 서비스 설정
- **포트 변경**: 기본 포트 8001 → 6000으로 변경
- **환경 변수**: `EMBEDDING_SERVICE_URL=http://localhost:6000`
- **모델**: kanana-nano-2.1b-embedding (1792 차원)
- **Fallback**: 서비스 다운 시 zero vector 반환하여 geo-weighted 추천 제공

### Place Description API
- 장소 상세 정보 API에서 **mohe_description만 반환**하도록 최적화
- `PlaceService.convertToSimplePlaceDto()` 메서드가 `mohe_description` 필드만 추출
- 불필요한 description 필드 제거로 API 응답 크기 감소

### 날씨 정보 API
- **WeatherController 추가**: 좌표 기반 실시간 날씨 조회 (`GET /api/weather/current`)
- **기상청 단기예보 API 통합**: 한국 좌표에 대해 정확한 날씨 정보 제공
- **위경도 → 격자 좌표 변환**: Lambert Conformal Conic 투영법 적용
- **OpenMeteo Fallback**: KMA API 키 미설정 시 또는 오류 시 자동 전환
- **10분 캐싱**: 성능 최적화 및 API 호출 제한 방지

### 주소 변환 API
- **Naver Reverse Geocoding 활성화**: 좌표를 정확한 도로명 주소로 변환
- **AddressController**: `GET /api/address/reverse` 엔드포인트
- **1시간 캐싱**: 빠른 응답 제공

## 👤 작성자

**Andrew Lim (임석현)**
- Email: sjsh1623@gmail.com

---

⭐ 이 프로젝트가 도움이 되었다면 Star를 눌러주세요!
