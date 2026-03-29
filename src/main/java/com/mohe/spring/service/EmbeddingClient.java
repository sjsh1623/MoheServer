package com.mohe.spring.service;

import com.mohe.spring.dto.embedding.EmbeddingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * OpenAI Embedding API 클라이언트
 * text-embedding-3-small 모델 사용 (1536 차원)
 */
@Slf4j
@Component
public class EmbeddingClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;

    private static final String OPENAI_EMBEDDINGS_URL = "https://api.openai.com/v1/embeddings";

    public EmbeddingClient(
        RestTemplate restTemplate,
        ObjectMapper objectMapper,
        @Value("${openai.api-key:}") String apiKey,
        @Value("${embedding.model:text-embedding-3-small}") String model
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
        this.model = model;
    }

    public EmbeddingResponse getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            return new EmbeddingResponse(List.of());
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[EmbeddingClient] OpenAI API key not configured");
            throw new EmbeddingServiceException("OpenAI API key not configured");
        }

        log.info("[EmbeddingClient] 📤 Requesting {} embeddings via OpenAI {}", texts.size(), model);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                "model", model,
                "input", texts
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                OPENAI_EMBEDDINGS_URL,
                HttpMethod.POST,
                request,
                String.class
            );

            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                JsonNode root = objectMapper.readTree(responseEntity.getBody());
                JsonNode dataArray = root.get("data");

                List<List<Double>> embeddings = new ArrayList<>();
                for (JsonNode item : dataArray) {
                    JsonNode embeddingNode = item.get("embedding");
                    List<Double> vector = new ArrayList<>();
                    for (JsonNode val : embeddingNode) {
                        vector.add(val.asDouble());
                    }
                    embeddings.add(vector);
                }

                log.info("[EmbeddingClient] ✅ Received {} embeddings ({}D)", embeddings.size(),
                        embeddings.isEmpty() ? 0 : embeddings.get(0).size());
                return new EmbeddingResponse(embeddings);
            } else {
                throw new EmbeddingServiceException("OpenAI returned: " + responseEntity.getStatusCode());
            }

        } catch (EmbeddingServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("[EmbeddingClient] ❌ OpenAI embedding failed: {}", e.getMessage());
            throw new EmbeddingServiceException("OpenAI embedding failed: " + e.getMessage(), e);
        }
    }

    public float[] getEmbedding(String text) {
        EmbeddingResponse response = getEmbeddings(List.of(text));
        if (!response.hasValidEmbeddings()) {
            throw new EmbeddingServiceException("No embeddings returned for text: " + text);
        }
        List<float[]> embeddings = response.getEmbeddingsAsFloatArrays();
        if (embeddings.isEmpty()) {
            throw new EmbeddingServiceException("Empty embeddings returned");
        }
        return embeddings.get(0);
    }

    public boolean isServiceAvailable() {
        return apiKey != null && !apiKey.isBlank();
    }

    public static class EmbeddingServiceException extends RuntimeException {
        public EmbeddingServiceException(String message) { super(message); }
        public EmbeddingServiceException(String message, Throwable cause) { super(message, cause); }
    }
}
