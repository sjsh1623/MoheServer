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

## Async Batch Processing (비동기 병렬 처리)

### Overview

The batch crawling process has been optimized with **AsyncItemProcessor** and **AsyncItemWriter** to enable parallel processing. This improves throughput by 5-10x compared to sequential processing.

### Architecture

```
┌─────────────────────────────────────────────────────┐
│ UpdateCrawledDataStep (Async Mode)                  │
├─────────────────────────────────────────────────────┤
│ Chunk Size: 20 (configurable)                       │
│ Thread Pool: 10-20 threads (configurable)           │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ ItemReader (Sequential)                             │
│ - Reads Place entities from DB                      │
│ - Returns List<Place> (chunk of 20)                 │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ AsyncItemProcessor (Parallel - 10 threads)          │
│ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐   │
│ │Thread 1 │ │Thread 2 │ │Thread 3 │ │Thread N │   │
│ └─────────┘ └─────────┘ └─────────┘ └─────────┘   │
│ Each thread executes:                               │
│ 1. Crawling (20-30s)                                │
│ 2. OpenAI description (3-5s)                        │
│ 3. Image download (2-5s)                            │
│ → Returns Future<Place>                             │
└─────────────────────────────────────────────────────┘
         ↓
┌─────────────────────────────────────────────────────┐
│ AsyncItemWriter (Parallel Write)                    │
│ - Waits for all Future<Place> to complete           │
│ - Writes to DB in batch                             │
└─────────────────────────────────────────────────────┘
```

### Configuration

**.env Settings**:
```bash
# Batch chunk size (items per chunk)
BATCH_CHUNK_SIZE=20

# Thread pool configuration (크롤러 서버 부하 방지)
BATCH_ASYNC_CORE_POOL_SIZE=5       # Minimum threads (권장: 5)
BATCH_ASYNC_MAX_POOL_SIZE=10       # Maximum threads (권장: 10)
BATCH_ASYNC_QUEUE_CAPACITY=100     # Queue size

# Crawler timeout (크롤링 타임아웃 - 분 단위)
CRAWLER_TIMEOUT_MINUTES=30         # 30분 (기본: 15분에서 증가)

# Database connection pool (must be >= max thread pool)
DB_HIKARI_MAX_POOL_SIZE=30         # Max DB connections
DB_HIKARI_MIN_IDLE=10              # Min idle connections
```

**application.yml**:
```yaml
batch:
  chunk-size: ${BATCH_CHUNK_SIZE:20}
  async:
    core-pool-size: ${BATCH_ASYNC_CORE_POOL_SIZE:5}
    max-pool-size: ${BATCH_ASYNC_MAX_POOL_SIZE:10}
    queue-capacity: ${BATCH_ASYNC_QUEUE_CAPACITY:100}

crawler:
  base-url: ${CRAWLER_SERVER_URL:http://localhost:4000}
  timeout-minutes: ${CRAWLER_TIMEOUT_MINUTES:30}
```

### Performance Comparison

| Configuration | Processing Time (1000 places) | Throughput | 크롤러 부하 |
|---------------|------------------------------|------------|------------|
| **Sequential (old)** | 8 hours 20 min | 2 places/min | 낮음 |
| **Async (5 threads)** | 100 minutes | 10 places/min | 낮음 ✅ |
| **Async (10 threads)** | 50 minutes | 20 places/min | 중간 |
| **Async (20 threads)** | 25 minutes | 40 places/min | 높음 ⚠️ |

**권장 설정**: 5-10 스레드 (크롤러 서버가 동시 요청을 많이 처리하면 타임아웃 발생)

**Performance Formula**:
```
Time per place (sequential) = 30s (crawling) + 5s (OpenAI) + 5s (images) = 40s
With 10 threads: 40s / 10 = 4s per place
1000 places = 1000 * 4s / 60 = 66 minutes (theoretical)
Actual: ~50 minutes (due to chunk overhead and I/O wait)
```

### Code Implementation

**UpdateCrawledDataJobConfig.java**:
```java
@Bean(name = "batchTaskExecutor")
public TaskExecutor batchTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);      // 10
    executor.setMaxPoolSize(maxPoolSize);        // 20
    executor.setQueueCapacity(queueCapacity);    // 100
    executor.setThreadNamePrefix("batch-async-");
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(60);
    executor.initialize();
    return executor;
}

@Bean
public Step updateCrawledDataStep(..., TaskExecutor batchTaskExecutor) {
    // AsyncItemProcessor setup
    AsyncItemProcessor<Place, Place> asyncItemProcessor = new AsyncItemProcessor<>();
    asyncItemProcessor.setDelegate(placeProcessor);
    asyncItemProcessor.setTaskExecutor(batchTaskExecutor);

    // AsyncItemWriter setup
    AsyncItemWriter<Place> asyncItemWriter = new AsyncItemWriter<>();
    asyncItemWriter.setDelegate(placeWriter);

    return new StepBuilder("updateCrawledDataStep", jobRepository)
        .<Place, Future<Place>>chunk(chunkSize, transactionManager)
        .reader(placeReader)
        .processor(asyncItemProcessor)  // ← Parallel execution
        .writer(asyncItemWriter)         // ← Async writes
        .faultTolerant()
        .skip(Exception.class)
        .skipLimit(Integer.MAX_VALUE)
        .build();
}
```

### Dependencies

**build.gradle**:
```gradle
// Spring Batch Integration (for AsyncItemProcessor and AsyncItemWriter)
implementation 'org.springframework.batch:spring-batch-integration:5.1.0'
implementation 'org.springframework.integration:spring-integration-core:6.2.0'
```

