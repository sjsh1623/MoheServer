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

    public String generateMoheDescription(String aiSummary, String category, boolean petFriendly, List<String> reviews) {
        // Input validation
        if (aiSummary == null || aiSummary.trim().isEmpty()) {
            System.err.println("Failed to generate Mohe description: AI summary is empty");
            return "AI 설명을 생성할 수 없습니다.";
        }

        // Prepare review summary
        String reviewSummary = "";
        if (reviews != null && !reviews.isEmpty()) {
            // Limit to first 5 reviews to avoid token overflow
            int reviewCount = Math.min(reviews.size(), 5);
            reviewSummary = String.join("\n", reviews.subList(0, reviewCount));
        } else {
            reviewSummary = "리뷰 정보 없음";
        }

        String prompt;

        if (petFriendly) {
            prompt = String.format(
                "당신은 여행지와 공간을 자연스럽고 친근하게 소개하는 작가입니다.\n" +
                    "문장은 정보 중심으로 시작하고, 마지막은 부드럽게 감성을 담아 마무리해주세요.\n" +
                    "감정을 직접 표현하지 말고, 읽는 사람이 느낄 수 있도록 써주세요.\n" +
                    "자연스러운 구어체(~좋아요, ~좋을 것 같아요, ~느껴져요)는 가능하지만, '~있더군요', '~하더라고요' 같은 관찰체 표현은 금지입니다.\n\n" +

                    "AI 요약: %s\n" +
                    "카테고리: %s\n" +
                    "리뷰 요약: %s\n\n" +

                    "📌 작성 가이드:\n" +
                    "1. 160~230자 이내의 한 문단으로 작성하세요.\n" +
                    "2. 첫 문장은 장소의 위치나 특징 등 **정보 중심**으로 시작하세요.\n" +
                    "3. 사람들의 후기를 참고하되 객관적인 사실만 담으세요.\n" +
                    "4. '~좋아요', '~좋을 것 같아요', '~느껴져요'처럼 자연스러운 어미를 사용하세요.\n" +
                    "5. 문맥상 자연스럽게 반려동물 동반 가능 여부를 포함하세요.\n" +
                    "6. **강조할 주요 명사(예: 인기 메뉴, 시설, 특징 등)는 '**'로 감싸 Bold 처리하세요. (최대 3개)**\n" +
                    "7. 장소명은 Bold 처리하지 마세요.\n" +
                    "8. 이모지는 절대 사용하지 마세요.\n" +
                    "9. 장소 설명문만 출력하세요.\n\n" +

                    "예시:\n" +
                    "서촌 골목 안쪽에 자리한 카페예요. 사람들은 **라떼**와 **당근케이크**의 조화가 좋다고 말해요. 반려동물과 함께 들러도 조용히 머물기 좋은 곳이에요.\n\n" +
                    "예시 2:\n" +
                    "한적한 거리에 자리한 브런치 카페예요. 사람들은 **브런치 세트**와 **크루아상**이 맛있다고 해요. 반려동물과 함께 여유로운 시간을 보내기 좋은 곳이에요.",
                aiSummary,
                category,
                reviewSummary
            );
        } else {
            prompt = String.format(
                "당신은 여행지와 공간을 자연스럽고 친근하게 소개하는 작가입니다.\n" +
                    "문장은 정보 중심으로 시작하고, 마지막은 부드럽게 감성을 담아 마무리해주세요.\n" +
                    "감정을 직접 표현하지 말고, 읽는 사람이 느낄 수 있도록 써주세요.\n" +
                    "자연스러운 구어체(~좋아요, ~좋을 것 같아요, ~느껴져요)는 가능하지만, '~있더군요', '~하더라고요' 같은 관찰체 표현은 금지입니다.\n\n" +

                    "AI 요약: %s\n" +
                    "카테고리: %s\n" +
                    "리뷰 요약: %s\n\n" +

                    "📌 작성 가이드:\n" +
                    "1. 160~230자 이내의 한 문단으로 작성하세요.\n" +
                    "2. 첫 문장은 장소의 위치나 특징 등 **정보 중심**으로 시작하세요.\n" +
                    "3. 사람들의 후기를 참고하되 객관적인 사실만 담으세요.\n" +
                    "4. '~좋아요', '~좋을 것 같아요', '~느껴져요'처럼 자연스러운 어미를 사용하세요.\n" +
                    "5. 문장 내에 반려동물 관련 표현은 포함하지 마세요.\n" +
                    "6. **강조할 주요 명사(예: 인기 메뉴, 시설, 특징 등)는 '**'로 감싸 Bold 처리하세요. (최대 3개)**\n" +
                    "7. 장소명은 Bold 처리하지 마세요.\n" +
                    "8. 이모지는 절대 사용하지 마세요.\n" +
                    "9. 장소 설명문만 출력하세요.\n\n" +

                    "예시:\n" +
                    "서촌 중심부에 자리한 카페예요. 사람들은 **아메리카노**와 **오트라떼**의 균형 잡힌 맛이 좋다고 말해요. 조용한 분위기 속에서 잠시 쉬어가기 좋은 곳이에요.\n\n" +
                    "예시 2:\n" +
                    "바다 전망이 보이는 레스토랑이에요. 사람들은 **스테이크**와 **와인 리스트** 구성이 알차다고 말해요. 식사 후 창가에 앉아 여유를 즐기기 좋아요.",
                aiSummary,
                category,
                reviewSummary
            );
        }

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
