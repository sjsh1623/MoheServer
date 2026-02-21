package com.mohe.spring.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.config.LlmProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.boot.web.client.RestTemplateBuilder;

public class OpenAiService implements LlmService {

    private static final Logger logger = LoggerFactory.getLogger(OpenAiService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final LlmProperties llmProperties;

    public OpenAiService(LlmProperties llmProperties, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.restTemplate = restTemplateBuilder.build();
        this.objectMapper = objectMapper;
        this.llmProperties = llmProperties;
    }

    @Override
    public LlmRecommendationResponse generatePlaceRecommendations(String prompt, List<String> availablePlaces) {
        try {
            logger.info("Sending recommendation request to OpenAI with prompt length: {}", prompt.length());

            String response = callOpenAi(prompt);
            logger.info("Received response from OpenAI, length: {}", response.length());

            return parseRecommendationResponse(response, availablePlaces);

        } catch (Exception e) {
            logger.error("Failed to generate recommendations using OpenAI: {}", e.getMessage(), e);
            return createFallbackResponse(availablePlaces);
        }
    }

    /**
     * Generate place recommendations with contextual information
     */
    public LlmRecommendationResponse generatePlaceRecommendations(
            String userLocation, String weatherCondition, String timeOfDay,
            String userMbti, List<String> availablePlaces) {

        // Build contextual prompt
        String prompt = buildContextualPrompt(userLocation, weatherCondition, timeOfDay, userMbti, availablePlaces);

        // Call the main method
        return generatePlaceRecommendations(prompt, availablePlaces);
    }

    private String buildContextualPrompt(String userLocation, String weatherCondition,
                                       String timeOfDay, String userMbti, List<String> availablePlaces) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("당신은 한국의 장소 추천 전문가입니다.\n\n");
        prompt.append("현재 상황:\n");
        prompt.append("- 위치: ").append(userLocation != null ? userLocation : "서울").append("\n");
        prompt.append("- 날씨: ").append(weatherCondition != null ? weatherCondition : "맑음").append("\n");
        prompt.append("- 시간대: ").append(timeOfDay != null ? timeOfDay : "오후").append("\n");

        if (userMbti != null) {
            prompt.append("- MBTI 성격유형: ").append(userMbti).append("\n");
        }

        prompt.append("\n이용 가능한 장소들:\n");
        for (int i = 0; i < availablePlaces.size() && i < 50; i++) {
            prompt.append("- ").append(availablePlaces.get(i)).append("\n");
        }

        prompt.append("\n현재 상황과 ");
        if (userMbti != null) {
            prompt.append("사용자의 MBTI 성격유형을 고려하여 ");
        }
        prompt.append("가장 적합한 장소 15개를 추천해주세요.\n");
        prompt.append("응답 형식:\n");
        prompt.append("추천장소: [장소1, 장소2, 장소3, ...]\n");
        prompt.append("이유: [추천 이유 설명]");

        return prompt.toString();
    }

