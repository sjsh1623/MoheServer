package com.mohe.spring.batch.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.batch.service.KakaoPlaceApiService;
import com.mohe.spring.dto.kakao.KakaoPlaceResponse;
import com.mohe.spring.entity.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Kakao Local API 장소 검색 서비스 구현체
 *
 * <p>Kakao Local API를 사용하여 키워드로 장소를 검색합니다.</p>
 * <p>페이지네이션을 통해 최대 300개(15개 × 20페이지)의 장소를 수집합니다.</p>
 *
 * @author Andrew Lim
 * @since 2.0
 */
@Service
public class KakaoPlaceApiServiceImpl implements KakaoPlaceApiService {

    private static final Logger logger = LoggerFactory.getLogger(KakaoPlaceApiServiceImpl.class);

    private static final String KAKAO_API_URL = "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final int SIZE_PER_PAGE = 15; // Kakao API 최대값
    private static final int MAX_PAGE = 20; // 충분한 데이터 수집을 위해 20페이지

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KakaoPlaceApiServiceImpl(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<Place> searchPlaces(String query, int maxResults) {
        List<Place> allPlaces = new ArrayList<>();
        int currentPage = 1;
        boolean hasMore = true;

        logger.info("🔍 Kakao API 검색 시작: query='{}', maxResults={}", query, maxResults);

        while (hasMore && allPlaces.size() < maxResults && currentPage <= MAX_PAGE) {
            try {
                // API 요청 URI 생성 (이중 인코딩 방지를 위해 URI 객체 직접 사용)
                URI uri = UriComponentsBuilder.fromHttpUrl(KAKAO_API_URL)
                        .queryParam("query", query)
                        .queryParam("size", SIZE_PER_PAGE)
                        .queryParam("page", currentPage)
                        .encode()  // UTF-8로 인코딩
                        .build()
                        .toUri();  // URI 객체로 변환 (RestTemplate이 다시 인코딩하지 않음)

                // DEBUG: Log generated URI
                if (currentPage == 1) {
                    logger.info("🔗 Generated URI: {}", uri.toString());
                }

                // HTTP 헤더 설정 (Authorization: KakaoAK {REST_API_KEY})
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "KakaoAK " + kakaoApiKey);
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<String> entity = new HttpEntity<>(headers);

                // API 호출 (URI 객체 사용)
                ResponseEntity<String> response = restTemplate.exchange(
                        uri,  // String 대신 URI 객체 전달
                        HttpMethod.GET,
                        entity,
                        String.class
                );

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    // DEBUG: Log response body
                    if (currentPage == 1) {
                        logger.info("🔍 Kakao API Response (첫 페이지): {}", response.getBody().substring(0, Math.min(500, response.getBody().length())));
                    }

                    KakaoPlaceResponse kakaoResponse = objectMapper.readValue(
                            response.getBody(),
                            KakaoPlaceResponse.class
                    );

                    // 응답 데이터를 Place 엔티티로 변환
                    List<Place> places = convertToPlaces(kakaoResponse.getDocuments());
                    allPlaces.addAll(places);

                    logger.debug("📄 Page {}: {} places found (total_count: {}, pageable_count: {})",
                            currentPage, places.size(),
                            kakaoResponse.getMeta().getTotalCount(),
                            kakaoResponse.getMeta().getPageableCount());

                    // 다음 페이지 확인
                    hasMore = !kakaoResponse.getMeta().isEnd();
                    currentPage++;

                } else {
                    logger.warn("⚠️ Kakao API returned status: {}", response.getStatusCode());
                    break;
                }

            } catch (Exception e) {
                logger.error("❌ Kakao API call failed for query: {} (page: {})", query, currentPage, e);
                break;
            }
        }

        logger.info("✅ Kakao API 검색 완료: query='{}', total={} places", query, allPlaces.size());
        return allPlaces;
    }

    /**
     * Kakao API 응답을 Place 엔티티 리스트로 변환
     */
    private List<Place> convertToPlaces(List<KakaoPlaceResponse.Document> documents) {
        List<Place> places = new ArrayList<>();

        for (KakaoPlaceResponse.Document doc : documents) {
            try {
                Place place = new Place();

                // 기본 정보
                place.setName(doc.getPlaceName());
                place.setRoadAddress(doc.getRoadAddressName());

                // 좌표 (Kakao는 x=경도, y=위도)
                if (doc.getX() != null && doc.getY() != null) {
                    place.setLongitude(new BigDecimal(doc.getX()));
                    place.setLatitude(new BigDecimal(doc.getY()));
                }

                // 카테고리 (Kakao 형식: "음식점 > 카페 > 디저트카페")
                if (doc.getCategoryName() != null) {
                    String[] categories = doc.getCategoryName().split(" > ");
                    place.setCategory(Arrays.asList(categories));
                } else {
                    place.setCategory(new ArrayList<>());
                }

                // URL
                place.setWebsiteUrl(doc.getPlaceUrl());

                // 전화번호는 별도 필드가 없으므로 비워둠
                // Rating, ReviewCount도 Kakao API에서 제공하지 않음

                place.setReady(false); // 초기값

                places.add(place);

                logger.debug("✅ Converted place: {} [{}] at ({}, {})",
                        place.getName(),
                        place.getCategory(),
                        place.getLatitude(),
                        place.getLongitude());

            } catch (Exception e) {
                logger.error("❌ Failed to convert place: {}", doc.getPlaceName(), e);
            }
        }

        return places;
    }
}
