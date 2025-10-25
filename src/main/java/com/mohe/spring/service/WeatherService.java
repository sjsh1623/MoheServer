package com.mohe.spring.service;

import com.mohe.spring.dto.WeatherResponse;
import com.mohe.spring.util.GeoGridConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Weather data provider interface for pluggable weather services
 */
interface WeatherProvider {
    WeatherData getCurrentWeather(double lat, double lon);
}

/**
 * Normalized weather data structure
 */
class WeatherData {
    private final double tempC;
    private final double tempF;
    private final String conditionCode; // "clear", "cloudy", "rain", "snow", etc.
    private final String conditionText;
    private final int humidity;
    private final double windSpeedKmh;
    private final String daypart; // "morning", "afternoon", "evening", "night"
    private final LocalDateTime timestamp;
    
    public WeatherData(double tempC, double tempF, String conditionCode, String conditionText,
                       int humidity, double windSpeedKmh, String daypart) {
        this.tempC = tempC;
        this.tempF = tempF;
        this.conditionCode = conditionCode;
        this.conditionText = conditionText;
        this.humidity = humidity;
        this.windSpeedKmh = windSpeedKmh;
        this.daypart = daypart;
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters
    public double getTempC() { return tempC; }
    public double getTempF() { return tempF; }
    public String getConditionCode() { return conditionCode; }
    public String getConditionText() { return conditionText; }
    public int getHumidity() { return humidity; }
    public double getWindSpeedKmh() { return windSpeedKmh; }
    public String getDaypart() { return daypart; }
    public LocalDateTime getTimestamp() { return timestamp; }
}

/**
 * Weather context for recommendations
 */
class WeatherContext {
    private final WeatherData weather;
    private final boolean isRainy;
    private final boolean isCold;
    private final boolean isHot;
    private final boolean isComfortable;
    private final List<String> recommendedActivities;
    
    public WeatherContext(WeatherData weather, boolean isRainy, boolean isCold, 
                         boolean isHot, boolean isComfortable, List<String> recommendedActivities) {
        this.weather = weather;
        this.isRainy = isRainy;
        this.isCold = isCold;
        this.isHot = isHot;
        this.isComfortable = isComfortable;
        this.recommendedActivities = new ArrayList<>(recommendedActivities);
    }
    
    public static WeatherContext from(WeatherData weather) {
        boolean isRainy = weather.getConditionCode().toLowerCase().contains("rain") || 
                         weather.getConditionCode().toLowerCase().contains("shower");
        boolean isCold = weather.getTempC() < 10;
        boolean isHot = weather.getTempC() > 28;
        boolean isComfortable = weather.getTempC() >= 18.0 && weather.getTempC() <= 25.0;
        
        List<String> recommendedActivities = new ArrayList<>();
        if (isRainy) {
            recommendedActivities.addAll(Arrays.asList("indoor", "cafe", "museum", "shopping"));
        } else if (isCold) {
            recommendedActivities.addAll(Arrays.asList("indoor", "warm_place", "hot_drink"));
        } else if (isHot) {
            recommendedActivities.addAll(Arrays.asList("air_conditioned", "cold_drink", "indoor"));
        } else if (isComfortable) {
            recommendedActivities.addAll(Arrays.asList("outdoor", "walking", "park"));
        }
        
        return new WeatherContext(weather, isRainy, isCold, isHot, isComfortable, recommendedActivities);
    }
    
    // Getters
    public WeatherData getWeather() { return weather; }
    public boolean isRainy() { return isRainy; }
    public boolean isCold() { return isCold; }
    public boolean isHot() { return isHot; }
    public boolean isComfortable() { return isComfortable; }
    public List<String> getRecommendedActivities() { return new ArrayList<>(recommendedActivities); }
}

/**
 * Open-Meteo implementation of WeatherProvider (no API key required)
 */
@Service
class OpenMeteoProvider implements WeatherProvider {
    
    private static final Logger logger = LoggerFactory.getLogger(OpenMeteoProvider.class);
    private static final String BASE_URL = "https://api.open-meteo.com/v1/forecast";
    
    private final WebClient webClient;
    
    public OpenMeteoProvider(WebClient webClient) {
        this.webClient = webClient;
    }
    
    @Override
    public WeatherData getCurrentWeather(double lat, double lon) {
        try {
            String url = BASE_URL + "?latitude=" + lat + "&longitude=" + lon + 
                        "&current_weather=true&timezone=auto&temperature_unit=celsius&windspeed_unit=kmh";
            
            logger.info("Fetching weather data from Open-Meteo: {}", url);
            
            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof WebClientResponseException)
                )
                .block(Duration.ofSeconds(10));
                
            if (response == null) {
                throw new RuntimeException("Empty response from Open-Meteo");
            }
            
