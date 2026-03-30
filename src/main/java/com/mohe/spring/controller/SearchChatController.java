package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.service.SearchChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/search/chat")
@RequiredArgsConstructor
@Tag(name = "Search Chat", description = "대화형 검색 + 히스토리")
public class SearchChatController {

    private final SearchChatService searchChatService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 검색 + 대화 저장
     */
    @PostMapping
    @Operation(summary = "대화형 검색", description = "검색 실행 + 대화 DB 저장")
    public ResponseEntity<ApiResponse<Map<String, Object>>> searchChat(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String query = (String) request.get("query");
        if (query == null || query.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("EMPTY_QUERY", "검색어를 입력해주세요"));
        }

        Long conversationId = request.get("conversationId") != null
                ? ((Number) request.get("conversationId")).longValue() : null;
        Double latitude = request.get("latitude") != null
                ? ((Number) request.get("latitude")).doubleValue() : null;
        Double longitude = request.get("longitude") != null
                ? ((Number) request.get("longitude")).doubleValue() : null;
        String sessionId = (String) request.get("sessionId");
        int limit = request.get("limit") != null
                ? ((Number) request.get("limit")).intValue() : 10;

        // JWT에서 userId 추출 (로그인한 경우)
        Long userId = extractUserId(authHeader);

        Map<String, Object> result = searchChatService.searchAndSave(
                query, conversationId, userId, sessionId, latitude, longitude, limit);

        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 대화 목록
     */
    @GetMapping("/conversations")
    @Operation(summary = "대화 목록", description = "최근 20개 대화 히스토리")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getConversations(
            @RequestParam(required = false) String sessionId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        Long userId = extractUserId(authHeader);
        List<Map<String, Object>> conversations = searchChatService.getConversations(userId, sessionId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    /**
     * 대화 상세
     */
    @GetMapping("/conversations/{id}")
    @Operation(summary = "대화 상세", description = "대화 메시지 전체 조회")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getConversation(@PathVariable Long id) {
        Map<String, Object> conversation = searchChatService.getConversation(id);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(ApiResponse.success(conversation));
    }

    /**
     * 대화 삭제
     */
    @DeleteMapping("/conversations/{id}")
    @Operation(summary = "대화 삭제")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteConversation(@PathVariable Long id) {
        boolean deleted = searchChatService.deleteConversation(id);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("message", deleted ? "삭제 완료" : "대화를 찾을 수 없습니다")
        ));
    }

    private Long extractUserId(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            try {
                String token = authHeader.substring(7);
                return jwtTokenProvider.getUserIdFromToken(token);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }
}
