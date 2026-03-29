package com.mohe.spring.service;

import com.mohe.spring.dto.embedding.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 키워드 벡터화 서비스
 * EmbeddingClient(OpenAI)를 통해 키워드를 1536차원 벡터로 변환
 */
@Slf4j
@Service
public class KeywordEmbeddingService {

    private static final int VECTOR_DIMENSIONS = 1536;
    private final EmbeddingClient embeddingClient;

    public KeywordEmbeddingService(EmbeddingClient embeddingClient) {
        this.embeddingClient = embeddingClient;
    }

    public float[] vectorizeKeywords(String[] keywords) {
        if (keywords == null || keywords.length == 0) {
            log.warn("Keywords array is null or empty");
            return new float[VECTOR_DIMENSIONS];
        }

        String combinedKeywords = String.join(" ", keywords);
        if (combinedKeywords.trim().isEmpty()) {
            log.warn("Combined keywords string is empty");
            return new float[VECTOR_DIMENSIONS];
        }

        try {
            EmbeddingResponse response = embeddingClient.getEmbeddings(List.of(combinedKeywords));
            if (response.hasValidEmbeddings()) {
                List<float[]> vectors = response.getEmbeddingsAsFloatArrays();
                if (!vectors.isEmpty()) {
                    return vectors.get(0);
                }
            }
            log.warn("No valid embeddings returned for keywords");
            return new float[VECTOR_DIMENSIONS];
        } catch (Exception e) {
            log.error("Failed to vectorize keywords: {}", e.getMessage());
            return new float[VECTOR_DIMENSIONS];
        }
    }
}
