# MoheSpring 프로젝트 구조

## 📦 패키지 구조 (Layered Architecture)

```
src/main/java/com/mohe/spring/
│
├── 🎯 MoheSpringApplication.java         # 애플리케이션 진입점
│
├── 📁 batch/                              # Spring Batch 레이어
│   ├── job/                               # Job 설정
│   │   └── PlaceCollectionJobConfig.java
│   ├── reader/                            # ItemReader 구현
│   │   └── PlaceQueryReader.java
│   ├── processor/                         # ItemProcessor 구현
│   │   └── PlaceDataProcessor.java
│   └── writer/                            # ItemWriter 구현
│       └── PlaceDataWriter.java
│
├── 📁 config/                             # 설정 레이어
│   ├── BatchConfiguration.java            # Batch 설정
│   ├── SecurityConfig.java                # Security 설정
│   ├── OpenApiConfig.java                 # Swagger/OpenAPI 설정
│   └── ApplicationConfig.java             # 기타 설정
│
├── 📁 controller/                         # 프레젠테이션 레이어 (API)
│   ├── PlaceController.java               # 장소 API
│   ├── UserController.java                # 사용자 API
│   ├── AuthController.java                # 인증 API
│   ├── BookmarkController.java            # 북마크 API
│   ├── ActivityController.java            # 활동 API
│   ├── BatchJobController.java            # Batch Job 실행 API
│   └── KoreanRegionController.java        # 지역 정보 API
│
├── 📁 dto/                                # Data Transfer Objects
│   ├── request/                           # 요청 DTO
│   │   ├── SignupRequest.java
│   │   ├── LoginRequest.java
│   │   └── PlaceEnhancementRequest.java
│   ├── response/                          # 응답 DTO
│   │   ├── ApiResponse.java              # 공통 응답 래퍼
│   │   ├── PlaceDetailResponse.java
│   │   └── UserProfileResponse.java
│   └── common/                            # 공통 DTO
│       ├── ErrorCode.java
│       └── ErrorDetail.java
│
├── 📁 entity/                             # 도메인 모델 (JPA Entities)
│   ├── Place.java                         # 장소
│   ├── PlaceImage.java                    # 장소 이미지
│   ├── PlaceSimilarity.java               # 장소 유사도
│   ├── User.java                          # 사용자
│   ├── TempUser.java                      # 임시 사용자
│   ├── Bookmark.java                      # 북마크
│   ├── Activity.java                      # 활동
│   ├── Preference.java                    # 선호도
│   └── RefreshToken.java                  # 리프레시 토큰
│
├── 📁 repository/                         # 데이터 접근 레이어
│   ├── PlaceRepository.java
│   ├── PlaceImageRepository.java
│   ├── UserRepository.java
│   ├── BookmarkRepository.java
│   └── ActivityRepository.java
│
├── 📁 service/                            # 비즈니스 로직 레이어
│   ├── PlaceService.java                  # 장소 비즈니스 로직
│   ├── PlaceDataCollectionService.java    # 장소 데이터 수집 (Naver/Google API)
│   ├── UserService.java                   # 사용자 비즈니스 로직
│   ├── BookmarkService.java               # 북마크 비즈니스 로직
│   ├── RecommendationService.java         # 추천 알고리즘
│   ├── VectorSearchService.java           # 벡터 검색
│   ├── OpenAiService.java                 # OpenAI API 통합
│   ├── KeywordEmbeddingService.java       # 키워드 벡터 임베딩
│   ├── ImageGenerationService.java        # 이미지 생성
│   ├── KoreanGovernmentApiService.java    # 정부 API 통합
│   └── EmailService.java                  # 이메일 전송
│
├── 📁 security/                           # 보안 레이어
│   ├── JwtTokenProvider.java             # JWT 토큰 관리
│   ├── CustomUserDetailsService.java     # UserDetails 구현
│   ├── JwtAuthenticationFilter.java      # JWT 인증 필터
│   └── SecurityUtils.java                # 보안 유틸리티
│
└── 📁 exception/                          # 예외 처리 레이어
    ├── GlobalExceptionHandler.java        # 전역 예외 핸들러
    ├── BusinessException.java             # 비즈니스 예외
    └── ResourceNotFoundException.java     # 리소스 없음 예외
```

