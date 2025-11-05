# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Development Commands

### Gradle Tasks
```bash
# Build the application
./gradlew build

# Run tests (uses H2 in-memory database)
./gradlew test

# Run specific test class
./gradlew test --tests "com.mohe.spring.MoheSpringApplicationTests"

# Run tests with profiles
./gradlew test -Dspring.profiles.active=test

# Clean build
./gradlew clean build

# Run the application locally (default: local profile)
./gradlew bootRun

# Run with specific profile
./gradlew bootRun -Dspring.profiles.active=docker
```

### Docker Development
```bash
# Start all services (PostgreSQL, Embedding Server, Spring app)
docker compose up --build

# Start only PostgreSQL for local development
docker compose up postgres

# Start PostgreSQL and Embedding service (for AI features)
docker compose up postgres embedding

# Stop all services
docker compose down

# View logs
docker compose logs -f app
docker compose logs -f embedding
```

### Application URLs
- **Health Check**: http://localhost:8080/health
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Embedding API**: http://localhost:8001 (when running via Docker)

### Place Recommendation Logic
- `/api/places/recommendations`, `/api/places/new`, `/api/places/popular`, `/api/places/current-time`, `/api/recommendations/contextual` now require `latitude`/`longitude` query params.
- Each request pulls 70% of candidates from within 15km and 30% from within 30km of the provided coordinates (Haversine distance via native SQL) before final sorting.
- Authenticated users get the same geo-weighted pool re-ranked with their vector preference scores for hybrid personalization; `/api/recommendations/contextual` also injects weather/time text into the vector query.
- Guest contextual requests run the same contextual query through `VectorSearchService.vectorSearchPlaces` and intersect against the geo-mixed candidates.
- Popular endpoints reorder the blended set by `review_count DESC, rating DESC`; current-time endpoints feed the geo-mixed pool into the LLM prompt.
- Update or add tests by stubbing `PlaceService#getRecommendations(latitude, longitude)`/`getPopularPlaces(latitude, longitude, limit)` and passing the lat/lon params in MockMvc requests.
- When touching `/api/recommendations/contextual`, expect the service to: (1) call `PlaceService#getLocationWeightedPlaces`, (2) fetch weather/time via `WeatherService`, (3) build a contextual query string `"<user-query> | weather:<text> | time:<slot>"`, and (4) feed it to `VectorSearchService` (authenticated uses `searchWithVectorSimilarity`, guest uses `vectorSearchPlaces`). Any change to the blend or query format should keep CLAUDE/README/API_GUIDE in sync.

## Architecture Overview

### Core Design Patterns

**API Response Pattern**: All controllers use a standardized `ApiResponse<T>` wrapper for consistent responses. The pattern is:
```java
// Success response
ResponseEntity.ok(ApiResponse.success(data))

// Error response
ResponseEntity.badRequest().body(ApiResponse.error(code, message, path))

// Unauthorized
ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error(...))
```

**Security Architecture**: JWT-based stateless authentication with:
- Access tokens (1 hour expiry) for API calls
- Refresh tokens (30 days expiry) for token renewal
- Spring Security filter chain with custom JWT authentication filter
- Role-based access control with `@PreAuthorize("hasRole('USER')")` or `@PreAuthorize("hasRole('ADMIN')")`

**Database Layer**: JPA/Hibernate with:
- PostgreSQL (with pgvector extension) for production/docker
- H2 in-memory for tests (profile: `test`, automatic configuration)
- HikariCP connection pooling (10 max connections, 5 min idle)
- Flyway migrations for schema versioning (`src/main/resources/db/migration/V*.sql`)
- Vector similarity search support via pgvector

### Package Structure

