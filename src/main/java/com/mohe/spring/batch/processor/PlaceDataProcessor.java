package com.mohe.spring.batch.processor;

import com.mohe.spring.entity.Place;
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
 * Naver API와 Google API를 활용하여 풍부한 장소 정보를 수집합니다.</p>
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li><b>Naver API 호출</b>: 검색 쿼리로 장소 검색 (최대 5개)</li>
 *   <li><b>필터링</b>: 편의점, 마트, 약국 등 제외</li>
 *   <li><b>중복 체크</b>: 이미 DB에 존재하는 장소 스킵</li>
 *   <li><b>Google API 보강</b>: 평점, 리뷰 수 등 상세 정보 추가</li>
 *   <li><b>검증된 Place 반환</b>: null 반환 시 Writer로 전달되지 않음</li>
 * </ol>
 *
 * <h3>입력/출력</h3>
 * <ul>
 *   <li><b>Input</b>: String (검색 쿼리, 예: "강남구 카페")</li>
 *   <li><b>Output</b>: Place 엔티티 (저장할 장소만) 또는 null (필터링/중복)</li>
 * </ul>
 *
 * <h3>Null 반환 케이스</h3>
 * <p>다음 경우 null을 반환하여 해당 아이템을 스킵합니다:</p>
 * <ul>
 *   <li>Naver API에서 검색 결과가 없는 경우</li>
 *   <li>편의점, 마트 등 필터링 대상인 경우</li>
 *   <li>이미 DB에 존재하는 중복 장소인 경우</li>
 * </ul>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemProcessor
 * @see com.mohe.spring.service.PlaceDataCollectionService
 */
@Component
public class PlaceDataProcessor implements ItemProcessor<String, Place> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceDataProcessor.class);

    /** 장소 데이터 수집 비즈니스 로직을 담당하는 서비스 */
    private final PlaceDataCollectionService placeDataCollectionService;

    /**
     * PlaceDataProcessor 생성자
     *
     * @param placeDataCollectionService Naver/Google API 호출 및 필터링 담당 서비스
     */
    public PlaceDataProcessor(PlaceDataCollectionService placeDataCollectionService) {
        this.placeDataCollectionService = placeDataCollectionService;
    }

    /**
     * 검색 쿼리를 Place 엔티티로 변환 (ItemProcessor 인터페이스 구현)
     *
     * <p>이 메서드는 각 검색 쿼리에 대해 한 번씩 호출되며,
     * 외부 API 호출과 데이터 검증을 수행합니다.</p>
     *
     * <h3>단계별 처리</h3>
     * <pre>
     * 1. Naver API 호출 → 5개 장소 검색
     * 2. 첫 번째 장소만 처리 (효율성)
     * 3. 필터링 체크 (편의점/마트 제외)
     * 4. 중복 체크 (DB 조회)
     * 5. Google API 보강 (평점, 리뷰 수)
     * 6. 검증된 Place 반환
     * </pre>
     *
     * <p><b>처리량 제한:</b> API 비용 절감을 위해 쿼리당 1개 장소만 처리합니다.
     * 나머지 4개는 다음 배치 실행 시 다른 쿼리로 수집될 가능성이 높습니다.</p>
     *
     * <p><b>에러 처리:</b> API 호출 실패 시 로그 출력 후 null 반환하여
     * 배치 전체 실패를 방지합니다.</p>
     *
     * @param query 검색 쿼리 문자열 (예: "강남구 카페", "종로구 맛집")
     * @return 검증된 Place 엔티티 (필터링/중복 시 null)
     * @throws Exception 처리 중 발생할 수 있는 예외
     */
    @Override
    public Place process(String query) throws Exception {
        logger.info("🔄 Processing query: {}", query);

        // 1. Naver Local Search API로 장소 검색 (최대 5개)
        List<Place> places = placeDataCollectionService.fetchPlacesFromNaver(query, 5);

        if (places.isEmpty()) {
            logger.warn("⚠️ No places found for query: {}", query);
            return null; // null 반환 시 Writer로 전달되지 않음
        }

        // 2. 첫 번째 장소만 처리 (API 비용 절감 및 효율성)
        Place place = places.get(0);

        // 3. 필터링 체크 - 편의점, 마트, 약국 등 제외
        if (placeDataCollectionService.shouldFilterOutPlace(place)) {
            logger.debug("🚫 Filtered out place: {} (category: {})", place.getName(), place.getCategory());
            return null;
        }

        // 4. 중복 체크 - DB에 이미 존재하는 장소인지 확인
        if (placeDataCollectionService.isDuplicate(place)) {
            logger.debug("⚠️ Duplicate place skipped: {}", place.getName());
            return null;
        }

        // 5. Google Places API로 상세 정보 보강 (평점, 리뷰 수 등)
        placeDataCollectionService.enhanceWithGooglePlaces(place);

        logger.info("✅ Processed place: {} (rating: {})", place.getName(), place.getRating());
        return place;
    }
}
