package com.mohe.spring.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Handles GPT-4.1-mini calls for batch description generation using prompt caching.
 */
@Service
public class OpenAiDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiDescriptionService.class);

    private static final String MODEL = "gpt-4.1-mini";
    private static final String PROMPT_CACHE_KEY = "mohe.batch.description.v3";
    private static final String PROMPT_TEMPLATE = """
        당신은 여행지와 공간을 자연스럽고 친근하게 소개하는 작가이자 콘텐츠 생성 AI입니다.
        입력은 JSON 형식으로 제공됩니다:

        {
          "ai_summary": "AI가 요약한 장소 설명",
          "review": "리뷰 요약 혹은 원문 리뷰 10개 이내",
          "description": "일반적인 장소 설명",
          "category": "장소의 카테고리",
          "pet_friendly": true
        }

        출력은 JSON 형식으로 다음 구조로 반환합니다:
        {
          "description": "정리된 장소 설명 (160~230자, 한 문단)",
          "keywords": ["기분_1", "기분_2", "날씨_1", "날씨_2", "분위기_1", "분위기_2", "명사_1", "명사_2", "명사_3"]
        }

        📌 작성 규칙:
        1. 160~230자 이내의 한 문단으로 작성하세요.
        2. 첫 문장은 장소의 위치나 특징 등 정보 중심으로 시작하세요.
        3. 사람들의 후기를 참고하되 객관적인 사실만 담으세요.
        4. '~좋아요', '~좋을 것 같아요', '~느껴져요'처럼 자연스러운 어미를 사용하세요.
        5. 감정을 직접 표현하지 말고, 읽는 사람이 느낄 수 있도록 써주세요.
        6. 강조할 주요 명사는 '##'로 감싸주세요. (최소 1개, 최대 3개)
        7. 장소명은 Bold 처리하지 마세요.
        8. 문맥상 자연스럽게 반려동물 관련 내용을 포함하세요 (pet_friendly가 true인 경우).
        9. 이모지나 불필요한 기호는 사용하지 마세요.
        10. 키워드는 총 9개를 추출하며 다음 구성으로 정확히 생성합니다:
            - 기분 관련 단어 2개 (예: 편안함, 설렘)
            - 날씨 관련 단어 2개 (예: 맑음, 흐림)
            - 분위기 관련 단어 2개 (예: 조용함, 활기참)
            - 주요 명사 3개 (예: 카페, 브런치, 바다)
        11. 출력은 반드시 JSON 형태로만 반환하며, 추가 텍스트는 포함하지 않습니다.

        📝 예시 1:
        입력:
        {
          "ai_summary": "바다 근처의 조용한 브런치 카페",
          "review": "라떼가 부드럽고 분위기가 좋다는 평이 많아요.",
          "description": "제주도 서쪽 해안에 위치한 감성 카페",
          "category": "카페",
          "pet_friendly": true
        }

        출력:
        {
          "description": "제주도 서쪽 해안에 위치한 ##감성 카페##로, 바다 전망과 함께 조용한 시간을 보내기 좋아요. ##라떼##가 부드럽고 분위기가 좋다는 평이 많으며, 반려동물과 함께 브런치를 즐기기에도 적합합니다. 바다 근처에서 여유로운 카페 타임을 원한다면 방문해보세요.",
          "keywords": ["편안함", "여유로움", "맑음", "해안가", "감성적", "조용함", "카페", "브런치", "바다"]
        }

        📝 예시 2:
        입력:
        {
          "ai_summary": "도심 속 힐링 공간, 조용한 서점 카페",
          "review": "책을 읽으며 여유를 즐길 수 있고, 커피와 디저트가 맛있다는 후기가 많습니다.",
          "description": "강남역 근처 독립서점과 카페가 결합된 복합문화공간",
          "category": "서점,카페",
          "pet_friendly": false
        }

        출력:
        {
          "description": "강남역 근처에 위치한 ##독립서점##과 카페가 결합된 복합문화공간으로, 도심 속에서 조용히 책을 읽으며 힐링하기 좋아요. ##커피##와 ##디저트##가 맛있다는 평이 많으며, 혼자만의 시간이나 독서 모임을 갖기에 적합한 공간입니다.",
          "keywords": ["평온함", "집중", "실내", "조용함", "문화적", "아늑함", "서점", "커피", "독서"]
        }
        """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public OpenAiDescriptionService(
        WebClient.Builder webClientBuilder,
        ObjectMapper objectMapper,
        @Value("${OPENAI_API_KEY:}") String apiKey
    ) {
        this.objectMapper = objectMapper;
        this.apiKey = apiKey != null ? apiKey.trim() : "";

        HttpClient httpClient = HttpClient.create()
            .responseTimeout(Duration.ofMinutes(2));

        this.webClient = webClientBuilder
            .baseUrl("https://api.openai.com/v1")
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader("OpenAI-Beta", "assistants=v2")
            .build();
    }

    public Optional<DescriptionResult> generateDescription(DescriptionPayload payload) {
        if (apiKey.isEmpty()) {
            log.error("OpenAI API key is not configured. Skipping description generation.");
            return Optional.empty();
        }

        // With cache_key, OpenAI automatically manages cache hit/miss
        DescriptionResult result = executeRequest(payload);

        if (result == null) {
            return Optional.empty();
        }

        // Log cache status
        if (result.cachedTokens() > 0) {
            log.debug("OpenAI prompt cache hit: {} tokens reused", result.cachedTokens());
        } else {
            log.debug("OpenAI prompt cache miss: registering prompt for cache_key={}", PROMPT_CACHE_KEY);
        }

        return Optional.of(result);
    }

    private DescriptionResult executeRequest(DescriptionPayload payload) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("messages", buildMessages(payload));
            requestBody.put("response_format", buildResponseFormat());
            requestBody.put("temperature", 0.7);
            requestBody.put("max_completion_tokens", 600);

            String rawResponse = webClient.post()
                .uri("/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block(Duration.ofMinutes(2));

            if (rawResponse == null || rawResponse.isEmpty()) {
                log.error("Received empty response from OpenAI.");
                return null;
            }

            JsonNode root = objectMapper.readTree(rawResponse);

            // Extract cached tokens from usage.prompt_tokens_details.cached_tokens
            int cachedTokens = root.path("usage").path("prompt_tokens_details").path("cached_tokens").asInt(0);

            // Extract content from choices[0].message.content
            String content = root.path("choices").path(0).path("message").path("content").asText(null);
            if (content == null || content.isBlank()) {
                log.error("OpenAI response missing content. Raw response: {}", rawResponse);
                return null;
            }

            JsonNode parsed = objectMapper.readTree(content);
            String description = parsed.path("description").asText(null);
            if (description == null || description.isBlank()) {
                log.error("Parsed OpenAI response missing description. Payload: {}", content);
                return null;
            }

            JsonNode keywordsNode = parsed.path("keywords");
            List<String> keywords = new ArrayList<>();
            if (keywordsNode.isArray()) {
                keywordsNode.forEach(node -> {
                    if (node.isTextual()) {
                        String keyword = node.asText().trim();
                        if (!keyword.isEmpty()) {
                            keywords.add(keyword);
                        }
                    }
                });
            }

            String sanitizedDescription = sanitizeDescription(description);
            if (sanitizedDescription.isEmpty()) {
                log.error("Sanitized description is empty. Original: {}", description);
                return null;
            }

            return new DescriptionResult(sanitizedDescription, Collections.unmodifiableList(keywords), cachedTokens);
        } catch (Exception ex) {
            log.error("Failed to call OpenAI description API: {}", ex.getMessage(), ex);
            return null;
        }
    }

    private List<Map<String, Object>> buildMessages(DescriptionPayload payload) throws Exception {
        List<Map<String, Object>> messages = new ArrayList<>();

        // System message with prompt caching
        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");

        // Always send full prompt with ephemeral cache marker and cache key
        Map<String, Object> textContent = new LinkedHashMap<>();
        textContent.put("type", "text");
        textContent.put("text", PROMPT_TEMPLATE);

        Map<String, Object> cacheControl = new LinkedHashMap<>();
        cacheControl.put("type", "ephemeral");
        cacheControl.put("cache_key", PROMPT_CACHE_KEY);
        textContent.put("cache_control", cacheControl);

        systemMessage.put("content", Collections.singletonList(textContent));

        messages.add(systemMessage);

        // User message with input data
        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", objectMapper.writeValueAsString(payload.toJsonMap()));

        messages.add(userMessage);

        return messages;
    }

    private Map<String, Object> buildResponseFormat() {
        Map<String, Object> descriptionSchema = new LinkedHashMap<>();
        descriptionSchema.put("type", "string");
        descriptionSchema.put("minLength", 160);
        descriptionSchema.put("maxLength", 230);

        Map<String, Object> keywordItems = new LinkedHashMap<>();
        keywordItems.put("type", "string");

        Map<String, Object> keywordsSchema = new LinkedHashMap<>();
        keywordsSchema.put("type", "array");
        keywordsSchema.put("items", keywordItems);
        keywordsSchema.put("minItems", 9);
        keywordsSchema.put("maxItems", 9);

        Map<String, Object> properties = new LinkedHashMap<>();
        properties.put("description", descriptionSchema);
        properties.put("keywords", keywordsSchema);

        Map<String, Object> schema = new LinkedHashMap<>();
        schema.put("type", "object");
        schema.put("properties", properties);
        schema.put("required", List.of("description", "keywords"));
        schema.put("additionalProperties", false);

        Map<String, Object> jsonSchema = new LinkedHashMap<>();
        jsonSchema.put("name", "mohe_description_response");
        jsonSchema.put("schema", schema);

        Map<String, Object> responseFormat = new LinkedHashMap<>();
        responseFormat.put("type", "json_schema");
        responseFormat.put("json_schema", jsonSchema);
        return responseFormat;
    }


    public record DescriptionPayload(
        String aiSummary,
        String review,
        String description,
        String category,
        boolean petFriendly
    ) {
        public Map<String, Object> toJsonMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("ai_summary", defaultString(aiSummary));
            map.put("review", defaultString(review));
            map.put("description", defaultString(description));
            map.put("category", defaultString(category));
            map.put("pet_friendly", petFriendly);
            return map;
        }

        private String defaultString(String value) {
            return (value == null || value.trim().isEmpty()) ? "정보 없음" : value.trim();
        }
    }

    public record DescriptionResult(
        String description,
        List<String> keywords,
        int cachedTokens
    ) {
        public DescriptionResult {
            keywords = CollectionUtils.isEmpty(keywords) ? List.of() : List.copyOf(keywords);
        }
    }

    private String sanitizeDescription(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\p{So}\\p{Cn}]", "").trim();
    }
}
