package com.mohe.spring.controller;

import com.mohe.spring.dto.*;
import com.mohe.spring.service.PlaceService;
import com.mohe.spring.service.VectorSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
// Import removed - using fully qualified name for ApiResponse to avoid conflict
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/places")
@Tag(name = "장소 관리", description = "장소 추천, 검색, 상세 정보 API")
public class PlaceController {
    
    private final PlaceService placeService;
    private final VectorSearchService vectorSearchService;
    
    public PlaceController(PlaceService placeService, VectorSearchService vectorSearchService) {
        this.placeService = placeService;
        this.vectorSearchService = vectorSearchService;
    }
    
    @GetMapping("/recommendations")
    @Operation(
        summary = "장소 추천 (게스트/회원 공통)",
        description = "로그인 사용자는 개인화된 추천, 게스트는 평점 기반 추천을 제공합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "추천 장소 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceRecommendationsResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<PlaceRecommendationsResponse>> getRecommendations(
            HttpServletRequest httpRequest) {
        try {
            PlaceRecommendationsResponse response = placeService.getRecommendations();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "추천 장소 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
    
    @GetMapping
    @Operation(
        summary = "장소 목록 조회",
        description = "페이지네이션과 필터링을 지원하는 장소 목록을 조회합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "장소 목록 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceListResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<PlaceListResponse>> getPlaces(
            @Parameter(description = "페이지 번호 (1부터 시작)", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지당 아이템 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "카테고리 필터", example = "cafe")
            @RequestParam(required = false) String category,
            @Parameter(description = "정렬 방식 (rating, popularity)", example = "rating")
            @RequestParam(defaultValue = "rating") String sort,
            HttpServletRequest httpRequest) {
        try {
            PlaceListResponse response = placeService.getPlaces(page, limit, category, sort);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "장소 목록 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "장소 상세 정보 조회",
        description = "지정된 장소의 상세 정보를 조회하고 최근 조회 이력에 추가합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "장소 상세 정보 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceDetailResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "장소를 찾을 수 없음"
            )
        }
    )
    public ResponseEntity<ApiResponse<PlaceDetailResponse>> getPlaceDetail(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable String id,
            HttpServletRequest httpRequest) {
        try {
            PlaceDetailResponse response = placeService.getPlaceDetail(id);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            String errorCode;
            if (e.getMessage() != null && e.getMessage().contains("찾을 수 없습니다")) {
                errorCode = ErrorCode.RESOURCE_NOT_FOUND;
            } else {
                errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
            }
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    errorCode,
                    e.getMessage() != null ? e.getMessage() : "장소 상세 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
    
    @GetMapping("/search")
    @Operation(
        summary = "장소 검색",
        description = "컴텍스트 기반 장소 검색 (날씨, 시간, 위치 고려)"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "장소 검색 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceSearchResponse.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "searchResults": [
                              {
                                "id": "1",
                                "name": "카페 무브먼트랩",
                                "hours": "09:00 ~ 19:00",
                                "location": "서울 성수동",
                                "rating": 4.7,
                                "carTime": "5분",
                                "busTime": "10분",
                                "tags": ["#조용한", "#카페", "#시원한"],
                                "image": "https://example.com/place.jpg",
                                "isBookmarked": false,
                                "weatherTag": {
                                  "text": "더운 날씨에 가기 좋은 카페",
                                  "color": "red",
                                  "icon": "thermometer"
                                }
                              }
                            ],
                            "searchContext": {
                              "weather": "더운 날씨",
                              "time": "오후 2시",
                              "recommendation": "지금은 멀지 않고 실내 장소들을 추천드릴께요."
                            }
                          }
                        }
                        """
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<PlaceSearchResponse>> searchPlaces(
            @Parameter(description = "검색 쿼리", required = true, example = "카페")
            @RequestParam String q,
            @Parameter(description = "위치 필터", example = "성수동")
            @RequestParam(required = false) String location,
            @Parameter(description = "날씨 컨텍스트 (hot, cold)", example = "hot")
            @RequestParam(required = false) String weather,
            @Parameter(description = "시간 컨텍스트 (morning, afternoon, evening)", example = "afternoon")
            @RequestParam(required = false) String time,
            HttpServletRequest httpRequest) {
        try {
            PlaceSearchResponse response = placeService.searchPlaces(q, location, weather, time);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "장소 검색에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/nearby")
    @Operation(
        summary = "주변 장소 조회",
        description = "좌표를 기준으로 반경 내 인기 장소를 조회합니다."
    )
    public ResponseEntity<ApiResponse<PlaceListResponse>> getNearbyPlaces(
            @Parameter(description = "사용자 위도", required = true, example = "37.5665")
            @RequestParam double latitude,
            @Parameter(description = "사용자 경도", required = true, example = "126.9780")
            @RequestParam double longitude,
            @Parameter(description = "검색 반경 (미터)", example = "3000")
            @RequestParam(defaultValue = "3000") double radius,
            @Parameter(description = "결과 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        try {
            PlaceListResponse response = placeService.getNearbyPlaces(latitude, longitude, radius, limit);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "주변 장소 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/debug")
    @Operation(
        summary = "디버그 - 장소 데이터 확인",
        description = "데이터베이스의 장소 데이터를 확인하기 위한 디버그 엔드포인트"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200", 
                description = "디버그 정보 조회 성공"
            )
        }
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> debugPlaces(
            HttpServletRequest httpRequest) {
        try {
            Map<String, Object> totalPlaces = placeService.getDebugInfo();
            return ResponseEntity.ok(ApiResponse.success(totalPlaces));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "디버그 정보 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/popular")
    @Operation(
        summary = "인기 장소 목록 조회",
        description = "사용자 위치를 기반으로 20km 이내의 인기 장소를 리뷰수와 평점 순으로 조회합니다. 게스트와 로그인 사용자 모두 접근 가능합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "인기 장소 목록 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceListResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<PlaceListResponse>> getPopularPlaces(
            @Parameter(description = "사용자 위도", required = true, example = "37.5665")
            @RequestParam double latitude,
            @Parameter(description = "사용자 경도", required = true, example = "126.9780")
            @RequestParam double longitude,
            HttpServletRequest httpRequest) {
        try {
            PlaceListResponse response = placeService.getPopularPlaces(latitude, longitude);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "인기 장소 목록 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/current-time")
    @Operation(
        summary = "지금 이 시간의 장소 추천",
        description = "현재 시간과 날씨를 기반으로 적합한 장소를 추천합니다. 로그인 여부와 관계없이 모든 사용자가 접근 가능합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "현재 시간대 추천 장소 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CurrentTimeRecommendationsResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<CurrentTimeRecommendationsResponse>> getCurrentTimePlaces(
            @Parameter(description = "사용자 위도", example = "37.5665")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "사용자 경도", example = "126.9780")
            @RequestParam(required = false) Double longitude,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int limit,
            HttpServletRequest httpRequest) {
        try {
            int safeLimit = limit < 1 ? 10 : Math.min(limit, 50);
            CurrentTimeRecommendationsResponse response = placeService.getCurrentTimePlaces(latitude, longitude, safeLimit);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "현재 시간 추천 장소 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @Operation(
        summary = "Get general places list",
        description = "Get a general list of places with pagination and sorting options"
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Places retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid parameters")
        }
    )
    @GetMapping("/list")
    public ResponseEntity<ApiResponse<PlaceListResponse>> getPlacesList(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10") 
            @RequestParam(defaultValue = "10") int limit,
            @Parameter(description = "정렬 기준 (popularity, rating, recent)", example = "popularity")
            @RequestParam(defaultValue = "popularity") String sort,
            HttpServletRequest httpRequest) {
        try {
            int safePage = Math.max(page, 0);
            int safeLimit = limit < 1 ? 10 : Math.min(limit, 100);
            
            PlaceListResponse response = placeService.getPlacesList(safePage, safeLimit, sort);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "장소 목록 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/vector-search")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "벡터 유사도 기반 장소 검색",
        description = "사용자의 취향을 반영한 벡터 유사도 기반 개인화된 장소 검색을 제공합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "벡터 검색 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = VectorSimilarityResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증이 필요한 서비스입니다"
            )
        }
    )
    public ResponseEntity<ApiResponse<VectorSimilarityResponse>> vectorSearchPlaces(
            @Parameter(description = "검색 쿼리", required = true, example = "조용한 카페")
            @RequestParam String query,
            @Parameter(description = "유사도 임계값 (0.0~1.0)", example = "0.3")
            @RequestParam(defaultValue = "0.3") double threshold,
            @Parameter(description = "반환할 최대 결과 수", example = "15")
            @RequestParam(defaultValue = "15") int limit,
            HttpServletRequest httpRequest) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userEmail = auth.getName();
            
            // Validate parameters
            double safeThreshold = Math.max(0.0, Math.min(1.0, threshold));
            int safeLimit = Math.max(1, Math.min(50, limit));
            
            VectorSimilarityResponse response = vectorSearchService.searchWithVectorSimilarity(
                query, userEmail, safeThreshold, safeLimit);
            
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "벡터 검색에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/new")
    @Operation(
        summary = "새로운 장소 추천",
        description = "추천 리스트를 갱신하여 새로운 장소들을 제공합니다. 기본 추천 API와 동일하게 동작합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "새로운 추천 장소 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = PlaceRecommendationsResponse.class)
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<PlaceRecommendationsResponse>> getNewRecommendations(
            HttpServletRequest httpRequest) {
        try {
            // Same as /recommendations - provides fresh recommendations
            PlaceRecommendationsResponse response = placeService.getRecommendations();
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "새로운 추천 장소 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
}
