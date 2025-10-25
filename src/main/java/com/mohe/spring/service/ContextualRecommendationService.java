package com.mohe.spring.service;

import com.mohe.spring.dto.ContextualRecommendationResponse;
import com.mohe.spring.dto.PlaceDto;
import com.mohe.spring.dto.VectorSimilarityResponse;
import com.mohe.spring.entity.Place;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ContextualRecommendationService {
    
    private static final Logger logger = LoggerFactory.getLogger(ContextualRecommendationService.class);
    
    private final WeatherService weatherService;
    private final PlaceService placeService;
    private final VectorSearchService vectorSearchService;
    
    @Autowired
    public ContextualRecommendationService(
            WeatherService weatherService,
            PlaceService placeService,
            VectorSearchService vectorSearchService) {
        this.weatherService = weatherService;
        this.placeService = placeService;
        this.vectorSearchService = vectorSearchService;
    }
    
    /**
     * Get contextual recommendations based on user authentication status
     */
    public ContextualRecommendationResponse getContextualRecommendations(
            String query, Double lat, Double lon, int limit) {
        
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");

        double safeLat = lat != null ? lat : 37.5665;
        double safeLon = lon != null ? lon : 126.9780;
        int safeLimit = Math.max(1, Math.min(limit, 20));

        // Build geo-blended candidate pool once (15km 70% + 30km 30%)
        List<Place> geoCandidates = placeService.getLocationWeightedPlaces(safeLat, safeLon, Math.max(safeLimit, 20));
        LinkedHashMap<Long, Place> candidateMap = geoCandidates.stream()
            .collect(Collectors.toMap(Place::getId, place -> place, (existing, replacement) -> existing, LinkedHashMap::new));

        WeatherData weatherData = fetchWeatherData(safeLat, safeLon);
        String weatherCondition = weatherData != null ? weatherData.getConditionText() : "날씨 정보를 가져올 수 없음";
        String timeOfDay = weatherData != null ? weatherData.getDaypart() : getCurrentTimeOfDay();
        String contextualQuery = buildContextualQuery(query, weatherCondition, timeOfDay);

        List<Place> prioritized;
        String algorithmSource;

        if (isAuthenticated) {
            prioritized = getPersonalizedVectorBlend(contextualQuery, auth.getName(), candidateMap, safeLimit);
            algorithmSource = "vector-location-hybrid";
        } else {
            prioritized = getGuestVectorBlend(contextualQuery, candidateMap, safeLimit);
            algorithmSource = "vector-location-public";
        }

        List<Place> finalPlaces = mergeWithFallback(prioritized, candidateMap.values(), safeLimit);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("weather", weatherCondition);
        context.put("timeOfDay", timeOfDay);
        context.put("query", contextualQuery);
        context.put("blendStrategy", "15km:70% + 30km:30%");
        context.put("algorithm", algorithmSource);
        context.put("location", Map.of("latitude", safeLat, "longitude", safeLon));
        context.put("limit", safeLimit);

        String contextMessage = String.format(
            "%s · %s 조건을 반영해 가까운 후보를 만들고 벡터 유사도로 추린 결과입니다.",
            weatherCondition,
            mapTimeContext(timeOfDay)
        );

        List<PlaceDto.PlaceResponse> placeResponses = finalPlaces.stream()
            .map(this::convertToPlaceResponse)
            .collect(Collectors.toList());

        return new ContextualRecommendationResponse(
            placeResponses,
            context,
            weatherCondition,
            mapTimeContext(timeOfDay),
            contextMessage
        );
    }

    private List<Place> getPersonalizedVectorBlend(String contextualQuery, String username,
                                                   Map<Long, Place> candidateMap, int limit) {
        List<Place> prioritized = new ArrayList<>();
        LinkedHashSet<Long> seen = new LinkedHashSet<>();

        try {
            VectorSimilarityResponse response = vectorSearchService.searchWithVectorSimilarity(
                contextualQuery,
                username,
                0.25,
                Math.max(limit * 3, 30)
            );

            if (response != null && response.getSimilarPlaces() != null) {
                response.getSimilarPlaces().forEach(similarPlace -> {
                    Long placeId = similarPlace.getPlace().getId();
                    if (candidateMap.containsKey(placeId) && seen.add(placeId)) {
                        prioritized.add(candidateMap.get(placeId));
                    }
                });
            }
        } catch (Exception e) {
            logger.warn("Vector similarity search failed for user {}: {}", username, e.getMessage());
        }

        return prioritized;
    }

    private List<Place> getGuestVectorBlend(String contextualQuery,
                                            Map<Long, Place> candidateMap,
                                            int limit) {
        List<Place> prioritized = new ArrayList<>();
        LinkedHashSet<Long> seen = new LinkedHashSet<>();

        try {
            List<Place> vectorResults = vectorSearchService.vectorSearchPlaces(
                contextualQuery,
                Math.max(limit * 3, 30)
            );

            for (Place place : vectorResults) {
                if (place == null || place.getId() == null) {
                    continue;
                }
                if (candidateMap.containsKey(place.getId()) && seen.add(place.getId())) {
                    prioritized.add(candidateMap.get(place.getId()));
                }
            }
        } catch (Exception e) {
            logger.warn("Guest vector search failed: {}", e.getMessage());
        }

        return prioritized;
    }

    private List<Place> mergeWithFallback(List<Place> prioritized, Iterable<Place> fallbackPool, int limit) {
        LinkedHashMap<Long, Place> ordered = new LinkedHashMap<>();
        prioritized.forEach(place -> ordered.putIfAbsent(place.getId(), place));

        for (Place place : fallbackPool) {
            if (ordered.size() >= limit) {
                break;
            }
            ordered.putIfAbsent(place.getId(), place);
        }

        List<Place> merged = new ArrayList<>(ordered.values());
        if (merged.size() > limit) {
            return merged.subList(0, limit);
        }
        return merged;
    }

    private WeatherData fetchWeatherData(double latitude, double longitude) {
        try {
            return weatherService.getCurrentWeather(latitude, longitude);
        } catch (Exception e) {
            logger.warn("Failed to fetch weather data for lat={}, lon={}: {}", latitude, longitude, e.getMessage());
            return null;
        }
    }

    private String buildContextualQuery(String query, String weatherCondition, String timeOfDay) {
        String base = (query != null && !query.isBlank()) ? query.trim() : "지금 가기 좋은 장소";
        return String.format("%s | weather:%s | time:%s", base, weatherCondition, mapTimeContext(timeOfDay));
    }

    private String mapTimeContext(String raw) {
        if (raw == null || raw.isBlank()) {
            return getCurrentTimeOfDay();
        }

        return switch (raw.toLowerCase()) {
            case "morning", "아침" -> "아침";
            case "afternoon", "오후" -> "오후";
            case "evening", "저녁" -> "저녁";
            case "night", "밤" -> "밤";
            default -> raw;
        };
    }

    private PlaceDto.PlaceResponse convertToPlaceResponse(Place place) {
        String category = (place.getCategory() != null && !place.getCategory().isEmpty())
            ? place.getCategory().get(0)
            : "기타";

        double rating = place.getRating() != null ? place.getRating().doubleValue() : 4.0;

        return new PlaceDto.PlaceResponse(
            place.getId(),
            place.getName(),
            null,
            List.of(),
            rating,
            category
        );
    }
    
    private String getCurrentTimeOfDay() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        if (hour >= 6 && hour < 12) return "아침";
        if (hour >= 12 && hour < 18) return "오후";
        if (hour >= 18 && hour < 22) return "저녁";
        return "밤";
    }
    
    /**
     * Get weather-based recommendations
     */
    public ContextualRecommendationResponse getWeatherBasedRecommendations(double latitude, double longitude) {
        return getContextualRecommendations(null, latitude, longitude, 20);
    }
}
