package com.mohe.spring.batch.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.batch.service.NaverPlaceApiService;
import com.mohe.spring.entity.Place;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Naver Place API 호출 서비스 구현체
 *
 * <p>Naver Local Search API를 호출하여 장소 데이터를 검색하고,
 * 응답을 Place 엔티티로 변환하는 기능을 제공합니다.</p>
 *
 * <h3>주요 구현 사항</h3>
 * <ul>
 *   <li>OkHttp3를 사용한 HTTP 통신</li>
 *   <li>Jackson을 사용한 JSON 파싱</li>
 *   <li>Jsoup을 사용한 HTML 태그 제거</li>
 *   <li>좌표 정규화 (mapx, mapy를 BigDecimal로 변환)</li>
 *   <li>Rate limiting 고려 (필요 시 추가 가능)</li>
 * </ul>
 *
 * <h3>API 설정</h3>
 * <p>application.yml에서 다음 설정이 필요합니다:</p>
 * <pre>
 * naver:
 *   place:
 *     clientId: ${NAVER_CLIENT_ID}
 *     clientSecret: ${NAVER_CLIENT_SECRET}
 * </pre>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see com.mohe.spring.batch.service.NaverPlaceApiService
 */
@Service
public class NaverPlaceApiServiceImpl implements NaverPlaceApiService {

    private static final Logger logger = LoggerFactory.getLogger(NaverPlaceApiServiceImpl.class);

    /** Naver Local Search API 엔드포인트 */
    private static final String NAVER_LOCAL_SEARCH_URL = "https://openapi.naver.com/v1/search/local";

    /** Naver API 클라이언트 ID (application.yml에서 주입) */
    @Value("${naver.place.clientId}")
    private String clientId;

    /** Naver API 클라이언트 Secret (application.yml에서 주입) */
    @Value("${naver.place.clientSecret}")
    private String clientSecret;

    /** HTTP 클라이언트 (OkHttp3) */
    private final OkHttpClient httpClient;

    /** JSON 파서 (Jackson) */
    private final ObjectMapper objectMapper;

    /**
     * NaverPlaceApiServiceImpl 생성자
     *
     * <p>OkHttpClient와 ObjectMapper를 초기화합니다.</p>
     */
    public NaverPlaceApiServiceImpl() {
        this.httpClient = new OkHttpClient();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Naver Local Search API를 호출하여 장소를 검색합니다
     *
     * <p>검색 쿼리를 기반으로 Naver API를 호출하고,
     * 응답 데이터를 Place 엔티티 리스트로 변환하여 반환합니다.</p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>검색 쿼리 URL 인코딩 (UTF-8)</li>
     *   <li>API 요청 URL 생성 (query, display, start, sort 파라미터 포함)</li>
     *   <li>HTTP 헤더 설정 (X-Naver-Client-Id, X-Naver-Client-Secret)</li>
     *   <li>HTTP GET 요청 실행</li>
     *   <li>응답 상태 코드 확인 (200 OK)</li>
     *   <li>JSON 응답 파싱</li>
     *   <li>items 배열 순회하며 Place 엔티티로 변환</li>
     * </ol>
     *
     * <h3>Place 변환 로직</h3>
     * <ul>
     *   <li><b>title:</b> HTML 태그 제거 (Jsoup.parse().text())</li>
     *   <li><b>category:</b> 그대로 저장</li>
     *   <li><b>roadAddress:</b> 도로명 주소</li>
     *   <li><b>mapx:</b> 경도 (longitude) → BigDecimal 변환 (10^7로 나눔)</li>
     *   <li><b>mapy:</b> 위도 (latitude) → BigDecimal 변환 (10^7로 나눔)</li>
     *   <li><b>searchQuery:</b> 검색에 사용된 쿼리 저장</li>
     * </ul>
     *
     * <h3>에러 처리</h3>
     * <ul>
     *   <li>API 호출 실패 (IOException): RuntimeException 발생</li>
     *   <li>응답 상태 코드 != 200: RuntimeException 발생</li>
     *   <li>JSON 파싱 실패: 빈 리스트 반환</li>
     *   <li>개별 아이템 변환 실패: 로그 출력 후 스킵</li>
     * </ul>
     *
     * <h3>좌표 정규화</h3>
     * <p>Naver API는 좌표를 10^7 배율로 반환하므로, BigDecimal 변환 시 10^7로 나눕니다:</p>
     * <pre>
     * API Response: mapx=1270390000, mapy=375120000
     * Converted: longitude=127.0390000, latitude=37.5120000
     * </pre>
     *
     * @param query 검색 쿼리 (예: "서울특별시 종로구 청운효자동 카페")
     * @param display 검색 결과 개수 (최대 50)
     * @return 검색된 장소 리스트 (Place 엔티티)
     * @throws RuntimeException API 호출 실패 시
     */
    @Override
    public List<Place> searchPlaces(String query, int display) {
        List<Place> places = new ArrayList<>();

        try {
            // 1. 검색 쿼리 URL 인코딩
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);

            // 2. API 요청 URL 생성 (sort=comment: 리뷰 개수 순)
            String url = String.format("%s?query=%s&display=%d&start=1&sort=comment",
                    NAVER_LOCAL_SEARCH_URL, encodedQuery, display);

            logger.debug("🔍 Calling Naver API: {}", url);

            // 3. HTTP 요청 생성 (헤더 설정)
            Request request = new Request.Builder()
                    .url(url)
                    .addHeader("X-Naver-Client-Id", clientId)
                    .addHeader("X-Naver-Client-Secret", clientSecret)
                    .get()
                    .build();

            // 4. HTTP 요청 실행
            try (Response response = httpClient.newCall(request).execute()) {
                // 5. 응답 상태 코드 확인
                if (!response.isSuccessful()) {
                    throw new RuntimeException("Naver API call failed with status: " + response.code());
                }

                // 6. JSON 응답 파싱
                String responseBody = response.body().string();
                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode items = root.path("items");

                // 7. items 배열 순회하며 Place 엔티티로 변환
                if (items.isArray()) {
                    for (JsonNode item : items) {
                        try {
                            Place place = convertToPlace(item, query);
                            if (place != null) {
                                places.add(place);
                            }
                        } catch (Exception e) {
                            logger.warn("⚠️ Failed to convert item to Place: {}", e.getMessage());
                        }
                    }
                }

                logger.info("✅ Naver API returned {} places for query: {}", places.size(), query);
            }

        } catch (IOException e) {
            logger.error("❌ Naver API call failed for query: {}", query, e);
            throw new RuntimeException("Failed to call Naver API", e);
        }

        return places;
    }

