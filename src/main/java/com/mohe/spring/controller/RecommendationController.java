package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.EnhancedRecommendationsResponse;
import com.mohe.spring.dto.ContextualRecommendationResponse;
import com.mohe.spring.dto.CurrentTimeRecommendationsResponse;
import com.mohe.spring.dto.PlaceDto;
import com.mohe.spring.dto.SimplePlaceDto;
import com.mohe.spring.service.*;
import com.mohe.spring.security.UserPrincipal;
import com.mohe.spring.repository.UserRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.BookmarkRepository;
import com.mohe.spring.entity.User;
import com.mohe.spring.entity.Place;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
// Import removed - using fully qualified name for ApiResponse to avoid conflict
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Enhanced recommendations controller using MBTI-weighted similarity calculations
 */
@RestController
@RequestMapping("/api/recommendations")
@Tag(name = "Enhanced Recommendations", description = "MBTI-weighted similarity-based place recommendations")
public class RecommendationController {

    private static final Logger logger = LoggerFactory.getLogger(RecommendationController.class);
    
    private final EnhancedRecommendationService enhancedRecommendationService;
    private final ContextualRecommendationService contextualRecommendationService;
    private final WeatherService weatherService;
    private final PlaceService placeService;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final BookmarkRepository bookmarkRepository;

    public RecommendationController(
            EnhancedRecommendationService enhancedRecommendationService,
            ContextualRecommendationService contextualRecommendationService,
            WeatherService weatherService,
            PlaceService placeService,
            UserRepository userRepository,
            PlaceRepository placeRepository,
            BookmarkRepository bookmarkRepository) {
        this.enhancedRecommendationService = enhancedRecommendationService;
        this.contextualRecommendationService = contextualRecommendationService;
        this.weatherService = weatherService;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.placeRepository = placeRepository;
        this.bookmarkRepository = bookmarkRepository;
    }

