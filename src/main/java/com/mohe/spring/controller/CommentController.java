package com.mohe.spring.controller;

import com.mohe.spring.dto.*;
import com.mohe.spring.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 댓글 관리 컨트롤러
 */
@RestController
@RequestMapping("/api")
@Tag(name = "댓글 관리", description = "장소에 대한 사용자 댓글 CRUD API")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    /**
     * 댓글 작성
     */
    @PostMapping("/places/{placeId}/comments")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "댓글 작성",
        description = "특정 장소에 댓글을 작성합니다. 평점은 선택 사항입니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 작성 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentResponse.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "id": 1,
                            "placeId": 123,
                            "placeName": "카페 무브먼트랩",
                            "userId": 10,
                            "userNickname": "홍길동",
                            "userProfileImage": "https://example.com/profile.jpg",
                            "content": "분위기가 정말 좋았어요!",
                            "rating": 4.5,
                            "createdAt": "2024-01-15T14:30:00",
                            "updatedAt": "2024-01-15T14:30:00",
                            "isAuthor": true
                          }
                        }
                        """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증이 필요합니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "장소를 찾을 수 없습니다"
            )
        }
    )
    public ResponseEntity<ApiResponse<CommentResponse>> createComment(
            @Parameter(description = "장소 ID", required = true, example = "123")
            @PathVariable Long placeId,
            @Parameter(description = "댓글 작성 요청", required = true)
            @Valid @RequestBody CommentCreateRequest request,
            HttpServletRequest httpRequest) {
        try {
            CommentResponse response = commentService.createComment(placeId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            String errorCode = e.getMessage().contains("찾을 수 없습니다")
                ? ErrorCode.RESOURCE_NOT_FOUND
                : ErrorCode.INTERNAL_SERVER_ERROR;
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    errorCode,
                    e.getMessage() != null ? e.getMessage() : "댓글 작성에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 특정 장소의 댓글 목록 조회
     */
    @GetMapping("/places/{placeId}/comments")
    @Operation(
        summary = "장소 댓글 목록 조회",
        description = "특정 장소에 작성된 댓글 목록을 최신순으로 페이징하여 조회합니다. 인증 없이 조회 가능합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 목록 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentListResponse.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "comments": [
                              {
                                "id": 1,
                                "placeId": 123,
                                "placeName": "카페 무브먼트랩",
                                "userId": 10,
                                "userNickname": "홍길동",
                                "userProfileImage": "https://example.com/profile.jpg",
                                "content": "분위기가 정말 좋았어요!",
                                "rating": 4.5,
                                "createdAt": "2024-01-15T14:30:00",
                                "updatedAt": "2024-01-15T14:30:00",
                                "isAuthor": false
                              }
                            ],
                            "totalPages": 5,
                            "totalElements": 48,
                            "currentPage": 0,
                            "pageSize": 10,
                            "averageRating": 4.3,
                            "totalComments": 48
                          }
                        }
                        """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "장소를 찾을 수 없습니다"
            )
        }
    )
    public ResponseEntity<ApiResponse<CommentListResponse>> getCommentsByPlace(
            @Parameter(description = "장소 ID", required = true, example = "123")
            @PathVariable Long placeId,
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            int safePage = Math.max(0, page);
            int safeSize = Math.max(1, Math.min(100, size));

            CommentListResponse response = commentService.getCommentsByPlaceId(placeId, safePage, safeSize);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            String errorCode = e.getMessage().contains("찾을 수 없습니다")
                ? ErrorCode.RESOURCE_NOT_FOUND
                : ErrorCode.INTERNAL_SERVER_ERROR;
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    errorCode,
                    e.getMessage() != null ? e.getMessage() : "댓글 목록 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 내가 작성한 댓글 목록 조회
     */
    @GetMapping("/user/comments")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "내가 작성한 댓글 목록 조회",
        description = "로그인한 사용자가 작성한 모든 댓글을 최신순으로 조회합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "내 댓글 목록 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentListResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증이 필요합니다"
            )
        }
    )
    public ResponseEntity<ApiResponse<CommentListResponse>> getMyComments(
            @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "페이지 크기", example = "10")
            @RequestParam(defaultValue = "10") int size,
            HttpServletRequest httpRequest) {
        try {
            int safePage = Math.max(0, page);
            int safeSize = Math.max(1, Math.min(100, size));

            CommentListResponse response = commentService.getMyComments(safePage, safeSize);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    ErrorCode.INTERNAL_SERVER_ERROR,
                    e.getMessage() != null ? e.getMessage() : "내 댓글 목록 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 댓글 수정
     */
    @PutMapping("/comments/{commentId}")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "댓글 수정",
        description = "본인이 작성한 댓글을 수정합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 수정 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = CommentResponse.class)
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증이 필요합니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인이 작성한 댓글만 수정할 수 있습니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "댓글을 찾을 수 없습니다"
            )
        }
    )
    public ResponseEntity<ApiResponse<CommentResponse>> updateComment(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId,
            @Parameter(description = "댓글 수정 요청", required = true)
            @Valid @RequestBody CommentUpdateRequest request,
            HttpServletRequest httpRequest) {
        try {
            CommentResponse response = commentService.updateComment(commentId, request);
            return ResponseEntity.ok(ApiResponse.success(response));
        } catch (Exception e) {
            String errorCode;
            if (e.getMessage().contains("찾을 수 없습니다")) {
                errorCode = ErrorCode.RESOURCE_NOT_FOUND;
            } else if (e.getMessage().contains("본인이 작성한")) {
                errorCode = ErrorCode.ACCESS_DENIED;
            } else {
                errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
            }
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    errorCode,
                    e.getMessage() != null ? e.getMessage() : "댓글 수정에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    /**
     * 댓글 삭제
     */
    @DeleteMapping("/comments/{commentId}")
    @PreAuthorize("hasRole('USER')")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(
        summary = "댓글 삭제",
        description = "본인이 작성한 댓글을 삭제합니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "댓글 삭제 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "message": "댓글이 삭제되었습니다"
                        }
                        """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "401",
                description = "인증이 필요합니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "403",
                description = "본인이 작성한 댓글만 삭제할 수 있습니다"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "댓글을 찾을 수 없습니다"
            )
        }
    )
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "댓글 ID", required = true, example = "1")
            @PathVariable Long commentId,
            HttpServletRequest httpRequest) {
        try {
            commentService.deleteComment(commentId);
            return ResponseEntity.ok(ApiResponse.success("댓글이 삭제되었습니다"));
        } catch (Exception e) {
            String errorCode;
            if (e.getMessage().contains("찾을 수 없습니다")) {
                errorCode = ErrorCode.RESOURCE_NOT_FOUND;
            } else if (e.getMessage().contains("본인이 작성한")) {
                errorCode = ErrorCode.ACCESS_DENIED;
            } else {
                errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
            }
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    errorCode,
                    e.getMessage() != null ? e.getMessage() : "댓글 삭제에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
}
