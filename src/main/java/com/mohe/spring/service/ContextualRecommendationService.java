package com.mohe.spring.service;

import com.mohe.spring.dto.ContextualRecommendationResponse;
import com.mohe.spring.dto.PlaceDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceKeywordEmbedding;
import com.mohe.spring.repository.PlaceKeywordEmbeddingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContextualRecommendationService {

    private static final Logger logger = LoggerFactory.getLogger(ContextualRecommendationService.class);

    private final WeatherService weatherService;
    private final PlaceService placeService;
    private final VectorSearchService vectorSearchService;
    private final KeywordEmbeddingService keywordEmbeddingService;
    private final PlaceKeywordEmbeddingRepository placeKeywordEmbeddingRepository;

    public ContextualRecommendationService(
            WeatherService weatherService,
            PlaceService placeService,
            VectorSearchService vectorSearchService,
            KeywordEmbeddingService keywordEmbeddingService,
            PlaceKeywordEmbeddingRepository placeKeywordEmbeddingRepository) {
        this.weatherService = weatherService;
        this.placeService = placeService;
        this.vectorSearchService = vectorSearchService;
        this.keywordEmbeddingService = keywordEmbeddingService;
        this.placeKeywordEmbeddingRepository = placeKeywordEmbeddingRepository;
    }

    public ContextualRecommendationResponse getContextualRecommendations(
            String query, Double lat, Double lon, int limit) {

        double safeLat = lat != null ? lat : 37.5665;
        double safeLon = lon != null ? lon : 126.9780;
        int safeLimit = Math.max(1, Math.min(limit, 20));

        var auth = org.springframework.security.core.context.SecurityContextHolder
            .getContext()
            .getAuthentication();
        boolean isAuthenticated = auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getName());
        String userEmail = isAuthenticated ? auth.getName() : null;

        List<Place> geoCandidates = placeService.getLocationWeightedPlaces(
            safeLat,
            safeLon,
            Math.max(safeLimit, 40)
        );
        LinkedHashMap<Long, Place> candidateMap = geoCandidates.stream()
            .collect(Collectors.toMap(
                Place::getId,
                place -> place,
                (existing, replacement) -> existing,
                LinkedHashMap::new
            ));

        WeatherData weatherData = fetchWeatherData(safeLat, safeLon);
        String weatherCondition = weatherData != null ? weatherData.getConditionText() : "날씨 정보를 가져올 수 없음";
        String timeOfDay = weatherData != null ? weatherData.getDaypart() : getCurrentTimeOfDay();

        List<String> contextKeywords = buildContextKeywords(query, weatherCondition, timeOfDay);
        List<Long> contextMatchedIds = findContextualPlaceIds(contextKeywords, candidateMap.keySet(), Math.max(safeLimit * 4, 60));
        List<Place> contextRankedPlaces = contextMatchedIds.stream()
            .map(candidateMap::get)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
        if (contextRankedPlaces.isEmpty()) {
            contextRankedPlaces = new ArrayList<>(candidateMap.values());
        }

        List<Place> prioritized;
        String algorithmSource;

        if (isAuthenticated) {
            List<Long> reRankedIds = vectorSearchService.rankCandidatePlaces(
                userEmail,
                contextRankedPlaces.stream().map(Place::getId).collect(Collectors.toList()),
                0.25,
                Math.max(safeLimit * 2, 40)
            );
            prioritized = reRankedIds.stream()
                .map(candidateMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            if (prioritized.isEmpty()) {
                prioritized = contextRankedPlaces;
                algorithmSource = "context-embedding-fallback";
            } else {
                algorithmSource = "context-embedding+user-vector";
            }
        } else {
            prioritized = contextRankedPlaces;
            algorithmSource = "context-embedding-public";
        }

        List<Place> finalPlaces = mergeWithFallback(prioritized, candidateMap.values(), safeLimit);

        // Filter by business hours - only show currently open places
        finalPlaces = placeService.filterOpenPlaces(finalPlaces);

        Map<String, Object> context = new LinkedHashMap<>();
        context.put("weather", weatherCondition);
        context.put("timeOfDay", timeOfDay);
        context.put("query", query != null ? query : "");
        context.put("keywords", contextKeywords);
        context.put("blendStrategy", "15km:70% + 30km:30%");
        context.put("algorithm", algorithmSource);
        context.put("location", Map.of("latitude", safeLat, "longitude", safeLon));
        context.put("limit", safeLimit);
        context.put("contextMatches", contextMatchedIds.size());

        String contextMessage = String.format(
            "%s · %s 조건을 반영해 가까운 후보를 벡터로 거른 결과입니다.",
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

    private List<Long> findContextualPlaceIds(List<String> keywords, Collection<Long> allowedPlaceIds, int limit) {
        if (keywords.isEmpty() || allowedPlaceIds.isEmpty()) {
            return List.of();
        }

        float[] embedding = keywordEmbeddingService.vectorizeKeywords(keywords.toArray(String[]::new));
        if (isZeroVector(embedding)) {
            return List.of();
        }

        String pgVectorLiteral = toPgVectorLiteral(embedding);
        List<PlaceKeywordEmbedding> similarEmbeddings;
        try {
            similarEmbeddings = placeKeywordEmbeddingRepository.findSimilarByEmbedding(pgVectorLiteral, Math.max(limit * 2, 80));
        } catch (Exception e) {
            logger.warn("Failed to run contextual embedding search: {}", e.getMessage());
            return List.of();
        }

        Set<Long> allowed = allowedPlaceIds instanceof Set
            ? (Set<Long>) allowedPlaceIds
            : new java.util.HashSet<>(allowedPlaceIds);

        LinkedHashSet<Long> ordered = new LinkedHashSet<>();
        for (PlaceKeywordEmbedding embeddingRow : similarEmbeddings) {
            Long placeId = embeddingRow.getPlaceId();
            if (allowed.contains(placeId)) {
                ordered.add(placeId);
                if (ordered.size() >= limit) {
                    break;
                }
            }
        }
        return new ArrayList<>(ordered);
    }

    private List<String> buildContextKeywords(String query, String weatherCondition, String timeOfDay) {
        List<String> tokens = new ArrayList<>();
        if (query != null && !query.isBlank()) {
            tokens.addAll(Arrays.stream(query.split("\\s+"))
                .filter(token -> token != null && !token.isBlank())
                .map(String::trim)
                .limit(5)
                .collect(Collectors.toList()));
        }

        String mappedTime = mapTimeContext(timeOfDay);
        tokens.add(mappedTime);

        if (weatherCondition != null && !weatherCondition.isBlank()) {
            tokens.add(weatherCondition);
            String normalizedWeather = weatherCondition.toLowerCase(Locale.KOREAN);
            if (normalizedWeather.contains("비")) {
                tokens.addAll(List.of("실내", "우중", "rainy", "카페"));
            } else if (normalizedWeather.contains("더움") || normalizedWeather.contains("hot")) {
                tokens.addAll(List.of("시원한", "에어컨", "실내"));
            } else if (normalizedWeather.contains("추움") || normalizedWeather.contains("cold")) {
                tokens.addAll(List.of("따뜻한", "실내", "tea"));
            } else if (normalizedWeather.contains("맑")) {
                tokens.addAll(List.of("야외", "산책", "공원"));
            }
        }

        if (mappedTime.equals("야간") || mappedTime.equals("밤")) {
            tokens.addAll(List.of("야경", "라운지"));
        } else if (mappedTime.equals("아침")) {
            tokens.add("브런치");
        }

        return tokens.stream()
            .filter(token -> token != null && !token.isBlank())
            .collect(Collectors.toList());
    }

    private List<Place> mergeWithFallback(List<Place> prioritized, Collection<Place> fallbackPool, int limit) {
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

    private String toPgVectorLiteral(float[] embedding) {
        StringBuilder builder = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(String.format(Locale.US, "%.6f", embedding[i]));
        }
        builder.append(']');
        return builder.toString();
    }

    private boolean isZeroVector(float[] embedding) {
        if (embedding == null || embedding.length == 0) {
            return true;
        }
        double sum = 0;
        for (float v : embedding) {
            sum += Math.abs(v);
        }
        return sum < 1e-3;
    }

    private PlaceDto.PlaceResponse convertToPlaceResponse(Place place) {
        String category = (place.getCategory() != null && !place.getCategory().isEmpty())
            ? place.getCategory().get(0)
            : "기타";
        double rating = place.getRating() != null ? place.getRating().doubleValue() : 4.0;
        List<String> imageUrls = placeService.getImageUrls(place.getId());
        String imageUrl = imageUrls.isEmpty() ? null : imageUrls.get(0);

        String fullAddress = place.getRoadAddress();
        String shortAddress = extractShortAddress(fullAddress);

        PlaceDto.PlaceResponse response = new PlaceDto.PlaceResponse(
            place.getId(),
            place.getName(),
            imageUrl,
            imageUrls,
            rating,
            category
        );
        if (place.getLatitude() != null && place.getLongitude() != null) {
            response.setDistance(0.0);
        }

        // Set address information
        response.setShortAddress(shortAddress);
        response.setFullAddress(fullAddress);
        response.setLocation(shortAddress);

        return response;
    }

    /**
     * Extract short address (구+동) from full road address
     */
    private String extractShortAddress(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return "";
        }

        try {
            // Remove province/city prefix (서울특별시, 경기도, etc.)
            String address = fullAddress.replaceFirst("^(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도|제주특별자치도)\\s*", "");

            // Split by spaces
            String[] parts = address.split("\\s+");

            if (parts.length >= 2) {
                // Extract district (구/군/시) and neighborhood (동/읍/면/리)
                String district = parts[0]; // 시/구/군
                String neighborhood = parts[1]; // 동/읍/면/리

                // Handle city names that include "시" (e.g., "성남시" should become "성남시 분당구")
                if (parts.length >= 3 && district.endsWith("시") && (parts[1].endsWith("구") || parts[1].endsWith("군"))) {
                    return parts[1] + " " + parts[2];
                }

                return district + " " + neighborhood;
            }

            // If we can't parse, return first part or empty
            return parts.length > 0 ? parts[0] : "";

        } catch (Exception e) {
            return "";
        }
    }

    private String getCurrentTimeOfDay() {
        java.time.LocalTime now = java.time.LocalTime.now();
        int hour = now.getHour();
        if (hour >= 6 && hour < 12) return "아침";
        if (hour >= 12 && hour < 18) return "오후";
        if (hour >= 18 && hour < 22) return "저녁";
        return "밤";
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

    public ContextualRecommendationResponse getWeatherBasedRecommendations(double latitude, double longitude) {
        return getContextualRecommendations(null, latitude, longitude, 20);
    }
}
