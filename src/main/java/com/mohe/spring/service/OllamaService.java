package com.mohe.spring.service;

import com.mohe.spring.config.LlmProperties;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class OllamaService implements LlmService {
    private final LlmProperties llmProperties;

    public OllamaService(LlmProperties llmProperties) {
        this.llmProperties = llmProperties;
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
        return "This is a mock description.";
    }

    public String[] generateKeywords(String aiSummary, String category, boolean petFriendly) {
        return new String[]{"mock_keyword1", "mock_keyword2"};
    }

    public float[] vectorizeKeywords(String[] keywords) {
        return new float[]{0.1f, 0.2f, 0.3f};
    }
}
