package com.mohe.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 장소 데이터 수집 비즈니스 로직
 * - Naver API를 통한 장소 검색
 * - Google Places API를 통한 상세 정보 보강
 * - 필터링 로직 (편의점, 마트 제외)
 */
@Service
public class PlaceDataCollectionService {

    private static final Logger logger = LoggerFactory.getLogger(PlaceDataCollectionService.class);

    @Value("${NAVER_CLIENT_ID:}")
    private String naverClientId;

    @Value("${NAVER_CLIENT_SECRET:}")
    private String naverClientSecret;

    @Value("${GOOGLE_PLACES_API_KEY:}")
    private String googleApiKey;

    private final PlaceRepository placeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PlaceDataCollectionService(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
    }

    /**
     * Naver Local Search API로 장소 검색
     */
    public List<Place> fetchPlacesFromNaver(String query, int count) {
        logger.info("🔍 Naver API 검색: query='{}', count={}", query, count);

        if (naverClientId == null || naverClientId.trim().isEmpty() ||
            naverClientSecret == null || naverClientSecret.trim().isEmpty()) {
            logger.error("❌ Naver API credentials not configured");
            return Collections.emptyList();
        }

        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlString = String.format(
                "https://openapi.naver.com/v1/search/local.json?query=%s&display=%d&start=1&sort=random",
                encodedQuery, Math.min(count, 100) // Naver API 최대 100개
            );

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();

            // HTTP 연결 설정
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Naver-Client-Id", naverClientId);
            connection.setRequestProperty("X-Naver-Client-Secret", naverClientSecret);
            connection.setRequestProperty("Accept", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "MoheSpring/1.0");
            connection.setRequestProperty("Accept-Encoding", "identity");
            connection.setRequestProperty("Cache-Control", "no-cache");
            connection.setUseCaches(false);
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(30000);

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                logger.error("❌ Naver API Error - Response Code: {}", responseCode);
                return Collections.emptyList();
            }

