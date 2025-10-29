package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.PlaceDto;
import com.mohe.spring.dto.SuggestedCategoriesResponse;
import com.mohe.spring.entity.Place;
import com.mohe.spring.enums.PlaceCategory;
import com.mohe.spring.service.CategoryRecommendationService;
import com.mohe.spring.service.PlaceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 카테고리 기반 장소 추천 컨트롤러
 */
@RestController
@RequestMapping("/api/categories")
@Tag(name = "Category Recommendations", description = "카테고리 기반 장소 추천 API")
public class CategoryController {

    private static final Logger logger = LoggerFactory.getLogger(CategoryController.class);

    private final CategoryRecommendationService categoryRecommendationService;
    private final PlaceService placeService;

    public CategoryController(
            CategoryRecommendationService categoryRecommendationService,
            PlaceService placeService) {
        this.categoryRecommendationService = categoryRecommendationService;
        this.placeService = placeService;
    }

    /**
     * API 1: 날씨/시간 기반 추천 카테고리 5개 조회
     *
     * GET /api/categories/suggested?latitude={lat}&longitude={lon}
     */
    @Operation(
        summary = "추천 카테고리 조회",
        description = "현재 시간대와 날씨를 기반으로 적합한 장소 카테고리 5개를 추천합니다. OpenAI를 사용하지 않고 Enum 기반 규칙 엔진으로 동작합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "추천 카테고리 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 파라미터"
            )
        }
    )
    @GetMapping("/suggested")
    public ResponseEntity<ApiResponse<SuggestedCategoriesResponse>> getSuggestedCategories(
            @Parameter(description = "위도", required = true, example = "37.5665")
            @RequestParam Double latitude,
            @Parameter(description = "경도", required = true, example = "126.9780")
            @RequestParam Double longitude) {
        try {
            logger.info("Fetching suggested categories for lat={}, lon={}", latitude, longitude);

            SuggestedCategoriesResponse response = categoryRecommendationService
                    .getSuggestedCategories(latitude, longitude);

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            logger.error("Failed to get suggested categories", e);
            return ResponseEntity.badRequest().body(
                    ApiResponse.error("CATEGORY_RECOMMENDATION_ERROR",
                            "Failed to get suggested categories: " + e.getMessage())
            );
        }
    }

    /**
     * API 2: 카테고리별 장소 조회
     *
     * GET /api/categories/{category}/places?latitude={lat}&longitude={lon}&limit={limit}
     */
    @Operation(
        summary = "카테고리별 장소 조회",
        description = "특정 카테고리에 속하는 장소들을 거리 가중 혼합 방식(15km 70% + 30km 30%)으로 조회합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "장소 조회 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 카테고리 또는 파라미터"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "카테고리를 찾을 수 없음"
            )
        }
    )
    @GetMapping("/{category}/places")
    public ResponseEntity<ApiResponse<List<PlaceDto.PlaceResponse>>> getPlacesByCategory(
            @Parameter(description = "카테고리 키 (예: cafe, restaurant, bar)", required = true)
            @PathVariable String category,
            @Parameter(description = "위도", required = true, example = "37.5665")
            @RequestParam Double latitude,
            @Parameter(description = "경도", required = true, example = "126.9780")
            @RequestParam Double longitude,
            @Parameter(description = "조회 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit) {
        try {
            // 1. 카테고리 키 유효성 검증
            PlaceCategory placeCategory = PlaceCategory.fromKey(category);
            if (placeCategory == null) {
                logger.warn("Invalid category key: {}", category);
                return ResponseEntity.badRequest().body(
                        ApiResponse.error("INVALID_CATEGORY",
                                "Invalid category: " + category + ". Please use valid category key.")
                );
            }

            logger.info("Fetching places for category={}, lat={}, lon={}, limit={}",
                    category, latitude, longitude, limit);

            // 2. 거리 가중 장소 목록 가져오기
            int fetchLimit = Math.max(limit * 3, 60); // 필터링 여유분 확보
            List<Place> locationWeightedPlaces = placeService.getLocationWeightedPlaces(
                    latitude, longitude, fetchLimit
            );

            // 3. 카테고리로 필터링
            List<Place> filteredPlaces = filterByCategory(locationWeightedPlaces, placeCategory);

            // 4. limit 만큼 자르기
            List<Place> limitedPlaces = filteredPlaces.stream()
                    .limit(limit)
                    .collect(Collectors.toList());

            // 5. DTO 변환
            List<PlaceDto.PlaceResponse> placeResponses = limitedPlaces.stream()
                    .map(place -> convertToPlaceResponse(place, latitude, longitude))
                    .collect(Collectors.toList());

            logger.info("Found {} places for category {}", placeResponses.size(), category);

            return ResponseEntity.ok(ApiResponse.success(placeResponses));

        } catch (Exception e) {
            logger.error("Failed to get places by category: {}", category, e);
            return ResponseEntity.status(500).body(
                    ApiResponse.error("INTERNAL_ERROR",
                            "Failed to get places by category: " + e.getMessage())
            );
        }
    }

    /**
     * 카테고리로 장소 필터링
     *
     * @param places 장소 리스트
     * @param placeCategory 필터링할 카테고리
     * @return 필터링된 장소 리스트
     */
    private List<Place> filterByCategory(List<Place> places, PlaceCategory placeCategory) {
        return places.stream()
                .filter(place -> place.getCategory() != null && !place.getCategory().isEmpty())
                .filter(place -> matchesCategory(place, placeCategory))
                .collect(Collectors.toList());
    }

    /**
     * 장소가 특정 카테고리와 매칭되는지 확인
     *
     * @param place 장소
     * @param placeCategory 카테고리
     * @return 매칭 여부
     */
    private boolean matchesCategory(Place place, PlaceCategory placeCategory) {
        // Place의 category 필드는 List<String>
        for (String placeCat : place.getCategory()) {
            // PlaceCategory의 keywords와 매칭
            for (String keyword : placeCategory.getKeywords()) {
                if (placeCat.toLowerCase().contains(keyword.toLowerCase()) ||
                    keyword.toLowerCase().contains(placeCat.toLowerCase())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Place를 PlaceResponse DTO로 변환
     *
     * @param place 장소
     * @param userLat 사용자 위도
     * @param userLon 사용자 경도
     * @return PlaceResponse DTO
     */
    private PlaceDto.PlaceResponse convertToPlaceResponse(Place place, Double userLat, Double userLon) {
        PlaceDto.PlaceResponse response = new PlaceDto.PlaceResponse();
        response.setId(place.getId());
        response.setName(place.getName());
        response.setAddress(place.getRoadAddress());
        response.setLatitude(place.getLatitude() != null ? place.getLatitude().doubleValue() : null);
        response.setLongitude(place.getLongitude() != null ? place.getLongitude().doubleValue() : null);
        response.setRating(place.getRating() != null ? place.getRating().doubleValue() : null);
        response.setReviewCount(place.getReviewCount());
        response.setCategory(place.getCategory());
        response.setKeyword(place.getKeyword());
        response.setParkingAvailable(place.getParkingAvailable());
        response.setPetFriendly(place.getPetFriendly());
        response.setWebsiteUrl(place.getWebsiteUrl());

        // 거리 계산
        if (place.getLatitude() != null && place.getLongitude() != null && userLat != null && userLon != null) {
            double distance = calculateDistance(
                    userLat, userLon,
                    place.getLatitude().doubleValue(),
                    place.getLongitude().doubleValue()
            );
            response.setDistance(distance);
        }

        // 이미지 URL
        List<String> imageUrls = placeService.getImageUrls(place.getId());
        response.setImages(imageUrls);

        return response;
    }

    /**
     * 두 지점 간 거리 계산 (Haversine formula)
     *
     * @param lat1 위도 1
     * @param lon1 경도 1
     * @param lat2 위도 2
     * @param lon2 경도 2
     * @return 거리 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);

        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }
}
