package com.mohe.spring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.*;

/**
 * Service for generating dynamic recommendation messages based on weather, time, and context
 */
@Service
public class DynamicMessageService {

    private static final Logger logger = LoggerFactory.getLogger(DynamicMessageService.class);
    private final Random random = new Random();

    // ============================
    // 맑은 날씨 메시지
    // ============================
    private static final List<String> CLEAR_MORNING_MESSAGES = List.of(
        "맑은 아침, 이런 곳은 어때요?",
        "날씨 좋은 아침에 가기 좋은 곳",
        "상쾌한 아침에 추천하는 플레이스",
        "오늘 아침 여기 어때요?",
        "아침 산책하기 좋은 날씨예요",
        "햇살 좋은 아침, 여기 추천해요",
        "맑은 아침에 어울리는 곳"
    );

    private static final List<String> CLEAR_AFTERNOON_MESSAGES = List.of(
        "날씨 좋은 오후, 여기 어때요?",
        "맑은 오후에 가볼 만한 곳",
        "오후 나들이하기 좋은 날이에요",
        "햇살 좋은 오후 추천 플레이스",
        "이런 날씨엔 여기가 딱이에요",
        "오후에 가기 좋은 곳 추천",
        "날씨 좋을 때 가면 좋은 곳",
        "맑은 오후, 이런 곳 어때요?"
    );

    private static final List<String> CLEAR_EVENING_MESSAGES = List.of(
        "저녁노을 보기 좋은 곳",
        "맑은 저녁, 여기 어때요?",
        "오늘 저녁 가볼 만한 곳",
        "퇴근 후 들르기 좋은 플레이스",
        "저녁 시간에 추천하는 곳",
        "노을 지는 저녁에 가기 좋아요",
        "맑은 저녁에 어울리는 곳"
    );

    private static final List<String> CLEAR_NIGHT_MESSAGES = List.of(
        "맑은 밤, 여기 어때요?",
        "야경 보기 좋은 곳 추천",
        "밤에 가면 좋은 플레이스",
        "오늘 밤 가볼 만한 곳",
        "밤 산책하기 좋은 날씨예요",
        "야경이 예쁜 곳 추천해요",
        "맑은 밤에 가기 좋은 곳"
    );

    // ============================
    // 흐린 날씨 메시지
    // ============================
    private static final List<String> CLOUDY_MORNING_MESSAGES = List.of(
        "흐린 아침, 이런 곳은 어때요?",
        "구름 낀 아침에 가기 좋은 곳",
        "흐린 날 아침 추천 플레이스",
        "오늘 아침 여기 어때요?",
        "흐린 아침에 어울리는 곳"
    );

    private static final List<String> CLOUDY_AFTERNOON_MESSAGES = List.of(
        "흐린 오후, 이런 곳은 어때요?",
        "구름 낀 오후에 가볼 만한 곳",
        "흐린 날씨에 가기 좋은 플레이스",
        "이런 날엔 여기가 좋아요",
        "흐린 오후 추천 장소",
        "오늘 오후 여기 어때요?"
    );

    private static final List<String> CLOUDY_EVENING_MESSAGES = List.of(
        "흐린 저녁, 이런 곳은 어때요?",
        "구름 낀 저녁에 가기 좋은 곳",
        "흐린 날 저녁 추천 플레이스",
        "오늘 저녁 여기 어때요?",
        "흐린 저녁에 어울리는 곳"
    );

    private static final List<String> CLOUDY_NIGHT_MESSAGES = List.of(
        "흐린 밤, 이런 곳은 어때요?",
        "구름 낀 밤에 가기 좋은 곳",
        "흐린 날 밤 추천 플레이스",
        "오늘 밤 여기 어때요?",
        "흐린 밤에 어울리는 곳"
    );

    // ============================
    // 비 오는 날씨 메시지
    // ============================
    private static final List<String> RAINY_MORNING_MESSAGES = List.of(
        "비 오는 아침, 이런 곳은 어때요?",
        "비 오는 날 아침에 가기 좋은 곳",
        "비 오는 아침 추천 플레이스",
        "오늘 아침 비가 와요, 여기 어때요?",
        "비 오는 아침에 어울리는 곳",
        "우산 쓰고 가볼 만한 곳"
    );

