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
- **service/**: Business logic layer (including OllamaService for AI integration)
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
- Service accessible at `http://embedding:8000` (Docker) or `http://localhost:8001` (host)

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
- **Google Places API** (optional): API key for enhanced place data (`GOOGLE_PLACES_API_KEY`)
- **OpenAI API** (optional): Text generation (`OPENAI_API_KEY`)
- **Gemini API** (optional): Image generation (`GEMINI_API_KEY`)
- **Redis** (optional): Token storage (`REDIS_HOST`, `REDIS_PORT`, `REDIS_PASSWORD`)
- **Web Crawler** (optional): Python crawler service (`CRAWLER_SERVER_URL`)
- **Email SMTP** (optional): For OTP verification (`MAIL_USERNAME`, `MAIL_PASSWORD`)

**Environment Variables**: The `.env.example` file contains all available configuration options:
- Database configuration (PostgreSQL connection details)
- JWT secret (minimum 64 characters required)
- API keys for external services
- Embedding service configuration (default: `http://embedding:8000`)
- Batch job settings (chunk size, concurrency, scheduling)
- Recommendation algorithm weights (Jaccard, Cosine, MBTI, time decay)
- Image storage configuration (local vs remote)

**Batch Job Status Tracking**: Places have two status flags:
- `crawler_found`: Whether the web crawler successfully found the place
- `ready`: Whether the place is fully processed and ready for API consumption
- Failed places can be reprocessed by re-running the batch job