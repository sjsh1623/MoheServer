
package com.mohe.spring.service;

import java.util.List;

public interface LlmService {
        LlmRecommendationResponse generatePlaceRecommendations(String prompt, List<String> availablePlaces);

        LlmRecommendationResponse generatePlaceRecommendations(
                String userLocation, String weatherCondition, String timeOfDay,
                String userMbti, List<String> availablePlaces);
}