### Tuning Guidelines

**1. Thread Pool Sizing**:
- **Core Pool Size**: Should match typical load (10 for steady state)
- **Max Pool Size**: Should handle peak load (20 for bursts)
- **Formula**: `max-pool-size = (target throughput × avg processing time) / chunk size`
- Example: `(40 places/min × 40s) / 20 = 13.3 → use 15-20`

**2. Chunk Size**:
- **Too small** (<10): High overhead, frequent DB commits
- **Too large** (>50): Long wait times, memory pressure
- **Optimal**: 20-30 for this workload

**3. Database Connection Pool**:
- **Rule**: `HikariCP max-pool-size >= thread pool max-pool-size + 10`
- Each thread needs 1 connection, plus extra for other queries
- Example: 20 threads + 10 overhead = 30 connections

**4. Queue Capacity**:
- **Purpose**: Buffer for when all threads are busy
- **Too small**: Tasks rejected under load
- **Too large**: Memory pressure, long wait times
- **Optimal**: 5-10× chunk size (100 for chunk size 20)

### Monitoring

**Logs to Watch**:
```
🚀 Batch TaskExecutor initialized: core=10, max=20, queue=100
🔧 Async batch step configured: chunkSize=20
🔍 Starting crawl for 'Place Name' with query: '...'
✅ Successfully crawled 'Place Name' - Reviews: 50, Images: 5, ...
💾 [15/20] Saved place 'Place Name' (ID: 123, crawler_found=true, ready=false)
✅ Successfully saved batch: 20/20 places written to database
```

**Metrics**:
- **Thread utilization**: Check active threads via logs
- **Queue size**: Monitor for backlog
- **DB connection pool**: Watch HikariCP metrics
- **Processing time**: Track time per chunk

### Troubleshooting

**Issue 1: "❌ Crawling failed - null response from crawler"**
```
원인: 크롤러 타임아웃 또는 서버 부하
해결책:
1. CRAWLER_TIMEOUT_MINUTES=30 → 45 (타임아웃 증가)
2. BATCH_ASYNC_MAX_POOL_SIZE=10 → 5 (스레드 감소)
3. 크롤러 서버 로그 확인: 메모리 부족, Selenium 오류 등
```

**Issue 2: "HikariPool exhausted"**
```
Cause: DB connection pool too small
Solution: Increase DB_HIKARI_MAX_POOL_SIZE to >= BATCH_ASYNC_MAX_POOL_SIZE + 10
```

**Issue 3: "OutOfMemoryError"**
```
Cause: Too many threads or too large chunk size
Solution: Reduce BATCH_ASYNC_MAX_POOL_SIZE or BATCH_CHUNK_SIZE
```

**Issue 4: "RejectedExecutionException"**
```
Cause: Queue full, threads saturated
Solution: Increase BATCH_ASYNC_QUEUE_CAPACITY or reduce load
```

**Issue 5: "java.util.concurrent.TimeoutException"**
```
원인: WebClient 응답 타임아웃 (15분 → 30분으로 증가됨)
해결책: 크롤러 서버 성능 향상 또는 동시 요청 수 감소
```

**Issue 6: "ObjectOptimisticLockingFailureException - Batch update returned unexpected row count"**
```
오류 메시지:
org.springframework.orm.ObjectOptimisticLockingFailureException:
Batch update returned unexpected row count from update [6];
actual row count: 0; expected: 1;
statement executed: delete from place_descriptions where id=?

추가 오류:
org.springframework.transaction.UnexpectedRollbackException:
Transaction silently rolled back because it has been marked as rollback-only

원인:
- Hibernate의 orphanRemoval=true와 detached 엔티티 충돌
- 비동기 병렬 처리에서 엔티티가 여러 스레드에서 수정됨
- saveAndFlush() 호출 시 Hibernate가 orphan 삭제를 시도하지만
  detached 상태에서 컬렉션 추적 실패
- 예외 발생 시 트랜잭션이 rollback-only로 마킹되어 전체 배치 실패

해결책: (이미 적용됨)
1. Writer에서 항상 fresh entity를 DB에서 로드
2. 컬렉션 clear() → flush() → 새 데이터 추가
3. Spring Batch skip policy로 예외 스킵 및 noRollback 설정

코드 예시:
// Writer: 항상 fresh entity 사용
Place freshPlace = placeRepository.findById(place.getId()).orElseThrow();
freshPlace.getDescriptions().clear();
placeRepository.flush();  // Clear orphans immediately
updatePlaceFields(freshPlace, place);
placeRepository.saveAndFlush(freshPlace);

// Step: Skip policy 설정
.faultTolerant()
.skip(ObjectOptimisticLockingFailureException.class)
.skip(StaleStateException.class)
.skipLimit(Integer.MAX_VALUE)
.noRollback(ObjectOptimisticLockingFailureException.class)
.noRollback(StaleStateException.class)
```

### Best Practices

1. **Start Conservative**: Begin with 5 threads, increase gradually
2. **Monitor Resources**: Watch CPU, memory, DB connections
3. **Test Under Load**: Verify behavior with 100+ items
4. **Fail-Safe**: Use `faultTolerant()` with proper skip limits
5. **Graceful Shutdown**: `setWaitForTasksToCompleteOnShutdown(true)`

### Future Optimizations

1. **Partitioning**: Split work across multiple servers (100x faster)
2. **Crawler Load Balancing**: Multiple crawler instances
3. **OpenAI Batch API**: Use batch endpoint (50% cost reduction)
4. **Connection Pooling**: Tune for higher concurrency
