# 새로운 지역 추가 가이드

이 가이드는 새로운 지역 Enum을 추가하는 방법을 설명합니다.

## 자동 등록 시스템

지역 Enum은 `LocationRegistry`를 통해 자동으로 등록되며, 배치 작업에서 사용할 수 있습니다.

## 새로운 지역 추가 방법 (예: 부산)

### 1단계: Location Enum 생성

`src/main/java/com/mohe/spring/batch/location/BusanLocation.java` 파일 생성:

```java
package com.mohe.spring.batch.location;

/**
 * 부산광역시 행정동 Enum
 */
public enum BusanLocation implements Location {
    // 해운대구
    BUSAN_HAEUNDAE_U_DONG("부산광역시", "해운대구", "우동"),
    BUSAN_HAEUNDAE_JUNG_1_DONG("부산광역시", "해운대구", "중1동"),
    BUSAN_HAEUNDAE_JUNG_2_DONG("부산광역시", "해운대구", "중2동"),
    // ... 더 많은 행정동 추가

    // 수영구
    BUSAN_SUYEONG_NAMCHEON_1_DONG("부산광역시", "수영구", "남천1동"),
    BUSAN_SUYEONG_NAMCHEON_2_DONG("부산광역시", "수영구", "남천2동");

    private final String city;      // "부산광역시"
    private final String district;  // "해운대구", "수영구", etc.
    private final String dong;      // 행정동 명

    BusanLocation(String city, String district, String dong) {
        this.city = city;
        this.district = district;
        this.dong = dong;
    }

    public String getCity() {
        return city;
    }

    public String getDistrict() {
        return district;
    }

    public String getDong() {
        return dong;
    }

    /**
     * 풀네임: 예) "부산광역시 해운대구 우동"
     */
    @Override
    public String getFullName() {
        return city + " " + district + " " + dong;
    }

    /**
     * 지역 코드 반환
     */
    @Override
    public String getRegionCode() {
        return "busan";
    }
}
```

### 2단계: LocationRegistry에 등록

`LocationRegistry.java`의 `registerAllLocations()` 메서드 수정:

```java
private void registerAllLocations() {
    logger.info("🗺️ Registering all location enums...");

    // 기존 지역들
    registerLocation("seoul", Arrays.asList(SeoulLocation.values()));
    registerLocation("jeju", Arrays.asList(JejuLocation.values()));
    registerLocation("yongin", Arrays.asList(YonginLocation.values()));

    // 🆕 부산 추가
    registerLocation("busan", Arrays.asList(BusanLocation.values()));

    logger.info("✅ Registered {} regions: {}", locationMap.size(), locationMap.keySet());
}
```

### 3단계: BatchJobController 문서 업데이트 (선택사항)

`BatchJobController.java`의 API 문서에 부산 추가:

```java
@PostMapping("/place-collection/{region}")
@Operation(
    summary = "장소 수집 배치 실행 (특정 지역)",
    description = "Naver API를 통해 특정 지역의 장소 데이터를 수집하여 DB에 저장합니다. " +
                  "지역: seoul (서울), jeju (제주), yongin (용인), busan (부산)"
)
```

## 완료!

이제 부산 지역이 자동으로 등록되었습니다.

### API 호출 예시

```bash
# 부산 지역만 수집
curl -X POST http://localhost:8080/api/batch/jobs/place-collection/busan

# 모든 지역 (서울, 제주, 용인, 부산) 수집
curl -X POST http://localhost:8080/api/batch/jobs/place-collection
```

### 시스템 동작 방식

1. **자동 인식**: `LocationRegistry`가 애플리케이션 시작 시 모든 지역 Enum을 등록
2. **로깅**: 등록된 지역 수와 코드를 로그로 출력
   ```
   🗺️ Registering all location enums...
     📍 Registered 'seoul': 424 locations
     📍 Registered 'jeju': 31 locations
     📍 Registered 'yongin': 32 locations
     📍 Registered 'busan': 50 locations
   ✅ Registered 4 regions: [seoul, jeju, yongin, busan]
   ```
3. **배치 작업**: `PlaceQueryReader`가 `LocationRegistry`에서 지역 정보를 가져와 검색 쿼리 생성
4. **API 처리**: 컨트롤러가 region 파라미터로 특정 지역만 처리하거나 전체 지역 처리

## 장점

- ✅ **코드 수정 최소화**: 하드코딩된 if 문 없음
- ✅ **확장성**: 새 지역 추가 시 2개 파일만 수정
- ✅ **유지보수성**: 중앙화된 등록 시스템
- ✅ **자동화**: 등록만 하면 모든 배치 작업에서 자동으로 사용 가능
