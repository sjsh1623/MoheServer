package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.refresh.BatchRefreshResponseDto;
import com.mohe.spring.dto.refresh.PlaceRefreshResponseDto;
import com.mohe.spring.service.refresh.PlaceRefreshService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 장소 데이터 새로고침 컨트롤러
 *
 * <p>특정 장소의 이미지와 리뷰를 네이버에서 새로 크롤링하여 업데이트합니다.</p>
 *
 * <h3>엔드포인트</h3>
 * <ul>
 *   <li>POST /api/places/{placeId}/refresh - 이미지 + 리뷰 전체 새로고침</li>
 *   <li>POST /api/places/{placeId}/refresh/images - 이미지만 새로고침</li>
 *   <li>POST /api/places/{placeId}/refresh/reviews - 리뷰만 새로고침</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/places")
@Tag(name = "장소 데이터 새로고침", description = "장소 이미지/리뷰 크롤링 및 업데이트 API")
public class PlaceRefreshController {
    private static final Logger logger = LoggerFactory.getLogger(PlaceRefreshController.class);

    private final PlaceRefreshService placeRefreshService;

    public PlaceRefreshController(PlaceRefreshService placeRefreshService) {
        this.placeRefreshService = placeRefreshService;
    }

    /**
     * 장소 데이터 전체 새로고침 (이미지 + 리뷰)
     *
     * @param placeId 장소 ID
     * @param request HTTP 요청
     * @return 새로고침 결과
     */
    @PostMapping("/{placeId}/refresh")
    @Operation(
        summary = "장소 데이터 새로고침",
        description = """
            특정 장소의 이미지와 리뷰를 네이버에서 새로 크롤링하여 업데이트합니다.

            - 이미지: 최대 5장까지 새로 가져와서 기존 이미지 대체
            - 리뷰: 중복되지 않는 새로운 리뷰만 추가 (최대 10개)
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장소를 찾을 수 없음"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "크롤링 실패"
        )
    })
    public ResponseEntity<ApiResponse<PlaceRefreshResponseDto>> refreshPlaceData(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long placeId,
            HttpServletRequest request) {
        try {
            logger.info("Refresh request for place ID: {}", placeId);

            PlaceRefreshResponseDto result = placeRefreshService.refreshPlaceData(placeId);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            logger.warn("Place not found: {}", placeId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("PLACE_NOT_FOUND", e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            logger.error("Refresh failed for place ID: {}", placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * 이미지만 새로고침
     *
     * @param placeId 장소 ID
     * @param request HTTP 요청
     * @return 새로고침 결과
     */
    @PostMapping("/{placeId}/refresh/images")
    @Operation(
        summary = "이미지 새로고침",
        description = """
            특정 장소의 이미지만 네이버에서 새로 크롤링하여 업데이트합니다.

            - 최대 5장까지 새로 가져와서 기존 이미지 대체
            - 이미지 전용 크롤링 API 사용으로 빠른 처리
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "이미지 새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장소를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<PlaceRefreshResponseDto>> refreshImages(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long placeId,
            HttpServletRequest request) {
        try {
            logger.info("Image refresh request for place ID: {}", placeId);

            PlaceRefreshResponseDto result = placeRefreshService.refreshImages(placeId);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            logger.warn("Place not found: {}", placeId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("PLACE_NOT_FOUND", e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            logger.error("Image refresh failed for place ID: {}", placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("IMAGE_REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * 리뷰만 새로고침
     *
     * @param placeId 장소 ID
     * @param request HTTP 요청
     * @return 새로고침 결과
     */
    @PostMapping("/{placeId}/refresh/reviews")
    @Operation(
        summary = "리뷰 새로고침",
        description = """
            특정 장소의 리뷰만 네이버에서 새로 크롤링하여 업데이트합니다.

            - 기존 리뷰와 중복되지 않는 새로운 리뷰만 추가
            - 최대 10개까지 저장
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "리뷰 새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장소를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<PlaceRefreshResponseDto>> refreshReviews(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long placeId,
            HttpServletRequest request) {
        try {
            logger.info("Review refresh request for place ID: {}", placeId);

            PlaceRefreshResponseDto result = placeRefreshService.refreshReviews(placeId);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            logger.warn("Place not found: {}", placeId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("PLACE_NOT_FOUND", e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            logger.error("Review refresh failed for place ID: {}", placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("REVIEW_REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * 영업시간만 새로고침
     *
     * @param placeId 장소 ID
     * @param request HTTP 요청
     * @return 새로고침 결과
     */
    @PostMapping("/{placeId}/refresh/business-hours")
    @Operation(
        summary = "영업시간 새로고침",
        description = """
            특정 장소의 영업시간만 네이버에서 새로 크롤링하여 업데이트합니다.

            - 요일별 영업시간 정보 갱신
            - 라스트오더 시간 포함
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "영업시간 새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장소를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<PlaceRefreshResponseDto>> refreshBusinessHours(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long placeId,
            HttpServletRequest request) {
        try {
            logger.info("Business hours refresh request for place ID: {}", placeId);

            PlaceRefreshResponseDto result = placeRefreshService.refreshBusinessHours(placeId);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            logger.warn("Place not found: {}", placeId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("PLACE_NOT_FOUND", e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            logger.error("Business hours refresh failed for place ID: {}", placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("BUSINESS_HOURS_REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * 메뉴만 새로고침
     *
     * @param placeId 장소 ID
     * @param request HTTP 요청
     * @return 새로고침 결과
     */
    @PostMapping("/{placeId}/refresh/menus")
    @Operation(
        summary = "메뉴 새로고침",
        description = """
            특정 장소의 메뉴를 네이버에서 새로 크롤링하여 업데이트합니다.

            - 기존 메뉴 삭제 후 새로 크롤링
            - 메뉴 이미지가 있는 경우 자동 다운로드
            - 최대 50개 메뉴까지 저장
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "메뉴 새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = PlaceRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "장소를 찾을 수 없음"
        )
    })
    public ResponseEntity<ApiResponse<PlaceRefreshResponseDto>> refreshMenus(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long placeId,
            HttpServletRequest request) {
        try {
            logger.info("Menu refresh request for place ID: {}", placeId);

            PlaceRefreshResponseDto result = placeRefreshService.refreshMenus(placeId);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (IllegalArgumentException e) {
            logger.warn("Place not found: {}", placeId);
            return ResponseEntity.status(404)
                    .body(ApiResponse.error("PLACE_NOT_FOUND", e.getMessage(), request.getRequestURI()));
        } catch (Exception e) {
            logger.error("Menu refresh failed for place ID: {}", placeId, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("MENU_REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * 전체 Places 배치 새로고침
     *
     * @param request HTTP 요청
     * @return 배치 새로고침 결과
     */
    @PostMapping("/refresh/all")
    @Operation(
        summary = "전체 장소 배치 새로고침",
        description = """
            모든 장소의 데이터를 새로 크롤링하여 업데이트합니다.

            - 이미지, 리뷰, 메뉴, 영업시간 전체 업데이트
            - 장소 수가 많으면 시간이 오래 걸릴 수 있음
            - 페이지네이션이 필요한 경우 /refresh/batch 사용 권장
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "배치 새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BatchRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "배치 새로고침 실패"
        )
    })
    public ResponseEntity<ApiResponse<BatchRefreshResponseDto>> refreshAllPlaces(
            HttpServletRequest request) {
        try {
            logger.info("Batch refresh request for all places");

            BatchRefreshResponseDto result = placeRefreshService.refreshAllPlaces();

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("Batch refresh failed", e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("BATCH_REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }

    /**
     * 페이지네이션된 Places 배치 새로고침
     *
     * @param offset  시작 위치 (기본: 0)
     * @param limit   최대 개수 (기본: 50)
     * @param request HTTP 요청
     * @return 배치 새로고침 결과
     */
    @PostMapping("/refresh/batch")
    @Operation(
        summary = "페이지네이션된 장소 배치 새로고침",
        description = """
            지정된 범위의 장소 데이터를 새로 크롤링하여 업데이트합니다.

            - offset: 시작 위치 (기본: 0)
            - limit: 최대 개수 (기본: 50, 최대: 100)
            - 대량의 장소를 처리할 때 부분적으로 실행 가능
            """
    )
    @ApiResponses(value = {
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "배치 새로고침 성공",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = BatchRefreshResponseDto.class)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 파라미터"
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "500",
            description = "배치 새로고침 실패"
        )
    })
    public ResponseEntity<ApiResponse<BatchRefreshResponseDto>> refreshPlacesBatch(
            @Parameter(description = "시작 위치", example = "0")
            @RequestParam(defaultValue = "0") int offset,
            @Parameter(description = "최대 개수 (최대 100)", example = "50")
            @RequestParam(defaultValue = "50") int limit,
            HttpServletRequest request) {
        try {
            // 파라미터 검증
            if (offset < 0) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_OFFSET", "Offset must be >= 0", request.getRequestURI()));
            }
            if (limit <= 0 || limit > 100) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("INVALID_LIMIT", "Limit must be between 1 and 100", request.getRequestURI()));
            }

            logger.info("Batch refresh request: offset={}, limit={}", offset, limit);

            BatchRefreshResponseDto result = placeRefreshService.refreshPlacesBatch(offset, limit);

            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            logger.error("Batch refresh failed: offset={}, limit={}", offset, limit, e);
            return ResponseEntity.status(500)
                    .body(ApiResponse.error("BATCH_REFRESH_FAILED", e.getMessage(), request.getRequestURI()));
        }
    }
}
