package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.service.PromptSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 프롬프트 기반 장소 검색 API
 * 사용자 문장을 임베딩하여 장소 설명과 벡터 유사도 비교
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Tag(name = "Prompt Search", description = "프롬프트 기반 벡터 유사도 장소 검색")
public class PromptSearchController {

    private final PromptSearchService promptSearchService;

    @GetMapping("/prompt")
    @Operation(summary = "프롬프트 검색", description = "자연어 문장으로 장소 검색 (벡터 유사도)")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchByPrompt(
            @RequestParam String query,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(defaultValue = "30") double distance,
            @RequestParam(defaultValue = "10") int limit) {

        List<Map<String, Object>> results = promptSearchService.searchByPrompt(
                query, latitude, longitude, distance, limit);

        return ResponseEntity.ok(ApiResponse.success(results));
    }
}
