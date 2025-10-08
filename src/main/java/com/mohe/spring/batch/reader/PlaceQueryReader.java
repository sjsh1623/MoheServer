package com.mohe.spring.batch.reader;

import com.mohe.spring.batch.category.SearchCategory;
import com.mohe.spring.batch.location.Location;
import com.mohe.spring.batch.location.LocationRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 장소 검색 쿼리를 생성하는 ItemReader
 *
 * <p>Spring Batch의 ItemReader 인터페이스를 구현하여
 * Naver API에 전달할 검색 쿼리를 순차적으로 제공합니다.</p>
 *
 * <h3>동작 방식</h3>
 * <ol>
 *   <li>LocationRegistry에서 등록된 모든 지역 정보 가져오기</li>
 *   <li>SearchCategory Enum의 모든 카테고리와 조합</li>
 *   <li>"지역 전체명 + 카테고리" 형태의 검색 쿼리 생성</li>
 *   <li>예시: "서울특별시 종로구 청운효자동 카페", "제주특별자치도 제주시 노형동 맛집" 등</li>
 * </ol>
 *
 * <h3>검색 카테고리</h3>
 * <p>SearchCategory Enum에 정의된 카테고리만 사용합니다:
 * 카페, 맛집, 레스토랑, 데이트, 바, 공방, 취미생활, 쇼핑</p>
 *
 * <h3>지역 자동 등록</h3>
 * <p>LocationRegistry에 등록된 모든 지역이 자동으로 처리됩니다.</p>
 * <p>새로운 지역을 추가하려면:</p>
 * <ol>
 *   <li>Location 인터페이스를 구현하는 새로운 Enum 생성 (예: BusanLocation)</li>
 *   <li>LocationRegistry의 registerAllLocations()에 등록</li>
 * </ol>
 *
 * <h3>Stateful Reader</h3>
 * <p>첫 번째 read() 호출 시 모든 검색 쿼리를 초기화하고,
 * 이후 호출마다 순차적으로 하나씩 반환합니다.
 * 모든 쿼리를 반환한 후에는 null을 반환하여 Step 종료를 알립니다.</p>
 *
 * <h3>Region 필터링</h3>
 * <p>setRegion() 메서드를 통해 특정 지역만 처리하도록 설정할 수 있습니다.
 * 설정하지 않으면 모든 지역을 처리합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemReader
 * @see com.mohe.spring.batch.location.Location
 * @see com.mohe.spring.batch.location.LocationRegistry
 * @see com.mohe.spring.batch.category.SearchCategory
 */
