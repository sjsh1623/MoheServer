package com.mohe.spring.batch.location;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 지역 Enum을 자동으로 등록하고 관리하는 레지스트리
 *
 * <p>새로운 지역 Enum을 추가하면 자동으로 인식하여 배치 작업에서 사용할 수 있습니다.</p>
 *
 * <h3>사용 방법</h3>
 * <ol>
 *   <li>새로운 지역 Enum 클래스 생성 (예: BusanLocation)</li>
 *   <li>Location 인터페이스 구현</li>
 *   <li>LocationRegistry에 등록 (registerLocation 호출)</li>
 * </ol>
 *
 * <h3>예시</h3>
 * <pre>
 * // BusanLocation.java
 * public enum BusanLocation implements Location {
 *     BUSAN_HAEUNDAE_U_DONG("부산광역시", "해운대구", "우동"),
 *     ...;
 *
 *     {@literal @}Override
 *     public String getRegionCode() {
 *         return "busan";
 *     }
 * }
 * </pre>
 *
 * @author Andrew Lim
 * @since 1.0
 */
@Component
public class LocationRegistry {

    private static final Logger logger = LoggerFactory.getLogger(LocationRegistry.class);

    /**
     * 지역 코드 → Location Enum 값 목록 매핑
     */
    private final Map<String, List<Location>> locationMap = new HashMap<>();

    /**
     * LocationRegistry 생성자
     *
     * <p>모든 지역 Enum을 자동으로 등록합니다.</p>
     */
    public LocationRegistry() {
        registerAllLocations();
    }

    /**
     * 모든 지역을 자동으로 등록합니다
     */
    private void registerAllLocations() {
        logger.info("🗺️ Registering all location enums...");

        // SeoulLocation 등록
        registerLocation("seoul", Arrays.asList(SeoulLocation.values()));

        // JejuLocation 등록
        registerLocation("jeju", Arrays.asList(JejuLocation.values()));

        // YonginLocation 등록
        registerLocation("yongin", Arrays.asList(YonginLocation.values()));

        // 새로운 지역을 추가하려면 여기에 등록하세요
        // 예: registerLocation("busan", Arrays.asList(BusanLocation.values()));

        logger.info("✅ Registered {} regions: {}", locationMap.size(), locationMap.keySet());
    }

    /**
     * 특정 지역 코드로 Location 목록을 등록합니다
     *
     * @param regionCode 지역 코드 (예: "seoul")
     * @param locations Location 배열
     */
    private void registerLocation(String regionCode, List<? extends Location> locations) {
        List<Location> locationList = new ArrayList<>(locations);
        locationMap.put(regionCode.toLowerCase(), locationList);
        logger.info("  📍 Registered '{}': {} locations", regionCode, locationList.size());
    }

    /**
     * 특정 지역 코드에 해당하는 모든 Location을 반환합니다
     *
     * @param regionCode 지역 코드 (예: "seoul", "jeju")
     * @return Location 목록 (없으면 빈 리스트)
     */
    public List<Location> getLocations(String regionCode) {
        return locationMap.getOrDefault(regionCode.toLowerCase(), Collections.emptyList());
    }

    /**
     * 모든 지역의 Location을 반환합니다
     *
     * @return 전체 Location 목록
     */
    public List<Location> getAllLocations() {
        List<Location> allLocations = new ArrayList<>();
        locationMap.values().forEach(allLocations::addAll);
        return allLocations;
    }

    /**
     * 등록된 모든 지역 코드를 반환합니다
     *
     * @return 지역 코드 Set
     */
    public Set<String> getAvailableRegions() {
        return new HashSet<>(locationMap.keySet());
    }

    /**
     * 해당 지역 코드가 등록되어 있는지 확인합니다
     *
     * @param regionCode 확인할 지역 코드
     * @return 등록 여부
     */
    public boolean isRegistered(String regionCode) {
        return locationMap.containsKey(regionCode.toLowerCase());
    }
}