            // 완전한 응답 읽기
            String responseBody;
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {

                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    byteArrayOutputStream.write(buffer, 0, bytesRead);
                }
                responseBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
            }

            connection.disconnect();

            // JSON 파싱
            JsonNode jsonResponse = objectMapper.readTree(responseBody);
            JsonNode items = jsonResponse.get("items");

            List<Place> places = new ArrayList<>();
            if (items != null && items.isArray()) {
                for (JsonNode item : items) {
                    Place place = convertNaverItemToPlace(item);
                    places.add(place);
                }
            }

            logger.info("✅ Naver API: '{}' 검색 결과 {}개 수집", query, places.size());
            return places;

        } catch (Exception e) {
            logger.error("❌ Naver API 호출 실패 for '{}': {}", query, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Naver API 응답을 Place 엔티티로 변환
     */
    private Place convertNaverItemToPlace(JsonNode item) {
        Place place = new Place();

        // 기본 정보
        String title = item.get("title").asText().replaceAll("<[^>]*>", "");
        place.setName(title);
        place.setCategory(List.of(item.get("category").asText().split(">")));
        place.setRoadAddress(item.has("roadAddress") ? item.get("roadAddress").asText() : null);

        // 좌표 변환 (Naver API 좌표계)
        // mapx = 경도(longitude), mapy = 위도(latitude)
        // Naver API는 좌표를 10000000을 곱한 정수로 제공
        if (item.has("mapy") && item.has("mapx")) {
            int mapyInt = item.get("mapy").asInt();
            int mapxInt = item.get("mapx").asInt();

            double latitude = mapyInt / 10000000.0;
            double longitude = mapxInt / 10000000.0;

            place.setLatitude(BigDecimal.valueOf(latitude));
            place.setLongitude(BigDecimal.valueOf(longitude));

            logger.debug("좌표 변환: {} - mapx={} → lng={}, mapy={} → lat={}",
                title, mapxInt, longitude, mapyInt, latitude);
        } else {
            logger.warn("좌표 정보 없음: {}", title);
        }

        place.setRating(BigDecimal.ZERO); // Google API로 나중에 보강
        place.setCreatedAt(java.time.LocalDateTime.now());

        return place;
    }

    /**
     * Google Places API로 평점 및 상세 정보 보강
     */
    public void enhanceWithGooglePlaces(Place place) {
        if (googleApiKey == null || googleApiKey.trim().isEmpty()) {
            logger.debug("Google Places API key not configured, skipping enhancement");
            return;
        }

        try {
            String query = place.getName() + " " + place.getRoadAddress();
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String urlString = String.format(
                "https://maps.googleapis.com/maps/api/place/textsearch/json?query=%s&key=%s&language=ko",
                encodedQuery, googleApiKey
            );

            URL url = new URL(urlString);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(30000);

            if (connection.getResponseCode() == 200) {
                String responseBody;
                try (InputStream inputStream = connection.getInputStream();
                     ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead);
                    }
                    responseBody = byteArrayOutputStream.toString(StandardCharsets.UTF_8.name());
                }

                JsonNode root = objectMapper.readTree(responseBody);
                JsonNode results = root.path("results");

                if (results.isArray() && results.size() > 0) {
                    JsonNode firstResult = results.get(0);

                    // 평점 설정
                    double rating = firstResult.path("rating").asDouble(0.0);
                    if (rating > 0) {
                        place.setRating(BigDecimal.valueOf(rating));
                    }

                    // 사용자 평점 수
                    int userRatingsTotal = firstResult.path("user_ratings_total").asInt(0);
                    if (userRatingsTotal > 0) {
                        place.setReviewCount(userRatingsTotal);
                    }

                    logger.debug("✅ Google Places 상세 정보 보강 완료: {} (평점: {})", place.getName(), rating);
                }
            }

            connection.disconnect();
            Thread.sleep(100); // API 호출 제한

        } catch (Exception e) {
            logger.error("❌ Google Places API 호출 실패 for '{}': {}", place.getName(), e.getMessage());
        }
    }

    /**
     * 장소 필터링 - 편의점, 마트, 약국 등 제외
     */
    public boolean shouldFilterOutPlace(Place place) {
        if (place == null) return true;

        String name = place.getName() != null ? place.getName().toLowerCase() : "";
        List<String> categories = place.getCategory();

        // 이름 필터링
        String[] nameFilters = {
            "슈퍼", "super", "수퍼", "마트", "mart", "약국", "pharmacy",
            "편의점", "convenience", "cvs", "7-eleven", "세븐일레븐", "gs25", "cu",
            "이마트", "emart", "롯데마트", "홈플러스", "코스트코"
        };

        for (String filter : nameFilters) {
            if (name.contains(filter)) {
                return true;
            }
        }

        // 카테고리 필터링
        if (categories != null) {
            String[] categoryFilters = {
                "슈퍼마켓", "편의점", "약국", "마트", "대형마트", "할인점",
                "supermarket", "convenience store", "pharmacy", "drugstore"
            };

            for (String category : categories) {
                for (String filter : categoryFilters) {
                    if (category.toLowerCase().contains(filter)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * 중복 체크 - 이름과 좌표를 모두 비교
     * 같은 이름이면서 좌표가 0.001도(약 100m) 이내인 경우 중복으로 판단
     */
    public boolean isDuplicate(Place place) {
        if (place.getName() == null || place.getLatitude() == null || place.getLongitude() == null) {
            return false;
        }

        // 0.001도 = 약 100미터 반경 내 중복 체크
        BigDecimal radius = BigDecimal.valueOf(0.001);
        Optional<Place> similarPlace = placeRepository.findSimilarPlace(
            place.getName(),
            place.getLatitude(),
            place.getLongitude(),
            radius
        );

        if (similarPlace.isPresent()) {
            logger.debug("중복 장소 발견: {} (기존 ID: {})", place.getName(), similarPlace.get().getId());
            return true;
        }

        return false;
    }

    /**
     * Place 저장
     */
    public Place savePlace(Place place) {
        return placeRepository.save(place);
    }

    /**
     * 모든 Place 데이터 삭제
     */
    public void deleteAllPlaces() {
        placeRepository.deleteAll();
        logger.info("✅ All Place data deleted");
    }
}