    private static final List<String> RAINY_AFTERNOON_MESSAGES = List.of(
        "비 오는 오후, 이런 곳은 어때요?",
        "비 오는 날에 가기 좋은 곳",
        "비 오는 오후 추천 플레이스",
        "비 올 때 가면 좋은 곳",
        "비 오는 오후에 어울리는 곳",
        "비 오는 날 추천하는 곳",
        "오늘 비가 와요, 여기 어때요?"
    );

    private static final List<String> RAINY_EVENING_MESSAGES = List.of(
        "비 오는 저녁, 이런 곳은 어때요?",
        "비 오는 저녁에 가기 좋은 곳",
        "비 오는 저녁 추천 플레이스",
        "비 올 때 저녁 먹기 좋은 곳",
        "비 오는 저녁에 어울리는 곳",
        "오늘 저녁 비가 와요, 여기 어때요?"
    );

    private static final List<String> RAINY_NIGHT_MESSAGES = List.of(
        "비 오는 밤, 이런 곳은 어때요?",
        "비 오는 밤에 가기 좋은 곳",
        "비 오는 밤 추천 플레이스",
        "비 올 때 밤에 가면 좋은 곳",
        "비 오는 밤에 어울리는 곳",
        "오늘 밤 비가 와요, 여기 어때요?"
    );

    // ============================
    // 눈 오는 날씨 메시지
    // ============================
    private static final List<String> SNOWY_MORNING_MESSAGES = List.of(
        "눈 오는 아침, 이런 곳은 어때요?",
        "눈 오는 날 아침에 가기 좋은 곳",
        "눈 오는 아침 추천 플레이스",
        "오늘 아침 눈이 와요, 여기 어때요?",
        "눈 오는 아침에 어울리는 곳"
    );

    private static final List<String> SNOWY_AFTERNOON_MESSAGES = List.of(
        "눈 오는 오후, 이런 곳은 어때요?",
        "눈 오는 날에 가기 좋은 곳",
        "눈 오는 오후 추천 플레이스",
        "눈 올 때 가면 좋은 곳",
        "눈 오는 오후에 어울리는 곳"
    );

    private static final List<String> SNOWY_EVENING_MESSAGES = List.of(
        "눈 오는 저녁, 이런 곳은 어때요?",
        "눈 오는 저녁에 가기 좋은 곳",
        "눈 오는 저녁 추천 플레이스",
        "눈 올 때 저녁 먹기 좋은 곳",
        "눈 오는 저녁에 어울리는 곳"
    );

    private static final List<String> SNOWY_NIGHT_MESSAGES = List.of(
        "눈 오는 밤, 이런 곳은 어때요?",
        "눈 오는 밤에 가기 좋은 곳",
        "눈 오는 밤 추천 플레이스",
        "눈 올 때 밤에 가면 좋은 곳",
        "눈 오는 밤에 어울리는 곳"
    );

    // ============================
    // 더운 날씨 메시지
    // ============================
    private static final List<String> HOT_MORNING_MESSAGES = List.of(
        "더운 아침, 시원한 곳 어때요?",
        "더운 날 아침에 가기 좋은 곳",
        "더운 아침 추천 플레이스",
        "오늘 아침 더워요, 여기 어때요?",
        "더운 아침에 시원하게 보낼 곳"
    );

    private static final List<String> HOT_AFTERNOON_MESSAGES = List.of(
        "더운 오후, 시원한 곳 어때요?",
        "더운 날에 가기 좋은 곳",
        "더운 오후 추천 플레이스",
        "더울 때 가면 좋은 곳",
        "더운 오후에 시원하게 보낼 곳",
        "오늘 더워요, 여기 어때요?",
        "더운 날 추천하는 곳"
    );

