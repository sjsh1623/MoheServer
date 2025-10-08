package com.mohe.spring.batch.location;

/**
 * 지역 정보를 제공하는 인터페이스
 *
 * <p>모든 지역 Enum이 이 인터페이스를 구현하여 일관된 방식으로 지역 정보를 제공합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 */
public interface Location {

    /**
     * 지역의 전체 이름을 반환합니다.
     *
     * @return 지역 전체명 (예: "서울특별시 강남구 역삼동")
     */
    String getFullName();

    /**
     * 지역 코드를 반환합니다.
     *
     * @return 지역 코드 (예: "seoul", "jeju", "yongin")
     */
    String getRegionCode();
}