- **batch/**: Spring Batch jobs for data collection and processing
  - `job/`: Job configurations (PlaceCollectionJob, UpdateCrawledDataJob, DistributedCrawlingJob)
  - `reader/`: Item readers for batch processing
  - `processor/`: Item processors for data transformation
  - `writer/`: Item writers for database persistence
  - `service/`: External API services (Naver, Kakao place APIs)
  - `location/`: Location registries (Seoul, Jeju, Yongin regions)
  - `category/`: Search categories and excluded categories
- **config/**: Spring configuration classes (Security, Batch, OpenAPI, LLM, Vector, Async)
- **controller/**: REST endpoints organized by domain (20+ controllers including Auth, User, Place, Recommendation, Batch)
- **dto/**: Data transfer objects with validation annotations
- **entity/**: JPA entities representing database tables
- **repository/**: Spring Data JPA repositories (with custom queries for vector search)
- **service/**: Business logic layer (including OpenAI integration for AI features)
- **security/**: JWT handling, user authentication, custom filters
- **exception/**: Global exception handler for consistent error responses

### Key Technical Details

**Swagger Integration**: Uses SpringDoc OpenAPI 3 with comprehensive Korean documentation. All controllers use `@SwaggerApiResponse` (aliased to avoid conflict with custom `ApiResponse` class).

**Spring Batch Architecture**: Large-scale data collection system using Spring Batch:
- **PlaceCollectionJob**: Collects place data from Naver/Kakao APIs by location and category
- **UpdateCrawledDataJob**: Enriches place data with crawled details, AI descriptions, and embeddings
- **DistributedCrawlingJob**: Distributed batch processing across multiple worker instances
- Batch jobs use chunk-oriented processing (configurable chunk size, default 50)
- Job execution tracking via Spring Batch metadata tables
- Batch controller at `/api/batch/jobs/*` for manual job triggering

**Embedding Service Integration**: FastAPI-based embedding service:
- Uses `kanana-nano-2.1b-embedding` model from Kakao Corp (https://huggingface.co/kakaocorp/kanana-nano-2.1b-embedding)
- Korean-optimized text embeddings (1792 dimensions)
- OpenAI-compatible API endpoints (`/v1/embeddings`)
- Generates keyword embeddings for place recommendations
- Vector embeddings stored in PostgreSQL with pgvector for similarity search
- Automatic model download during Docker build
- Falls back to default values if service is unavailable
- Service accessible at `http://embedding:2000` (Docker) or `http://localhost:2000` (host)

**MBTI-Based Recommendations**: Core business logic includes MBTI personality type matching for place recommendations. The `places` table includes MBTI scoring fields, and the recommendation algorithm considers user preferences and personality type. Advanced recommendation engine includes configurable weights for Jaccard/Cosine similarity, time decay, diversity, and scheduled similarity matrix updates.

**Multi-Step Registration**: Authentication flow supports:
1. Email signup → OTP verification → nickname/password setup
2. Temporary user storage during registration process
3. Email verification with 5-digit OTP codes

**Profile Management**: Users can set comprehensive preferences including MBTI type, age range, transportation method, and space preferences (workshop, exhibition, nature, etc.).

**Web Crawler Integration**: External Python crawler service for place data enrichment:
- Configured via `CRAWLER_SERVER_URL` (default: `http://localhost:5000`)
- Provides detailed place information not available via public APIs
- Batch jobs mark places as `crawler_found=true/false` and `ready=true/false` for data quality tracking

## Important Implementation Notes

**Language and Build System**:
- Primary language: **Java 21** (not Kotlin)
- Build tool: Gradle 8.5+ with Kotlin DSL (`build.gradle.kts`)
- Source code location: `src/main/java/com/mohe/spring/`
- Framework: Spring Boot 3.2.0

**Database Connection**:
- Docker profile: `postgres:5432` (internal Docker network hostname)
- Local profile: `localhost:16239` (external port mapped from Docker)
- Production: `DB_HOST:DB_PORT` (configurable via environment variables)
- Test profile: H2 in-memory database (automatic, no configuration needed)

**Environment Profiles**:
- `docker`: Containerized deployment with embedding service integration
- `local`: Local development with external PostgreSQL and embedding service
- `test`: Automated testing with H2 database (no external dependencies)

**Security Configuration**: Public endpoints (no authentication required):
- `/api/auth/**` - Authentication endpoints
- `/health` - Health check
- `/swagger-ui/**` - API documentation
- `/v3/api-docs/**` - OpenAPI specification

**External Integrations**: The application requires/supports:
- **Naver Place API** (required): Client ID/secret for place search (`NAVER_CLIENT_ID`, `NAVER_CLIENT_SECRET`)
- **Kakao Local API** (required): API key for place data (`KAKAO_API_KEY`)
- **Embedding Service** (required): FastAPI service for text embeddings (`EMBEDDING_SERVICE_URL`)
- **OpenAI API** (optional): AI description generation (`OPENAI_API_KEY`, `OPENAI_MODEL`)
- **Gemini API** (optional): Image generation (`GEMINI_API_KEY`, `GEMINI_BASE_URL`)
- **Korean Government APIs** (optional): Weather/region data (`KMA_SERVICE_KEY`, `GOVT_API_KEY`, `VWORLD_API_KEY`, `SGIS_API_KEY`)
- **Web Crawler** (optional): Python crawler service (`CRAWLER_SERVER_URL`)
- **Image Processor** (optional): Node.js image processing (`IMAGE_PROCESSOR_URL`)
- **Email SMTP** (optional): For OTP verification (`MAIL_USERNAME`, `MAIL_PASSWORD`)

**Environment Variables**: The `.env.example` file is the central control panel for all configuration:
- **Section 1**: Application basics (profile, port, log level)
- **Section 2**: Database connection (PostgreSQL, HikariCP pool settings)
- **Section 3**: JWT security (secret, token expiration)
- **Section 4**: External APIs (Kakao, Naver - required for place data)
- **Section 5**: Government APIs (optional - weather, regions, with fallback support)
- **Section 6**: AI/ML services (embedding, OpenAI, Gemini)
- **Section 7**: Web crawler (Python service for detailed place info)
- **Section 8**: Image processor (Node.js service for image optimization)
- **Section 9**: Email/SMTP (OTP verification)
- **Section 10**: Batch job configuration (chunk size, concurrency, scheduling)
- **Section 11**: Recommendation algorithm weights (highly tunable for personalization)
- **Section 12**: Cache settings (weather, region data)
- **Section 13**: JPA/Hibernate (DDL, SQL logging, Flyway)
- **Section 14**: Development/test (mock coordinates for testing)
- **Section 15**: Monitoring (Grafana, Prometheus)

All hardcoded values have been moved to environment variables for easy control.

**Batch Job Status Tracking**: Places have two status flags:
- `crawler_found`: Whether the web crawler successfully found the place
- `ready`: Whether the place is fully processed and ready for API consumption
- Failed places can be reprocessed by re-running the batch job

**Batch Job Control**: Monitor and control running batch jobs:
- GET `/api/batch/jobs/running` - List all running jobs with executionId, status, and step details
- POST `/api/batch/jobs/stop/{executionId}` - Stop specific job gracefully after current chunk
- POST `/api/batch/jobs/stop-all` - Stop all running jobs

## Common Issues and Solutions

### Batch Processing with Hibernate

**Problem**: `LazyInitializationException` when accessing collections in batch jobs
- **Cause**: Collections are accessed outside of Hibernate session
- **Solution**: Use custom ItemReader with two-step query approach:
  1. Load Place IDs page-by-page with pagination
  2. Fetch full entities individually with `@EntityGraph` for critical collections
  3. Force-load remaining collections with `.size()` calls

**Problem**: `MultipleBagFetchException` - "cannot simultaneously fetch multiple bags"
- **Cause**: Cannot use multiple `LEFT JOIN FETCH` or `@EntityGraph` for multiple List collections simultaneously
- **Solution**: Only use `@EntityGraph` for one critical collection (e.g., `descriptions`), then force-load others individually:
```java
Place place = placeRepository.findByIdWithCollections(id).orElse(null);
// Force-load collections individually
place.getImages().size();
place.getBusinessHours().size();
place.getSns().size();
place.getReviews().size();
```

**Problem**: `HHH90003004` - "firstResult/maxResults specified with collection fetch; applying in memory"
- **Cause**: Using `LEFT JOIN FETCH` or `@EntityGraph` with pagination causes Hibernate to load all data into memory
- **Solution**: Implement two-step query pattern:
  1. Query IDs only with pagination: `SELECT p.id FROM Place p WHERE ... ORDER BY p.id`
  2. Fetch entities one by one: `findByIdWithCollections(id)`

**Problem**: `NonSkippableReadException` - Method invocation failure in RepositoryItemReader
- **Cause**: `setArguments(List.of())` conflicts with method requiring Pageable parameter
- **Solution**: Remove `setArguments()` call or use custom ItemReader

**Example Implementation** (UpdateCrawledDataReader):
```java
// Step 1: Load IDs page-by-page
private void loadNextPageIds() {
    Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by("id").ascending());
    Page<Long> idsPage = placeRepository.findPlaceIdsForBatchProcessing(pageable);
    currentPageIds = new ArrayList<>(idsPage.getContent());
    hasMorePages = idsPage.hasNext();
    currentPage++;
}

// Step 2: Fetch entity with collections
public Place read() throws Exception {
    Long placeId = currentPageIds.get(currentIdIndex);
    Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

    if (place != null) {
        // Force-load collections to avoid LazyInitializationException
        place.getImages().size();
        place.getBusinessHours().size();
        place.getSns().size();
        place.getReviews().size();
    }
    return place;
}
```

**Repository Methods**:
```java
// Method 1: Return IDs only (efficient pagination)
@Query("""
    SELECT p.id FROM Place p
    WHERE (p.crawlerFound IS NULL OR p.crawlerFound = false)
    AND (p.ready IS NULL OR p.ready = false)
    ORDER BY p.id ASC
""")
Page<Long> findPlaceIdsForBatchProcessing(Pageable pageable);

// Method 2: Load single entity with descriptions
@EntityGraph(attributePaths = {"descriptions"})
@Query("SELECT p FROM Place p WHERE p.id = :id")
Optional<Place> findByIdWithCollections(@Param("id") Long id);
```

**Key Benefits**:
- **Memory Efficient**: Only loads 10 IDs at a time (configurable page size)
- **No Pagination Issues**: Avoids Hibernate's in-memory pagination warning
- **No MultipleBagFetchException**: Fetches collections separately
- **No LazyInitializationException**: All collections force-loaded in session

## Live Mode (실시간 데이터 처리)

### Overview

Live Mode는 사용자 조회 시점에 `ready=false`인 장소를 실시간으로 처리하는 기능입니다. 배치 작업 대신 사용자 요청에 따라 즉시 크롤링 → AI 요약 → 벡터화 → 이미지 저장을 수행합니다.

### Architecture

```
User Request → PlaceService.filterReady()
                    ↓
        [LIVE_MODE_ENABLED=true?]
                    ↓ YES
    LiveModeService.processPlaceRealtime(place)
                    ↓
        CompletableFuture (비동기 처리)
                    ↓
    ┌───────────────────────────────────┐
    │ Step 1: performCrawlingAndAI()    │
    │  - CrawlingService.crawlPlaceData │
    │  - OpenAI description generation  │
    │  - ImageService.downloadAndSave   │
    │  - Reviews, BusinessHours, SNS    │
    │  → crawler_found=true, ready=false│
    └───────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────┐
    │ Step 2: performVectorization()    │
    │  - EmbeddingClient.getEmbeddings  │
    │  - KeywordEmbeddingSaveService    │
    └───────────────────────────────────┘
                    ↓
    ┌───────────────────────────────────┐
    │ Step 3: place.setReady(true)      │
    │  - placeRepository.save()         │
    └───────────────────────────────────┘
                    ↓
        Return processed Place
```

### Configuration

**.env.example**:
```bash
# Live Mode 활성화 (true | false)
LIVE_MODE_ENABLED=false

# 처리 타임아웃 (밀리초)
LIVE_MODE_TIMEOUT=120000

# 캐시 TTL (초)
LIVE_MODE_CACHE_TTL=3600

# 캐시 최대 크기
LIVE_MODE_CACHE_MAX_SIZE=1000
```

**application.yml**:
```yaml
live:
  mode:
    enabled: ${LIVE_MODE_ENABLED:false}
    timeout: ${LIVE_MODE_TIMEOUT:120000}
    cache:
      ttl: ${LIVE_MODE_CACHE_TTL:3600}
      max-size: ${LIVE_MODE_CACHE_MAX_SIZE:1000}
```

### Key Components

#### 1. LiveModeService

**Location**: `src/main/java/com/mohe/spring/service/livemode/LiveModeService.java`

**Responsibilities**:
- 실시간으로 장소 데이터 완전 처리 (크롤링 + AI + 벡터화)
- Caffeine Cache를 사용한 중복 처리 방지
- CompletableFuture를 통한 비동기 처리 + 타임아웃 관리
- 배치 로직 재사용 (`UpdateCrawledDataJob` + `VectorEmbeddingJob`)

**Key Methods**:
```java
// 실시간 처리 진입점
public Place processPlaceRealtime(Place place)

// Step 1: 크롤링 + AI 요약 + 이미지
protected Place performCrawlingAndAI(Place place)

// Step 2: 벡터화
protected boolean performVectorization(Place place)

// 전체 파이프라인
protected Place performFullProcessing(Place place)
```

**Cache Strategy**:
- **Key**: `placeId`
- **Value**: `ProcessingStatus` (IN_PROGRESS, COMPLETED, FAILED)
- **TTL**: 3600초 (1시간, 설정 가능)
- **Max Size**: 1000개 (설정 가능)

**Timeout Handling**:
```java
CompletableFuture<Place> future = CompletableFuture.supplyAsync(() -> {...});
Place result = future.get(liveModeTimeout, TimeUnit.MILLISECONDS);
```
타임아웃 발생 시 부분 데이터 반환 (원본 Place 객체)

#### 2. ProcessingStatus Enum

**Location**: `src/main/java/com/mohe/spring/service/livemode/ProcessingStatus.java`

```java
public enum ProcessingStatus {
    IN_PROGRESS,   // 처리 중
    COMPLETED,     // 처리 완료
    FAILED         // 처리 실패
}
```

#### 3. PlaceService Integration

**Modification**: `PlaceService.filterReady()` 메서드

**Before**:
```java
private List<Place> filterReady(List<Place> places) {
    return places.stream()
        .filter(this::isReady)
        .collect(Collectors.toList());
}
```

**After**:
```java
private List<Place> filterReady(List<Place> places) {
    if (liveModeEnabled && liveModeService != null) {
        logger.info("🚀 Live Mode enabled - processing {} places", places.size());
        return places.stream()
            .map(place -> {
                if (!isReady(place)) {
                    return liveModeService.processPlaceRealtime(place);
                }
                return place;
            })
            .filter(this::isReady)
            .collect(Collectors.toList());
    }

    // 기존 방식
    return places.stream()
        .filter(this::isReady)
        .collect(Collectors.toList());
}
```

#### 4. PlaceRepository Queries

**New Query** (Live Mode용):
```java
/**
 * Live Mode용: ready 필터 제거
 * Live Mode 활성화 시 ready=false인 장소도 조회하여 실시간 처리
 */