    private static final List<String> HOT_EVENING_MESSAGES = List.of(
        "더운 저녁, 시원한 곳 어때요?",
        "더운 저녁에 가기 좋은 곳",
        "더운 저녁 추천 플레이스",
        "더울 때 저녁 먹기 좋은 곳",
        "더운 저녁에 시원하게 보낼 곳"
    );

    private static final List<String> HOT_NIGHT_MESSAGES = List.of(
        "더운 밤, 시원한 곳 어때요?",
        "더운 밤에 가기 좋은 곳",
        "더운 밤 추천 플레이스",
        "열대야에 가면 좋은 곳",
        "더운 밤에 시원하게 보낼 곳"
    );

    // ============================
    // 추운 날씨 메시지
    // ============================
    private static final List<String> COLD_MORNING_MESSAGES = List.of(
        "추운 아침, 따뜻한 곳 어때요?",
        "추운 날 아침에 가기 좋은 곳",
        "추운 아침 추천 플레이스",
        "오늘 아침 추워요, 여기 어때요?",
        "추운 아침에 따뜻하게 보낼 곳"
    );

    private static final List<String> COLD_AFTERNOON_MESSAGES = List.of(
        "추운 오후, 따뜻한 곳 어때요?",
        "추운 날에 가기 좋은 곳",
        "추운 오후 추천 플레이스",
        "추울 때 가면 좋은 곳",
        "추운 오후에 따뜻하게 보낼 곳",
        "오늘 추워요, 여기 어때요?"
    );

    private static final List<String> COLD_EVENING_MESSAGES = List.of(
        "추운 저녁, 따뜻한 곳 어때요?",
        "추운 저녁에 가기 좋은 곳",
        "추운 저녁 추천 플레이스",
        "추울 때 저녁 먹기 좋은 곳",
        "추운 저녁에 따뜻하게 보낼 곳"
    );

    private static final List<String> COLD_NIGHT_MESSAGES = List.of(
        "추운 밤, 따뜻한 곳 어때요?",
        "추운 밤에 가기 좋은 곳",
        "추운 밤 추천 플레이스",
        "추울 때 밤에 가면 좋은 곳",
        "추운 밤에 따뜻하게 보낼 곳"
    );

    // ============================
    // 안개 낀 날씨 메시지
    // ============================
    private static final List<String> FOGGY_MESSAGES = List.of(
        "안개 낀 날, 이런 곳은 어때요?",
        "안개 낀 날에 가기 좋은 곳",
        "안개 낀 날 추천 플레이스",
        "오늘 안개가 꼈어요, 여기 어때요?",
        "안개 낀 날에 어울리는 곳"
    );

    // ============================
    // 주말 메시지
    // ============================
    private static final List<String> WEEKEND_MORNING_MESSAGES = List.of(
        "주말 아침, 이런 곳은 어때요?",
        "주말 아침에 가기 좋은 곳",
        "주말 아침 추천 플레이스"
    );

    private static final List<String> WEEKEND_AFTERNOON_MESSAGES = List.of(
        "주말 오후, 이런 곳은 어때요?",
        "주말 오후에 가기 좋은 곳",
        "주말 오후 추천 플레이스"
    );

    /**
     * Generate a dynamic message based on weather condition and time of day
     */
    public String generateMessage(String weatherConditionCode, String weatherConditionText, String timeOfDay) {
        String normalizedTime = normalizeTimeOfDay(timeOfDay);
        String normalizedWeather = normalizeWeatherCondition(weatherConditionCode, weatherConditionText);

        logger.info("Generating message for weather={}, time={}", normalizedWeather, normalizedTime);

        // Check if it's weekend for special messages (with 30% probability)
        if (isWeekend() && random.nextDouble() < 0.3) {
            String weekendMessage = getWeekendMessage(normalizedTime);
            if (weekendMessage != null) {
                return weekendMessage;
            }
        }

        List<String> messages = getMessagesForWeatherAndTime(normalizedWeather, normalizedTime);
        if (messages != null && !messages.isEmpty()) {
            String message = messages.get(random.nextInt(messages.size()));
            logger.info("Selected message: {}", message);
            return message;
        }

        return "지금 가기 좋은 플레이스";
    }