            return parseOpenMeteoResponse(response);
            
        } catch (Exception e) {
            logger.error("Failed to fetch weather data from Open-Meteo for lat={}, lon={}: {}", lat, lon, e.getMessage());
            throw new RuntimeException("Unable to retrieve weather data right now", e);
        }
    }
    
    @SuppressWarnings("unchecked")
    private WeatherData parseOpenMeteoResponse(Map<String, Object> response) {
        Map<String, Object> currentWeather = (Map<String, Object>) response.get("current_weather");
        
        if (currentWeather == null) {
            throw new RuntimeException("No current_weather data in Open-Meteo response");
        }
        
        double tempC = ((Number) currentWeather.get("temperature")).doubleValue();
        double tempF = tempC * 9 / 5 + 32;
        double windSpeedKmh = ((Number) currentWeather.get("windspeed")).doubleValue();
        int weatherCode = ((Number) currentWeather.get("weathercode")).intValue();
        
        String conditionCode = mapWeatherCodeToCondition(weatherCode);
        String conditionText = getWeatherDescription(weatherCode, tempC);
        
        String daypart = determineDaypart();
        
        // Open-Meteo doesn't provide humidity, so we'll estimate based on weather conditions
        int estimatedHumidity = estimateHumidity(weatherCode);
        
        logger.info("Parsed Open-Meteo weather: {}°C, {}, wind: {}km/h", tempC, conditionText, windSpeedKmh);
        
        return new WeatherData(tempC, tempF, conditionCode, conditionText, estimatedHumidity, windSpeedKmh, daypart);
    }
    
    /**
     * Maps Open-Meteo weather codes to normalized condition codes
     * Reference: https://open-meteo.com/en/docs
     */
    private String mapWeatherCodeToCondition(int weatherCode) {
        switch (weatherCode) {
            case 0: return "clear";
            case 1: case 2: case 3: return "cloudy";
            case 45: case 48: return "fog";
            case 51: case 53: case 55: return "drizzle";
            case 56: case 57: return "freezing_drizzle";
            case 61: case 63: case 65: return "rain";
            case 66: case 67: return "freezing_rain";
            case 71: case 73: case 75: return "snow";
            case 77: return "snow_grains";
            case 80: case 81: case 82: return "rain_showers";
            case 85: case 86: return "snow_showers";
            case 95: return "thunderstorm";
            case 96: case 99: return "thunderstorm_with_hail";
            default: return "unknown";
        }
    }
    
    /**
     * Get Korean weather description based on weather code and temperature
     */
    private String getWeatherDescription(int weatherCode, double tempC) {
        switch (weatherCode) {
            case 0: return tempC > 25 ? "맑고 더움" : tempC < 10 ? "맑고 추움" : "맑음";
            case 1: return "대체로 맑음";
            case 2: return "부분적으로 흐림";
            case 3: return "흐림";
            case 45: case 48: return "안개";
            case 51: return "가벼운 이슬비";
            case 53: return "보통 이슬비";
            case 55: return "심한 이슬비";
            case 56: case 57: return "얼어붙는 이슬비";
            case 61: return "가벼운 비";
            case 63: return "보통 비";
            case 65: return "심한 비";
            case 66: case 67: return "얼어붙는 비";
            case 71: return "가벼운 눈";
            case 73: return "보통 눈";
            case 75: return "심한 눈";
            case 77: return "눈알갱이";
            case 80: return "가벼운 소나기";
            case 81: return "보통 소나기";
            case 82: return "심한 소나기";
            case 85: return "가벼운 눈보라";
            case 86: return "심한 눈보라";
            case 95: return "천둥번개";
            case 96: case 99: return "천둥번개와 우박";
            default: return "알 수 없는 날씨";
        }
    }
    
    /**
     * Estimate humidity based on weather conditions
     */
    private int estimateHumidity(int weatherCode) {
        switch (weatherCode) {
            case 0: return 45; // Clear
            case 1: case 2: return 55; // Partly cloudy
            case 3: return 70; // Overcast
            case 45: case 48: return 95; // Fog
            case 51: case 53: case 55: return 85; // Drizzle
            case 61: case 63: case 65: return 90; // Rain
            case 71: case 73: case 75: return 80; // Snow
            case 80: case 81: case 82: return 85; // Rain showers
            case 95: case 96: case 99: return 85; // Thunderstorm
            default: return 60; // Default
        }
    }
    
    private String determineDaypart() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 6 && hour <= 11) return "morning";
        if (hour >= 12 && hour <= 17) return "afternoon";
        if (hour >= 18 && hour <= 21) return "evening";
        return "night";
    }
}

/**
 * Korean Meteorological Administration (KMA) implementation of WeatherProvider
 */
