package com.mohe.spring.service;

import com.mohe.spring.dto.CategoryDto;
import com.mohe.spring.dto.SuggestedCategoriesResponse;
import com.mohe.spring.enums.CategoryRecommendationRule;
import com.mohe.spring.enums.PlaceCategory;
import com.mohe.spring.enums.TimeSlot;
import com.mohe.spring.enums.WeatherCondition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
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

    public CategoryRecommendationService(WeatherService weatherService) {
        this.weatherService = weatherService;
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