    /**
     * Naver API 응답 아이템을 Place 엔티티로 변환합니다
     *
     * <p>JSON 노드에서 필요한 필드를 추출하고,
     * HTML 태그 제거 및 좌표 정규화를 수행합니다.</p>
     *
     * <h3>변환 매핑</h3>
     * <pre>
     * item.title        → place.name (HTML 태그 제거)
     * item.category     → place.category
     * item.roadAddress  → place.roadAddress
     * item.mapx         → place.longitude (10^7로 나눔)
     * item.mapy         → place.latitude (10^7로 나눔)
     * query             → place.searchQuery
     * </pre>
     *
     * <h3>검증 로직</h3>
     * <ul>
     *   <li>title이 비어있으면 null 반환</li>
     *   <li>mapx, mapy가 0이면 null 반환</li>
     *   <li>roadAddress가 비어있으면 경고 로그 출력</li>
     * </ul>
     *
     * @param item Naver API 응답의 items 배열 원소
     * @param query 검색 쿼리
     * @return 변환된 Place 엔티티, 실패 시 null
     */
    private Place convertToPlace(JsonNode item, String query) {
        // title 필드 추출 및 HTML 태그 제거
        String title = item.path("title").asText();
        if (title.isEmpty()) {
            logger.warn("⚠️ Empty title in Naver API response");
            return null;
        }

        // Jsoup을 사용하여 HTML 태그 제거 (예: "<b>카페</b>" → "카페")
        String cleanTitle = Jsoup.parse(title).text();

        // category 필드 추출
        String category = item.path("category").asText();

        // roadAddress 필드 추출
        String roadAddress = item.path("roadAddress").asText();
        if (roadAddress.isEmpty()) {
            logger.warn("⚠️ Empty roadAddress for place: {}", cleanTitle);
        }

        // 좌표 추출 및 정규화 (Naver는 10^7 배율로 반환)
        long mapx = item.path("mapx").asLong();
        long mapy = item.path("mapy").asLong();

        if (mapx == 0 || mapy == 0) {
            logger.warn("⚠️ Invalid coordinates for place: {}", cleanTitle);
            return null;
        }

        // BigDecimal로 변환 (10^7로 나눔)
        BigDecimal longitude = BigDecimal.valueOf(mapx).divide(BigDecimal.valueOf(10_000_000));
        BigDecimal latitude = BigDecimal.valueOf(mapy).divide(BigDecimal.valueOf(10_000_000));

        // Place 엔티티 생성
        Place place = new Place();
        place.setName(cleanTitle);
        place.setCategory(List.of(category.split(">")));
        place.setRoadAddress(roadAddress);
        place.setLongitude(longitude);
        place.setLatitude(latitude);

        logger.debug("✅ Converted place: {} [{}] at ({}, {})",
                cleanTitle, category, latitude, longitude);

        return place;
    }
}
