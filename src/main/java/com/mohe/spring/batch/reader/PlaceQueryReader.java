package com.mohe.spring.batch.reader;

import com.mohe.spring.service.KoreanGovernmentApiService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 장소 검색 쿼리를 생성하는 Reader
 * <p>
 * 지역 정보 (정부 API) + 카테고리를 조합하여 검색 쿼리 생성
 * 예: "강남구 카페", "종로구 맛집" 등
 */
@Component
public class PlaceQueryReader implements ItemReader<String> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceQueryReader.class);

    private final KoreanGovernmentApiService governmentApiService;
    private List<String> searchQueries;
    private int currentIndex = 0;

    // 검색 카테고리 (편의점, 마트 제외)
    private static final List<String> SEARCH_CATEGORIES = Arrays.asList(
            "카페", "맛집", "데이트", "이색 체험", "공방", "박물관", "갤러리", "공원", "디저트"
    );

    public PlaceQueryReader(KoreanGovernmentApiService governmentApiService) {
        this.governmentApiService = governmentApiService;
    }

    /**
     * 첫 호출 시 검색 쿼리 목록 초기화
     */
    private void initializeQueries() {
        if (searchQueries == null) {
            searchQueries = new ArrayList<>();

            try {
                // 정부 API에서 지역 정보 가져오기
                List<String> regionNames = governmentApiService.fetchLocationNamesForSearch();
                logger.info("📍 정부 API에서 {}개 지역 정보 가져옴", regionNames.size());

                // 지역 + 카테고리 조합으로 검색 쿼리 생성
                for (String region : regionNames) {
                    for (String category : SEARCH_CATEGORIES) {
                        searchQueries.add(region + " " + category);
                    }
                }

                logger.info("✅ 총 {}개 검색 쿼리 생성 완료", searchQueries.size());

            } catch (Exception e) {
                logger.error("❌ 정부 API 호출 실패, 기본 쿼리로 대체", e);
                // Fallback: 기본 카테고리만 사용
                searchQueries.addAll(SEARCH_CATEGORIES);
            }

            currentIndex = 0;
        }
    }

    @Override
    public String read() {
        initializeQueries();

        if (currentIndex < searchQueries.size()) {
            String query = searchQueries.get(currentIndex);
            currentIndex++;
            logger.info("📖 Reading query {}/{}: {}", currentIndex, searchQueries.size(), query);
            return query;
        }

        // 모든 쿼리 읽기 완료
        return null;
    }
}
