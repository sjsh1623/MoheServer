package com.mohe.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.config.LlmProperties;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class KeywordEmbeddingService implements LlmService {
    private final LlmProperties llmProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public KeywordEmbeddingService(
        LlmProperties llmProperties,
        WebClient.Builder webClientBuilder,
        ObjectMapper objectMapper
    ) {
        this.llmProperties = llmProperties;

        // HttpClient 설정: Ollama는 응답 시간이 매우 길 수 있음 (특히 큰 모델)
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(10))  // 응답 타임아웃 10분
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)  // 연결 타임아웃 30초
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS))  // 읽기 타임아웃 10분
                        .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));  // 쓰기 타임아웃 2분

        this.webClient = webClientBuilder
                .baseUrl(llmProperties.getOllama().getBaseUrl())
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
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

    public String[] generateKeywords(String aiSummary, String category, boolean petFriendly) {
        // Input validation
        if (aiSummary == null || aiSummary.trim().isEmpty()) {
            System.err.println("Failed to generate keywords: AI summary is empty");
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        }

        String prompt = String.format(
            "다음 장소의 특징을 나타내는 한글 단어 키워드를 정확히 6개 생성해주세요.\n\n" +
            "AI 요약: %s\n" +
            "카테고리: %s\n" +
            "반려동물 동반 가능: %s\n\n" +
            "📌 중요 규칙:\n" +
            "1. 반드시 한글로만 작성하세요 (영어, 숫자, 특수문자 금지)\n" +
            "2. 각 키워드는 단일 단어여야 합니다 (문장이나 구절 금지)\n" +
            "3. 쉼표(,)로 구분하여 정확히 6개만 출력하세요\n" +
            "4. 중괄호, 대괄호, 따옴표 등 특수문자 없이 키워드만 출력하세요\n" +
            "5. 이모지를 절대 사용하지 마세요\n" +
            "6. 띄어쓰기가 포함된 키워드는 붙여서 작성하세요\n\n" +
            "올바른 예시: 카페,조용함,힐링,와이파이,데이트,감성적\n" +
            "잘못된 예시: 카페 (space),조용한 분위기,작업하기 좋은,wifi,coffee,데이트 코스\n\n" +
            "위 규칙을 반드시 지켜서 키워드만 출력하세요:",
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

            if (response == null || response.trim().isEmpty()) {
                System.err.println("Failed to generate keywords: Empty response from Ollama");
                return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode responseNode = jsonNode.get("response");

            if (responseNode == null || responseNode.isNull()) {
                System.err.println("Failed to generate keywords: No 'response' field in Ollama output. Raw response: " + response);
                return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
            }

            String keywordsText = responseNode.asText().trim();

            // Remove all emojis first (Unicode emoji ranges)
            keywordsText = keywordsText.replaceAll("[\\p{So}\\p{Cn}]", "");

            // Remove common wrapper patterns: {...}, [...], "...", quotes, etc.
            keywordsText = keywordsText.replaceAll("^[\\{\\[\"']+", "")  // Remove leading {, [, ", '
                                       .replaceAll("[\\}\\]\"']+$", "")  // Remove trailing }, ], ", '
                                       .trim();

            // Parse keywords (split by comma, newline, or semicolon)
            String[] keywords = keywordsText.split("[,;\\n]+");

            // Clean and ensure exactly 6 keywords
            String[] cleanedKeywords = new String[6];
            int count = 0;
            for (String keyword : keywords) {
                if (count >= 6) break;
                // Remove all special characters and whitespace around keyword
                String cleaned = keyword.trim()
                                       .replaceAll("^[\\{\\[\"'\\s]+", "")  // Remove leading special chars
                                       .replaceAll("[\\}\\]\"'\\s]+$", "")  // Remove trailing special chars
                                       .trim();

                // Only add non-empty keywords
                if (!cleaned.isEmpty() && cleaned.length() > 0) {
                    cleanedKeywords[count++] = cleaned;
                }
            }

            // Fill remaining with default keywords if less than 6
            while (count < 6) {
                cleanedKeywords[count++] = "키워드" + count;
            }

            return cleanedKeywords;
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            System.err.println("Failed to generate keywords: Cannot connect to Ollama - " + e.getMessage());
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.err.println("Failed to generate keywords: Ollama returned error " + e.getStatusCode() + " - " + e.getMessage());
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Failed to generate keywords: Invalid JSON response from Ollama - " + e.getMessage());
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        } catch (Exception e) {
            System.err.println("Failed to generate keywords: Unexpected error - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        }
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
