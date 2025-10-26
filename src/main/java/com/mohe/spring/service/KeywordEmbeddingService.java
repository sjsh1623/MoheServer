package com.mohe.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Service
public class KeywordEmbeddingService {
    private final ObjectMapper objectMapper;

    public KeywordEmbeddingService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public float[] vectorizeKeywords(String[] keywords) {
        // Input validation
        if (keywords == null || keywords.length == 0) {
            System.err.println("Failed to vectorize keywords: Keywords array is null or empty");
            return new float[1792];  // kanana-nano-2.1b-embedding vector size
        }

        String combinedKeywords = String.join(" ", keywords);

        if (combinedKeywords.trim().isEmpty()) {
            System.err.println("Failed to vectorize keywords: Combined keywords string is empty");
            return new float[1792];
        }

        // Create request as array of strings for /embed endpoint
        List<String> texts = List.of(combinedKeywords);

        try {
            // Get embedding service URL from environment
            String embeddingServiceUrl = System.getenv().getOrDefault(
                "EMBEDDING_SERVICE_URL",
                "http://localhost:2000"
            );

            String response = WebClient.builder()
                .baseUrl(embeddingServiceUrl)
                .build()
                .post()
                .uri("/embed")
                .bodyValue(texts)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            if (response == null || response.trim().isEmpty()) {
                System.err.println("Failed to vectorize keywords: Empty response from embedding service");
                return new float[1792];
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode embeddingsNode = jsonNode.get("embeddings");

            if (embeddingsNode == null || !embeddingsNode.isArray() || embeddingsNode.size() == 0) {
                System.err.println("Failed to vectorize keywords: No valid 'embeddings' field in response. Raw response: " + response);
                return new float[1792];
            }

            JsonNode embeddingNode = embeddingsNode.get(0);

            if (embeddingNode == null || embeddingNode.isNull() || !embeddingNode.isArray()) {
                System.err.println("Failed to vectorize keywords: No valid embedding vector in response data");
                return new float[1792];
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            return vector;
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            System.err.println("Failed to vectorize keywords: Cannot connect to embedding service - " + e.getMessage());
            return new float[1792];
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.err.println("Failed to vectorize keywords: Embedding service returned error " + e.getStatusCode() + " - " + e.getMessage());
            return new float[1792];
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Failed to vectorize keywords: Invalid JSON response from embedding service - " + e.getMessage());
            return new float[1792];
        } catch (Exception e) {
            System.err.println("Failed to vectorize keywords: Unexpected error - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new float[1792];
        }
    }
}
