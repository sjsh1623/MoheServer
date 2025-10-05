package com.mohe.spring.batch.processor;

import com.mohe.spring.entity.Place;
import com.mohe.spring.service.PlaceDataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 장소 데이터 처리 Processor
 *
 * 입력: 검색 쿼리 (예: "강남구 카페")
 * 처리:
 *   1. Naver API로 장소 검색
 *   2. Google API로 상세 정보 보강
 *   3. 필터링 (편의점, 마트 제외)
 *   4. 중복 체크
 * 출력: Place 엔티티 (저장할 장소만)
 */
@Component
public class PlaceDataProcessor implements ItemProcessor<String, Place> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceDataProcessor.class);

    private final PlaceDataCollectionService placeDataCollectionService;

    public PlaceDataProcessor(PlaceDataCollectionService placeDataCollectionService) {
        this.placeDataCollectionService = placeDataCollectionService;
    }

    @Override
    public Place process(String query) throws Exception {
        logger.info("🔄 Processing query: {}", query);

        // 1. Naver API로 장소 검색 (최대 5개)
        List<Place> places = placeDataCollectionService.fetchPlacesFromNaver(query, 5);

        if (places.isEmpty()) {
            logger.warn("⚠️ No places found for query: {}", query);
            return null; // null 반환 시 Writer로 전달되지 않음
        }

        // 2. 첫 번째 장소만 처리 (나머지는 다음 배치에서 처리)
        Place place = places.get(0);

        // 3. 필터링 체크
        if (placeDataCollectionService.shouldFilterOutPlace(place)) {
            logger.debug("🚫 Filtered out place: {} (category: {})", place.getName(), place.getCategory());
            return null;
        }

        // 4. 중복 체크
        if (placeDataCollectionService.isDuplicate(place)) {
            logger.debug("⚠️ Duplicate place skipped: {}", place.getName());
            return null;
        }

        // 5. Google API로 상세 정보 보강
        placeDataCollectionService.enhanceWithGooglePlaces(place);

        logger.info("✅ Processed place: {} (rating: {})", place.getName(), place.getRating());
        return place;
    }
}