@Service
class KMAWeatherProvider implements WeatherProvider {

    private static final Logger logger = LoggerFactory.getLogger(KMAWeatherProvider.class);
    private static final String BASE_URL = "http://apis.data.go.kr/1360000/VilageFcstInfoService_2.0/getUltraSrtNcst";

    private final WebClient webClient;
    private final String serviceKey;

    public KMAWeatherProvider(
            WebClient webClient,
            @Value("${api.kma.service-key:}") String serviceKey) {
        this.webClient = webClient;
        this.serviceKey = serviceKey;
    }

    @Override
    public WeatherData getCurrentWeather(double lat, double lon) {
        if (serviceKey == null || serviceKey.isBlank()) {
            throw new RuntimeException("KMA API service key is not configured");
        }

        if (!GeoGridConverter.isValidKoreanCoordinate(lat, lon)) {
            throw new RuntimeException("Coordinates are outside Korean territory");
        }

        try {
            GeoGridConverter.GridCoordinate grid = GeoGridConverter.toGrid(lat, lon);

            LocalDateTime now = LocalDateTime.now();
            // KMA API provides data at hourly intervals, use current hour
            String baseDate = now.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String baseTime = now.format(DateTimeFormatter.ofPattern("HH00"));

            // Build query URL
            String url = BASE_URL +
                "?serviceKey=" + serviceKey +
                "&pageNo=1" +
                "&numOfRows=100" +
                "&dataType=JSON" +
                "&base_date=" + baseDate +
                "&base_time=" + baseTime +
                "&nx=" + grid.getNx() +
                "&ny=" + grid.getNy();

            logger.info("Fetching KMA weather data: baseDate={}, baseTime={}, nx={}, ny={}",
                       baseDate, baseTime, grid.getNx(), grid.getNy());

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(
                    Retry.backoff(2, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(3))
                        .filter(throwable -> throwable instanceof WebClientResponseException)
                )
                .block(Duration.ofSeconds(10));

            if (response == null) {
                throw new RuntimeException("Empty response from KMA API");
            }

            return parseKMAResponse(response);

        } catch (Exception e) {
            logger.error("Failed to fetch weather data from KMA for lat={}, lon={}: {}", lat, lon, e.getMessage());
            throw new RuntimeException("Unable to retrieve weather data from KMA: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private WeatherData parseKMAResponse(Map<String, Object> response) {
        Map<String, Object> responseObj = (Map<String, Object>) response.get("response");
        if (responseObj == null) {
            throw new RuntimeException("No response object in KMA API response");
        }

        Map<String, Object> header = (Map<String, Object>) responseObj.get("header");
        if (header != null) {
            String resultCode = (String) header.get("resultCode");
            if (!"00".equals(resultCode)) {
                String resultMsg = (String) header.get("resultMsg");
                throw new RuntimeException("KMA API error: " + resultMsg);
            }
        }

        Map<String, Object> body = (Map<String, Object>) responseObj.get("body");
        if (body == null) {
            throw new RuntimeException("No body in KMA API response");
        }

        Map<String, Object> items = (Map<String, Object>) body.get("items");
        if (items == null) {
            throw new RuntimeException("No items in KMA API response");
        }

        List<Map<String, Object>> itemList = (List<Map<String, Object>>) items.get("item");
        if (itemList == null || itemList.isEmpty()) {
            throw new RuntimeException("No weather data items in KMA API response");
        }

        // Parse weather data from items
        Double tempC = null;
        Double windSpeedMs = null;
        Integer humidity = null;
        Double precipitationMm = null;

        for (Map<String, Object> item : itemList) {
            String category = (String) item.get("category");
            String obsrValue = (String) item.get("obsrValue");

            if (obsrValue == null || obsrValue.isEmpty()) continue;

            switch (category) {
                case "T1H": // 기온
                    tempC = Double.parseDouble(obsrValue);
                    break;
                case "RN1": // 1시간 강수량
                    precipitationMm = Double.parseDouble(obsrValue);
                    break;
                case "REH": // 습도
                    humidity = (int) Double.parseDouble(obsrValue);
                    break;
                case "WSD": // 풍속
                    windSpeedMs = Double.parseDouble(obsrValue);
                    break;
            }
        }

        if (tempC == null) {
            throw new RuntimeException("Temperature data not found in KMA response");
        }

        double tempF = tempC * 9 / 5 + 32;
        double windSpeedKmh = windSpeedMs != null ? windSpeedMs * 3.6 : 0.0;
        int humidityValue = humidity != null ? humidity : 50;

        // Determine weather condition
        String conditionCode;
        String conditionText;
        if (precipitationMm != null && precipitationMm > 0) {
            if (precipitationMm >= 10.0) {
                conditionCode = "rain";
                conditionText = "강한 비";
            } else if (precipitationMm >= 3.0) {
                conditionCode = "rain";
                conditionText = "보통 비";
            } else {
                conditionCode = "drizzle";
                conditionText = "약한 비";
            }
        } else if (tempC < 0) {
            conditionCode = "cold";
            conditionText = "맑고 추움";
        } else if (tempC > 28) {
            conditionCode = "clear";
            conditionText = "맑고 더움";
        } else {
            conditionCode = "clear";
            conditionText = "맑음";
        }

        String daypart = determineDaypart();

        logger.info("Parsed KMA weather: {}°C, {}, wind: {}km/h, humidity: {}%",
                   tempC, conditionText, windSpeedKmh, humidityValue);

        return new WeatherData(tempC, tempF, conditionCode, conditionText,
                              humidityValue, windSpeedKmh, daypart);
    }

    private String determineDaypart() {
        int hour = LocalDateTime.now().getHour();
        if (hour >= 6 && hour <= 11) return "morning";
        if (hour >= 12 && hour <= 17) return "afternoon";
        if (hour >= 18 && hour <= 21) return "evening";
        return "night";
    }
}

/**
 * Weather service orchestrator with pluggable providers
 */
@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);
    private static final Duration CACHE_TIMEOUT = Duration.ofMinutes(10);