    @Operation(
        summary = "Get enhanced recommendations",
        description = "Get personalized place recommendations using MBTI-weighted similarity calculations based on user's bookmark history"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Enhanced recommendations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate recommendations")
        }
    )
    @GetMapping("/enhanced")
    public ResponseEntity<ApiResponse<EnhancedRecommendationsResponse>> getEnhancedRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "Maximum number of recommendations to return (default: 15, max: 50)")
            @RequestParam(defaultValue = "15") int limit,
            @Parameter(description = "Whether to exclude already bookmarked places (default: true)")
            @RequestParam(defaultValue = "true") boolean excludeBookmarked) {
        try {
            if (limit < 1 || limit > 50) {
                ApiResponse<EnhancedRecommendationsResponse> errorResponse = ApiResponse.error("VALIDATION_ERROR", "Limit must be between 1 and 50");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            logger.info("Generating enhanced recommendations for user {} with limit {}", user.getId(), limit);

            EnhancedRecommendationsResponse recommendations = enhancedRecommendationService.getEnhancedRecommendations(
                user, limit, excludeBookmarked
            );

            return ResponseEntity.ok(ApiResponse.success(recommendations));

        } catch (Exception ex) {
            logger.error("Failed to get enhanced recommendations for user {}", userPrincipal.getId(), ex);
            ApiResponse<EnhancedRecommendationsResponse> errorResponse = ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to generate recommendations: " + ex.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(
        summary = "Get MBTI-specific recommendations",
        description = "Get recommendations specifically tailored to a particular MBTI type (useful for exploring different personality preferences)"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "MBTI-specific recommendations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid MBTI type or parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate recommendations")
        }
    )
    @GetMapping("/mbti/{mbtiType}")
    public ResponseEntity<ApiResponse<EnhancedRecommendationsResponse>> getMbtiSpecificRecommendations(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Parameter(description = "MBTI type (e.g., INTJ, ENFP)", required = true)
            @PathVariable String mbtiType,
            @Parameter(description = "Maximum number of recommendations to return (default: 15, max: 50)")
            @RequestParam(defaultValue = "15") int limit) {
        try {
            if (!isValidMbtiType(mbtiType)) {
                ApiResponse<EnhancedRecommendationsResponse> errorResponse = ApiResponse.error("VALIDATION_ERROR", "Invalid MBTI type: " + mbtiType);
                return ResponseEntity.badRequest().body(errorResponse);
            }

            if (limit < 1 || limit > 50) {
                ApiResponse<EnhancedRecommendationsResponse> errorResponse = ApiResponse.error("VALIDATION_ERROR", "Limit must be between 1 and 50");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // Create a copy of user with specified MBTI type
            User mbtiUser = user.copy(
                user.getEmail(),
                user.getPasswordHash(),
                user.getNickname(),
                mbtiType.toUpperCase(),
                user.getAgeRange(),
                user.getTransportation(),
                user.getProfileImageUrl(),
                user.getIsOnboardingCompleted(),
                user.getLastLoginAt()
            );

            logger.info("Generating MBTI-specific recommendations for user {} with MBTI {}", user.getId(), mbtiType);

            EnhancedRecommendationsResponse recommendations = enhancedRecommendationService.getEnhancedRecommendations(
                mbtiUser, limit, true
            );

            return ResponseEntity.ok(ApiResponse.success(recommendations));

        } catch (Exception ex) {
            logger.error("Failed to get MBTI-specific recommendations for user {} with MBTI {}", userPrincipal.getId(), mbtiType, ex);
            ApiResponse<EnhancedRecommendationsResponse> errorResponse = ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to generate MBTI-specific recommendations: " + ex.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(
        summary = "Get recommendation explanation",
        description = "Get detailed explanation of why specific places were recommended to the user"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendation explanation retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "User not authenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate explanation")
        }
    )
    @GetMapping("/explanation")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecommendationExplanation(
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            Map<String, Object> explanation = Map.of(
                "userId", user.getId(),
                "userMbti", user.getMbti() != null ? user.getMbti() : "unknown",
                "algorithm", "mbti_similarity_based",
                "explanation", Map.of(
                    "step1", "사용자의 북마크된 장소들을 분석합니다",
                    "step2", "북마크된 장소와 유사한 다른 장소들을 찾습니다",
                    "step3", "MBTI 성향에 따라 가중치를 적용합니다",
                    "step4", "다양성과 인기도 균형을 맞춰 최종 추천 목록을 생성합니다"
                ),
                "factors", Map.of(
                    "similarity", "북마크 기반 유사도 (자카드, 코사인 유사도)",
                    "mbti", "MBTI 성향별 장소 선호도 가중치",
                    "diversity", "카테고리와 지역 다양성 보장",
                    "freshness", "최근 데이터에 더 높은 가중치 부여",
                    "popularity", "인기 편향 완화를 위한 패널티 적용"
                )
            );

            return ResponseEntity.ok(ApiResponse.success(explanation));

        } catch (Exception ex) {
            logger.error("Failed to get recommendation explanation for user {}", userPrincipal.getId(), ex);
            ApiResponse<Map<String, Object>> errorResponse = ApiResponse.error("INTERNAL_SERVER_ERROR", "Failed to generate explanation: " + ex.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(
        summary = "Get contextual recommendations (dual mode)",
        description = """
        Get place recommendations based on weather, time, and location:
        - For authenticated users: Uses MBTI + bookmarks + weather + time
        - For guest users: Uses popular bookmarked places + weather + time
        """
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate recommendations")
        }
    )
    @GetMapping("/contextual")
    public ResponseEntity<ApiResponse<ContextualRecommendationResponse>> getContextualRecommendations(
            @Parameter(description = "User latitude", required = true)
            @RequestParam double lat,
            @Parameter(description = "User longitude", required = true)
            @RequestParam double lon,
            @Parameter(description = "Search query or keywords", required = false)
            @RequestParam(required = false) String query,
            @Parameter(description = "Maximum number of recommendations (default: 10, max: 20)")
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // Validate inputs
            if (limit > 20) {
                ApiResponse<ContextualRecommendationResponse> errorResponse = ApiResponse.error("INVALID_LIMIT", "Limit cannot exceed 20");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String timeContext = getCurrentTimeContext();
            logger.info("Processing contextual recommendations for authenticated={}, lat={}, lon={}", userPrincipal != null, lat, lon);

            ContextualRecommendationResponse response;
            if (userPrincipal != null) {
                // Authenticated user: Use MBTI + bookmarks + weather + time
                response = getPersonalizedContextualRecommendations(userPrincipal, lat, lon, query, timeContext, limit);
            } else {
                // Guest user: Use popular places + weather + time
                response = getGuestContextualRecommendations(lat, lon, query, timeContext, limit);
            }

            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            logger.error("Failed to generate contextual recommendations", e);
            ApiResponse<ContextualRecommendationResponse> errorResponse = ApiResponse.error("RECOMMENDATION_ERROR", "Failed to generate recommendations: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @Operation(
        summary = "Query recommendations (legacy)",
        description = "Legacy POST endpoint that redirects to contextual recommendations"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Recommendations retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Failed to generate recommendations")
        }
    )
    @PostMapping("/query")
    public ResponseEntity<ApiResponse<ContextualRecommendationResponse>> queryRecommendations(
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            double lat = Optional.ofNullable(request.get("lat"))
                .map(Object::toString)
                .map(Double::parseDouble)
                .orElse(37.5665);
            double lon = Optional.ofNullable(request.get("lon"))
                .map(Object::toString)
                .map(Double::parseDouble)
                .orElse(126.9780);
            String query = Optional.ofNullable(request.get("query"))
                .map(Object::toString)
                .orElse("좋은 장소");
            int limit = Optional.ofNullable(request.get("limit"))
                .map(Object::toString)
                .map(Integer::parseInt)
                .orElse(10);

            logger.info("POST query recommendations: lat={}, lon={}, query={}, limit={}", lat, lon, query, limit);

            // Redirect to contextual recommendations
            return getContextualRecommendations(lat, lon, query, limit, userPrincipal);
        } catch (Exception e) {
            logger.error("Failed to process query recommendations", e);
            ApiResponse<ContextualRecommendationResponse> errorResponse = ApiResponse.error("QUERY_ERROR", "Failed to process recommendations query: " + e.getMessage());
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    // Private helper methods
    private String getCurrentTimeContext() {
        LocalTime now = LocalTime.now();
        int hour = now.getHour();
        if (hour >= 6 && hour <= 11) return "morning";
        if (hour >= 12 && hour <= 17) return "afternoon";
        if (hour >= 18 && hour <= 22) return "evening";
        return "night";
    }

    private boolean isValidMbtiType(String mbti) {
        Set<String> validMbtiTypes = Set.of(
            "INTJ", "INTP", "ENTJ", "ENTP",
            "INFJ", "INFP", "ENFJ", "ENFP",
            "ISTJ", "ISFJ", "ESTJ", "ESFJ",
            "ISTP", "ISFP", "ESTP", "ESFP"
        );
        return validMbtiTypes.contains(mbti.toUpperCase());
    }

    // Note: The complex private methods for personalized and guest recommendations
    // would need to be implemented based on the specific service interfaces and entities.
    // This is a simplified version focusing on the controller structure conversion.

    private ContextualRecommendationResponse getPersonalizedContextualRecommendations(
            UserPrincipal userPrincipal, double lat, double lon, String query, String timeContext, int limit) {
        // Use ContextualRecommendationService for authenticated users
        return contextualRecommendationService.getContextualRecommendations(query, lat, lon, limit);
    }

    private ContextualRecommendationResponse getGuestContextualRecommendations(
            double lat, double lon, String query, String timeContext, int limit) {
        // Use ContextualRecommendationService for unauthenticated users
        return contextualRecommendationService.getContextualRecommendations(query, lat, lon, limit);
    }

    @GetMapping("/current-time")
    @Operation(
        summary = "지금 이시간 장소 추천",
        description = "현재 시간, 날씨, 위치를 기반으로 적합한 장소를 추천합니다. 게스트와 로그인 사용자 모두 이용 가능합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "현재 시간 기반 추천 성공",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json",
                    schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CurrentTimeRecommendationsResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<CurrentTimeRecommendationsResponse>> getCurrentTimeRecommendations(
            @Parameter(description = "사용자 위도", example = "37.5665")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "사용자 경도", example = "126.9780")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "추천 개수", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            int safeLimit = limit < 1 ? 10 : Math.min(limit, 50);
            CurrentTimeRecommendationsResponse response = placeService.getCurrentTimePlaces(latitude, longitude, safeLimit);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            logger.error("Failed to get current time recommendations", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    "CURRENT_TIME_RECOMMENDATION_ERROR",
                    e.getMessage() != null ? e.getMessage() : "현재 시간 추천에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
    
    @GetMapping("/bookmark-based")
    @Operation(
        summary = "북마크 기반 장소 추천",
        description = "사용자 위치 기반으로 북마크가 많은 장소들을 추천합니다. 오늘은 이런 곳 어떠세요 섹션에서 사용됩니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "북마크 기반 추천 성공",
                content = @io.swagger.v3.oas.annotations.media.Content(
                    mediaType = "application/json"
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<List<SimplePlaceDto>>> getBookmarkBasedRecommendations(
            @Parameter(description = "사용자 위도", example = "37.5665")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "사용자 경도", example = "126.9780")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "거리 (km)", example = "20")
            @RequestParam(defaultValue = "20.0") Double distance,
            @Parameter(description = "추천 개수", example = "15")
            @RequestParam(defaultValue = "15") int limit,
            jakarta.servlet.http.HttpServletRequest httpRequest) {
        try {
            int safeLimit = Math.max(1, Math.min(limit, 50));
            List<Place> places;

            if (latitude != null && longitude != null) {
                // Get bookmark-based recommendations within distance
                PageRequest pageRequest = PageRequest.of(0, safeLimit);
                places = bookmarkRepository.findMostBookmarkedPlacesWithinDistance(latitude, longitude, distance, pageRequest);
            } else {
                // Fallback to global bookmark-based recommendations
                PageRequest pageRequest = PageRequest.of(0, safeLimit);
                places = bookmarkRepository.findMostBookmarkedPlaces(pageRequest);
            }

            List<SimplePlaceDto> placeDtos = places.stream()
                .map(this::convertToSimplePlaceDto)
                .collect(Collectors.toList());

            return ResponseEntity.ok(ApiResponse.success(placeDtos));
        } catch (Exception e) {
            logger.error("Failed to get bookmark-based recommendations", e);
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    "BOOKMARK_RECOMMENDATION_ERROR",
                    e.getMessage() != null ? e.getMessage() : "북마크 기반 추천에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    private SimplePlaceDto convertToSimplePlaceDto(Place place) {
        SimplePlaceDto dto = new SimplePlaceDto();
        dto.setId(place.getId().toString());
        dto.setName(place.getName());
        dto.setCategory(place.getCategory().get(0));
        dto.setRating(place.getRating() != null ? place.getRating().doubleValue() : null);
        dto.setReviewCount(place.getReviewCount());
        dto.setAddress(place.getRoadAddress());
        dto.setLocation(place.getRoadAddress()); // For backward compatibility
        dto.setImageUrl(null); // Gallery field removed
        dto.setDistance(0.0); // Will be calculated if needed
        dto.setIsBookmarked(false); // Will be set based on user authentication
        return dto;
    }
}