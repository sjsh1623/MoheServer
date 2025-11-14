package com.mohe.spring.batch.location;

import com.mohe.spring.dto.KoreanRegionDto;
import com.mohe.spring.service.KoreanGovernmentApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 정부 API 기반 지역 레지스트리
 *
 * <p>한국 정부 표준지역코드 API로부터 실시간으로 행정구역 데이터를 가져와서
 * 배치 작업에서 사용할 수 있도록 Location 객체로 변환합니다.</p>
 *
 * <h3>변경 사항 (2025-11-14)</h3>
 * <ul>
 *   <li>하드코딩된 Enum 방식에서 정부 API 기반 동적 로딩으로 전환</li>
 *   <li>행정구역 변경 시 자동 반영 (화재 복구 후 재가동)</li>
 *   <li>Fallback 지원: API 실패 시 하드코딩된 데이터 사용</li>
 * </ul>
 *
 * <h3>데이터 소스</h3>
 * <ul>
 *   <li><b>1차:</b> 정부 표준지역코드 API (24시간 캐싱)</li>
 *   <li><b>2차:</b> FallbackRegionService (하드코딩된 1000+ 지역)</li>
 *   <li><b>3차:</b> 기존 Enum (SeoulLocation, JejuLocation, YonginLocation) - Deprecated</li>
 * </ul>
 *
 * @author Andrew Lim
 * @since 1.0
 * @version 2.0 (Government API Integration)
 */
@Component
public class LocationRegistry {

    private static final Logger logger = LoggerFactory.getLogger(LocationRegistry.class);

    private final KoreanGovernmentApiService governmentApiService;

    @Value("${batch.location.use-government-api:true}")
    private boolean useGovernmentApi;

    @Value("${batch.location.use-legacy-enums:false}")
    private boolean useLegacyEnums;

    /**
     * 정부 API 데이터를 Location 객체로 캐싱
     */
    private List<Location> cachedLocations = null;

    /**
     * 지역 코드별 Location 매핑 (legacy 호환성)
     */
    private final Map<String, List<Location>> locationMap = new HashMap<>();

    @Autowired
    public LocationRegistry(KoreanGovernmentApiService governmentApiService) {
        this.governmentApiService = governmentApiService;
    }

    /**
     * 모든 지역을 로드합니다 (정부 API 또는 Legacy Enum)
     */
    private void loadLocations() {
        if (cachedLocations != null) {
            return; // 이미 로드됨
        }

        logger.info("🗺️ Loading locations (useGovernmentApi={}, useLegacyEnums={})",
                   useGovernmentApi, useLegacyEnums);

        if (useGovernmentApi) {
            loadFromGovernmentApi();
        } else if (useLegacyEnums) {
            loadFromLegacyEnums();
        } else {
            logger.warn("⚠️ Both government API and legacy enums are disabled. Loading government API as fallback.");
            loadFromGovernmentApi();
        }
    }

    /**
     * 정부 API로부터 동 단위 행정구역 데이터를 가져옵니다
     */
    private void loadFromGovernmentApi() {
        logger.info("🏛️ Loading locations from Korean Government API...");

        try {
            // 동 단위 행정구역만 가져오기 (검색용으로 적합)
            List<KoreanRegionDto> regions = governmentApiService.fetchDongLevelRegions();

            if (regions.isEmpty()) {
                logger.warn("⚠️ Government API returned no regions. Falling back to legacy enums.");
                loadFromLegacyEnums();
                return;
            }

            // KoreanRegionDto → Location 변환
            cachedLocations = regions.stream()
                .map(this::convertToLocation)
                .collect(Collectors.toList());

            // 지역 코드별로 분류 (시도 코드 기준)
            categorizeLocationsBySido();

            logger.info("✅ Loaded {} locations from Government API", cachedLocations.size());
            logger.info("📊 Regions: {}", locationMap.keySet());

        } catch (Exception e) {
            logger.error("❌ Failed to load locations from Government API", e);
            logger.info("🔄 Falling back to legacy enum locations...");
            loadFromLegacyEnums();
        }
    }

    /**
     * KoreanRegionDto를 Location으로 변환
     */
    private Location convertToLocation(KoreanRegionDto region) {
        return new Location() {
            @Override
            public String getFullName() {
                // "서울특별시 강남구 신사동" 형식
                return region.getSimpleLocationName();
            }

            @Override
            public String getRegionCode() {
                // 시도 코드 기반 지역 분류
                return getRegionCodeFromSido(region.getSidoCode());
            }

            @Override
            public String toString() {
                return getFullName();
            }
        };
    }