    private List<String> getMessagesForWeatherAndTime(String weather, String time) {
        return switch (weather) {
            case "clear", "sunny" -> switch (time) {
                case "morning" -> CLEAR_MORNING_MESSAGES;
                case "afternoon" -> CLEAR_AFTERNOON_MESSAGES;
                case "evening" -> CLEAR_EVENING_MESSAGES;
                case "night" -> CLEAR_NIGHT_MESSAGES;
                default -> CLEAR_AFTERNOON_MESSAGES;
            };
            case "cloudy", "overcast" -> switch (time) {
                case "morning" -> CLOUDY_MORNING_MESSAGES;
                case "afternoon" -> CLOUDY_AFTERNOON_MESSAGES;
                case "evening" -> CLOUDY_EVENING_MESSAGES;
                case "night" -> CLOUDY_NIGHT_MESSAGES;
                default -> CLOUDY_AFTERNOON_MESSAGES;
            };
            case "rain", "drizzle", "shower" -> switch (time) {
                case "morning" -> RAINY_MORNING_MESSAGES;
                case "afternoon" -> RAINY_AFTERNOON_MESSAGES;
                case "evening" -> RAINY_EVENING_MESSAGES;
                case "night" -> RAINY_NIGHT_MESSAGES;
                default -> RAINY_AFTERNOON_MESSAGES;
            };
            case "snow" -> switch (time) {
                case "morning" -> SNOWY_MORNING_MESSAGES;
                case "afternoon" -> SNOWY_AFTERNOON_MESSAGES;
                case "evening" -> SNOWY_EVENING_MESSAGES;
                case "night" -> SNOWY_NIGHT_MESSAGES;
                default -> SNOWY_AFTERNOON_MESSAGES;
            };
            case "hot" -> switch (time) {
                case "morning" -> HOT_MORNING_MESSAGES;
                case "afternoon" -> HOT_AFTERNOON_MESSAGES;
                case "evening" -> HOT_EVENING_MESSAGES;
                case "night" -> HOT_NIGHT_MESSAGES;
                default -> HOT_AFTERNOON_MESSAGES;
            };
            case "cold" -> switch (time) {
                case "morning" -> COLD_MORNING_MESSAGES;
                case "afternoon" -> COLD_AFTERNOON_MESSAGES;
                case "evening" -> COLD_EVENING_MESSAGES;
                case "night" -> COLD_NIGHT_MESSAGES;
                default -> COLD_AFTERNOON_MESSAGES;
            };
            case "fog", "mist" -> FOGGY_MESSAGES;
            default -> switch (time) {
                case "morning" -> CLEAR_MORNING_MESSAGES;
                case "afternoon" -> CLEAR_AFTERNOON_MESSAGES;
                case "evening" -> CLEAR_EVENING_MESSAGES;
                case "night" -> CLEAR_NIGHT_MESSAGES;
                default -> CLEAR_AFTERNOON_MESSAGES;
            };
        };
    }

    private String getWeekendMessage(String time) {
        return switch (time) {
            case "morning" -> WEEKEND_MORNING_MESSAGES.get(random.nextInt(WEEKEND_MORNING_MESSAGES.size()));
            case "afternoon" -> WEEKEND_AFTERNOON_MESSAGES.get(random.nextInt(WEEKEND_AFTERNOON_MESSAGES.size()));
            default -> null;
        };
    }

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private boolean isWeekend() {
        DayOfWeek day = LocalDate.now(KOREA_ZONE).getDayOfWeek();
        return day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY;
    }

