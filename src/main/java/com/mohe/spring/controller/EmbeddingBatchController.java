package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.embedding.BatchEmbeddingResult;
import com.mohe.spring.service.EmbeddingBatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * REST Controller for managing keyword embedding batch operations
 * Provides endpoints to trigger embedding batch processing for places
 */
@Slf4j
@RestController
@RequestMapping("/api/batch/embeddings")
@Tag(name = "Embedding Batch", description = "키워드 임베딩 배치 처리 API")
@RequiredArgsConstructor
public class EmbeddingBatchController {

    private final EmbeddingBatchService embeddingBatchService;

    /**
     * Run keyword embedding batch process
     * Processes all places where crawler_found = true
     */
    @PostMapping("/run")
    @Operation(
        summary = "키워드 임베딩 배치 실행",
        description = """
            crawler_found = true인 모든 장소의 키워드를 처리하여 벡터 임베딩을 생성합니다.

            **처리 과정:**
            1. crawler_found = true인 장소 목록 조회
            2. 각 장소의 keywords 배열(최대 9개)을 Kanana 임베딩 서버로 전송
            3. 반환받은 벡터를 place_keyword_embeddings 테이블에 저장

            **배치 설정:**
            - 한 번에 최대 9개 장소 단위로 처리
            - 각 장소는 독립적인 트랜잭션으로 처리 (에러 발생 시 다른 장소에 영향 없음)
            - 임베딩 서버 타임아웃: 150초

            **임베딩 서버:**
            - Endpoint: POST http://localhost:8000/embed
            - Model: Kanana nano 2.1b (1792 dimensions)
            """
    )
    public ResponseEntity<ApiResponse<BatchEmbeddingResult>> runEmbeddingBatch() {
        try {
            log.info("🚀 Triggering Keyword Embedding Batch");

            // Check if embedding service is available
            if (!embeddingBatchService.isEmbeddingServiceAvailable()) {
                log.error("❌ Embedding service is not available");
                return ResponseEntity.status(503).body(
                    ApiResponse.error("SERVICE_UNAVAILABLE",
                        "Embedding service is not available at http://localhost:8000",
                        "/api/batch/embeddings/run")
                );
            }

            // Run batch process
            BatchEmbeddingResult result = embeddingBatchService.runBatchEmbedding();

            // Check if batch process had errors
            if (result.getErrorMessage() != null) {
                log.error("❌ Batch embedding failed: {}", result.getErrorMessage());
                return ResponseEntity.status(500).body(
                    ApiResponse.error("BATCH_FAILED",
                        "Batch embedding process failed: " + result.getErrorMessage(),
                        "/api/batch/embeddings/run")
                );
            }

            log.info("✅ Keyword Embedding Batch completed successfully");
            log.info("📊 Statistics: Total={}, Success={}, Failed={}, Skipped={}, Embeddings={}, Time={}s",
                result.getTotalPlaces(),
                result.getSuccessfulPlaces(),
                result.getFailedPlaces(),
                result.getSkippedPlaces(),
                result.getTotalEmbeddings(),
                result.getProcessingTimeSec());

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Failed to run embedding batch", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("BATCH_ERROR",
                    "Failed to run embedding batch: " + e.getMessage(),
                    "/api/batch/embeddings/run")
            );
        }
    }

    /**
     * Get embedding statistics
     */
    @GetMapping("/stats")
    @Operation(
        summary = "임베딩 통계 조회",
        description = "전체 임베딩 개수와 임베딩이 존재하는 장소 개수를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getEmbeddingStats() {
        try {
            String stats = embeddingBatchService.getEmbeddingStats();
            log.info("📊 Embedding Stats: {}", stats);

            Map<String, Object> result = new HashMap<>();
            result.put("statistics", stats);

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Failed to get embedding stats", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("STATS_ERROR",
                    "Failed to get embedding stats: " + e.getMessage(),
                    "/api/batch/embeddings/stats")
            );
        }
    }

    /**
     * Delete embeddings for a specific place
     */
    @DeleteMapping("/place/{placeId}")
    @Operation(
        summary = "특정 장소의 임베딩 삭제",
        description = "지정된 장소의 모든 키워드 임베딩을 삭제합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> deleteEmbeddingsForPlace(
        @Parameter(description = "장소 ID", required = true)
        @PathVariable Long placeId
    ) {
        try {
            log.info("🗑️ Deleting embeddings for place_id={}", placeId);

            embeddingBatchService.deleteEmbeddingsForPlace(placeId);

            Map<String, Object> result = new HashMap<>();
            result.put("placeId", placeId);
            result.put("status", "DELETED");

            log.info("✅ Deleted embeddings for place_id={}", placeId);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            log.error("❌ Failed to delete embeddings for place_id={}", placeId, e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("DELETE_ERROR",
                    "Failed to delete embeddings: " + e.getMessage(),
                    "/api/batch/embeddings/place/" + placeId)
            );
        }
    }

    /**
     * Check if embedding service is available
     */
    @GetMapping("/health")
    @Operation(
        summary = "임베딩 서비스 상태 확인",
        description = "Kanana 임베딩 서버의 연결 상태를 확인합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkEmbeddingServiceHealth() {
        try {
            boolean available = embeddingBatchService.isEmbeddingServiceAvailable();

            Map<String, Object> result = new HashMap<>();
            result.put("available", available);
            result.put("serviceUrl", "http://localhost:8000");

            if (available) {
                log.info("✅ Embedding service is available");
                return ResponseEntity.ok(ApiResponse.success(result));
            } else {
                log.warn("⚠️ Embedding service is not available");
                return ResponseEntity.status(503).body(
                    ApiResponse.error("SERVICE_UNAVAILABLE",
                        "Embedding service is not available",
                        "/api/batch/embeddings/health")
                );
            }

        } catch (Exception e) {
            log.error("❌ Failed to check embedding service health", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("HEALTH_CHECK_ERROR",
                    "Failed to check service health: " + e.getMessage(),
                    "/api/batch/embeddings/health")
            );
        }
    }
}