    /**
     * 시도 코드를 지역 코드로 변환 (배치 작업 호환성)
     */
    private String getRegionCodeFromSido(String sidoCode) {
        if (sidoCode == null) return "unknown";

        return switch (sidoCode) {
            case "11" -> "seoul";        // 서울특별시
            case "26" -> "busan";        // 부산광역시
            case "27" -> "daegu";        // 대구광역시
            case "28" -> "incheon";      // 인천광역시
            case "29" -> "gwangju";      // 광주광역시
            case "30" -> "daejeon";      // 대전광역시
            case "31" -> "ulsan";        // 울산광역시
            case "36" -> "sejong";       // 세종특별자치시
            case "41" -> "gyeonggi";     // 경기도
            case "42" -> "gangwon";      // 강원도
            case "43" -> "chungbuk";     // 충청북도
            case "44" -> "chungnam";     // 충청남도
            case "45" -> "jeonbuk";      // 전라북도
            case "46" -> "jeonnam";      // 전라남도
            case "47" -> "gyeongbuk";    // 경상북도
            case "48" -> "gyeongnam";    // 경상남도
            case "50" -> "jeju";         // 제주특별자치도
            default -> "unknown";
        };
    }

    /**
     * 시도 코드별로 Location 분류 (region 파라미터 지원용)
     */
    private void categorizeLocationsBySido() {
        locationMap.clear();

        for (Location location : cachedLocations) {
            String regionCode = location.getRegionCode();
            locationMap.computeIfAbsent(regionCode, k -> new ArrayList<>())
                       .add(location);
        }

        // 로그 출력
        locationMap.forEach((code, locations) -> {
            logger.info("  📍 Registered '{}': {} locations", code, locations.size());
        });
    }

    /**
     * Legacy Enum 방식으로 지역 로드 (Deprecated - 하위 호환성)
     */
    @Deprecated
    private void loadFromLegacyEnums() {
        logger.info("🗺️ Loading locations from legacy enums (Deprecated)...");

        cachedLocations = new ArrayList<>();

        // SeoulLocation 등록
        registerLegacyLocation("seoul", Arrays.asList(SeoulLocation.values()));

        // JejuLocation 등록
        registerLegacyLocation("jeju", Arrays.asList(JejuLocation.values()));

        // YonginLocation 등록
        registerLegacyLocation("yongin", Arrays.asList(YonginLocation.values()));

        logger.info("✅ Registered {} legacy regions: {}", locationMap.size(), locationMap.keySet());
    }

    /**
     * Legacy Enum 등록 (Deprecated)
     */
    @Deprecated
    private void registerLegacyLocation(String regionCode, List<? extends Location> locations) {
        List<Location> locationList = new ArrayList<>(locations);
        locationMap.put(regionCode.toLowerCase(), locationList);
        cachedLocations.addAll(locationList);
        logger.info("  📍 Registered legacy '{}': {} locations", regionCode, locationList.size());
    }

    /**
     * 특정 지역 코드에 해당하는 모든 Location을 반환합니다
     *
     * @param regionCode 지역 코드 (예: "seoul", "jeju", "busan")
     * @return Location 목록 (없으면 빈 리스트)
     */
    public List<Location> getLocations(String regionCode) {
        loadLocations(); // Lazy loading
        return locationMap.getOrDefault(regionCode.toLowerCase(), Collections.emptyList());
    }

    /**
     * 모든 지역의 Location을 반환합니다
     *
     * @return 전체 Location 목록
     */
    public List<Location> getAllLocations() {
        loadLocations(); // Lazy loading
        return new ArrayList<>(cachedLocations);
    }

    /**
     * 등록된 모든 지역 코드를 반환합니다
     *
     * @return 지역 코드 Set
     */
    public Set<String> getAvailableRegions() {
        loadLocations(); // Lazy loading
        return new HashSet<>(locationMap.keySet());
    }

    /**
     * 해당 지역 코드가 등록되어 있는지 확인합니다
     *
     * @param regionCode 확인할 지역 코드
     * @return 등록 여부
     */
    public boolean isRegistered(String regionCode) {
        loadLocations(); // Lazy loading
        return locationMap.containsKey(regionCode.toLowerCase());
    }

    /**
     * 캐시된 Location 데이터를 초기화합니다 (재로드 시 사용)
     */
    public void clearCache() {
        cachedLocations = null;
        locationMap.clear();
        governmentApiService.clearCache();
        logger.info("🗑️ LocationRegistry cache cleared");
    }

    /**
     * 현재 로드된 Location 개수를 반환합니다
     */
    public int getLocationCount() {
        loadLocations(); // Lazy loading
        return cachedLocations != null ? cachedLocations.size() : 0;
    }
}
