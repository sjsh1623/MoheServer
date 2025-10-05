# Spring Batch 설정 가이드

## 📦 프로젝트 구조

```
src/main/java/com/mohe/spring/
├── batch/                          # Spring Batch 관련 코드
│   ├── job/                        # Job 설정
│   │   └── PlaceCollectionJobConfig.java
│   ├── reader/                     # ItemReader 구현
│   │   └── PlaceQueryReader.java
│   ├── processor/                  # ItemProcessor 구현
│   │   └── PlaceDataProcessor.java
│   └── writer/                     # ItemWriter 구현
│       └── PlaceDataWriter.java
├── config/
│   └── BatchConfiguration.java    # @EnableBatchProcessing
├── service/
│   ├── PlaceDataCollectionService.java  # 비즈니스 로직 (Naver API, Google API)
│   └── KoreanGovernmentApiService.java  # 지역 정보 API
├── controller/
│   └── BatchJobController.java    # Batch Job 실행 API
└── entity/
    └── Place.java                 # 장소 엔티티
```

## 🚀 Batch Job 실행 방법

### 1. API를 통한 수동 실행

```bash
# Place 수집 Batch Job 실행
curl -X POST http://localhost:8080/api/batch/jobs/place-collection
```

### 2. 응답 예시

```json
{
  "success": true,
  "data": {
    "status": "SUCCESS",
    "message": "Place Collection Job started successfully",
    "startTime": 1696502400000
  }
}
```

## 📊 Batch Job 흐름

### PlaceCollectionJob

```
1. PlaceQueryReader
   ├─ 정부 API에서 지역 정보 가져오기
   ├─ 카테고리 목록 준비
   └─ "지역명 + 카테고리" 조합 생성
         예: "강남구 카페", "종로구 맛집"

2. PlaceDataProcessor
   ├─ Naver API로 장소 검색
   ├─ 필터링 (편의점, 마트 제외)
   ├─ 중복 체크
   └─ Google API로 상세 정보 보강 (평점, 리뷰 수)

3. PlaceDataWriter
   └─ DB에 Place 저장 (10개씩 chunk 처리)
```

## 🔧 환경 변수 설정

`.env` 파일에 다음 API 키 설정 필요:

```bash
# Naver API
NAVER_CLIENT_ID=your_naver_client_id
NAVER_CLIENT_SECRET=your_naver_client_secret

# Google Places API (선택사항 - 평점 보강용)
GOOGLE_PLACES_API_KEY=your_google_api_key

# DB 설정
DB_USERNAME=mohe_user
DB_PASSWORD=your_db_password
```

## 📝 비즈니스 로직 서비스

### PlaceDataCollectionService

핵심 비즈니스 로직을 담당하는 서비스:

- `fetchPlacesFromNaver(query, count)`: Naver API 호출
- `enhanceWithGooglePlaces(place)`: Google API로 평점 보강
- `shouldFilterOutPlace(place)`: 편의점/마트 필터링
- `isDuplicate(place)`: 중복 체크
- `savePlace(place)`: Place 저장

**재사용 가능**: 다른 컨트롤러나 서비스에서도 주입받아 사용 가능

## 🗄️ Spring Batch 메타데이터 테이블

자동으로 생성되는 테이블:
- `BATCH_JOB_INSTANCE`: Job 인스턴스 정보
- `BATCH_JOB_EXECUTION`: Job 실행 이력
- `BATCH_STEP_EXECUTION`: Step 실행 이력
- `BATCH_JOB_EXECUTION_PARAMS`: Job 파라미터
- `BATCH_JOB_EXECUTION_CONTEXT`: Job 실행 컨텍스트
- `BATCH_STEP_EXECUTION_CONTEXT`: Step 실행 컨텍스트

## 🎯 다음 단계: 크롤링 통합 준비

Python 크롤링 서버와 통합할 때:

1. 새로운 Reader 생성: `PythonCrawlerReader`
2. API 엔드포인트 호출하여 크롤링 데이터 가져오기
3. 동일한 Processor/Writer 재사용 가능

예시:
```java
@Component
public class PythonCrawlerReader implements ItemReader<CrawledData> {
    // Python 서버 API 호출 로직
}
```

## 🛠️ 개발 팁

### Batch Job 디버깅

로그에서 다음 키워드 검색:
- `🔍` : 검색 쿼리
- `✅` : 성공
- `❌` : 에러
- `⚠️` : 경고

### Chunk Size 조정

`PlaceCollectionJobConfig.java`에서 chunk 크기 변경:

```java
.chunk(10, transactionManager) // 10 → 원하는 크기로 변경
```

## 📖 참고 자료

- Spring Batch 공식 문서: https://spring.io/projects/spring-batch
- Spring Batch 5.x (Spring Boot 3.x) 마이그레이션 가이드
