package com.mohe.spring.batch.service;

import com.mohe.spring.entity.Place;

import java.util.List;

/**
 * Naver Place API 호출 서비스 인터페이스
 *
 * @deprecated Kakao Local API로 교체되었습니다. {@link KakaoPlaceApiService}를 사용하세요.
 *
 * <p>Naver Local Search API를 호출하여 장소 데이터를 검색하고,
 * 응답을 Place 엔티티로 변환하는 기능을 정의합니다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>Naver Local Search API 호출</li>
 *   <li>검색 결과를 Place 엔티티로 변환</li>
 *   <li>페이징 처리 (display, start 파라미터)</li>
 *   <li>정렬 옵션 지원 (sort=comment)</li>
 * </ul>
 *
 * <h3>API 스펙</h3>
 * <ul>
 *   <li><b>Endpoint:</b> https://openapi.naver.com/v1/search/local.json</li>
 *   <li><b>Method:</b> GET</li>
 *   <li><b>Headers:</b>
 *     <ul>
 *       <li>X-Naver-Client-Id: {clientId}</li>
 *       <li>X-Naver-Client-Secret: {clientSecret}</li>
 *     </ul>
 *   </li>
 *   <li><b>Query Parameters:</b>
 *     <ul>
 *       <li>query: 검색어 (예: "서울특별시 종로구 청운효자동 카페")</li>
 *       <li>display: 결과 개수 (기본 5, 최대 5)</li>
 *       <li>start: 시작 위치 (기본 1)</li>
 *       <li>sort: 정렬 기준 (comment: 리뷰 개수 순)</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * <h3>응답 필드 매핑</h3>
 * <pre>
 * Naver API Response → Place Entity
 * ─────────────────────────────────
 * title             → name (HTML 태그 제거)
 * category          → category
 * roadAddress       → roadAddress
 * mapx              → longitude
 * mapy              → latitude
 * </pre>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see com.mohe.spring.batch.service.impl.NaverPlaceApiServiceImpl
 * @see KakaoPlaceApiService
 */
@Deprecated
public interface NaverPlaceApiService {

    /**
     * Naver Local Search API를 호출하여 장소를 검색합니다
     *
     * @deprecated Kakao Local API로 교체되었습니다. {@link KakaoPlaceApiService#searchPlaces(String, int)}를 사용하세요.
     *
     * <p>검색 쿼리를 기반으로 Naver API를 호출하고,
     * 응답 데이터를 Place 엔티티 리스트로 변환하여 반환합니다.</p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>검색 쿼리 인코딩 (UTF-8)</li>
     *   <li>Naver API HTTP GET 요청 (헤더에 clientId, clientSecret 포함)</li>
     *   <li>JSON 응답 파싱</li>
     *   <li>각 item을 Place 엔티티로 변환:
     *     <ul>
     *       <li>title에서 HTML 태그 제거 (Jsoup 사용)</li>
     *       <li>mapx, mapy를 BigDecimal로 변환 (좌표 정규화)</li>
     *       <li>searchQuery 저장</li>
     *     </ul>
     *   </li>
     *   <li>변환된 Place 리스트 반환</li>
     * </ol>
     *
     * <h3>예외 처리</h3>
     * <ul>
     *   <li>API 호출 실패 시: RuntimeException 발생</li>
     *   <li>응답 파싱 실패 시: 빈 리스트 반환</li>
     *   <li>잘못된 좌표 데이터: 해당 아이템 스킵</li>
     * </ul>
     *
     * <h3>사용 예시</h3>
     * <pre>
     * String query = "서울특별시 종로구 청운효자동 카페";
     * List&lt;Place&gt; places = naverPlaceApiService.searchPlaces(query, 5);
     * // 최대 5개의 Place 반환
     * </pre>
     *
     * @param query 검색 쿼리 (예: "서울특별시 강남구 역삼동 맛집")
     * @param display 검색 결과 개수 (최대 5)
     * @return 검색된 장소 리스트 (Place 엔티티)
     * @throws RuntimeException API 호출 실패 시
     */
    @Deprecated
    List<Place> searchPlaces(String query, int display);
}
