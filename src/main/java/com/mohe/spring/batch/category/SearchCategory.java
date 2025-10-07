package com.mohe.spring.batch.category;

/**
 * 장소 검색 카테고리 Enum
 *
 * <p>Naver Local Search API 호출 시 사용되는 검색 카테고리입니다.
 * 지역(city + dong)과 조합하여 검색 쿼리를 생성합니다.</p>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * String query = "서울특별시 강남구 " + SearchCategory.CAFE.getKeyword();
 * // 결과: "서울특별시 강남구 카페"
 * </pre>
 *
 * <h3>검색 전략</h3>
 * <ul>
 *   <li>카페: 독립 카페, 프랜차이즈 카페, 디저트 카페 등</li>
 *   <li>맛집: 일반 음식점 (모든 종류)</li>
 *   <li>레스토랑: 고급 음식점, 정찬 레스토랑</li>
 *   <li>데이트: 데이트 장소로 인기 있는 곳</li>
 *   <li>바: 바, 펍, 와인바 등</li>
 *   <li>공방: 체험 공방, 수공예 공방</li>
 *   <li>취미생활: 취미 활동 관련 장소</li>
 *   <li>쇼핑: 쇼핑몰, 상점, 마켓 등</li>
 * </ul>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see com.mohe.spring.batch.category.ExcludedCategory
 */
public enum SearchCategory {

    /** 카페 (독립 카페, 프랜차이즈 포함) */
    CAFE("카페"),

    /** 맛집 (모든 종류의 음식점) */
    RESTAURANT("맛집"),

    /** 레스토랑 (고급 음식점, 정찬) */
    FINE_DINING("레스토랑"),

    /** 데이트 장소 */
    DATE("데이트"),

    /** 바, 펍, 와인바 */
    BAR("바"),

    /** 체험 공방, 수공예 */
    WORKSHOP("공방"),

    /** 취미 활동 관련 장소 */
    HOBBY("취미생활"),

    /** 쇼핑 장소 (쇼핑몰, 마켓 등) */
    SHOPPING("쇼핑");

    /** 검색 키워드 */
    private final String keyword;

    /**
     * SearchCategory 생성자
     *
     * @param keyword 검색 키워드
     */
    SearchCategory(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 검색 키워드 반환
     *
     * @return 검색 키워드 (예: "카페", "맛집")
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 전체 카테고리 수 반환
     *
     * @return 카테고리 개수
     */
    public static int getTotalCategories() {
        return values().length;
    }

    /**
     * 키워드로 SearchCategory 찾기
     *
     * @param keyword 검색 키워드
     * @return 일치하는 SearchCategory, 없으면 null
     */
    public static SearchCategory fromKeyword(String keyword) {
        for (SearchCategory category : values()) {
            if (category.keyword.equals(keyword)) {
                return category;
            }
        }
        return null;
    }
}
