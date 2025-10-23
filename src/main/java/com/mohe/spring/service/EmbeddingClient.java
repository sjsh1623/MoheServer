package com.mohe.spring.service;

import com.mohe.spring.dto.embedding.EmbeddingRequest;
import com.mohe.spring.dto.embedding.EmbeddingResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Client for communicating with Kanana embedding service
 * Handles HTTP communication with the local embedding server at http://localhost:8000/embed
 */
@Slf4j
@Component
public class EmbeddingClient {

    private final RestTemplate restTemplate;
    private final String embeddingServiceUrl;

    public EmbeddingClient(
        RestTemplate restTemplate,
        @Value("${embedding.service.url:http://localhost:8000}") String embeddingServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.embeddingServiceUrl = embeddingServiceUrl;
    }

    /**
     * Get embeddings for a list of text strings
     *
     * @param texts List of texts to embed (up to 9 texts recommended)
     * @return EmbeddingResponse containing list of embedding vectors
     * @throws EmbeddingServiceException if the embedding service fails
     */
    public EmbeddingResponse getEmbeddings(List<String> texts) {
        if (texts == null || texts.isEmpty()) {
            log.warn("Attempted to get embeddings for empty text list");
            return new EmbeddingResponse(List.of());
        }

        String url = embeddingServiceUrl + "/embed";
        log.info("[EmbeddingClient] 📤 Requesting embeddings for {} texts from {}", texts.size(), url);
        log.info("[EmbeddingClient] 📝 Texts to embed: {}", texts);

        try {
            // Embedding server expects array directly, not wrapped in object
            // Correct format: ["text1", "text2"]
            // NOT: {"texts": ["text1", "text2"]}

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            // Send texts directly as JSON array
            HttpEntity<List<String>> requestEntity = new HttpEntity<>(texts, headers);

            log.info("[EmbeddingClient] 🚀 Sending request to embedding server...");

            // Make HTTP POST request
            ResponseEntity<EmbeddingResponse> responseEntity = restTemplate.exchange(
                url,
                HttpMethod.POST,
                requestEntity,
                EmbeddingResponse.class
            );

            // Check response
            if (responseEntity.getStatusCode() == HttpStatus.OK && responseEntity.getBody() != null) {
                EmbeddingResponse response = responseEntity.getBody();
                log.info("[EmbeddingClient] ✅ Successfully received {} embeddings", response.getEmbeddingCount());
                return response;
            } else {
                log.error("[EmbeddingClient] ❌ Unexpected response status: {}", responseEntity.getStatusCode());
                throw new EmbeddingServiceException(
                    "Unexpected response status: " + responseEntity.getStatusCode()
                );
            }

        } catch (HttpClientErrorException e) {
            log.error("Client error when calling embedding service: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingServiceException(
                "Client error: " + e.getStatusCode() + " - " + e.getMessage(), e
            );

        } catch (HttpServerErrorException e) {
            log.error("Server error when calling embedding service: {} - {}",
                e.getStatusCode(), e.getResponseBodyAsString());
            throw new EmbeddingServiceException(
                "Server error: " + e.getStatusCode() + " - " + e.getMessage(), e
            );

        } catch (ResourceAccessException e) {
            log.error("Failed to connect to embedding service at {}: {}", url, e.getMessage());
            throw new EmbeddingServiceException(
                "Failed to connect to embedding service: " + e.getMessage(), e
            );

        } catch (Exception e) {
            log.error("Unexpected error when calling embedding service", e);
            throw new EmbeddingServiceException(
                "Unexpected error: " + e.getMessage(), e
            );
        }
    }

    /**
     * Get embedding for a single text string
     *
     * @param text Text to embed
     * @return Embedding vector as float array
     * @throws EmbeddingServiceException if the embedding service fails
     */
    public float[] getEmbedding(String text) {
        EmbeddingResponse response = getEmbeddings(List.of(text));

        if (!response.hasValidEmbeddings()) {
            throw new EmbeddingServiceException("No embeddings returned for text: " + text);
        }

        List<float[]> embeddings = response.getEmbeddingsAsFloatArrays();
        if (embeddings.isEmpty()) {
            throw new EmbeddingServiceException("Empty embeddings returned for text: " + text);
        }

        return embeddings.get(0);
    }

    /**
     * Check if the embedding service is available
     *
     * @return true if the service is reachable, false otherwise
     */
    public boolean isServiceAvailable() {
        try {
            String healthUrl = embeddingServiceUrl + "/health";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            return response.getStatusCode() == HttpStatus.OK;
        } catch (Exception e) {
            log.debug("Embedding service is not available: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Custom exception for embedding service errors
     */
    public static class EmbeddingServiceException extends RuntimeException {
        public EmbeddingServiceException(String message) {
            super(message);
        }

        public EmbeddingServiceException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
