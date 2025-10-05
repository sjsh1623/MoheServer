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
 * 장소 검색 쿼리를 생성하는 ItemReader
 *
 * <p>Spring Batch의 ItemReader 인터페이스를 구현하여
 * Naver API에 전달할 검색 쿼리를 순차적으로 제공합니다.</p>
 *
 * <h3>동작 방식</h3>
 * <ol>
 *   <li>정부 공공데이터 API에서 한국의 모든 지역명 가져오기 (시/군/구 단위)</li>
 *   <li>미리 정의된 카테고리 목록과 조합</li>
 *   <li>"지역명 + 카테고리" 형태의 검색 쿼리 생성</li>
 *   <li>예시: "강남구 카페", "종로구 맛집", "제주시 관광지" 등</li>
 * </ol>
 *
 * <h3>검색 카테고리</h3>
 * <p>편의점, 마트, 약국 등 일반적인 생활시설은 제외하고,
 * 문화/여가/체험 중심의 장소만 포함합니다.</p>
 *
 * <h3>Stateful Reader</h3>
 * <p>첫 번째 read() 호출 시 모든 검색 쿼리를 초기화하고,
 * 이후 호출마다 순차적으로 하나씩 반환합니다.
 * 모든 쿼리를 반환한 후에는 null을 반환하여 Step 종료를 알립니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.item.ItemReader
 */
@Component
public class PlaceQueryReader implements ItemReader<String> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceQueryReader.class);

    private final KoreanGovernmentApiService governmentApiService;

    /** 생성된 검색 쿼리 목록 (지역명 + 카테고리 조합) */
    private List<String> searchQueries;

    /** 현재 읽고 있는 쿼리의 인덱스 */
    private int currentIndex = 0;

    /**
     * 검색 대상 카테고리 목록
     *
     * <p><b>포함:</b> 카페, 맛집, 문화시설, 체험 공간 등</p>
     * <p><b>제외:</b> 편의점, 마트, 약국 등 일반 생활시설</p>
     */
    private static final List<String> SEARCH_CATEGORIES = Arrays.asList(
            "카페", "맛집", "데이트", "이색 체험", "공방",
            "박물관", "갤러리", "공원", "디저트"
    );

    /**
     * PlaceQueryReader 생성자
     *
     * @param governmentApiService 정부 공공데이터 API 서비스 (지역 정보 조회용)
     */
    public PlaceQueryReader(KoreanGovernmentApiService governmentApiService) {
        this.governmentApiService = governmentApiService;
    }

    /**
     * 검색 쿼리 목록 초기화 (Lazy Initialization)
     *
     * <p>첫 번째 read() 호출 시에만 실행되며, 다음 작업을 수행합니다:</p>
     * <ol>
     *   <li>정부 API에서 모든 지역명 가져오기</li>
     *   <li>각 지역과 카테고리를 조합하여 쿼리 생성</li>
     *   <li>생성된 쿼리 개수 로깅</li>
     * </ol>
     *
     * <p><b>Fallback 처리:</b> 정부 API 호출 실패 시,
     * 지역명 없이 카테고리만 사용하여 전국 검색 수행</p>
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
