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
    private final com.mohe.spring.config.LocationProperties locationProperties;

    public RecommendationController(
            EnhancedRecommendationService enhancedRecommendationService,
            ContextualRecommendationService contextualRecommendationService,
            WeatherService weatherService,
            PlaceService placeService,
            UserRepository userRepository,
            PlaceRepository placeRepository,
            BookmarkRepository bookmarkRepository,
            com.mohe.spring.config.LocationProperties locationProperties) {
        this.enhancedRecommendationService = enhancedRecommendationService;
        this.contextualRecommendationService = contextualRecommendationService;
        this.weatherService = weatherService;
        this.placeService = placeService;
        this.userRepository = userRepository;
        this.placeRepository = placeRepository;
        this.bookmarkRepository = bookmarkRepository;
        this.locationProperties = locationProperties;
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

    /**
     * 컨텍스트 기반 장소 추천 API
     *
     * <p>날씨, 시간, 위치를 고려한 컨텍스트 기반 추천을 제공합니다.
     *
     * <h3>위치 파라미터</h3>
     * <ul>
     *   <li>파라미터가 없을 경우: 기본 위치 사용 (서울 중구: 37.5636, 126.9976)</li>
     *   <li>파라미터 지정: 해당 위치 기준으로 추천</li>
     * </ul>
     *
     * <h3>거리 기반 혼합 전략</h3>
     * <ul>
     *   <li>15km 이내 데이터: 70%</li>
     *   <li>30km 이내 데이터: 30%</li>
     *   <li>벡터 검색 결과와 교집합하여 최종 추천</li>
     * </ul>
     *
     * <h3>예시</h3>
     * <pre>
     * // 기본 위치 사용 (서울 중구)
     * GET /api/recommendations/contextual?limit=10
     *
     * // 강남역 기준
     * GET /api/recommendations/contextual?lat=37.4979&lon=127.0276&limit=10
     * </pre>
     *
     * @param lat 위도 (optional, 기본값: 37.5636 서울 중구)
     * @param lon 경도 (optional, 기본값: 126.9976 서울 중구)
     * @param query 검색 키워드 (optional)
     * @param limit 추천 개수 (기본: 10, 최대: 20)
     * @param userPrincipal 인증 사용자 정보 (optional)
     * @return 컨텍스트 기반 추천 결과
     */
    @Operation(
        summary = "Get contextual recommendations (dual mode)",
        description = """
        요청 좌표를 기준으로 15km 이내 70% + 30km 이내 30% 후보를 만든 뒤 날씨/시간/쿼리를 결합한 벡터 검색을 수행합니다.
        - 인증 사용자: 개인 선호 벡터 + 컨텍스트 쿼리로 재정렬 (vector-location-hybrid)
        - 게스트: 동일한 컨텍스트 쿼리로 공개 벡터 검색을 수행하고, 거리 가중 후보와 교집합을 반환합니다.
        - 위치 파라미터 없이 호출 시 기본 위치(서울 중구: 37.5636, 126.9976) 사용
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
            @Parameter(description = "위도 (optional, 기본값: 37.5636 서울 중구)", required = false, example = "37.5636")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "경도 (optional, 기본값: 126.9976 서울 중구)", required = false, example = "126.9976")
            @RequestParam(required = false) Double lon,
            @Parameter(description = "Search query or keywords", required = false)
            @RequestParam(required = false) String query,
            @Parameter(description = "Maximum number of recommendations (default: 10, max: 20)")
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal userPrincipal) {
        try {
            // ENV에 위치가 설정되어 있으면 강제로 사용 (파라미터 무시)
            // ENV에 없으면 파라미터 사용
            double latitude;
            double longitude;

            if (locationProperties.getDefaultLatitude() != null && locationProperties.getDefaultLongitude() != null) {
                // ENV에 설정된 값 강제 사용 (개발 환경 테스트용)
                latitude = locationProperties.getDefaultLatitude();
                longitude = locationProperties.getDefaultLongitude();
                logger.info("Using configured mock location from ENV: lat={}, lon={}", latitude, longitude);
            } else {
                // ENV에 없으면 파라미터 사용 (기존 로직)
                if (lat == null || lon == null) {
                    throw new IllegalArgumentException("위도/경도 파라미터가 필요합니다");
                }
                latitude = lat;
                longitude = lon;
                logger.info("Using user-provided location: lat={}, lon={}", latitude, longitude);
            }

            // Validate inputs
            if (limit > 20) {
                ApiResponse<ContextualRecommendationResponse> errorResponse = ApiResponse.error("INVALID_LIMIT", "Limit cannot exceed 20");
                return ResponseEntity.badRequest().body(errorResponse);
            }

            String timeContext = getCurrentTimeContext();
            logger.info("Processing contextual recommendations for authenticated={}, lat={}, lon={}", userPrincipal != null, latitude, longitude);

            ContextualRecommendationResponse response;
            if (userPrincipal != null) {
                // Authenticated user: Use MBTI + bookmarks + weather + time
                response = getPersonalizedContextualRecommendations(userPrincipal, latitude, longitude, query, timeContext, limit);
            } else {
                // Guest user: Use popular places + weather + time
                response = getGuestContextualRecommendations(latitude, longitude, query, timeContext, limit);
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
