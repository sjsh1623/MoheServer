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
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Handles GPT-4.1-mini calls for batch description generation using prompt caching.
 */
@Service
public class OpenAiDescriptionService {

    private static final Logger log = LoggerFactory.getLogger(OpenAiDescriptionService.class);

    private static final String MODEL = "gpt-4.1-mini";
    private static final String PROMPT_CACHE_KEY = "mohe.batch.description.v1";
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
        6. 강조할 주요 명사는 '**'로 감싸 Bold 처리하세요. (최소 1개, 최대 3개)
        7. 장소명은 Bold 처리하지 마세요.
        8. 문맥상 자연스럽게 반려동물 관련 내용을 포함하세요 (pet_friendly가 true인 경우).
        9. 이모지나 불필요한 기호는 사용하지 마세요.
        10. 키워드는 총 9개를 추출하며 다음 구성으로 생성합니다:
            - 기분 관련 단어 2개
            - 날씨 관련 단어 2개
            - 분위기 관련 단어 2개
            - 주요 명사 3개
        11. 출력은 반드시 JSON 형태로만 반환하며, 추가 텍스트는 포함하지 않습니다.
        """;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final AtomicBoolean cachePrimed = new AtomicBoolean(false);
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

        CacheMode initialMode = cachePrimed.get() ? CacheMode.USE_CACHED : CacheMode.PERSIST;
        DescriptionResult result = executeRequest(payload, initialMode);

        if (result == null) {
            return Optional.empty();
        }

        if (initialMode == CacheMode.USE_CACHED && result.cachedTokens() == 0) {
            log.info("OpenAI prompt cache expired. Re-registering prompt with persist mode.");
            cachePrimed.set(false);
            result = executeRequest(payload, CacheMode.PERSIST);
            if (result == null) {
                return Optional.empty();
            }
            cachePrimed.set(true);
        } else if (initialMode == CacheMode.PERSIST) {
            cachePrimed.set(true);
        }

        return Optional.of(result);
    }

    private DescriptionResult executeRequest(DescriptionPayload payload, CacheMode cacheMode) {
        try {
            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("input", buildInput(payload, cacheMode));
            requestBody.put("response_format", buildResponseFormat());
            requestBody.put("temperature", 0.7);
            requestBody.put("max_output_tokens", 600);

            String rawResponse = webClient.post()
                .uri("/responses")
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
            int cachedTokens = root.path("usage").path("cached_tokens").asInt(0);

            String outputText = extractOutputText(root);
            if (outputText == null || outputText.isBlank()) {
                log.error("OpenAI response did not include output_text. Raw response: {}", rawResponse);
                return null;
            }

            JsonNode parsed = objectMapper.readTree(outputText);
            String description = parsed.path("description").asText(null);
            if (description == null || description.isBlank()) {
                log.error("Parsed OpenAI response missing description. Payload: {}", outputText);
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

    private List<Map<String, Object>> buildInput(DescriptionPayload payload, CacheMode cacheMode) throws Exception {
        Map<String, Object> cacheControl = new LinkedHashMap<>();
        cacheControl.put("type", cacheMode == CacheMode.PERSIST ? "persist" : "use_cache");
        cacheControl.put("key", PROMPT_CACHE_KEY);

        Map<String, Object> systemContent = new LinkedHashMap<>();
        systemContent.put("type", "text");
        systemContent.put("text", cacheMode == CacheMode.PERSIST ? PROMPT_TEMPLATE : ""); // reuse cached prompt without resending instructions
        systemContent.put("cache_control", cacheControl);

        Map<String, Object> systemMessage = new LinkedHashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", Collections.singletonList(systemContent));

        Map<String, Object> userContent = new LinkedHashMap<>();
        userContent.put("type", "input_text");
        userContent.put("text", objectMapper.writeValueAsString(payload.toJsonMap()));

        Map<String, Object> userMessage = new LinkedHashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", Collections.singletonList(userContent));

        List<Map<String, Object>> input = new ArrayList<>();
        input.add(systemMessage);
        input.add(userMessage);
        return input;
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

    private String extractOutputText(JsonNode root) {
        JsonNode outputNode = root.path("output");
        if (outputNode.isArray()) {
            for (JsonNode item : outputNode) {
                if (item.has("content") && item.get("content").isArray()) {
                    for (JsonNode content : item.get("content")) {
                        String type = content.path("type").asText("");
                        if (("output_text".equals(type) || "text".equals(type)) && content.has("text")) {
                            String text = content.get("text").asText(null);
                            if (text != null && !text.isBlank()) {
                                return text;
                            }
                        }
                    }
                }

                String itemType = item.path("type").asText("");
                if ("output_text".equals(itemType) && item.has("text")) {
                    String text = item.get("text").asText(null);
                    if (text != null && !text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        JsonNode directOutput = root.get("output_text");
        if (directOutput != null) {
            if (directOutput.isTextual()) {
                return directOutput.asText();
            }
            if (directOutput.isArray() && directOutput.size() > 0) {
                for (JsonNode node : directOutput) {
                    if (node.isTextual()) {
                        return node.asText();
                    }
                }
            }
        }

        JsonNode choices = root.get("choices");
        if (choices != null && choices.isArray()) {
            for (JsonNode choice : choices) {
                JsonNode message = choice.get("message");
                if (message != null) {
                    JsonNode content = message.get("content");
                    if (content != null && content.isArray()) {
                        for (JsonNode part : content) {
                            String type = part.path("type").asText("");
                            if (("output_text".equals(type) || "text".equals(type)) && part.has("text")) {
                                String text = part.get("text").asText(null);
                                if (text != null && !text.isBlank()) {
                                    return text;
                                }
                            }
                        }
                    }
                }
            }
        }

        return null;
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

    private enum CacheMode {
        PERSIST,
        USE_CACHED
    }

    private String sanitizeDescription(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[\\p{So}\\p{Cn}]", "").trim();
    }
}