@Query("""
    SELECT p FROM Place p
    WHERE (p.rating >= 0.0 OR p.rating IS NULL)
    ORDER BY p.rating DESC, p.name ASC
""")
Page<Place> findAllPlacesForLiveMode(Pageable pageable);
```

**Existing Query** (유지):
```java
@Query("""
    SELECT p FROM Place p
    WHERE (p.rating >= 0.0 OR p.rating IS NULL)
    AND p.ready = true
    ORDER BY p.rating DESC, p.name ASC
""")
Page<Place> findRecommendablePlaces(Pageable pageable);
```

### When to Use Live Mode

✅ **Use Cases**:
- 개발/테스트 환경에서 빠른 데이터 확인
- 소규모 데이터셋 (<1000개 장소)
- 배치 작업 스케줄 설정 전 초기 테스트

❌ **Avoid**:
- 프로덕션 환경 (비용, 성능, 안정성 이슈)
- 대규모 데이터셋 (>10,000개 장소)
- OpenAI/Embedding 서비스가 다운된 상태

### Performance Considerations

| 항목 | 배치 방식 | Live Mode |
|------|----------|-----------|
| 응답 시간 | ~100ms | 30초~2분 |
| OpenAI 비용 | 고정 (배치 1회) | 조회마다 증가 |
| 서버 부하 | 낮음 (스케줄링) | 높음 (동시 요청) |
| 데이터 신선도 | 배치 주기 | 실시간 |

### Troubleshooting

**1. LiveModeService not found**
```bash
# 확인
grep LIVE_MODE_ENABLED .env

