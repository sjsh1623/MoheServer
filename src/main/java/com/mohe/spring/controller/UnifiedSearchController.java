package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.ErrorCode;
import com.mohe.spring.dto.UnifiedSearchResponse;
import com.mohe.spring.service.UnifiedSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 통합 검색 API Controller
 *
 * 장소명, 지역명, 음식, 활동 등 의미론적 Embedding 검색 지원
 * 게스트/회원 모두 사용 가능
 */
@RestController
@RequestMapping("/api/search")
@Tag(name = "통합 검색", description = "Embedding 기반 의미론적 검색 API")
public class UnifiedSearchController {

    private final UnifiedSearchService unifiedSearchService;

    public UnifiedSearchController(UnifiedSearchService unifiedSearchService) {
        this.unifiedSearchService = unifiedSearchService;
    }

    /**
     * 통합 검색 API
     *
     * 검색어를 Embedding 벡터로 변환하여 의미론적 유사도 검색을 수행합니다.
     * 키워드 검색과 Embedding 검색을 하이브리드로 결합하여 정확도를 높입니다.
     *
     * 지원 검색 예시:
     * - 장소명: "스타벅스", "무브먼트랩"
     * - 지역명: "성수동", "강남역", "홍대"
     * - 음식: "파스타", "라멘", "브런치"
     * - 활동: "데이트", "혼밥", "모임", "공부"
     * - 분위기: "조용한", "힙한", "감성"
     */
    @GetMapping
    @Operation(
        summary = "통합 검색 (Embedding + 키워드)",
        description = """
            Embedding 벡터 기반 의미론적 검색과 키워드 검색을 결합한 하이브리드 검색입니다.

            **검색 가능 항목:**
            - 장소명: "스타벅스", "무브먼트랩"
            - 지역명: "성수동", "강남역", "홍대"
            - 음식: "파스타", "라멘", "브런치", "떡볶이"
            - 활동: "데이트", "혼밥", "모임", "공부", "작업"
            - 분위기: "조용한", "힙한", "감성", "뷰맛집"

            **검색 알고리즘:**
            1. 검색어를 Embedding 벡터로 변환
            2. pgvector 코사인 유사도로 의미적으로 유사한 장소 검색
            3. 키워드 LIKE 검색으로 장소명/주소 매칭
            4. 두 결과를 병합하여 정확도 향상
            5. 위치 정보가 있으면 거리순 정렬

            **인증 불필요:** 게스트/회원 모두 사용 가능
            """
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "검색 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = UnifiedSearchResponse.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "places": [
                              {
                                "id": "1",
                                "name": "파스타 전문점",
                                "category": "음식점",
                                "rating": 4.5,
                                "location": "강남구 역삼동",
                                "image": "https://example.com/image.jpg",
                                "distance": 1.2
                              }
                            ],
                            "totalResults": 15,
                            "query": "파스타",
                            "searchType": "hybrid",
                            "searchTimeMs": 123,
                            "message": "'파스타' 검색 결과 15개를 찾았습니다."
                          }
                        }
                        """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"
            )
        }
    )
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> search(
            @Parameter(description = "검색어 (장소명, 지역명, 음식, 활동, 분위기 등)", required = true, example = "파스타")
            @RequestParam String q,
            @Parameter(description = "사용자 위도 (선택, 거리순 정렬에 사용)", example = "37.5665")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "사용자 경도 (선택, 거리순 정렬에 사용)", example = "126.9780")
            @RequestParam(required = false) Double lon,
            @Parameter(description = "결과 개수 (최대 50)", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        try {
            UnifiedSearchResponse response = unifiedSearchService.search(q, lat, lon, limit);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "검색에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 음식 검색 API (Embedding 특화)
     */
    @GetMapping("/food")
    @Operation(
        summary = "음식 검색",
        description = """
            음식/메뉴 관련 Embedding 검색입니다.

            **검색 예시:**
            - "파스타", "라멘", "브런치", "떡볶이", "치킨"
            - "이탈리안", "일식", "한식", "중식"
            - "비건", "샐러드", "디저트"
            """
    )
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> searchFood(
            @Parameter(description = "음식/메뉴 검색어", required = true, example = "파스타")
            @RequestParam String q,
            @Parameter(description = "사용자 위도 (선택)")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "사용자 경도 (선택)")
            @RequestParam(required = false) Double lon,
            @Parameter(description = "결과 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        try {
            // 음식 관련 컨텍스트 추가
            String enrichedQuery = q + " 맛집 음식점";
            UnifiedSearchResponse response = unifiedSearchService.search(enrichedQuery, lat, lon, limit);
            // 원래 쿼리로 응답
            return ResponseEntity.ok(ApiResponse.success(
                UnifiedSearchResponse.builder()
                    .places(response.getPlaces())
                    .totalResults(response.getTotalResults())
                    .query(q)
                    .searchType(response.getSearchType())
                    .searchTimeMs(response.getSearchTimeMs())
                    .message(response.getMessage().replace(enrichedQuery, q))
                    .build()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "음식 검색에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 활동 검색 API (Embedding 특화)
     */
    @GetMapping("/activity")
    @Operation(
        summary = "활동 검색",
        description = """
            활동/목적 관련 Embedding 검색입니다.

            **검색 예시:**
            - "데이트", "혼밥", "모임", "회식"
            - "공부", "작업", "독서"
            - "산책", "운동", "힐링"
            """
    )
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> searchActivity(
            @Parameter(description = "활동/목적 검색어", required = true, example = "데이트")
            @RequestParam String q,
            @Parameter(description = "사용자 위도 (선택)")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "사용자 경도 (선택)")
            @RequestParam(required = false) Double lon,
            @Parameter(description = "결과 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        try {
            // 활동 관련 컨텍스트 추가
            String enrichedQuery = q + " 장소 추천";
            UnifiedSearchResponse response = unifiedSearchService.search(enrichedQuery, lat, lon, limit);
            // 원래 쿼리로 응답
            return ResponseEntity.ok(ApiResponse.success(
                UnifiedSearchResponse.builder()
                    .places(response.getPlaces())
                    .totalResults(response.getTotalResults())
                    .query(q)
                    .searchType(response.getSearchType())
                    .searchTimeMs(response.getSearchTimeMs())
                    .message(response.getMessage().replace(enrichedQuery, q))
                    .build()
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "활동 검색에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 지역 검색 API
     */
    @GetMapping("/location")
    @Operation(
        summary = "지역 검색",
        description = """
            지역명/위치 기반 검색입니다.

            **검색 예시:**
            - "성수동", "강남역", "홍대", "이태원"
            - "용인", "분당", "판교"
            """
    )
    public ResponseEntity<ApiResponse<UnifiedSearchResponse>> searchLocation(
            @Parameter(description = "지역명 검색어", required = true, example = "성수동")
            @RequestParam String q,
            @Parameter(description = "사용자 위도 (선택)")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "사용자 경도 (선택)")
            @RequestParam(required = false) Double lon,
            @Parameter(description = "결과 개수", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            HttpServletRequest httpRequest) {
        try {
            UnifiedSearchResponse response = unifiedSearchService.search(q, lat, lon, limit);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "지역 검색에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
}