@Component
public class PlaceQueryReader implements ItemReader<String> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceQueryReader.class);

    /** 지역 정보를 관리하는 레지스트리 */
    private final LocationRegistry locationRegistry;

    /** 생성된 검색 쿼리 목록 (지역명 + 카테고리 조합) */
    private List<String> searchQueries;

    /** 현재 읽고 있는 쿼리의 인덱스 */
    private int currentIndex = 0;

    /** 처리할 지역 필터 (null이면 모든 지역 처리) */
    private String regionFilter = null;

    /**
     * PlaceQueryReader 생성자
     *
     * @param locationRegistry 지역 정보 레지스트리
     */
    public PlaceQueryReader(LocationRegistry locationRegistry) {
        this.locationRegistry = locationRegistry;
    }

    /**
     * 특정 지역만 처리하도록 필터 설정
     *
     * <p>Job 실행 전에 이 메서드를 호출하여 처리할 지역을 지정할 수 있습니다.</p>
     *
     * @param region 지역 코드 ("seoul", "jeju", "yongin", null이면 전체)
     */
    public void setRegion(String region) {
        this.regionFilter = region;
        logger.info("🌍 Region filter set to: {}", region != null ? region : "ALL");
    }

    /**
     * 검색 쿼리 목록 초기화 (Lazy Initialization)
     *
     * <p>첫 번째 read() 호출 시에만 실행되며, 다음 작업을 수행합니다:</p>
     * <ol>
     *   <li>Location Enum에서 모든 지역 가져오기 (필터 적용)</li>
     *   <li>SearchCategory Enum의 모든 카테고리 가져오기</li>
     *   <li>각 지역 전체명과 카테고리를 조합하여 쿼리 생성</li>
     *   <li>생성된 쿼리 개수 로깅</li>
     * </ol>
     */
    private void initializeQueries() {
        if (searchQueries == null) {
            searchQueries = new ArrayList<>();

            // 1. Location Enum에서 지역 정보 가져오기
            List<String> locations = getLocationsBasedOnFilter();
            logger.info("📍 총 {}개 지역 로드", locations.size());

            // 2. 각 지역과 모든 카테고리를 조합하여 검색 쿼리 생성
            for (String location : locations) {
                for (SearchCategory category : SearchCategory.values()) {
                    // "서울특별시 종로구 청운효자동 카페" 형식
                    String query = location + " " + category.getKeyword();
                    searchQueries.add(query);
                }
            }

            logger.info("✅ 총 {}개 검색 쿼리 생성 완료 (지역: {}, 카테고리: {})",
                    searchQueries.size(), locations.size(), SearchCategory.values().length);

            currentIndex = 0;
        }
    }

    /**
     * 지역 필터에 따라 처리할 지역 목록을 반환합니다
     *
     * <p>regionFilter 값에 따라 다음과 같이 동작합니다:</p>
     * <ul>
     *   <li><b>특정 지역 코드 (예: "seoul", "jeju", "yongin"):</b> 해당 지역만</li>
     *   <li><b>null:</b> LocationRegistry에 등록된 모든 지역</li>
     * </ul>
     *
     * <p>새로운 지역이 LocationRegistry에 등록되면 자동으로 처리됩니다.</p>
     *
     * @return 지역 전체명 리스트 (예: ["서울특별시 종로구 청운효자동", ...])
     */
    private List<String> getLocationsBasedOnFilter() {
        List<Location> locations;

        if (regionFilter == null) {
            // 모든 지역 가져오기
            locations = locationRegistry.getAllLocations();
            logger.info("📍 Processing ALL regions: {}", locationRegistry.getAvailableRegions());
        } else {
            // 특정 지역만 가져오기
            locations = locationRegistry.getLocations(regionFilter);
            if (locations.isEmpty()) {
                logger.warn("⚠️ No locations found for region: {}. Available regions: {}",
                           regionFilter, locationRegistry.getAvailableRegions());
            } else {
                logger.info("📍 Processing region: {}", regionFilter);
            }
        }

        return locations.stream()
                .map(Location::getFullName)
                .collect(Collectors.toList());
    }

    /**
     * 다음 검색 쿼리를 읽어옵니다 (ItemReader 인터페이스 구현)
     *
     * <p>Spring Batch는 이 메서드를 반복적으로 호출하여
     * 모든 아이템을 순차적으로 가져옵니다.</p>
     *
     * <h3>동작 흐름</h3>
     * <ol>
     *   <li>첫 호출: initializeQueries() 실행하여 모든 쿼리 생성</li>
     *   <li>쿼리 목록에서 현재 인덱스의 쿼리 반환</li>
     *   <li>인덱스 증가</li>
     *   <li>모든 쿼리 소진 시 null 반환 → Step 종료</li>
     * </ol>
     *
     * <p><b>예시 출력:</b></p>
     * <pre>
     * 📖 Reading query 1/450: 강남구 카페
     * 📖 Reading query 2/450: 강남구 맛집
     * ...
     * 📖 Reading query 450/450: 제주시 디저트
     * </pre>
     *
     * @return 다음 검색 쿼리 문자열, 더 이상 없으면 null
     * @throws Exception read 과정에서 발생할 수 있는 예외
     */
    @Override
    public String read() throws Exception {
        // 첫 호출 시 쿼리 목록 초기화
        initializeQueries();

        // 읽을 쿼리가 남아있는지 확인
        if (currentIndex < searchQueries.size()) {
            String query = searchQueries.get(currentIndex);
            currentIndex++;
            logger.info("📖 Reading query {}/{}: {}", currentIndex, searchQueries.size(), query);
            return query;
        }

        // 모든 쿼리 읽기 완료 - null 반환하여 Step 종료 신호
        logger.info("✅ 모든 검색 쿼리 읽기 완료 (총 {}개)", searchQueries.size());
        return null;
    }
}
