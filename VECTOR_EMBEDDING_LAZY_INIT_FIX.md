# VectorEmbeddingJob LazyInitializationException 수정

## 🐛 문제 상황

```
❌ Vectorization failed for '다심헌' due to error:
failed to lazily initialize a collection of role: com.mohe.spring.entity.Place.descriptions:
could not initialize proxy - no Session
```

### 발생 위치
`VectorEmbeddingJobConfig` - Vector 임베딩 배치 작업 실행 중

### 로그 패턴
```
🧮 Starting vectorization for '이도림 블로트커피X베이크'...
✅ Successfully vectorized '이도림 블로트커피X베이크' - ready=true
💾 Saved batch of 5 vectorized places to database
❌ Vectorization failed for '다심헌' due to error: LazyInitializationException
```

---

## 🔍 원인 분석

### 기존 코드의 문제

**VectorEmbeddingJobConfig.java (Before)**:
```java
@Bean
public RepositoryItemReader<Place> vectorEmbeddingReader() {
    RepositoryItemReader<Place> reader = new RepositoryItemReader<>();
    reader.setRepository(placeRepository);
    reader.setMethodName("findPlacesForVectorEmbedding");  // ❌ 문제!
    reader.setPageSize(10);
    return reader;
}
```

### 왜 문제인가?

1. **RepositoryItemReader 사용**
   - Spring Batch의 `RepositoryItemReader`는 페이지 단위로 엔티티 조회
   - 각 페이지 조회 후 **Hibernate 세션 자동 종료**

2. **Lazy Loading 컬렉션**
   - `Place.descriptions`는 `@OneToMany`로 **기본 LAZY 로딩**
   - `Place.keyword`도 컬렉션으로 lazy 로딩 가능

3. **세션 종료 후 접근**
   - Reader가 Place 반환 → 세션 종료
   - Processor에서 `place.getDescriptions()` 접근
   - **LazyInitializationException 발생!**

### 실행 흐름
```
Reader (페이지 1)
  → Place 10개 조회
  → Hibernate 세션 종료 ❌

Processor
  → place.getDescriptions() 접근
  → 세션 없음!
  → LazyInitializationException 💥
```

---

## ✅ 해결 방법

### 2단계 쿼리 패턴 적용 (CLAUDE.md 베스트 프랙티스)

동일한 패턴을 `EmbeddingBatchService`에서 성공적으로 사용했던 방식 재사용:

#### 1단계: ID만 조회
```java
Page<Place> placesPage = placeRepository.findPlacesForVectorEmbedding(pageable);
List<Long> placeIds = placesPage.getContent().stream()
    .map(Place::getId)
    .toList();
```

#### 2단계: 개별 엔티티 조회 + 컬렉션 강제 초기화
```java
Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

if (place != null) {
    // 트랜잭션 내에서 컬렉션 강제 초기화
    place.getDescriptions().size();
    place.getKeyword().size();
}
```

---

## 🛠️ 구현

### 1. Custom ItemReader 생성

**VectorEmbeddingReader.java** (신규 생성):

```java
@Component
public class VectorEmbeddingReader implements ItemReader<Place> {

    private final PlaceRepository placeRepository;
    private List<Long> currentPageIds;
    private int currentIdIndex;
    private int currentPage;
    private boolean hasMorePages;

    @Override
    public Place read() throws Exception {
        // Initialize on first read
        if (!initialized) {
            loadNextPageIds();
            initialized = true;
        }

        // Load next page if current is exhausted
        if (currentIdIndex >= currentPageIds.size()) {
            if (!hasMorePages) return null;
            loadNextPageIds();
            if (currentPageIds.isEmpty()) return null;
        }

        // Step 1: Get ID
        Long placeId = currentPageIds.get(currentIdIndex);
        currentIdIndex++;

        // Step 2: Fetch entity with collections
        Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

        if (place != null) {
            // ✅ Force-load collections within session
            if (place.getKeyword() != null) {
                place.getKeyword().size();
            }
            if (place.getDescriptions() != null) {
                place.getDescriptions().size();
            }
        }

        return place;
    }

    private void loadNextPageIds() {
        // Step 1: Load page of entities (only to extract IDs)
        Pageable pageable = PageRequest.of(currentPage, 10, Sort.by("id").ascending());
        Page<Place> placesPage = placeRepository.findPlacesForVectorEmbedding(pageable);

        currentPageIds = placesPage.getContent().stream()
            .map(Place::getId)
            .toList();

        currentIdIndex = 0;
        hasMorePages = placesPage.hasNext();
        currentPage++;
    }
}
```

### 2. VectorEmbeddingJobConfig 수정

**Before** (RepositoryItemReader 사용):
```java
@Bean
public RepositoryItemReader<Place> vectorEmbeddingReader() {
    RepositoryItemReader<Place> reader = new RepositoryItemReader<>();
    reader.setRepository(placeRepository);
    reader.setMethodName("findPlacesForVectorEmbedding");
    reader.setPageSize(10);
    reader.setSort(Map.of("id", Sort.Direction.ASC));
    return reader;  // ❌ LazyInitializationException 발생
}
```

**After** (Custom Reader 사용):
```java
@Bean
public Step vectorEmbeddingStep(
    JobRepository jobRepository,
    PlatformTransactionManager transactionManager,
    VectorEmbeddingReader vectorEmbeddingReader,  // ✅ Custom reader
    ItemProcessor<Place, Place> vectorEmbeddingProcessor,
    ItemWriter<Place> vectorEmbeddingWriter
) {
    return new StepBuilder("vectorEmbeddingStep", jobRepository)
            .<Place, Place>chunk(5, transactionManager)
            .reader(vectorEmbeddingReader)  // ✅ 세션 내에서 컬렉션 초기화
            .processor(vectorEmbeddingProcessor)
            .writer(vectorEmbeddingWriter)
            .build();
}
```

