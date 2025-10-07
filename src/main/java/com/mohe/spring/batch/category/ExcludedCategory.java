package com.mohe.spring.batch.category;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 장소 수집 시 제외할 카테고리 Enum
 *
 * <p>Naver API 응답의 category 필드를 검사하여
 * 아래 카테고리에 해당하는 장소는 DB에 저장하지 않습니다.</p>
 *
 * <h3>제외 대상</h3>
 * <ul>
 *   <li><b>학원</b>: 입시학원, 어학원, 과외, 교습소 등</li>
 *   <li><b>병원</b>: 종합병원, 의원, 클리닉 등 의료시설</li>
 *   <li><b>보건소</b>: 공공 보건의료 기관</li>
 *   <li><b>요양원</b>: 노인요양원, 요양병원</li>
 *   <li><b>교회</b>: 기독교 예배 장소</li>
 *   <li><b>성당</b>: 천주교 예배 장소</li>
 *   <li><b>종교시설</b>: 사찰, 절, 성지, 기타 종교 관련 시설</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * String naverCategory = "음식점>한식>제육덮밥";
 * if (ExcludedCategory.shouldExclude(naverCategory)) {
 *     return null; // 제외
 * }
 * </pre>
 *
 * <h3>필터링 로직</h3>
 * <p>Naver API의 category 필드는 "대분류>중분류>소분류" 형식입니다.
 * 이 Enum의 키워드가 category 문자열에 포함되어 있으면 제외됩니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see com.mohe.spring.batch.category.SearchCategory
 */
public enum ExcludedCategory {

    /** 학원 (입시, 어학, 과외, 교습소 등) */
    ACADEMY("학원", Arrays.asList("학원", "교습소", "과외")),

    /** 병원 (종합병원, 의원, 클리닉) */
    HOSPITAL("병원", Arrays.asList("병원", "의원", "클리닉", "한의원")),

    /** 보건소 (공공 보건의료 기관) */
    PUBLIC_HEALTH_CENTER("보건소", Arrays.asList("보건소")),

    /** 요양원 (노인요양원, 요양병원) */
    NURSING_HOME("요양원", Arrays.asList("요양원", "요양병원")),

    /** 교회 (기독교 예배 장소) */
    CHURCH("교회", Arrays.asList("교회")),

    /** 성당 (천주교 예배 장소) */
    CATHEDRAL("성당", Arrays.asList("성당")),

    /** 종교시설 (사찰, 절, 성지 등) */
    RELIGIOUS_FACILITY("종교시설", Arrays.asList("사찰", "절", "성지", "교리", "기도원"));

    /** 카테고리 이름 */
    private final String name;

    /** 필터링 키워드 리스트 (이 중 하나라도 포함되면 제외) */
    private final List<String> keywords;

    /**
     * ExcludedCategory 생성자
     *
     * @param name 카테고리 이름
     * @param keywords 필터링 키워드 리스트
     */
    ExcludedCategory(String name, List<String> keywords) {
        this.name = name;
        this.keywords = keywords;
    }

    /**
     * 카테고리 이름 반환
     *
     * @return 카테고리 이름 (예: "학원", "병원")
     */
    public String getName() {
        return name;
    }

    /**
     * 필터링 키워드 리스트 반환
     *
     * @return 키워드 리스트
     */
    public List<String> getKeywords() {
        return keywords;
    }

    /**
     * 주어진 카테고리 문자열이 제외 대상인지 확인
     *
     * <h3>동작 방식</h3>
     * <ol>
     *   <li>모든 ExcludedCategory를 순회</li>
     *   <li>각 카테고리의 키워드 리스트를 검사</li>
     *   <li>키워드 중 하나라도 category에 포함되면 true 반환</li>
     * </ol>
     *
     * <h3>예시</h3>
     * <pre>
     * shouldExclude("교육>학원>입시학원"); // true (학원 포함)
     * shouldExclude("의료>병원>내과");   // true (병원 포함)
     * shouldExclude("음식점>카페>디저트"); // false (제외 대상 아님)
     * </pre>
     *
     * @param category Naver API 응답의 category 문자열
     * @return 제외 대상이면 true, 아니면 false
     */
    public static boolean shouldExclude(String category) {
        if (category == null || category.isEmpty()) {
            return false; // 카테고리 정보 없으면 제외하지 않음
        }

        // 모든 ExcludedCategory를 순회하며 키워드 매칭
        for (ExcludedCategory excluded : values()) {
            for (String keyword : excluded.keywords) {
                if (category.contains(keyword)) {
                    return true; // 제외 대상
                }
            }
        }

        return false; // 제외 대상 아님
    }

    /**
     * 제외 대상 카테고리 이름 리스트 반환
     *
     * <p>로깅이나 디버깅 시 유용합니다.</p>
     *
     * @return 제외 카테고리 이름 리스트 (예: ["학원", "병원", "보건소", ...])
     */
    public static List<String> getExcludedNames() {
        return Arrays.stream(values())
                .map(ExcludedCategory::getName)
                .collect(Collectors.toList());
    }

    /**
     * 모든 제외 키워드를 하나의 리스트로 반환
     *
     * <p>전체 필터링 키워드 목록을 확인할 때 유용합니다.</p>
     *
     * @return 모든 제외 키워드를 포함하는 리스트
     */
    public static List<String> getAllKeywords() {
        return Arrays.stream(values())
                .flatMap(excluded -> excluded.keywords.stream())
                .distinct()
                .collect(Collectors.toList());
    }
}
