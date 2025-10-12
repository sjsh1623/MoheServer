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
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class OllamaService implements LlmService {
    private final LlmProperties llmProperties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public OllamaService(LlmProperties llmProperties, WebClient.Builder webClientBuilder, ObjectMapper objectMapper) {
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

    public String generateMoheDescription(String aiSummary, String category, boolean petFriendly) {
        // Input validation
        if (aiSummary == null || aiSummary.trim().isEmpty()) {
            System.err.println("Failed to generate Mohe description: AI summary is empty");
            return "AI 설명을 생성할 수 없습니다.";
        }

        String prompt = String.format(
            "다음 장소에 대한 친근한 문체의 설명을 작성해주세요.\n\n" +
            "AI 요약: %s\n" +
            "카테고리: %s\n" +
            "반려동물 동반 가능: %s\n\n" +
            "중요한 규칙:\n" +
            "1. 100-130자 사이로 작성하세요 (너무 길면 문장이 끊깁니다)\n" +
            "2. 반드시 완전한 문장으로 끝내세요 (마침표, 느낌표, 물음표로 끝나야 함)\n" +
            "3. 문장이 중간에 끊기지 않도록 주의하세요\n" +
            "4. 친근하고 매력적인 문체를 사용하세요\n" +
            "5. 다른 설명 없이 장소 설명문만 출력하세요\n" +
            "6. 이모지를 절대 사용하지 마세요 (❌, ✨, 💕, 🌟 등 모든 이모지 금지)\n\n" +
            "예시: 서촌의 숨은 보석 같은 카페입니다. 조용한 분위기와 맛있는 커피로 힐링하기 좋아요. 반려동물과 함께 방문할 수 있어서 더욱 특별합니다.",
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
                System.err.println("Failed to generate Mohe description: Empty response from Ollama");
                return "AI 설명을 생성할 수 없습니다.";
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode responseNode = jsonNode.get("response");

            if (responseNode == null || responseNode.isNull()) {
                System.err.println("Failed to generate Mohe description: No 'response' field in Ollama output. Raw response: " + response);
                return "AI 설명을 생성할 수 없습니다.";
            }

            String description = responseNode.asText().trim();

            // Remove all emojis (Unicode emoji ranges)
            description = description.replaceAll("[\\p{So}\\p{Cn}]", "").trim();

            if (description.isEmpty()) {
                System.err.println("Failed to generate Mohe description: Empty description from Ollama");
                return "AI 설명을 생성할 수 없습니다.";
            }

            // Validate sentence completion - must end with proper punctuation
            char lastChar = description.charAt(description.length() - 1);
            if (lastChar != '.' && lastChar != '!' && lastChar != '?' && lastChar != '요' && lastChar != '다' && lastChar != '니') {
                System.err.println("Failed to generate Mohe description: Incomplete sentence detected - '" + description + "'");

                // Try to find the last complete sentence
                int lastPeriod = Math.max(description.lastIndexOf('.'), description.lastIndexOf('!'));
                lastPeriod = Math.max(lastPeriod, description.lastIndexOf('?'));

                if (lastPeriod > 50) {  // At least 50 characters before the last sentence
                    description = description.substring(0, lastPeriod + 1).trim();
                    System.err.println("Recovered by truncating to last complete sentence: '" + description + "'");
                } else {
                    // Cannot recover - return error
                    return "AI 설명을 생성할 수 없습니다.";
                }
            }

            // Limit to 150 characters if too long, but preserve sentence completeness
            // IMPORTANT: Don't force-truncate if no good sentence boundary found
            // The user prefers longer but complete sentences over 150-char limit
            if (description.length() > 150) {
                // Find the last sentence boundary before 150 chars
                String truncated = description.substring(0, 150);
                int lastPeriod = Math.max(truncated.lastIndexOf('.'), truncated.lastIndexOf('!'));
                lastPeriod = Math.max(lastPeriod, truncated.lastIndexOf('?'));

                if (lastPeriod > 50) {
                    // Found a good sentence boundary - truncate there
                    description = truncated.substring(0, lastPeriod + 1).trim();
                }
                // else: No good sentence boundary - keep the full description even if > 150 chars
            }

            return description;
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            System.err.println("Failed to generate Mohe description: Cannot connect to Ollama - " + e.getMessage());
            return "AI 설명을 생성할 수 없습니다.";
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.err.println("Failed to generate Mohe description: Ollama returned error " + e.getStatusCode() + " - " + e.getMessage());
            return "AI 설명을 생성할 수 없습니다.";
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Failed to generate Mohe description: Invalid JSON response from Ollama - " + e.getMessage());
            return "AI 설명을 생성할 수 없습니다.";
        } catch (Exception e) {
            System.err.println("Failed to generate Mohe description: Unexpected error - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return "AI 설명을 생성할 수 없습니다.";
        }
    }

    public String[] generateKeywords(String aiSummary, String category, boolean petFriendly) {
        // Input validation
        if (aiSummary == null || aiSummary.trim().isEmpty()) {
            System.err.println("Failed to generate keywords: AI summary is empty");
            return new String[]{"키워드1", "키워드2", "키워드3", "키워드4", "키워드5", "키워드6"};
        }

        String prompt = String.format(
            "다음 장소에 대한 키워드를 정확히 6개 생성해주세요.\n\n" +
            "AI 요약: %s\n" +
            "카테고리: %s\n" +
            "반려동물 동반 가능: %s\n\n" +
            "키워드는 쉼표(,)로 구분하여 정확히 6개만 출력하세요.\n" +
            "중괄호, 대괄호, 따옴표 등 특수문자 없이 키워드만 출력하세요.\n" +
            "이모지를 절대 사용하지 마세요 (❌, ✨, 💕, 🌟 등 모든 이모지 금지).\n" +
            "예시: 카페,조용한,작업하기좋은,와이파이,커피맛집,힐링\n" +
            "절대 {키워드1,키워드2} 형식으로 출력하지 마세요.",
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
            return new float[1024];
        }

        String combinedKeywords = String.join(" ", keywords);

        if (combinedKeywords.trim().isEmpty()) {
            System.err.println("Failed to vectorize keywords: Combined keywords string is empty");
            return new float[1024];
        }

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

            if (response == null || response.trim().isEmpty()) {
                System.err.println("Failed to vectorize keywords: Empty response from Ollama");
                return new float[1024];
            }

            JsonNode jsonNode = objectMapper.readTree(response);
            JsonNode embeddingNode = jsonNode.get("embedding");

            if (embeddingNode == null || embeddingNode.isNull() || !embeddingNode.isArray()) {
                System.err.println("Failed to vectorize keywords: No valid 'embedding' field in Ollama output. Raw response: " + response);
                return new float[1024];
            }

            float[] vector = new float[embeddingNode.size()];
            for (int i = 0; i < embeddingNode.size(); i++) {
                vector[i] = (float) embeddingNode.get(i).asDouble();
            }

            return vector;
        } catch (org.springframework.web.reactive.function.client.WebClientRequestException e) {
            System.err.println("Failed to vectorize keywords: Cannot connect to Ollama - " + e.getMessage());
            return new float[1024];
        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            System.err.println("Failed to vectorize keywords: Ollama returned error " + e.getStatusCode() + " - " + e.getMessage());
            return new float[1024];
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            System.err.println("Failed to vectorize keywords: Invalid JSON response from Ollama - " + e.getMessage());
            return new float[1024];
        } catch (Exception e) {
            System.err.println("Failed to vectorize keywords: Unexpected error - " + e.getClass().getName() + ": " + e.getMessage());
            e.printStackTrace();
            return new float[1024];
        }
    }
}
