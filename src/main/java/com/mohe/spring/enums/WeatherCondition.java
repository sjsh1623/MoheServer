package com.mohe.spring.enums;

import java.util.Arrays;
import java.util.List;

/**
 * 날씨 상태 Enum
 *
 * <p>날씨 API 응답을 표준화된 날씨 상태로 변환합니다.</p>
 *
 * @author Andrew Lim
 * @since 1.0
 */
public enum WeatherCondition {

    /** 맑음 */
    SUNNY("맑음", "sunny", Arrays.asList(
        "맑음", "clear", "sunny", "fine", "fair", "晴"
    )),

    /** 흐림 */
    CLOUDY("흐림", "cloudy", Arrays.asList(
        "흐림", "cloudy", "overcast", "partly cloudy", "구름", "曇"
    )),

    /** 비 */
    RAINY("비", "rainy", Arrays.asList(
        "비", "rain", "rainy", "drizzle", "shower", "소나기", "雨"
    )),

    /** 눈 */
    SNOWY("눈", "snowy", Arrays.asList(
        "눈", "snow", "snowy", "blizzard", "설", "雪"
    ));

    private final String displayName;
    private final String key;
    private final List<String> matchKeywords;

    WeatherCondition(String displayName, String key, List<String> matchKeywords) {
        this.displayName = displayName;
        this.key = key;
        this.matchKeywords = matchKeywords;
    }

    /**
     * 날씨 텍스트로부터 WeatherCondition 매칭
     *
     * @param weatherText 날씨 텍스트 (예: "맑음", "cloudy", "rain")
     * @return 매칭되는 WeatherCondition, 없으면 SUNNY (기본값)
     */
    public static WeatherCondition fromText(String weatherText) {
        if (weatherText == null || weatherText.trim().isEmpty()) {
            return SUNNY; // 기본값
        }

        String lowerText = weatherText.toLowerCase().trim();

        for (WeatherCondition condition : values()) {
            for (String keyword : condition.matchKeywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return condition;
                }
            }
        }

        // 기본값: 맑음
        return SUNNY;
    }

    /**
     * 날씨 코드로부터 WeatherCondition 매칭 (OpenWeather API 등)
     *
     * @param weatherCode 날씨 코드
     * @return 매칭되는 WeatherCondition
     */
    public static WeatherCondition fromCode(int weatherCode) {
        // OpenWeatherMap API 날씨 코드 기준
        if (weatherCode >= 200 && weatherCode < 600) {
            // Thunderstorm, Drizzle, Rain
            return RAINY;
        } else if (weatherCode >= 600 && weatherCode < 700) {
            // Snow
            return SNOWY;
        } else if (weatherCode >= 801 && weatherCode <= 804) {
            // Clouds
            return CLOUDY;
        } else if (weatherCode == 800) {
            // Clear
            return SUNNY;
        }

        return SUNNY; // 기본값
    }

    // Getters
    public String getDisplayName() {
        return displayName;
    }

    public String getKey() {
        return key;
    }

    public List<String> getMatchKeywords() {
        return matchKeywords;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
