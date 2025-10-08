package com.mohe.spring.batch.processor;

import com.mohe.spring.batch.category.ExcludedCategory;
import com.mohe.spring.batch.service.NaverPlaceApiService;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.PlaceDataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 장소 데이터 처리 ItemProcessor
 *
 * <p>검색 쿼리를 입력받아 실제 장소 데이터로 변환하는 핵심 처리 로직입니다.
 * Naver API를 직접 호출하여 장소 정보를 수집합니다.</p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li><b>Naver API 호출</b>: NaverPlaceApiService를 통해 검색 쿼리로 장소 검색 (최대 5개)</li>
 *   <li><b>필터링</b>: ExcludedCategory를 사용하여 학원, 병원, 종교시설 등 제외</li>
 *   <li><b>중복 체크</b>: 이미 DB에 존재하는 장소 스킵 (roadAddress 기준)</li>
 *   <li><b>검증된 Place 반환</b>: null 반환 시 Writer로 전달되지 않음</li>
 * </ol>
 *
 * <h3>입력/출력</h3>
 * <ul>
 *   <li><b>Input</b>: String (검색 쿼리, 예: "서울특별시 강남구 역삼동 카페")</li>
 *   <li><b>Output</b>: Place 엔티티 (저장할 장소만) 또는 null (필터링/중복)</li>
 * </ul>
 *
 * <h3>Null 반환 케이스</h3>
 * <p>다음 경우 null을 반환하여 해당 아이템을 스킵합니다:</p>
 * <ul>
 *   <li>Naver API에서 검색 결과가 없는 경우</li>
 *   <li>학원, 병원 등 ExcludedCategory에 해당하는 경우</li>
 *   <li>이미 DB에 존재하는 중복 장소인 경우</li>
 * </ul>
 *
 * <h3>제외 카테고리</h3>
 * <p>ExcludedCategory Enum에 정의된 다음 카테고리는 자동으로 필터링됩니다:</p>
 * <ul>
 *   <li>학원 (입시학원, 어학원, 과외, 교습소)</li>
 *   <li>병원 (종합병원, 의원, 클리닉, 한의원)</li>
 *   <li>보건소</li>
 *   <li>요양원 (노인요양원, 요양병원)</li>
 *   <li>교회</li>
 *   <li>성당</li>
 *   <li>종교시설 (사찰, 절, 성지, 기도원)</li>
 * </ul>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemProcessor
 * @see com.mohe.spring.batch.service.NaverPlaceApiService
 * @see com.mohe.spring.batch.category.ExcludedCategory
 */
