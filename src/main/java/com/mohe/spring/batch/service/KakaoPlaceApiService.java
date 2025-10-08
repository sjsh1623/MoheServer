package com.mohe.spring.batch.service;

import com.mohe.spring.entity.Place;

import java.util.List;

/**
 * Kakao Local API를 사용한 장소 검색 서비스 인터페이스
 *
 * @author Andrew Lim
 * @since 2.0
 */
public interface KakaoPlaceApiService {

    /**
     * Kakao Local API를 통해 키워드로 장소를 검색합니다.
     *
     * <p>page 1부터 20까지, size 15로 최대 300개의 장소를 수집합니다.</p>
     *
     * @param query 검색 쿼리 (예: "서울특별시 강남구 역삼동 카페")
     * @param maxResults 최대 결과 개수 (기본 300개)
     * @return 검색된 Place 엔티티 리스트
     */
    List<Place> searchPlaces(String query, int maxResults);
}