## 🏗️ 레이어별 책임

### 1. Presentation Layer (controller, dto)
- **책임**: HTTP 요청/응답 처리, 데이터 검증
- **의존성**: Service 레이어만 의존
- **규칙**: 비즈니스 로직 포함 금지

### 2. Business Layer (service)
- **책임**: 비즈니스 규칙, 트랜잭션 관리
- **의존성**: Repository, 외부 API Service
- **규칙**: Entity와 DTO 간 변환 담당

### 3. Data Access Layer (repository, entity)
- **책임**: 데이터베이스 CRUD
- **의존성**: JPA, DB
- **규칙**: Repository는 단순 데이터 접근만

### 4. Infrastructure Layer (config, security, batch)
- **책임**: 기술적 구현, 외부 시스템 통합
- **의존성**: Spring Framework, 외부 라이브러리
- **규칙**: 도메인 로직 포함 금지

## 📋 네이밍 컨벤션

### Controller
- `{Domain}Controller.java`
- 예: `PlaceController`, `UserController`

### Service
- `{Domain}Service.java` - 비즈니스 로직
- `{Domain}{Feature}Service.java` - 특정 기능
- 예: `PlaceService`, `PlaceDataCollectionService`

### Repository
- `{Entity}Repository.java`
- 예: `PlaceRepository`, `UserRepository`

### DTO
- Request: `{Action}Request.java`
- Response: `{Domain}Response.java`
- 예: `SignupRequest`, `PlaceDetailResponse`

### Entity
- 단수형 명사
- 예: `Place`, `User`, `Bookmark`

## 🚀 개발 가이드

### 새로운 기능 추가 시

1. **Entity 생성** (필요 시)
   ```java
   // entity/NewDomain.java
   @Entity
   @Table(name = "new_domains")
   public class NewDomain { ... }
   ```

2. **Repository 생성**
   ```java
   // repository/NewDomainRepository.java
   public interface NewDomainRepository extends JpaRepository<NewDomain, Long> { }
   ```

3. **Service 생성**
   ```java
   // service/NewDomainService.java
   @Service
   public class NewDomainService { ... }
   ```

4. **DTO 생성**
   ```java
   // dto/request/NewDomainRequest.java
   // dto/response/NewDomainResponse.java
   ```

5. **Controller 생성**
   ```java
   // controller/NewDomainController.java
   @RestController
   @RequestMapping("/api/new-domain")
   public class NewDomainController { ... }
   ```

### Batch Job 추가 시

1. **Reader 생성**: `batch/reader/NewDataReader.java`
2. **Processor 생성**: `batch/processor/NewDataProcessor.java`
3. **Writer 생성**: `batch/writer/NewDataWriter.java`
4. **Job Config 생성**: `batch/job/NewDataJobConfig.java`

## 📝 모범 사례

### DO ✅
- DTO와 Entity 분리 (절대 Entity를 API 응답으로 사용하지 말 것)
- Service 메서드는 단일 책임 원칙 준수
- Controller는 얇게 유지 (비즈니스 로직 금지)
- 예외는 GlobalExceptionHandler에서 처리
- 트랜잭션은 Service 레이어에서 관리

### DON'T ❌
- Controller에서 Repository 직접 호출
- Entity에 비즈니스 로직 포함
- Service 간 순환 의존성
- DTO 없이 Entity 직접 반환
- 하드코딩된 값 (환경변수 사용)

## 🔧 기술 스택

- **Framework**: Spring Boot 3.2.0
- **Language**: Java 21
- **Database**: PostgreSQL (production), H2 (test)
- **Batch**: Spring Batch 5.x
- **Security**: Spring Security + JWT
- **API Docs**: SpringDoc OpenAPI 3
- **Build**: Gradle 8.5

## 📖 관련 문서

- [BATCH_GUIDE.md](BATCH_GUIDE.md) - Spring Batch 사용 가이드
- [CLAUDE.md](CLAUDE.md) - 개발 가이드
- [README.md](README.md) - 프로젝트 소개
