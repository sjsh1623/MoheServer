package com.mohe.spring.service;

import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceDescriptionEmbeddingRepository;
import com.mohe.spring.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.util.*;

/**
 * 프롬프트 기반 벡터 유사도 검색 서비스
 * 사용자 문장 → 임베딩 → place_description_embeddings와 cosine 유사도 비교
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class PromptSearchService {

    private final EmbeddingClient embeddingClient;
    private final PlaceDescriptionEmbeddingRepository descEmbeddingRepository;
    private final PlaceService placeService;

    /**
     * 프롬프트 기반 장소 검색
     *
     * @param query 사용자 프롬프트 (예: "비 오는 날 조용히 책 읽으면서 커피 마실 수 있는 곳")
     * @param latitude 사용자 위도
     * @param longitude 사용자 경도
     * @param distance 검색 반경 (km)
     * @param limit 결과 수
     * @return 유사도 순 장소 리스트
     */
    public List<Map<String, Object>> searchByPrompt(
            String query, Double latitude, Double longitude, double distance, int limit) {

        // 1. 사용자 프롬프트 임베딩
        float[] queryEmbedding;
        try {
            queryEmbedding = embeddingClient.getEmbedding(query);
        } catch (Exception e) {
            log.error("Failed to embed prompt: {}", e.getMessage());
            return List.of();
        }

        // 2. pgvector 포맷으로 변환
        String vectorString = toVectorString(queryEmbedding);

        // 3. 벡터 유사도 검색 (거리 필터 포함)
        List<Object[]> rows;
        if (latitude != null && longitude != null) {
            rows = descEmbeddingRepository.findSimilarPlacesByPrompt(
                    vectorString, latitude, longitude, distance, limit);
        } else {
            rows = descEmbeddingRepository.findSimilarPlaces(vectorString, limit);
        }

        // 4. 결과 변환
        List<Map<String, Object>> results = new ArrayList<>();
        for (Object[] row : rows) {
            Map<String, Object> place = new LinkedHashMap<>();
            place.put("id", ((Number) row[0]).longValue());
            place.put("name", row[1]);
            place.put("latitude", row[2] != null ? ((Number) row[2]).doubleValue() : null);
            place.put("longitude", row[3] != null ? ((Number) row[3]).doubleValue() : null);
            place.put("roadAddress", row[4]);
            place.put("rating", row[5] != null ? ((Number) row[5]).doubleValue() : null);

            // similarity는 마지막 컬럼
            Object simObj = row[row.length - 1];
            double similarity = simObj != null ? ((Number) simObj).doubleValue() : 0;
            place.put("similarity", Math.round(similarity * 1000) / 1000.0);

            // 이미지
            Long placeId = ((Number) row[0]).longValue();
            List<String> imageUrls = placeService.getImageUrls(placeId);
            place.put("imageUrl", imageUrls.isEmpty() ? null : imageUrls.get(0));

            results.add(place);
        }

        log.info("Prompt search: '{}' → {} results", query, results.size());
        return results;
    }

    private String toVectorString(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}
