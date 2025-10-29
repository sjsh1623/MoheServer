package com.mohe.spring.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 시간대 × 날씨별 카테고리 추천 규칙
 *
 * <p>시간대와 날씨 조건에 따라 적합한 장소 카테고리를 추천합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 */
public enum CategoryRecommendationRule {

    // ============================================
    // 새벽 (0-5시)
    // ============================================
    EARLY_MORNING_SUNNY(
        TimeSlot.EARLY_MORNING,
        WeatherCondition.SUNNY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.PUB,
            PlaceCategory.HANGANG_PARK
        ),
        "맑은 새벽, 밤하늘을 보며 여유를"
    ),

    EARLY_MORNING_CLOUDY(
        TimeSlot.EARLY_MORNING,
        WeatherCondition.CLOUDY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.JAZZ_BAR,
            PlaceCategory.CRAFT_BEER
        ),
        "흐린 새벽, 따뜻한 실내에서 시간을"
    ),

    EARLY_MORNING_RAINY(
        TimeSlot.EARLY_MORNING,
        WeatherCondition.RAINY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.WINE_BAR,
            PlaceCategory.JAZZ_BAR
        ),
        "비 오는 새벽, 감성 있는 실내 공간에서"
    ),

    EARLY_MORNING_SNOWY(
        TimeSlot.EARLY_MORNING,
        WeatherCondition.SNOWY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.TEA_HOUSE,
            PlaceCategory.BAR,
            PlaceCategory.KOREAN_FOOD
        ),
        "눈 오는 새벽, 따뜻한 곳에서 몸을 녹이며"
    ),

    // ============================================
    // 아침 (6-11시)
    // ============================================
    MORNING_SUNNY(
        TimeSlot.MORNING,
        WeatherCondition.SUNNY,
        Arrays.asList(
            PlaceCategory.BRUNCH_CAFE,
            PlaceCategory.BAKERY,
            PlaceCategory.CAFE,
            PlaceCategory.PARK,
            PlaceCategory.WALKING_TRAIL
        ),
        "맑은 아침, 신선한 공기를 마시며 브런치를"
    ),

    MORNING_CLOUDY(
        TimeSlot.MORNING,
        WeatherCondition.CLOUDY,
        Arrays.asList(
            PlaceCategory.CAFE,
            PlaceCategory.BRUNCH_CAFE,
            PlaceCategory.BAKERY,
            PlaceCategory.BOOKSTORE,
            PlaceCategory.LIBRARY_CAFE
        ),
        "흐린 아침, 실내에서 여유롭게 시작하세요"
    ),

    MORNING_RAINY(
        TimeSlot.MORNING,
        WeatherCondition.RAINY,
        Arrays.asList(
            PlaceCategory.CAFE,
            PlaceCategory.BAKERY,
            PlaceCategory.TEA_HOUSE,
            PlaceCategory.BOOKSTORE,
            PlaceCategory.MUSEUM
        ),
        "비 오는 아침, 창가에 앉아 커피 한 잔"
    ),

    MORNING_SNOWY(
        TimeSlot.MORNING,
        WeatherCondition.SNOWY,
        Arrays.asList(
            PlaceCategory.CAFE,
            PlaceCategory.TEA_HOUSE,
            PlaceCategory.BRUNCH_CAFE,
            PlaceCategory.BAKERY,
            PlaceCategory.KOREAN_FOOD
        ),
        "눈 오는 아침, 따뜻한 곳에서 아침을"
    ),

    // ============================================
    // 오후 (12-17시)
    // ============================================
    AFTERNOON_SUNNY(
        TimeSlot.AFTERNOON,
        WeatherCondition.SUNNY,
        Arrays.asList(
            PlaceCategory.RESTAURANT,
            PlaceCategory.PARK,
            PlaceCategory.HANGANG_PARK,
            PlaceCategory.GALLERY,
            PlaceCategory.WALKING_TRAIL
        ),
        "화창한 오후, 야외에서 산책하며 즐기세요"
    ),

    AFTERNOON_CLOUDY(
        TimeSlot.AFTERNOON,
        WeatherCondition.CLOUDY,
        Arrays.asList(
            PlaceCategory.CAFE,
            PlaceCategory.RESTAURANT,
            PlaceCategory.GALLERY,
            PlaceCategory.MUSEUM,
            PlaceCategory.SHOPPING_MALL
        ),
        "흐린 오후, 실내에서 문화생활을"
    ),

    AFTERNOON_RAINY(
        TimeSlot.AFTERNOON,
        WeatherCondition.RAINY,
        Arrays.asList(
            PlaceCategory.CAFE,
            PlaceCategory.DESSERT_CAFE,
            PlaceCategory.CINEMA,
            PlaceCategory.SHOPPING_MALL,
            PlaceCategory.WORKSHOP
        ),
        "비 오는 오후, 실내 활동이 제격이에요"
    ),

    AFTERNOON_SNOWY(
        TimeSlot.AFTERNOON,
        WeatherCondition.SNOWY,
        Arrays.asList(
            PlaceCategory.CAFE,
            PlaceCategory.RESTAURANT,
            PlaceCategory.KOREAN_FOOD,
            PlaceCategory.MUSEUM,
            PlaceCategory.TEA_HOUSE
        ),
        "눈 오는 오후, 따뜻한 실내에서 힐링"
    ),

    // ============================================
    // 저녁 (18-22시)
    // ============================================
    EVENING_SUNNY(
        TimeSlot.EVENING,
        WeatherCondition.SUNNY,
        Arrays.asList(
            PlaceCategory.RESTAURANT,
            PlaceCategory.FINE_DINING,
            PlaceCategory.ROOFTOP_BAR,
            PlaceCategory.SCENIC_SPOT,
            PlaceCategory.HANGANG_PARK
        ),
        "맑은 저녁, 아름다운 야경과 함께 식사를"
    ),

    EVENING_CLOUDY(
        TimeSlot.EVENING,
        WeatherCondition.CLOUDY,
        Arrays.asList(
            PlaceCategory.RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.WINE_BAR,
            PlaceCategory.JAZZ_BAR,
            PlaceCategory.CAFE
        ),
        "흐린 저녁, 분위기 있는 실내에서"
    ),

    EVENING_RAINY(
        TimeSlot.EVENING,
        WeatherCondition.RAINY,
        Arrays.asList(
            PlaceCategory.RESTAURANT,
            PlaceCategory.KOREAN_FOOD,
            PlaceCategory.WINE_BAR,
            PlaceCategory.JAZZ_BAR,
            PlaceCategory.THEATER
        ),
        "비 오는 저녁, 감성 가득한 실내 공간에서"
    ),

    EVENING_SNOWY(
        TimeSlot.EVENING,
        WeatherCondition.SNOWY,
        Arrays.asList(
            PlaceCategory.KOREAN_FOOD,
            PlaceCategory.RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.WINE_BAR,
            PlaceCategory.CAFE
        ),
        "눈 오는 저녁, 따뜻한 음식과 분위기를"
    ),

    // ============================================
    // 밤 (23시)
    // ============================================
    LATE_NIGHT_SUNNY(
        TimeSlot.LATE_NIGHT,
        WeatherCondition.SUNNY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.CRAFT_BEER,
            PlaceCategory.HANGANG_PARK
        ),
        "맑은 밤, 야외에서 시원한 바람을 맞으며"
    ),

    LATE_NIGHT_CLOUDY(
        TimeSlot.LATE_NIGHT,
        WeatherCondition.CLOUDY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.PUB,
            PlaceCategory.JAZZ_BAR
        ),
        "흐린 밤, 실내에서 여유로운 시간을"
    ),

    LATE_NIGHT_RAINY(
        TimeSlot.LATE_NIGHT,
        WeatherCondition.RAINY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.BAR,
            PlaceCategory.WINE_BAR,
            PlaceCategory.JAZZ_BAR
        ),
        "비 오는 밤, 감성 있는 공간에서"
    ),

    LATE_NIGHT_SNOWY(
        TimeSlot.LATE_NIGHT,
        WeatherCondition.SNOWY,
        Arrays.asList(
            PlaceCategory.LATE_NIGHT_CAFE,
            PlaceCategory.NIGHT_RESTAURANT,
            PlaceCategory.KOREAN_FOOD,
            PlaceCategory.BAR,
            PlaceCategory.TEA_HOUSE
        ),
        "눈 오는 밤, 따뜻한 곳에서 밤을 즐기세요"
    );

    // ============================================
    // 필드 및 생성자
    // ============================================
    private final TimeSlot timeSlot;
    private final WeatherCondition weatherCondition;
    private final List<PlaceCategory> recommendedCategories;
    private final String reasonText;

    CategoryRecommendationRule(TimeSlot timeSlot, WeatherCondition weather,
                               List<PlaceCategory> categories, String reason) {
        this.timeSlot = timeSlot;
        this.weatherCondition = weather;
        this.recommendedCategories = categories;
        this.reasonText = reason;
    }

    // ============================================
    // 유틸리티 메서드
    // ============================================

    /**
     * 현재 시간과 날씨에 맞는 추천 규칙 찾기
     *
     * @param timeSlot 시간대
     * @param weather 날씨 상태
     * @return 매칭되는 추천 규칙
     */
    public static CategoryRecommendationRule findRule(TimeSlot timeSlot, WeatherCondition weather) {
        for (CategoryRecommendationRule rule : values()) {
            if (rule.timeSlot == timeSlot && rule.weatherCondition == weather) {
                return rule;
            }
        }
        // 기본값: 해당 시간대의 맑음 날씨 규칙 반환
        return findDefaultRule(timeSlot);
    }

    /**
     * 시간대별 기본 규칙 (날씨 정보 없을 때)
     *
     * @param timeSlot 시간대
     * @return 기본 추천 규칙 (맑음 기준)
     */
    private static CategoryRecommendationRule findDefaultRule(TimeSlot timeSlot) {
        switch (timeSlot) {
            case EARLY_MORNING: return EARLY_MORNING_SUNNY;
            case MORNING: return MORNING_SUNNY;
            case AFTERNOON: return AFTERNOON_SUNNY;
            case EVENING: return EVENING_SUNNY;
            case LATE_NIGHT: return LATE_NIGHT_SUNNY;
            default: return AFTERNOON_SUNNY;
        }
    }

    // Getters
    public TimeSlot getTimeSlot() {
        return timeSlot;
    }

    public WeatherCondition getWeatherCondition() {
        return weatherCondition;
    }

    public List<PlaceCategory> getRecommendedCategories() {
        return recommendedCategories;
    }

    public String getReasonText() {
        return reasonText;
    }
}
