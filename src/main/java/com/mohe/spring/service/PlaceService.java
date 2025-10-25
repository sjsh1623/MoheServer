package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.BookmarkRepository;
import com.mohe.spring.service.LlmService;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PlaceService {
    
    private final PlaceRepository placeRepository;
    private final VectorSearchService vectorSearchService;
    private final BookmarkRepository bookmarkRepository;
    private final LlmService llmService;

    public PlaceService(PlaceRepository placeRepository, VectorSearchService vectorSearchService, BookmarkRepository bookmarkRepository, LlmService llmService) {
        this.placeRepository = placeRepository;
        this.vectorSearchService = vectorSearchService;
        this.bookmarkRepository = bookmarkRepository;
        this.llmService = llmService;
    }
    
    public PlaceRecommendationsResponse getRecommendations(Double latitude, Double longitude) {
        final int recommendationLimit = 20;

        final boolean hasLocation = latitude != null && longitude != null;

        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");

            List<Place> places;
            String recommendationType;

            if (hasLocation) {
                places = getLocationWeightedPlaces(latitude, longitude, recommendationLimit);
                recommendationType = isAuthenticated ? "vector-location-hybrid" : "location-weighted";
            } else if (isAuthenticated) {
                places = vectorSearchService.getPersonalizedRecommendations(auth.getName(), recommendationLimit);
                recommendationType = "vector-personalized";
            } else {
                places = placeRepository.findRecommendablePlaces(PageRequest.of(0, recommendationLimit)).getContent();
                recommendationType = "rating-based";
            }

            if (isAuthenticated && hasLocation) {
                List<Place> personalized = vectorSearchService.getPersonalizedRecommendations(auth.getName(), recommendationLimit * 2);
                Map<Long, Integer> preferenceScore = buildPreferenceScore(personalized);
                places.sort(Comparator.comparingInt((Place place) -> preferenceScore.getOrDefault(place.getId(), 0)).reversed());
            }

            List<SimplePlaceDto> placeDtos = places.stream()
                .map(place -> hasLocation ? convertToSimplePlaceDto(place, latitude, longitude) : convertToSimplePlaceDto(place))
                .collect(Collectors.toList());

            return new PlaceRecommendationsResponse(placeDtos, placeDtos.size(), recommendationType);
        } catch (Exception e) {
            PageRequest pageRequest = PageRequest.of(0, recommendationLimit);
            List<Place> places = placeRepository.findRecommendablePlaces(pageRequest).getContent();

            List<SimplePlaceDto> placeDtos = places.stream()
                .map(place -> hasLocation ? convertToSimplePlaceDto(place, latitude, longitude) : convertToSimplePlaceDto(place))
                .collect(Collectors.toList());

            return new PlaceRecommendationsResponse(placeDtos, placeDtos.size(), "rating-based-fallback");
        }
    }
    
    public PlaceListResponse getPlaces(int page, int limit, String category, String sort) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit); // Convert to 0-based indexing
        
        Page<Place> placePage;
        if (category != null && !category.trim().isEmpty()) {
            placePage = placeRepository.findByCategory(category, pageRequest);
        } else {
            // Default to recommendable places sorted by rating
            placePage = placeRepository.findRecommendablePlaces(pageRequest);
        }
        
        List<SimplePlaceDto> placeDtos = placePage.getContent().stream()
            .map(this::convertToSimplePlaceDto)
            .collect(Collectors.toList());
        
        return new PlaceListResponse(placeDtos, (int) placePage.getTotalElements(), 
                                   placePage.getTotalPages(), page, limit);
    }
    
    public PlaceDetailResponse getPlaceDetail(String id) {
        Optional<Place> placeOpt = placeRepository.findById(Long.parseLong(id));
        if (placeOpt.isEmpty()) {
            throw new RuntimeException("장소를 찾을 수 없습니다: " + id);
        }
        
        Place place = placeOpt.get();
        SimplePlaceDto placeDto = convertToSimplePlaceDto(place);

        // Get similar places (simple implementation - same category)
        List<SimplePlaceDto> similarPlaces = List.of(); // TODO: Implement similarity logic

        return new PlaceDetailResponse(placeDto, List.of(), false, similarPlaces);
    }
    
    public PlaceSearchResponse searchPlaces(String q, String location, String weather, String time) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            boolean isAuthenticated = auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser");
            
            List<Place> searchResults;
            String searchType;
            int totalResults;
            
            if (isAuthenticated) {
                // Use vector-based personalized search for authenticated users
                VectorSimilarityResponse vectorResponse = vectorSearchService.searchWithVectorSimilarity(
                    q, auth.getName(), 0.2, 20);
                
                // Extract places from vector similarity response  
                searchResults = vectorResponse.getSimilarPlaces().stream()
                    .map(similarPlace -> {
                        // Convert PlaceResponse back to Place for consistency
                        try {
                            PlaceDto.PlaceResponse placeDto = similarPlace.getPlace();
                            Long placeId = placeDto.getId();
                            return placeRepository.findById(placeId).orElse(null);
                        } catch (Exception e) {
                            return null;
                        }
                    })
                    .filter(place -> place != null)
                    .collect(Collectors.toList());
                
                searchType = "vector-similarity";
                totalResults = vectorResponse.getTotalMatches() != null ? vectorResponse.getTotalMatches() : searchResults.size();
                
                // Fallback to database search if no vector results
                if (searchResults.isEmpty()) {
                    PageRequest pageRequest = PageRequest.of(0, 20);
                    Page<Place> searchPage = placeRepository.searchPlaces(q, pageRequest);
                    searchResults = searchPage.getContent();
                    searchType = "database-fallback";
                    totalResults = (int) searchPage.getTotalElements();
                }
                
            } else {
                // Use simple database search for unauthenticated users
                PageRequest pageRequest = PageRequest.of(0, 20);
                Page<Place> searchPage = placeRepository.searchPlaces(q, pageRequest);
                searchResults = searchPage.getContent();
                searchType = "database-search";
                totalResults = (int) searchPage.getTotalElements();
            }
            
            List<SimplePlaceDto> placeDtos = searchResults.stream()
                .map(this::convertToSimplePlaceDto)
                .collect(Collectors.toList());
            
            // Create enhanced search context
            String contextRecommendation = isAuthenticated ? 
                "개인 취향을 반영한 검색 결과입니다." : 
                "키워드 기반 검색 결과입니다.";
                
            if (weather != null && weather.equals("hot")) {
                contextRecommendation += " 더운 날씨에 적합한 실내 장소를 우선 추천합니다.";
            } else if (weather != null && weather.equals("cold")) {
                contextRecommendation += " 추운 날씨에 따뜻한 실내 장소를 우선 추천합니다.";
            }
            
            Map<String, Object> searchContext = Map.of(
                "weather", weather != null ? weather : "알 수 없음",
                "time", time != null ? time : "알 수 없음",
                "searchType", searchType,
                "recommendation", contextRecommendation
            );
            
            return new PlaceSearchResponse(placeDtos, searchContext, totalResults, q);
            
        } catch (Exception e) {
            // Fallback to basic keyword search on error
            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<Place> searchResults = placeRepository.searchPlaces(q, pageRequest);
            
            List<SimplePlaceDto> placeDtos = searchResults.getContent().stream()
                .map(this::convertToSimplePlaceDto)
                .collect(Collectors.toList());
            
            Map<String, Object> searchContext = Map.of(
                "weather", weather != null ? weather : "알 수 없음",
                "time", time != null ? time : "알 수 없음",
                "searchType", "keyword-fallback",
                "recommendation", "기본 검색 결과입니다."
            );
            
            return new PlaceSearchResponse(placeDtos, searchContext, (int) searchResults.getTotalElements(), q);
        }
    }
    
    public Map<String, Object> getDebugInfo() {
        try {
            long totalPlaces = placeRepository.count();
            long recommendablePlaces = placeRepository.countRecommendablePlaces();
            List<String> categories = placeRepository.findAllCategories();
            
            return Map.of(
                "totalPlaces", totalPlaces,
                "recommendablePlaces", recommendablePlaces,
                "categories", categories,
                "timestamp", java.time.OffsetDateTime.now().toString()
            );
        } catch (Exception e) {
            // If database error, return error info
            return Map.of(
                "error", "Database error: " + e.getMessage(),
                "totalPlaces", 0,
                "recommendablePlaces", 0,
                "categories", List.of(),
                "timestamp", java.time.OffsetDateTime.now().toString()
            );
        }
    }
    
    public PlaceListResponse getPopularPlaces(double latitude, double longitude, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));

        try {
            List<Place> weightedPlaces = getLocationWeightedPlaces(latitude, longitude, safeLimit);

            List<Place> popularPlaces = weightedPlaces.stream()
                .sorted((p1, p2) -> {
                    int reviewCompare = Integer.compare(
                        p2.getReviewCount() != null ? p2.getReviewCount() : 0,
                        p1.getReviewCount() != null ? p1.getReviewCount() : 0
                    );
                    if (reviewCompare != 0) {
                        return reviewCompare;
                    }
                    double rating1 = p1.getRating() != null ? p1.getRating().doubleValue() : 0.0;
                    double rating2 = p2.getRating() != null ? p2.getRating().doubleValue() : 0.0;
                    return Double.compare(rating2, rating1);
                })
                .limit(safeLimit)
                .collect(Collectors.toList());

            List<SimplePlaceDto> placeDtos = popularPlaces.stream()
                .map(place -> convertToSimplePlaceDto(place, latitude, longitude))
                .collect(Collectors.toList());

            return new PlaceListResponse(placeDtos, placeDtos.size(), 1, 1, safeLimit);
        } catch (Exception e) {
            return new PlaceListResponse(List.of(), 0, 0, 1, safeLimit);
        }
    }
    
    public CurrentTimeRecommendationsResponse getCurrentTimePlaces(Double latitude, Double longitude, int limit) {
        try {
            List<Place> candidatePlaces;

            if (latitude != null && longitude != null) {
                candidatePlaces = getLocationWeightedPlaces(latitude, longitude, Math.max(limit, 30));
            } else {
                PageRequest pageRequest = PageRequest.of(0, Math.max(limit, 30));
                candidatePlaces = placeRepository.findGeneralPlacesForLLM(pageRequest);
            }

            // Generate LLM-based recommendations
            List<Place> places = generateLLMRecommendations(candidatePlaces, latitude, longitude, limit);
            
            List<SimplePlaceDto> placeDtos = places.stream()
                .map(place -> latitude != null && longitude != null
                    ? convertToSimplePlaceDto(place, latitude, longitude)
                    : convertToSimplePlaceDto(place))
                .collect(Collectors.toList());
            
            // Create time context with better logic
            java.time.LocalTime now = java.time.LocalTime.now();
            String timeOfDay = getTimeOfDay(now);
            String weatherCondition = "맑음"; // TODO: integrate actual weather API
            String locationText = (latitude != null && longitude != null) ? "주변 " : "";
            
            Map<String, Object> timeContext = Map.of(
                "timeOfDay", timeOfDay,
                "weatherCondition", weatherCondition,
                "recommendationMessage", timeOfDay + "에 " + locationText + "좋은 장소들을 추천드립니다."
            );
            
            return new CurrentTimeRecommendationsResponse(placeDtos, timeContext, weatherCondition, timeOfDay);
        } catch (Exception e) {
            // Fallback to basic recommendations on error
            PageRequest pageRequest = PageRequest.of(0, Math.min(limit, 10));
            List<Place> places = placeRepository.findRecommendablePlaces(pageRequest).getContent();
            
            List<SimplePlaceDto> placeDtos = places.stream()
                .map(place -> latitude != null && longitude != null
                    ? convertToSimplePlaceDto(place, latitude, longitude)
                    : convertToSimplePlaceDto(place))
                .collect(Collectors.toList());
            
            java.time.LocalTime now = java.time.LocalTime.now();
            String timeOfDay = getTimeOfDay(now);
            Map<String, Object> timeContext = Map.of(
                "timeOfDay", timeOfDay,
                "weatherCondition", "맑음",
                "recommendationMessage", "추천 장소를 불러왔습니다."
            );
            
            return new CurrentTimeRecommendationsResponse(placeDtos, timeContext, "맑음", timeOfDay);
        }
    }

    public PlaceListResponse getPlacesList(int page, int limit, String sort) {
        PageRequest pageRequest = PageRequest.of(page, limit);
        Page<Place> placePage;
        
        switch (sort.toLowerCase()) {
            case "popularity":
                placePage = placeRepository.findPopularPlaces(pageRequest);
                break;
            case "rating":
                placePage = placeRepository.findTopRatedPlaces(3.0, pageRequest);
                break;
            case "recent":
            default:
                placePage = placeRepository.findRecommendablePlaces(pageRequest);
                break;
        }
        
        List<SimplePlaceDto> placeDtos = placePage.getContent().stream()
            .map(this::convertToSimplePlaceDto)
            .collect(Collectors.toList());
        
        return new PlaceListResponse(placeDtos, (int) placePage.getTotalElements(), 
                                   placePage.getTotalPages(), page, limit);
    }

    public PlaceListResponse getNearbyPlaces(double latitude, double longitude, double radiusMeters, int limit) {
        double safeRadiusMeters = radiusMeters > 0 ? radiusMeters : 3000.0;
        double radiusKilometers = safeRadiusMeters / 1000.0;
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Place> places = placeRepository.findNearbyPlacesForLLM(latitude, longitude, radiusKilometers, safeLimit);

        List<SimplePlaceDto> placeDtos = places.stream()
            .map(place -> convertToSimplePlaceDto(place, latitude, longitude))
            .collect(Collectors.toList());

        return new PlaceListResponse(placeDtos, placeDtos.size(), 1, 0, placeDtos.size());
    }
    
    /**
     * Get places with images for home page
     */
    public List<PlaceDto.PlaceResponse> getPlacesWithImages(int limit) {
        // Get top rated places
        Pageable pageable = PageRequest.of(0, limit);
        List<Place> places = placeRepository.findTopRatedPlaces(3.0, pageable).getContent();

        return places.stream()
            .map(place -> {
                // Get primary image
                String imageUrl = getPlaceImageUrl(place);

                return new PlaceDto.PlaceResponse(
                    place.getId(),
                    place.getName(),
                    imageUrl,
                    List.of(),
                    place.getRating() != null ? place.getRating().doubleValue() : 4.0,
                    place.getCategory() != null && !place.getCategory().isEmpty() ? place.getCategory().get(0) : "카테고리 없음"
                );
            })
            .collect(Collectors.toList());
    }

    private Map<Long, Integer> buildPreferenceScore(List<Place> personalized) {
        Map<Long, Integer> scoreMap = new HashMap<>();
        if (personalized == null || personalized.isEmpty()) {
            return scoreMap;
        }

        int weight = personalized.size();
        for (Place place : personalized) {
            if (place != null && place.getId() != null) {
                scoreMap.putIfAbsent(place.getId(), weight--);
            }
        }
        return scoreMap;
    }

    /**
     * Build a geo-weighted candidate list: 70% within 15km, 30% within 30km.
     */
    public List<Place> getLocationWeightedPlaces(Double latitude, Double longitude, int limit) {
        if (latitude == null || longitude == null) {
            return placeRepository.findRecommendablePlaces(PageRequest.of(0, limit)).getContent();
        }

        int safeLimit = Math.max(1, limit);
        int innerTarget = (int) Math.ceil(safeLimit * 0.7);
        int outerTarget = Math.max(safeLimit - innerTarget, 0);

        LinkedHashMap<Long, Place> selected = new LinkedHashMap<>();

        addPlacesWithinDistance(
            placeRepository.findNearbyPlacesForLLM(latitude, longitude, 15.0, Math.max(innerTarget * 2, safeLimit)),
            selected,
            latitude,
            longitude,
            0.0,
            15.0,
            innerTarget
        );

        if (selected.size() < innerTarget) {
            addPlacesWithinDistance(
                placeRepository.findNearbyPlacesForLLM(latitude, longitude, 20.0, Math.max(innerTarget * 3, safeLimit)),
                selected,
                latitude,
                longitude,
                0.0,
                15.0,
                innerTarget
            );
        }

        if (outerTarget > 0) {
            addPlacesWithinDistance(
                placeRepository.findNearbyPlacesForLLM(latitude, longitude, 30.0, Math.max(outerTarget * 3, safeLimit)),
                selected,
                latitude,
                longitude,
                15.0,
                30.0,
                innerTarget + outerTarget
            );
        }

        if (selected.size() < safeLimit) {
            addPlacesWithinDistance(
                placeRepository.findNearbyPlacesForLLM(latitude, longitude, 30.0, safeLimit * 4),
                selected,
                latitude,
                longitude,
                0.0,
                30.0,
                safeLimit
            );
        }

        if (selected.size() < safeLimit) {
            placeRepository.findRecommendablePlaces(PageRequest.of(0, safeLimit)).getContent()
                .forEach(place -> selected.putIfAbsent(place.getId(), place));
        }

        return selected.values().stream()
            .limit(safeLimit)
            .collect(Collectors.toList());
    }

    private void addPlacesWithinDistance(
        List<Place> candidates,
        LinkedHashMap<Long, Place> selected,
        Double latitude,
        Double longitude,
        double minDistanceKm,
        double maxDistanceKm,
        int targetSize
    ) {
        if (candidates == null) {
            return;
        }

        for (Place place : candidates) {
            if (place == null || place.getId() == null) {
                continue;
            }

            double distance = calculateDistanceKm(latitude, longitude, place);
            if (distance == Double.MAX_VALUE) {
                continue;
            }

            if (distance > minDistanceKm && distance <= maxDistanceKm) {
                selected.putIfAbsent(place.getId(), place);
            }

            if (selected.size() >= targetSize) {
                break;
            }
        }
    }

    private double calculateDistanceKm(Double latitude, Double longitude, Place place) {
        if (latitude == null || longitude == null) {
            return Double.MAX_VALUE;
        }

        BigDecimal placeLat = place.getLatitude();
        BigDecimal placeLon = place.getLongitude();

        if (placeLat == null || placeLon == null) {
            return Double.MAX_VALUE;
        }

        return haversine(latitude, longitude, placeLat.doubleValue(), placeLon.doubleValue());
    }

    private double haversine(double lat1, double lon1, double lat2, double lon2) {
        final double earthRadius = 6371.0; // Kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
    
    private SimplePlaceDto convertToSimplePlaceDto(Place place) {
        String imageUrl = getPlaceImageUrl(place);

        SimplePlaceDto dto = new SimplePlaceDto(
            place.getId().toString(),
            place.getName(),
            place.getCategory() != null && !place.getCategory().isEmpty() ? place.getCategory().get(0) : "기타",
            place.getRating() != null ? place.getRating().doubleValue() : 4.0,
            place.getRoadAddress(),
            imageUrl
        );

        // Set additional fields
        dto.setReviewCount(place.getReviewCount() != null ? place.getReviewCount() : 0);
        dto.setAddress(place.getRoadAddress());
        dto.setDistance(0.0); // TODO: Calculate actual distance
        dto.setIsBookmarked(false); // TODO: Check if bookmarked by current user
        dto.setIsDemo(false);

        // Set mohe_description only
        String moheDescription = place.getDescriptions().stream()
            .filter(desc -> desc.getMoheDescription() != null && !desc.getMoheDescription().isEmpty())
            .map(desc -> desc.getMoheDescription())
            .findFirst()
            .orElse(null);
        dto.setDescription(moheDescription);

        return dto;
    }

    private SimplePlaceDto convertToSimplePlaceDto(Place place, Double latitude, Double longitude) {
        SimplePlaceDto dto = convertToSimplePlaceDto(place);
        double distance = calculateDistanceKm(latitude, longitude, place);
        if (distance != Double.MAX_VALUE) {
            dto.setDistance(Math.round(distance * 10.0) / 10.0);
        }
        return dto;
    }
    
    private String getPlaceImageUrl(Place place) {
        // Return null since gallery field is removed
        return null;
    }

    /**
     * Generate LLM-based recommendations using weather and time context
     */
    private List<Place> generateLLMRecommendations(List<Place> candidatePlaces, Double latitude, Double longitude, int limit) {
        if (candidatePlaces.isEmpty()) {
            return new ArrayList<>();
        }

        try {
            // Get current time and weather context
            java.time.LocalTime now = java.time.LocalTime.now();
            String timeOfDay = getTimeOfDay(now);
            String weatherCondition = "맑음"; // TODO: integrate actual weather API
            int hour = now.getHour();

            // Build context-aware prompt for LLM
            String locationContext = latitude != null && longitude != null ? "사용자 위치 주변" : "일반적인";
            String prompt = String.format(
                """
                현재 시간: %s (%d시), 날씨: %s, 위치: %s

                다음 장소 목록에서 현재 시간과 날씨에 가장 적합한 %d개 장소를 추천해주세요.
                추천 시 고려사항:
                - 현재 시간대에 적합한 활동
                - 날씨 조건에 맞는 장소
                - 장소의 특성과 분위기
                - 거리 순으로 가까운 곳 우선

                장소 목록:
                %s

                응답 형식: "추천장소1,추천장소2,추천장소3..." (쉼표로 구분, 장소명만)
                """,
                timeOfDay, hour, weatherCondition, locationContext, limit,
                candidatePlaces.stream()
                    .map(p -> String.format("- %s (%s, 평점: %.1f)",
                        p.getName(), p.getCategory(), p.getRating() != null ? p.getRating() : 0.0))
                    .collect(Collectors.joining("\n"))
            );

            // Get LLM recommendations
            List<String> placeNames = candidatePlaces.stream()
                .map(Place::getName)
                .collect(Collectors.toList());

            OllamaRecommendationResponse llmResponse = llmService.generatePlaceRecommendations(prompt, placeNames);

            if (llmResponse != null && llmResponse.getRecommendedPlaces() != null && !llmResponse.getRecommendedPlaces().isEmpty()) {
                // Map LLM recommendations back to Place objects
                List<Place> recommendedPlaces = new ArrayList<>();
                for (String recommendedName : llmResponse.getRecommendedPlaces()) {
                    candidatePlaces.stream()
                        .filter(place -> place.getName().equals(recommendedName))
                        .findFirst()
                        .ifPresent(recommendedPlaces::add);
                }

                // If we have enough recommendations, return them
                if (recommendedPlaces.size() >= Math.min(limit, 5)) {
                    return recommendedPlaces.subList(0, Math.min(limit, recommendedPlaces.size()));
                }
            }

            // Fallback: return top places by rating if LLM fails
            return candidatePlaces.stream()
                .sorted((p1, p2) -> {
                    Double rating1 = p1.getRating() != null ? p1.getRating().doubleValue() : 0.0;
                    Double rating2 = p2.getRating() != null ? p2.getRating().doubleValue() : 0.0;
                    return rating2.compareTo(rating1);
                })
                .limit(limit)
                .collect(Collectors.toList());

        } catch (Exception e) {
            // Fallback on any error
            return candidatePlaces.stream()
                .limit(limit)
                .collect(Collectors.toList());
        }
    }

    private String getTimeOfDay(java.time.LocalTime time) {
        int hour = time.getHour();
        if (hour >= 6 && hour < 12) return "아침";
        if (hour >= 12 && hour < 18) return "오후";
        if (hour >= 18 && hour < 22) return "저녁";
        return "밤";
    }
}