# 해결
LIVE_MODE_ENABLED=true
```

**2. Processing timeout**
```
⏱️ Live mode timeout (120000 ms) for place: XXX
```
→ `LIVE_MODE_TIMEOUT=180000` (3분으로 증가)

**3. Crawling failed**
```
❌ Crawling failed for 'XXX' - not found by crawler (404)
```
→ 크롤러 서버 상태 확인: `curl http://localhost:5000/health`

**4. Vectorization failed**
```
⚠️ No valid embeddings returned for 'XXX'
```
→ Embedding 서비스 확인: `curl http://localhost:2000/health`

**5. Cache not working**
```
🎬 Starting real-time processing (should be cached)
```
→ 캐시 TTL 만료 또는 서버 재시작됨

### Monitoring

**Logs to Watch**:
```
🚀 LiveModeService initialized - timeout: 120000ms, cache TTL: 3600s
🚀 Live Mode enabled - processing 5 places
🎬 Starting real-time processing for place: 강남 카페 (ID: 123)
✅ Real-time processing completed for place: 강남 카페 (ready=true)
```

**Cache Hit Rate**:
```
⏳ Place 123 is already being processed by another request  ← IN_PROGRESS
✅ Place 123 already processed (cached), fetching from DB  ← COMPLETED
```

### Dependencies

**build.gradle**:
```gradle
// Caffeine Cache for Live Mode processing cache
implementation 'com.github.ben-manes.caffeine:caffeine:3.1.8'
```

**Required Services**:
- CrawlingService (Python crawler)
- OpenAiDescriptionService (OpenAI API)
- ImageService (Image processor)
- EmbeddingClient (Kanana embedding)
- KeywordEmbeddingSaveService (Vector storage)

### Migration Path

**From Batch to Live Mode**:
1. `.env`에서 `LIVE_MODE_ENABLED=true` 설정
2. 크롤러, OpenAI, Embedding 서비스 모두 실행 확인
3. 애플리케이션 재시작
4. 로그에서 `🚀 LiveModeService initialized` 확인
5. 테스트: `ready=false` 장소 조회 시 자동 처리 확인

**From Live Mode to Batch**:
1. `.env`에서 `LIVE_MODE_ENABLED=false` 설정
2. 배치 작업 스케줄 활성화:
   - `BATCH_SCHEDULING_ENABLED=true`
   - `BATCH_SCHEDULING_CRON=0 */1 * * * ?`
3. 애플리케이션 재시작
4. 배치 작업 자동 실행 확인