    /**
     * Generate search keywords based on weather and time context
     */
    public List<String> generateSearchKeywords(String weatherConditionCode, String weatherConditionText, String timeOfDay) {
        List<String> keywords = new ArrayList<>();
        String normalizedTime = normalizeTimeOfDay(timeOfDay);
        String normalizedWeather = normalizeWeatherCondition(weatherConditionCode, weatherConditionText);

        // Add time-based keywords
        switch (normalizedTime) {
            case "morning" -> keywords.addAll(List.of("아침", "브런치", "모닝", "조식", "카페", "베이커리"));
            case "afternoon" -> keywords.addAll(List.of("점심", "오후", "런치", "낮", "디저트", "휴식"));
            case "evening" -> keywords.addAll(List.of("저녁", "디너", "석양", "노을", "맛집", "레스토랑"));
            case "night" -> keywords.addAll(List.of("야경", "밤", "야간", "라운지", "바", "분위기"));
        }

        // Add weather-based keywords
        switch (normalizedWeather) {
            case "rain", "drizzle", "shower" -> keywords.addAll(List.of("실내", "아늑한", "카페", "우중", "창가", "비"));
            case "clear", "sunny" -> keywords.addAll(List.of("야외", "테라스", "산책", "공원", "루프탑", "햇살"));
            case "cloudy", "overcast" -> keywords.addAll(List.of("분위기", "포근한", "아늑한", "감성", "조용한"));
            case "hot" -> keywords.addAll(List.of("시원한", "에어컨", "아이스", "쿨링", "빙수", "냉방"));
            case "cold" -> keywords.addAll(List.of("따뜻한", "온기", "히터", "따스한", "국물", "핫초코"));
            case "snow" -> keywords.addAll(List.of("눈", "겨울", "로맨틱", "창가", "따뜻한", "분위기"));
            case "fog", "mist" -> keywords.addAll(List.of("몽환적", "분위기", "아늑한", "감성", "조용한"));
        }

        logger.info("Generated search keywords: {}", keywords);
        return keywords;
    }

    private String normalizeTimeOfDay(String timeOfDay) {
        if (timeOfDay == null || timeOfDay.isBlank()) {
            return getCurrentTimeOfDay();
        }

        return switch (timeOfDay.toLowerCase()) {
            case "morning", "아침" -> "morning";
            case "afternoon", "오후", "낮" -> "afternoon";
            case "evening", "저녁" -> "evening";
            case "night", "밤", "야간" -> "night";
            default -> getCurrentTimeOfDay();
        };
    }

    private String normalizeWeatherCondition(String conditionCode, String conditionText) {
        // First try to normalize from code
        if (conditionCode != null && !conditionCode.isBlank()) {
            String lowerCode = conditionCode.toLowerCase();
            if (lowerCode.contains("rain") || lowerCode.contains("shower")) return "rain";
            if (lowerCode.contains("drizzle")) return "drizzle";
            if (lowerCode.contains("snow")) return "snow";
            if (lowerCode.contains("clear") || lowerCode.contains("sunny")) return "clear";
            if (lowerCode.contains("cloud") || lowerCode.contains("overcast")) return "cloudy";
            if (lowerCode.contains("fog") || lowerCode.contains("mist")) return "fog";
            if (lowerCode.contains("hot")) return "hot";
            if (lowerCode.contains("cold")) return "cold";
        }

        // Then try to normalize from Korean text
        if (conditionText != null && !conditionText.isBlank()) {
            if (conditionText.contains("비") || conditionText.contains("소나기")) return "rain";
            if (conditionText.contains("이슬비")) return "drizzle";
            if (conditionText.contains("눈")) return "snow";
            if (conditionText.contains("맑")) return "clear";
            if (conditionText.contains("흐") || conditionText.contains("구름")) return "cloudy";
            if (conditionText.contains("안개")) return "fog";
            if (conditionText.contains("더") || conditionText.contains("hot")) return "hot";
            if (conditionText.contains("추") || conditionText.contains("cold")) return "cold";
        }

        return "clear"; // Default
    }

    private String getCurrentTimeOfDay() {
        int hour = LocalTime.now(KOREA_ZONE).getHour();
        logger.debug("Current Korea time hour: {}", hour);
        if (hour >= 6 && hour < 12) return "morning";
        if (hour >= 12 && hour < 18) return "afternoon";
        if (hour >= 18 && hour < 22) return "evening";
        return "night";
    }
}