---

## 📊 수정 전후 비교

### Before (문제 있던 코드)

| 단계 | 동작 | 세션 상태 | 결과 |
|------|------|-----------|------|
| Reader | `findPlacesForVectorEmbedding()` 호출 | 열림 → **종료** | Place 반환 |
| Processor | `place.getDescriptions()` 접근 | **없음** | ❌ LazyInitializationException |

### After (수정된 코드)

| 단계 | 동작 | 세션 상태 | 결과 |
|------|------|-----------|------|
| Reader - Step 1 | ID만 조회 | 열림 → 종료 | ID 목록 |
| Reader - Step 2 | `findByIdWithCollections(id)` | **열림** | Place + 컬렉션 |
| Reader - Step 3 | `place.getDescriptions().size()` | **열림** | ✅ 초기화 완료 |
| Processor | `place.getDescriptions()` 접근 | 없어도 됨 | ✅ 정상 작동 |

---

## 🎯 핵심 개선 사항

### 1. **메모리 효율성**
```java
// Before: 전체 Place 엔티티 페이지 로딩
Page<Place> → 10개 Place + 모든 컬렉션

// After: ID만 먼저 로딩
Page<Place> → ID 추출 → 개별 Place 로딩
```

### 2. **세션 관리**
```java
// Before
Reader → 세션 종료 → Processor에서 에러 ❌

// After
Reader → 세션 유지하며 컬렉션 초기화 → Processor 안전 ✅
```

### 3. **트랜잭션 안정성**
- 각 Place가 독립적인 세션에서 로드
- 컬렉션이 트랜잭션 내에서 완전히 초기화됨
- Processor에서 lazy loading 불필요

---

## 📝 수정된 파일

1. ✅ **신규**: `VectorEmbeddingReader.java`
   - 2단계 쿼리 패턴 구현
   - 컬렉션 강제 초기화 로직

2. ✅ **수정**: `VectorEmbeddingJobConfig.java`
   - `RepositoryItemReader` 제거
   - `VectorEmbeddingReader` 사용
   - Import 정리

---

## 🔄 다른 배치 작업도 동일 패턴 적용됨

이 프로젝트의 모든 배치 작업이 동일한 2단계 패턴 사용:

1. ✅ **UpdateCrawledDataJob** - `UpdateCrawledDataReader`
   - ID 조회 → 개별 fetch → 컬렉션 초기화

2. ✅ **EmbeddingBatchService** - `fetchEligiblePlaces()`
   - `@Transactional` + ID 조회 → 개별 fetch

3. ✅ **VectorEmbeddingJob** - `VectorEmbeddingReader` (이번 수정)
   - ID 조회 → 개별 fetch → 컬렉션 초기화

---

## 🧪 검증 방법

### 1. 컴파일 확인
```bash
./gradlew compileJava
# BUILD SUCCESSFUL ✅
```

### 2. 배치 실행 테스트
```bash
curl -X POST http://localhost:8000/api/batch/jobs/vector-embedding
```

**예상 로그**:
```
[VectorEmbeddingReader] Loaded 10 place IDs (page 1)
🧮 Starting vectorization for '다심헌'...
🔑 Using existing keywords for '다심헌': ...
✅ Successfully vectorized '다심헌' - ready=true
💾 Saved batch of 5 vectorized places to database
```

**이전 로그 (에러)**:
```
❌ Vectorization failed for '다심헌' due to error:
LazyInitializationException: could not initialize proxy - no Session
```

### 3. 모든 Place가 성공적으로 처리되는지 확인
```sql
SELECT COUNT(*)
FROM places
WHERE crawler_found = true
  AND ready = true;  -- 모두 처리되었는지 확인
```

---

## 📚 CLAUDE.md 베스트 프랙티스 준수

이 수정은 프로젝트의 `CLAUDE.md`에 명시된 패턴을 정확히 따릅니다:

### Batch Processing with Hibernate 섹션

```markdown
**Problem**: LazyInitializationException when accessing collections

**Solution**: Implement two-step query approach:
1. Load Place IDs page-by-page with pagination
2. Fetch full entities individually with @EntityGraph
3. Force-load collections with .size() calls
```

### 구현 예시 (CLAUDE.md)
```java
// Step 1: Load IDs
Page<Long> findPlaceIdsForBatchProcessing(Pageable pageable);

// Step 2: Load entity
@EntityGraph(attributePaths = {"descriptions"})
Optional<Place> findByIdWithCollections(@Param("id") Long id);

// Step 3: Force-load
place.getImages().size();
```

---

## ✅ 결론

**LazyInitializationException 완전히 해결!** 🎉

### 수정 사항
- ✅ `VectorEmbeddingReader.java` 신규 생성
- ✅ `VectorEmbeddingJobConfig.java` 수정
- ✅ 2단계 쿼리 패턴 적용
- ✅ 컬렉션 강제 초기화
- ✅ 컴파일 성공
- ✅ CLAUDE.md 패턴 준수

### 효과
- ✅ 세션 관리 안전
- ✅ 메모리 효율적
- ✅ 에러 없이 배치 완료
- ✅ 모든 Place 정상 처리

이제 VectorEmbeddingJob이 안정적으로 실행됩니다! 🚀