    /**
     * 장소 설명 생성 전용 메소드
     */
    public String generatePlaceDescription(String placeName, String category, String address) {
        try {
            String prompt = String.format(
                "다음 장소에 대해 감성적이고 매력적인 설명을 한국어로 100자 내외로 작성해주세요.\n\n" +
                "장소명: %s\n" +
                "카테고리: %s\n" +
                "주소: %s\n\n" +
                "방문객들이 이 장소에 대해 흥미를 느낄 수 있도록 매력적으로 작성해주세요. " +
                "설명만 답변하고 다른 내용은 포함하지 마세요.",
                placeName,
                category != null ? category : "일반",
                address != null ? address : ""
            );

            return callOpenAi(prompt);

        } catch (Exception e) {
            logger.error("❌ OpenAI 설명 생성 실패 for {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    /**
     * 키워드 추출 전용 메소드
     */
    public List<String> extractKeywords(String placeName, String category, String description) {
        try {
            String prompt = String.format(
                "다음 장소 정보에서 핵심 키워드 5개를 추출해주세요. 콤마로 구분해서 키워드만 답변해주세요.\n\n" +
                "장소명: %s\n" +
                "카테고리: %s\n" +
                "설명: %s\n\n" +
                "예시: 카페, 디저트, 분위기, 데이트, 힐링",
                placeName,
                category != null ? category : "",
                description != null ? description : ""
            );

            String response = callOpenAi(prompt);
            if (response != null && !response.trim().isEmpty()) {
                return Arrays.asList(response.split(","))
                    .stream()
                    .map(String::trim)
                    .filter(keyword -> !keyword.isEmpty())
                    .collect(Collectors.toList());
            }

        } catch (Exception e) {
            logger.error("❌ 키워드 추출 실패 for {}: {}", placeName, e.getMessage());
        }

        return Collections.emptyList();
    }

    /**
     * 사용자 검색 쿼리 분석 - 카테고리, 키워드, 의도 추출
     * @param userQuery 사용자의 자연어 검색 쿼리
     * @return 분석 결과 (카테고리, 키워드, 검색 의도)
     */
    public QueryAnalysisResult analyzeSearchQuery(String userQuery) {
        try {
            String prompt = String.format(
                "다음 사용자의 검색 쿼리를 분석해주세요.\n\n" +
                "쿼리: \"%s\"\n\n" +
                "다음 형식으로만 답변해주세요 (다른 설명 없이):\n" +
                "카테고리: [음식/카페/활동/분위기/장소/기타 중 하나]\n" +
                "키워드: [검색에 사용할 핵심 키워드 3-5개, 콤마로 구분]\n" +
                "의도: [사용자가 찾고자 하는 것을 한 문장으로]\n" +
                "감정: [편안함/활기참/로맨틱/힐링/모험/일상 중 하나]",
                userQuery
            );

            String response = callOpenAi(prompt);
            return parseQueryAnalysis(response, userQuery);

        } catch (Exception e) {
            logger.error("❌ 쿼리 분석 실패: {}", e.getMessage());
            return new QueryAnalysisResult(userQuery, "기타", List.of(userQuery), userQuery, "일상");
        }
    }

    private QueryAnalysisResult parseQueryAnalysis(String response, String originalQuery) {
        String category = "기타";
        List<String> keywords = new ArrayList<>();
        String intent = originalQuery;
        String mood = "일상";

        try {
            String[] lines = response.split("\n");
            for (String line : lines) {
                line = line.trim();
                if (line.startsWith("카테고리:")) {
                    category = line.substring("카테고리:".length()).trim().replaceAll("[\\[\\]]", "");
                } else if (line.startsWith("키워드:")) {
                    String keywordStr = line.substring("키워드:".length()).trim().replaceAll("[\\[\\]]", "");
                    keywords = Arrays.stream(keywordStr.split(","))
                        .map(String::trim)
                        .filter(k -> !k.isEmpty())
                        .collect(Collectors.toList());
                } else if (line.startsWith("의도:")) {
                    intent = line.substring("의도:".length()).trim().replaceAll("[\\[\\]]", "");
                } else if (line.startsWith("감정:")) {
                    mood = line.substring("감정:".length()).trim().replaceAll("[\\[\\]]", "");
                }
            }
        } catch (Exception e) {
            logger.warn("쿼리 분석 파싱 실패, 기본값 사용: {}", e.getMessage());
        }

        if (keywords.isEmpty()) {
            keywords = List.of(originalQuery);
        }

        return new QueryAnalysisResult(originalQuery, category, keywords, intent, mood);
    }

    /**
     * 쿼리 분석 결과 클래스
     */
    public static class QueryAnalysisResult {
        private final String originalQuery;
        private final String category;
        private final List<String> keywords;
        private final String intent;
        private final String mood;

        public QueryAnalysisResult(String originalQuery, String category, List<String> keywords, String intent, String mood) {
            this.originalQuery = originalQuery;
            this.category = category;
            this.keywords = keywords;
            this.intent = intent;
            this.mood = mood;
        }

        public String getOriginalQuery() { return originalQuery; }
        public String getCategory() { return category; }
        public List<String> getKeywords() { return keywords; }
        public String getIntent() { return intent; }
        public String getMood() { return mood; }

        public String getEnrichedQuery() {
            // 키워드들을 결합하여 향상된 검색어 생성
            StringBuilder sb = new StringBuilder();
            sb.append(String.join(" ", keywords));
            if (!mood.equals("일상")) {
                sb.append(" ").append(mood);
            }
            return sb.toString();
        }
    }

    /**
     * 이미지 생성 프롬프트 생성
     */
    public String generateImagePrompt(String placeName, String category, String description) {
        try {
            String prompt = String.format(
                "다음 장소의 이미지를 생성하기 위한 영어 프롬프트를 작성해주세요.\n\n" +
                "장소명: %s\n" +
                "카테고리: %s\n" +
                "설명: %s\n\n" +
                "이미지 생성 AI가 이해할 수 있도록 영어로 상세하고 시각적인 프롬프트를 작성해주세요. " +
                "프롬프트만 답변하고 다른 내용은 포함하지 마세요.",
                placeName,
                category != null ? category : "",
                description != null ? description : ""
            );

            return callOpenAi(prompt);

        } catch (Exception e) {
            logger.error("❌ 이미지 프롬프트 생성 실패 for {}: {}", placeName, e.getMessage());
            return null;
        }
    }

    private String callOpenAi(String prompt) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(llmProperties.getOpenai().getApiKey());

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");
        message.put("content", prompt);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", llmProperties.getOpenai().getModel());
        requestBody.put("messages", Collections.singletonList(message));
        requestBody.put("stream", false);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);

        if (response.getStatusCode() == HttpStatus.OK) {
            try {
                OpenAiChatCompletionResponse openAiResponse = objectMapper.readValue(response.getBody(), OpenAiChatCompletionResponse.class);
                return openAiResponse.getChoices().get(0).getMessage().getContent();
            } catch (Exception e) {
                throw new RuntimeException("Failed to parse OpenAI response", e);
            }
        } else {
            throw new RuntimeException("OpenAI API returned status: " + response.getStatusCode());
        }
    }

    private LlmRecommendationResponse parseRecommendationResponse(String response, List<String> availablePlaces) {
        try {
            Pattern pattern = Pattern.compile("추천장소:([^이유]*)", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(response);

            LlmRecommendationResponse result = new LlmRecommendationResponse();
            result.setRawResponse(response);

            if (matcher.find()) {
                String placesText = matcher.group(1).trim();
                String[] places = placesText.split("[,，]");

                for (String place : places) {
                    String cleanPlace = place.trim().replaceAll("[[\\]\s]", "");
                    if (!cleanPlace.isEmpty()) {
                        for (String availablePlace : availablePlaces) {
                            if (availablePlace.contains(cleanPlace) || cleanPlace.contains(availablePlace)) {
                                result.getRecommendedPlaces().add(availablePlace);
                                break;
                            }
                        }
                    }
                }
            }

            Pattern reasonPattern = Pattern.compile("이유:(.+)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher reasonMatcher = reasonPattern.matcher(response);
            if (reasonMatcher.find()) {
                result.setReasoning(reasonMatcher.group(1).trim());
            }

            if (result.getRecommendedPlaces().isEmpty()) {
                return createFallbackResponse(availablePlaces);
            }

            if (result.getRecommendedPlaces().size() > 15) {
                result.setRecommendedPlaces(result.getRecommendedPlaces().subList(0, 15));
            }

            result.setSuccess(true);
            return result;

        } catch (Exception e) {
            logger.error("Error parsing OpenAI response: {}", e.getMessage(), e);
            return createFallbackResponse(availablePlaces);
        }
    }

    private LlmRecommendationResponse createFallbackResponse(List<String> availablePlaces) {
        LlmRecommendationResponse response = new LlmRecommendationResponse();
        response.setSuccess(false);
        response.setRawResponse("OpenAI LLM을 사용할 수 없어 기본 추천을 제공합니다.");
        response.setReasoning("현재 시간과 위치를 고려한 기본 추천입니다.");

        int limit = Math.min(15, availablePlaces.size());
        response.setRecommendedPlaces(availablePlaces.subList(0, limit));

        return response;
    }

    private static class OpenAiChatCompletionResponse {
        private List<Choice> choices;

        public List<Choice> getChoices() {
            return choices;
        }

        public void setChoices(List<Choice> choices) {
            this.choices = choices;
        }

        public static class Choice {
            private Message message;

            public Message getMessage() {
                return message;
            }

            public void setMessage(Message message) {
                this.message = message;
            }
        }

        public static class Message {
            private String role;
            private String content;

            public String getRole() {
                return role;
            }

            public void setRole(String role) {
                this.role = role;
            }

            public String getContent() {
                return content;
            }

            public void setContent(String content) {
                this.content = content;
            }
        }
    }
}
