package com.mohe.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.config.LlmProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OllamaService implements LlmService {
    private final LlmProperties llmProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OllamaService(LlmProperties llmProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
        this.llmProperties = llmProperties;
        this.webClient = webClientBuilder.baseUrl(llmProperties.getOllama().getBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    @Override
    public OllamaRecommendationResponse generatePlaceRecommendations(String prompt, List<String> availablePlaces) {
        return new OllamaRecommendationResponse();
    }

    @Override
    public OllamaRecommendationResponse generatePlaceRecommendations(String userLocation, String weatherCondition, String timeOfDay, String userMbti, List<String> availablePlaces) {
        return new OllamaRecommendationResponse();
    }

    public String generateMoheDescription(String aiSummary, String category, boolean petFriendly) {
        String prompt = String.format(
            "다음 장소에 대한 친근한 문체의 설명을 120-150자로 작성해주세요.\n\n" +
            "AI 요약: %s\n" +
            "카테고리: %s\n" +
            "반려동물 동반 가능: %s\n\n" +
            "친근하고 매력적인 문체로 작성하되, 정확히 120-150자 사이로 작성해주세요. 설명문만 출력하세요.",
            aiSummary,
            category,
            petFriendly ? "가능" : "불가능"
        );

        Map<String, Object> request = new HashMap<>();
        request.put("model", llmProperties.getOllama().getModel());
        request.put("prompt", prompt);
        request.put("stream", false);

        try {
            String response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            String description = jsonNode.get("response").asText().trim();

            // Limit to 150 characters if too long
            if (description.length() > 150) {
                description = description.substring(0, 150);
            }

            return description;
        } catch (Exception e) {
            System.err.println("Failed to generate Mohe description: " + e.getMessage());
            return "AI 설명을 생성할 수 없습니다.";
        }
    }

    public String[] generateKeywords(String aiSummary, String category, boolean petFriendly) {
        String prompt = String.format(
            "다음 장소에 대한 키워드를 정확히 6개 생성해주세요.\n\n" +
            "AI 요약: %s\n" +
            "카테고리: %s\n" +
            "반려동물 동반 가능: %s\n\n" +
            "키워드는 쉼표(,)로 구분하여 정확히 6개만 출력하세요. 다른 설명 없이 키워드만 출력하세요.\n" +
            "예시: 카페,조용한,작업하기좋은,와이파이,커피맛집,힐링",
            aiSummary,
            category,
            petFriendly ? "가능" : "불가능"
        );

        Map<String, Object> request = new HashMap<>();
        request.put("model", llmProperties.getOllama().getModel());
        request.put("prompt", prompt);
        request.put("stream", false);

        try {
            String response = webClient.post()
                .uri("/api/generate")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            String keywordsText = jsonNode.get("response").asText().trim();

            // Parse keywords
            String[] keywords = keywordsText.split("[,\\n]");

            // Clean and ensure exactly 6 keywords
            String[] cleanedKeywords = new String[6];
            int count = 0;
            for (String keyword : keywords) {
                if (count >= 6) break;
                String cleaned = keyword.trim();
                if (!cleaned.isEmpty()) {
                    cleanedKeywords[count++] = cleaned;
                }
            }

            // Fill remaining with default keywords if less than 6
            while (count < 6) {
                cleanedKeywords[count++] = "키워드" + count;
            }

            return cleanedKeywords;
        } catch (Exception e) {
            System.err.println("Failed to generate keywords: " + e.getMessage());
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        }
    }

    public float[] vectorizeKeywords(String[] keywords) {
        String combinedKeywords = String.join(" ", keywords);

        Map<String, Object> request = new HashMap<>();
        request.put("model", "mxbai-embed-large");
        request.put("prompt", combinedKeywords);

        try {
            String response = webClient.post()
                .uri("/api/embeddings")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .block();

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode embeddingNode = jsonNode.get("embedding");

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            return vector;
        } catch (Exception e) {
            System.err.println("Failed to vectorize keywords: " + e.getMessage());
            // Return a default vector of size 1024 (mxbai-embed-large dimension)
            return new float[1024];
        }
    }
}