@Component
public class PlaceDataProcessor implements ItemProcessor<String, Place> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceDataProcessor.class);

    /** Naver Place API 호출 서비스 */
    private final NaverPlaceApiService naverPlaceApiService;

    /** Place 엔티티 Repository (중복 체크용) */
    private final PlaceRepository placeRepository;

    /** Place 저장 서비스 */
    private final PlaceDataCollectionService placeDataCollectionService;

    /**
     * PlaceDataProcessor 생성자
     *
     * @param naverPlaceApiService Naver API 호출 담당 서비스
     * @param placeRepository Place 엔티티 저장소 (중복 체크용)
     * @param placeDataCollectionService Place 저장 서비스
     */
    public PlaceDataProcessor(NaverPlaceApiService naverPlaceApiService,
                              PlaceRepository placeRepository,
                              PlaceDataCollectionService placeDataCollectionService) {
        this.naverPlaceApiService = naverPlaceApiService;
        this.placeRepository = placeRepository;
        this.placeDataCollectionService = placeDataCollectionService;
    }

    /**
     * 검색 쿼리를 Place 엔티티로 변환 (ItemProcessor 인터페이스 구현)
     *
     * <p>이 메서드는 각 검색 쿼리에 대해 한 번씩 호출되며,
     * Naver API 호출과 데이터 검증을 수행합니다.</p>
     *
     * <h3>단계별 처리</h3>
     * <pre>
     * 1. NaverPlaceApiService를 통해 Naver API 호출 → 최대 5개 장소 검색
     * 2. 첫 번째 장소만 처리 (효율성 및 중복 최소화)
     * 3. ExcludedCategory로 필터링 (학원, 병원 등 제외)
     * 4. 중복 체크 (DB에 동일 roadAddress 존재 여부)
     * 5. 검증된 Place 반환
     * </pre>
     *
     * <h3>필터링 로직</h3>
     * <p>ExcludedCategory.shouldExclude() 메서드를 사용하여
     * Naver API 응답의 category 필드를 검사합니다.</p>
     *
     * <p><b>예시:</b></p>
     * <pre>
     * category = "교육>학원>입시학원" → 제외 (학원 키워드 포함)
     * category = "의료>병원>내과"     → 제외 (병원 키워드 포함)
     * category = "음식점>카페>디저트" → 통과 (제외 대상 아님)
     * </pre>
     *
     * <h3>중복 체크 로직</h3>
     * <p>roadAddress를 기준으로 DB에 이미 존재하는 장소인지 확인합니다.
     * 동일한 주소의 장소가 이미 있으면 스킵합니다.</p>
     *
     * <p><b>에러 처리:</b> API 호출 실패 시 로그 출력 후 null 반환하여
     * 배치 전체 실패를 방지합니다.</p>
     *
     * @param query 검색 쿼리 문자열 (예: "서울특별시 종로구 청운효자동 카페")
     * @return 검증된 Place 엔티티 (필터링/중복 시 null)
     * @throws Exception 처리 중 발생할 수 있는 예외
     */
    @Override
    public Place process(String query) throws Exception {
        logger.info("🔄 Processing query: {}", query);

        try {
            // API Rate Limit 방지 - 요청 사이에 짧은 딜레이 추가
            Thread.sleep(100); // 100ms 딜레이 (초당 10개 요청으로 제한)

            // 1. Naver API를 통해 장소 검색 (최대 50개)
            List<Place> places = naverPlaceApiService.searchPlaces(query, 50);

            if (places.isEmpty()) {
                logger.warn("⚠️ No places found for query: {}", query);
                return null; // null 반환 시 Writer로 전달되지 않음
            }

            logger.info("📦 Found {} places for query: {}", places.size(), query);

            // 2. 모든 장소를 처리하고 직접 저장 (필터링 및 중복 체크)
            int savedCount = 0;
            int skippedCount = 0;
            int filteredCount = 0;

            for (Place place : places) {
                try {
                    // 3. ExcludedCategory로 필터링 - 학원, 병원, 종교시설 등 제외
                    if (ExcludedCategory.shouldExclude(place.getCategory())) {
                        logger.debug("🚫 Filtered out place: {} (category: {})",
                                place.getName(), place.getCategory());
                        filteredCount++;
                        continue;
                    }

                    // 4. 중복 체크 - DB에 동일 roadAddress가 존재하는지 확인
                    if (place.getRoadAddress() != null && !place.getRoadAddress().isEmpty()) {
                        boolean exists = placeRepository.existsByRoadAddress(place.getRoadAddress());
                        if (exists) {
                            logger.debug("⚠️ Duplicate place skipped: {} (address: {})",
                                    place.getName(), place.getRoadAddress());
                            skippedCount++;
                            continue;
                        }
                    }

                    // 5. PlaceDescription 추가
                    PlaceDescription description = new PlaceDescription();
                    description.setPlace(place);
                    description.setSearchQuery(query);
                    place.getDescriptions().add(description);

                    // 6. 직접 저장
                    placeDataCollectionService.savePlace(place);
                    logger.info("✅ Saved place: {} [{}] at {}",
                            place.getName(), place.getCategory(), place.getRoadAddress());
                    savedCount++;

                } catch (Exception e) {
                    logger.error("❌ Failed to save place: {} - {}", place.getName(), e.getMessage());
                }
            }

            logger.info("📊 Query '{}' - Total: {}, Saved: {}, Skipped: {}, Filtered: {}",
                    query, places.size(), savedCount, skippedCount, filteredCount);

            // Writer로 전달하지 않고 Processor에서 직접 처리했으므로 null 반환
            return null;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("❌ Thread interrupted while processing query: {}", query, e);
            return null;
        } catch (Exception e) {
            logger.error("❌ Failed to process query: {}", query, e);
            return null; // 에러 발생 시 null 반환하여 배치 계속 진행
        }
    }
}

