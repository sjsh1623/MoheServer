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
    private final com.mohe.spring.config.LocationProperties locationProperties;

    public PlaceController(PlaceService placeService, VectorSearchService vectorSearchService, com.mohe.spring.config.LocationProperties locationProperties) {
        this.placeService = placeService;
        this.vectorSearchService = vectorSearchService;
        this.locationProperties = locationProperties;
    }
    
    /**
     * 장소 추천 API (게스트/회원 공통)
     *
     * <p>위치 기반 장소 추천을 제공합니다. 인증 사용자는 개인 선호도를 반영한 추천을 받습니다.
     *
     * <h3>위치 파라미터</h3>
     * <ul>
     *   <li>파라미터가 없고 ENV 기본값이 설정된 경우: 해당 기본 위치 사용</li>
     *   <li>파라미터 지정: 해당 위치 기준으로 추천</li>
     * </ul>
     *
     * <h3>거리 기반 혼합 전략</h3>
     * <ul>
     *   <li>15km 이내 데이터: 70%</li>
     *   <li>30km 이내 데이터: 30%</li>
     *   <li>인증 사용자: 벡터 기반 선호도로 재정렬</li>
     * </ul>
     *
     * <h3>예시</h3>
     * <pre>
     * // ENV에 기본 좌표가 설정되어 있고 파라미터가 없을 때
     * GET /api/places/recommendations
     *
     * // 강남역 기준
     * GET /api/places/recommendations?latitude=37.4979&longitude=127.0276
     * </pre>
     *
     * @param latitude 위도 (optional, 기본값: 37.5636 서울 중구)
     * @param longitude 경도 (optional, 기본값: 126.9976 서울 중구)
     * @param httpRequest HTTP 요청 정보
     * @return 추천 장소 목록
     */
    @GetMapping("/recommendations")
    @Operation(
        summary = "장소 추천 (게스트/회원 공통)",
        description = """
        요청 좌표를 기준으로 15km 이내 데이터 70%, 30km 이내 데이터 30%를 혼합해 기본 후보군을 만들고,
        인증 사용자는 벡터 기반 선호도를 반영해 같은 후보군을 재정렬합니다.
        위치 파라미터가 없으면 ENV에 설정된 기본 좌표(있는 경우)에 한해 사용합니다.
        """
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
            @Parameter(description = "위도 (미지정 시 ENV 기본값 사용)", required = false, example = "37.5665")
            @RequestParam(required = false) Double latitude,
            @Parameter(description = "경도 (미지정 시 ENV 기본값 사용)", required = false, example = "126.9780")
            @RequestParam(required = false) Double longitude,
            HttpServletRequest httpRequest) {
        try {
            Double lat = latitude != null ? latitude : locationProperties.getDefaultLatitude();
            Double lon = longitude != null ? longitude : locationProperties.getDefaultLongitude();

            if (lat == null || lon == null) {
                throw new IllegalArgumentException("위도/경도 파라미터가 필요합니다");
            }

            PlaceRecommendationsResponse response = placeService.getRecommendations(lat, lon);
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
        description = """
        카테고리 지정 시 해당 카테고리 컬럼을 직접 조회하고, 미지정 시 내부 추천 가능 장소를 평점 DESC → 리뷰수 DESC 순으로 페이지네이션 합니다.
        sort 파라미터는 rating(평점 위주), popularity(리뷰/평점 복합), recent(최신 크롤링) 순서를 각각 적용합니다.
        """
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
        description = """
        places 테이블의 PK로 단건을 조회한 뒤 저장된 상세 설명과 리뷰 수, 카테고리 정보를 그대로 반환합니다.
        유사 장소 슬롯은 동일 카테고리 기반의 향후 확장 지점을 나타내며 현재는 빈 배열을 돌려줍니다.
        """
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
        description = """
        인증 사용자는 선호 벡터와 검색어를 함께 사용해 후보를 만들고, 게스트는 키워드(이름/도로명) LIKE 검색으로 결과를 만듭니다.
        날씨·시간 파라미터는 응답 컨텍스트 메시지를 조정해 추천 사유를 명시합니다.
        """
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
        description = """
        Haversine 기반 native query로 요청 좌표에서 radius 미터(기본 3km)를 km로 변환해 필터링하고,
        평점 3점 이상 장소를 거리 오름차순으로 반환합니다.
        """
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
        description = """
        places 테이블을 직접 조회해 전체 건수, 추천 가능(ready) 건수, 카테고리 분포를 반환하는 내부 점검용 API입니다.
        """
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
        description = """
        요청 좌표 반경 30km 이내 데이터를 15km(70%) + 30km(30%) 비율로 뽑은 뒤,
        review_count DESC → rating DESC 순으로 정렬해 인기 순위를 산출합니다.
        """
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
            @Parameter(description = "반환할 최대 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        try {
            int safeLimit = limit < 1 ? 10 : Math.min(limit, 50);
            PlaceListResponse response = placeService.getPopularPlaces(latitude, longitude, safeLimit);
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
        description = """
        요청 좌표 기준 15km/30km 가중치를 먼저 적용해 후보를 만들고,
        현재 시간대 및 모의 날씨 정보를 LLM Prompt에 넣어 메시지와 순서를 재조정합니다.
        """
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
            @Parameter(description = "사용자 위도", required = true, example = "37.5665")
            @RequestParam double latitude,
            @Parameter(description = "사용자 경도", required = true, example = "126.9780")
            @RequestParam double longitude,
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
        description = """
        내부 배치로 준비된 recommendable_places를 페이지 단위로 반환합니다.
        sort=popularity 는 review_count DESC → rating DESC,
        sort=rating 은 최소 평점 3 이상으로 필터링 후 rating DESC,
        그 외는 최신 업데이트 순서를 따릅니다.
        """
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
        description = """
        사용자 선호 벡터와 장소 설명 벡터를 비교해 weighted similarity ≥ threshold 인 결과만 반환하며,
        벡터 캐시가 없으면 키워드 검색과 실시간 유사도 계산을 혼합합니다.
        """
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
        description = """
        /recommendations 와 동일한 15km/30km 가중치 로직을 사용하지만 클라이언트가 강제로 새 데이터를 요청할 때 사용합니다.
        """
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
            @Parameter(description = "사용자 위도", required = true, example = "37.5665")
            @RequestParam double latitude,
            @Parameter(description = "사용자 경도", required = true, example = "126.9780")
            @RequestParam double longitude,
            HttpServletRequest httpRequest) {
        try {
            PlaceRecommendationsResponse response = placeService.getRecommendations(latitude, longitude);
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