    private final WeatherProvider weatherProvider;
    private final WeatherProvider fallbackProvider;

    // Simple in-memory cache for 10 minutes
    private final Map<String, CacheEntry> weatherCache = new ConcurrentHashMap<>();

    public WeatherService(KMAWeatherProvider kmaWeatherProvider,
                         OpenMeteoProvider openMeteoProvider,
                         @Value("${api.kma.service-key:}") String kmaServiceKey) {
        // Use KMA as primary provider if configured, otherwise use OpenMeteo
        if (kmaServiceKey != null && !kmaServiceKey.isBlank()) {
            this.weatherProvider = kmaWeatherProvider;
            this.fallbackProvider = openMeteoProvider;
            logger.info("Weather service initialized with KMA as primary provider");
        } else {
            this.weatherProvider = openMeteoProvider;
            this.fallbackProvider = null;
            logger.info("Weather service initialized with OpenMeteo provider (KMA not configured)");
        }
    }
    
    public WeatherData getCurrentWeather(double lat, double lon) {
        String cacheKey = lat + "_" + lon;
        CacheEntry cached = weatherCache.get(cacheKey);

        if (cached != null && Duration.between(cached.timestamp, LocalDateTime.now()).compareTo(CACHE_TIMEOUT) < 0) {
            logger.debug("Returning cached weather data for lat={}, lon={}", lat, lon);
            return cached.weatherData;
        }

        logger.info("Fetching fresh weather data for lat={}, lon={}", lat, lon);

        WeatherData weather;
        try {
            weather = weatherProvider.getCurrentWeather(lat, lon);
        } catch (Exception e) {
            logger.warn("Primary weather provider failed, attempting fallback: {}", e.getMessage());
            if (fallbackProvider != null) {
                weather = fallbackProvider.getCurrentWeather(lat, lon);
            } else {
                throw e;
            }
        }

        weatherCache.put(cacheKey, new CacheEntry(weather, LocalDateTime.now()));

        // Clean old cache entries
        cleanupCache();

        return weather;
    }
    
    public WeatherContext getWeatherContext(double lat, double lon) {
        WeatherData weather = getCurrentWeather(lat, lon);
        return WeatherContext.from(weather);
    }

    /**
     * Get weather as WeatherResponse DTO for API consumption
     */
    public WeatherResponse getCurrentWeatherResponse(double lat, double lon, String provider) {
        WeatherData weather = getCurrentWeather(lat, lon);
        return new WeatherResponse(
            weather.getTempC(),
            weather.getTempF(),
            weather.getConditionCode(),
            weather.getConditionText(),
            weather.getHumidity(),
            weather.getWindSpeedKmh(),
            weather.getDaypart(),
            lat,
            lon,
            provider
        );
    }

    private void cleanupCache() {
        LocalDateTime now = LocalDateTime.now();
        List<String> expiredKeys = weatherCache.entrySet().stream()
                .filter(entry -> Duration.between(entry.getValue().timestamp, now).compareTo(CACHE_TIMEOUT) > 0)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        
        expiredKeys.forEach(weatherCache::remove);
    }
    
    private static class CacheEntry {
        final WeatherData weatherData;
        final LocalDateTime timestamp;
        
        CacheEntry(WeatherData weatherData, LocalDateTime timestamp) {
            this.weatherData = weatherData;
            this.timestamp = timestamp;
        }
    }
}