package com.mohe.spring.service;

import com.mohe.spring.dto.CategoryDto;
import com.mohe.spring.dto.PlaceDto;
import com.mohe.spring.dto.SuggestedCategoriesResponse;
import com.mohe.spring.entity.Place;
import com.mohe.spring.enums.CategoryRecommendationRule;
import com.mohe.spring.enums.PlaceCategory;
import com.mohe.spring.enums.TimeSlot;
import com.mohe.spring.enums.WeatherCondition;
import com.mohe.spring.repository.BookmarkRepository;
import com.mohe.spring.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 카테고리 추천 서비스
 *
 * <p>시간대와 날씨 정보를 기반으로 적합한 장소 카테고리를 추천합니다.</p>
 */
@Service
public class CategoryRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryRecommendationService.class);

    private final WeatherService weatherService;
    private final BookmarkRepository bookmarkRepository;
    private final PlaceRepository placeRepository;
    private final PlaceService placeService;

    // MBTI별 선호 카테고리 (fallback용)
    private static final Map<String, List<PlaceCategory>> MBTI_PREFERENCES = Map.ofEntries(
        Map.entry("ENFP", List.of(PlaceCategory.CAFE, PlaceCategory.WORKSHOP, PlaceCategory.EXHIBITION, PlaceCategory.LIVE_MUSIC)),
        Map.entry("ENFJ", List.of(PlaceCategory.RESTAURANT, PlaceCategory.GALLERY, PlaceCategory.FLOWER_CAFE, PlaceCategory.TERRACE)),
        Map.entry("INFP", List.of(PlaceCategory.LIBRARY_CAFE, PlaceCategory.GALLERY, PlaceCategory.WALKING_TRAIL, PlaceCategory.TEA_HOUSE)),
        Map.entry("INFJ", List.of(PlaceCategory.BOOKSTORE, PlaceCategory.TEA_HOUSE, PlaceCategory.BOTANICAL_GARDEN, PlaceCategory.CAFE)),
        Map.entry("ENTP", List.of(PlaceCategory.BAR, PlaceCategory.ROOFTOP_BAR, PlaceCategory.ESCAPE_ROOM, PlaceCategory.CRAFT_BEER)),
        Map.entry("ENTJ", List.of(PlaceCategory.FINE_DINING, PlaceCategory.LOUNGE_BAR, PlaceCategory.RESTAURANT, PlaceCategory.SCENIC_SPOT)),
        Map.entry("INTP", List.of(PlaceCategory.BOOKSTORE, PlaceCategory.MUSEUM, PlaceCategory.STUDY_CAFE, PlaceCategory.CAFE)),
        Map.entry("INTJ", List.of(PlaceCategory.MUSEUM, PlaceCategory.CAFE, PlaceCategory.WINE_BAR, PlaceCategory.GALLERY)),
        Map.entry("ESFP", List.of(PlaceCategory.KARAOKE, PlaceCategory.BOWLING, PlaceCategory.PUB, PlaceCategory.CRAFT_BEER)),
        Map.entry("ESTP", List.of(PlaceCategory.SPORTS, PlaceCategory.BAR, PlaceCategory.BOARD_GAME, PlaceCategory.ROOFTOP_BAR)),
        Map.entry("ISFP", List.of(PlaceCategory.VINTAGE_SHOP, PlaceCategory.WORKSHOP, PlaceCategory.PHOTO_BOOTH, PlaceCategory.FLOWER_CAFE)),
        Map.entry("ISTP", List.of(PlaceCategory.CRAFT_BEER, PlaceCategory.BILLIARDS, PlaceCategory.CAFE, PlaceCategory.FITNESS)),
        Map.entry("ESFJ", List.of(PlaceCategory.RESTAURANT, PlaceCategory.KOREAN_FOOD, PlaceCategory.BAKERY, PlaceCategory.DESSERT_CAFE)),
        Map.entry("ESTJ", List.of(PlaceCategory.FINE_DINING, PlaceCategory.RESTAURANT, PlaceCategory.MEAT, PlaceCategory.KOREAN_FOOD)),
        Map.entry("ISFJ", List.of(PlaceCategory.CAFE, PlaceCategory.BAKERY, PlaceCategory.PARK, PlaceCategory.TEA_HOUSE)),
        Map.entry("ISTJ", List.of(PlaceCategory.CAFE, PlaceCategory.RESTAURANT, PlaceCategory.BOOKSTORE, PlaceCategory.PARK))
    );

    // MBTI별 "오늘은 이런 곳 어때요?" 타이틀
    private static final Map<String, List<String>> MBTI_TITLES = Map.ofEntries(
        Map.entry("ENFP", List.of("오늘은 이런 곳 어때요?", "새로운 곳 탐험해볼까요?", "영감이 필요한 하루")),
        Map.entry("ENFJ", List.of("오늘은 이런 곳 어때요?", "함께하면 더 좋은 곳", "특별한 하루를 만들어요")),
        Map.entry("INFP", List.of("오늘은 이런 곳 어때요?", "조용히 나만의 시간을", "감성 충전이 필요한 날")),
        Map.entry("INFJ", List.of("오늘은 이런 곳 어때요?", "마음이 편해지는 곳", "깊이 있는 시간을")),
        Map.entry("ENTP", List.of("오늘은 이런 곳 어때요?", "뭔가 재밌는 거 없을까?", "색다른 경험을 찾아서")),
        Map.entry("ENTJ", List.of("오늘은 이런 곳 어때요?", "특별한 곳으로 가볼까요?", "품격 있는 시간을")),
        Map.entry("INTP", List.of("오늘은 이런 곳 어때요?", "혼자만의 시간이 필요해", "집중하기 좋은 곳")),
        Map.entry("INTJ", List.of("오늘은 이런 곳 어때요?", "효율적인 하루를 위해", "나만의 공간을 찾아서")),
        Map.entry("ESFP", List.of("오늘은 이런 곳 어때요?", "신나는 하루 보내요!", "놀 거리가 필요해!")),
        Map.entry("ESTP", List.of("오늘은 이런 곳 어때요?", "액티브한 하루!", "뭔가 짜릿한 거 없을까?")),
        Map.entry("ISFP", List.of("오늘은 이런 곳 어때요?", "예쁜 곳 찾아 떠나요", "감성 가득한 곳으로")),
        Map.entry("ISTP", List.of("오늘은 이런 곳 어때요?", "조용히 즐길 수 있는 곳", "나만 아는 숨은 공간")),
        Map.entry("ESFJ", List.of("오늘은 이런 곳 어때요?", "맛있는 곳 찾았어요!", "함께 가면 좋은 곳")),
        Map.entry("ESTJ", List.of("오늘은 이런 곳 어때요?", "검증된 맛집으로!", "확실한 곳으로 가요")),
        Map.entry("ISFJ", List.of("오늘은 이런 곳 어때요?", "편안한 곳에서 쉬어요", "소소한 행복을 찾아서")),
        Map.entry("ISTJ", List.of("오늘은 이런 곳 어때요?", "믿을 수 있는 곳으로", "안정적인 선택"))
    );

    public CategoryRecommendationService(
            WeatherService weatherService,
            BookmarkRepository bookmarkRepository,
            PlaceRepository placeRepository,
            PlaceService placeService) {
        this.weatherService = weatherService;
        this.bookmarkRepository = bookmarkRepository;
        this.placeRepository = placeRepository;
        this.placeService = placeService;
    }

    /**
     * 현재 시간과 날씨 기반 카테고리 추천
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 추천 카테고리 응답
     */
    public SuggestedCategoriesResponse getSuggestedCategories(Double latitude, Double longitude) {
        // 1. 현재 시간대 파악
        TimeSlot currentTimeSlot = TimeSlot.fromCurrentTime();
        logger.info("Current time slot: {}", currentTimeSlot.getDisplayName());

        // 2. 날씨 정보 가져오기
        WeatherCondition weatherCondition = getWeatherCondition(latitude, longitude);
        logger.info("Weather condition: {}", weatherCondition.getDisplayName());

        // 3. 추천 규칙 찾기
        CategoryRecommendationRule rule = CategoryRecommendationRule.findRule(currentTimeSlot, weatherCondition);
        logger.info("Applied rule: {} / {}", rule.getTimeSlot().getDisplayName(), rule.getWeatherCondition().getDisplayName());

        // 4. 추천 카테고리 변환
        List<CategoryDto> suggestedCategories = rule.getRecommendedCategories().stream()
                .map(this::toCategoryDto)
                .collect(Collectors.toList());

        // 5. 응답 생성
        SuggestedCategoriesResponse response = new SuggestedCategoriesResponse();
        response.setTimeSlot(currentTimeSlot.getDisplayName());
        response.setWeather(weatherCondition.getDisplayName());
        response.setReason(rule.getReasonText());
        response.setSuggestedCategories(suggestedCategories);
        response.setLocation(new SuggestedCategoriesResponse.LocationInfo(latitude, longitude));

        return response;
    }

    /**
     * 홈 화면 통합 데이터: MBTI 첫줄 + 시간/날씨 기반 카테고리
     */
    public Map<String, Object> getHomeData(Double lat, Double lon, String mbti, int placesPerCategory) {
        Map<String, Object> result = new LinkedHashMap<>();

        // 시간+날씨 기반 카테고리
        TimeSlot currentTimeSlot = TimeSlot.fromCurrentTime();
        WeatherCondition weatherCondition = getWeatherCondition(lat, lon);
        CategoryRecommendationRule rule = CategoryRecommendationRule.findRule(currentTimeSlot, weatherCondition);

        result.put("timeSlot", currentTimeSlot.getDisplayName());
        result.put("weather", weatherCondition.getDisplayName());
        result.put("reason", rule.getReasonText());

        // 1. MBTI 첫 줄 (로그인 + MBTI 있을 때만)
        if (mbti != null && !mbti.isBlank()) {
            String upperMbti = mbti.toUpperCase().trim();
            Map<String, Object> mbtiRow = buildMbtiRow(upperMbti, lat, lon, placesPerCategory);
            if (mbtiRow != null) {
                result.put("mbtiRow", mbtiRow);
            }
        }

        // 2. 시간+날씨 기반 카테고리 행들
        List<Map<String, Object>> categoryRows = new ArrayList<>();
        for (PlaceCategory category : rule.getRecommendedCategories()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("key", category.getKey());
            row.put("displayName", category.getDisplayName());
            row.put("description", category.getDescription());
            row.put("emoji", category.getEmoji());
            categoryRows.add(row);
        }
        result.put("categoryRows", categoryRows);

        return result;
    }

    /**
     * MBTI 기반 첫 줄 데이터 구성
     */
    private Map<String, Object> buildMbtiRow(String mbti, Double lat, Double lon, int limit) {
        Map<String, Object> row = new LinkedHashMap<>();

        // 타이틀
        List<String> titles = MBTI_TITLES.getOrDefault(mbti, List.of("오늘은 이런 곳 어때요?"));
        row.put("title", titles.get(new Random().nextInt(titles.size())));
        row.put("mbti", mbti);

        // 1차: 동일 MBTI 사용자 좋아요 순
        List<Place> mbtiPlaces;
        try {
            mbtiPlaces = bookmarkRepository.findPopularPlacesByMbti(mbti, lat, lon, 50.0, limit);
        } catch (Exception e) {
            logger.warn("MBTI bookmark query failed: {}", e.getMessage());
            mbtiPlaces = List.of();
        }

        if (mbtiPlaces.size() >= 3) {
            row.put("source", "bookmark");
            row.put("places", mbtiPlaces.stream()
                    .map(p -> placeToSimpleDto(p, lat, lon))
                    .toList());
            return row;
        }

        // 2차 fallback: MBTI 선호 카테고리 키워드로 검색
        List<PlaceCategory> preferredCategories = MBTI_PREFERENCES.get(mbti);
        if (preferredCategories == null) return null;

        // 선호 카테고리별로 장소 검색해서 합침
        Set<Long> seenIds = new HashSet<>();
        List<Place> fallbackPlaces = new ArrayList<>();
        for (PlaceCategory cat : preferredCategories) {
            if (fallbackPlaces.size() >= limit) break;
            try {
                String kws = cat.getKeywords().stream().map(String::toLowerCase).collect(Collectors.joining(","));
                List<Place> places = placeRepository.findNearbyPlacesByCategory(lat, lon, 30.0, kws, 5);
                for (Place p : places) {
                    if (seenIds.add(p.getId())) fallbackPlaces.add(p);
                }
            } catch (Exception e) {
                logger.debug("MBTI fallback search failed for {}: {}", cat.getKey(), e.getMessage());
            }
        }

        if (!fallbackPlaces.isEmpty()) {
            row.put("source", "keyword");
            row.put("places", fallbackPlaces.stream()
                    .limit(limit)
                    .map(p -> placeToSimpleDto(p, lat, lon))
                    .toList());
            return row;
        }

        return null;
    }

    private Map<String, Object> placeToSimpleDto(Place place, Double userLat, Double userLon) {
        Map<String, Object> dto = new LinkedHashMap<>();
        dto.put("id", place.getId());
        dto.put("name", place.getName());
        dto.put("rating", place.getRating());
        dto.put("roadAddress", place.getRoadAddress());

        // 카테고리 (구체적인 것 우선)
        if (place.getCategory() != null && !place.getCategory().isEmpty()) {
            String cat = place.getCategory().stream()
                    .filter(c -> !c.equalsIgnoreCase("음식점") && !c.equalsIgnoreCase("restaurant"))
                    .findFirst().orElse(place.getCategory().get(0));
            dto.put("category", cat);
        }

        // 이미지
        List<String> imageUrls = placeService.getImageUrls(place.getId());
        dto.put("imageUrl", imageUrls.isEmpty() ? null : imageUrls.get(0));

        // 거리
        if (place.getLatitude() != null && place.getLongitude() != null && userLat != null && userLon != null) {
            double dist = 6371 * Math.acos(Math.min(1.0, Math.max(-1.0,
                    Math.cos(Math.toRadians(userLat)) * Math.cos(Math.toRadians(place.getLatitude().doubleValue())) *
                    Math.cos(Math.toRadians(place.getLongitude().doubleValue()) - Math.toRadians(userLon)) +
                    Math.sin(Math.toRadians(userLat)) * Math.sin(Math.toRadians(place.getLatitude().doubleValue()))
            )));
            dto.put("distance", Math.round(dist * 10) / 10.0);
        }

        return dto;
    }

    /**
     * 날씨 정보 가져오기
     *
     * @param latitude 위도
     * @param longitude 경도
     * @return 날씨 상태
     */
    private WeatherCondition getWeatherCondition(Double latitude, Double longitude) {
        try {
            WeatherData weatherData = weatherService.getCurrentWeather(latitude, longitude);
            if (weatherData != null && weatherData.getConditionText() != null) {
                return WeatherCondition.fromText(weatherData.getConditionText());
            }
        } catch (Exception e) {
            logger.warn("Failed to fetch weather data: {}", e.getMessage());
        }

        // 날씨 정보를 가져오지 못한 경우 기본값
        return WeatherCondition.SUNNY;
    }

    /**
     * PlaceCategory를 CategoryDto로 변환
     *
     * @param category PlaceCategory
     * @return CategoryDto
     */
    private CategoryDto toCategoryDto(PlaceCategory category) {
        return new CategoryDto(
                category.getKey(),
                category.getDisplayName(),
                category.getDescription(),
                category.getEmoji()
        );
    }
}
