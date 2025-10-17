package com.mohe.spring.controller;

import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.OpenAiDescriptionService;
import com.mohe.spring.service.KeywordEmbeddingService;
import com.mohe.spring.service.crawling.CrawlingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test controller for OpenAI description generation with prompt caching
 */
@RestController
@RequestMapping("/api/test")
public class DescriptionTestController {

    private static final Logger log = LoggerFactory.getLogger(DescriptionTestController.class);

    private final PlaceRepository placeRepository;
    private final CrawlingService crawlingService;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final KeywordEmbeddingService keywordEmbeddingService;

    public DescriptionTestController(
        PlaceRepository placeRepository,
        CrawlingService crawlingService,
        OpenAiDescriptionService openAiDescriptionService,
        KeywordEmbeddingService keywordEmbeddingService
    ) {
        this.placeRepository = placeRepository;
        this.crawlingService = crawlingService;
        this.openAiDescriptionService = openAiDescriptionService;
        this.keywordEmbeddingService = keywordEmbeddingService;
    }

    @PostMapping("/description-generation")
    public ResponseEntity<Map<String, Object>> testDescriptionGeneration() {
        log.info("🧪 Starting description generation test...");

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> results = new ArrayList<>();

        try {
            // Get 5 places that need processing
            List<Place> places = placeRepository.findTop5ByReadyFalseOrReadyIsNull();

            if (places.isEmpty()) {
                response.put("success", false);
                response.put("message", "No places found for testing. All places are already processed.");
                return ResponseEntity.ok(response);
            }

            log.info("📋 Found {} places for testing", places.size());

            int successCount = 0;
            int failureCount = 0;

            for (int i = 0; i < places.size(); i++) {
                Place place = places.get(i);
                Map<String, Object> placeResult = new HashMap<>();
                placeResult.put("placeId", place.getId());
                placeResult.put("placeName", place.getName());

                try {
                    log.info("\n🔍 [{}/{}] Processing: {}", i + 1, places.size(), place.getName());

                    // Step 1: Crawl data
                    String searchQuery = place.getRoadAddress();
                    if (!place.getDescriptions().isEmpty() && place.getDescriptions().get(0).getSearchQuery() != null) {
                        searchQuery = place.getDescriptions().get(0).getSearchQuery();
                    }

                    log.info("  📡 Crawling data for: {}", searchQuery);
                    CrawledDataDto crawledData = crawlingService.crawlPlaceData(searchQuery, place.getName())
                        .block()
                        .getData();

                    if (crawledData == null) {
                        placeResult.put("success", false);
                        placeResult.put("error", "Crawling returned no data");
                        results.add(placeResult);
                        failureCount++;
                        continue;
                    }

                    // Step 2: Prepare data for OpenAI
                    String aiSummaryText = "";
                    if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                        aiSummaryText = String.join("\n", crawledData.getAiSummary());
                    }

                    String textForAI;
                    if (aiSummaryText != null && !aiSummaryText.trim().isEmpty()) {
                        textForAI = aiSummaryText;
                    } else if (crawledData.getOriginalDescription() != null && !crawledData.getOriginalDescription().trim().isEmpty()) {
                        textForAI = crawledData.getOriginalDescription();
                    } else if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                        int reviewCount = Math.min(crawledData.getReviews().size(), 3);
                        textForAI = String.join("\n", crawledData.getReviews().subList(0, reviewCount));
                    } else {
                        placeResult.put("success", false);
                        placeResult.put("error", "No text available for description generation");
                        results.add(placeResult);
                        failureCount++;
                        continue;
                    }

                    String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
                    String reviewsForPrompt = prepareReviewSnippet(crawledData.getReviews());

                    // Step 3: Generate description with OpenAI
                    log.info("  🤖 Generating OpenAI description (with prompt caching)...");
                    OpenAiDescriptionService.DescriptionPayload payload =
                        new OpenAiDescriptionService.DescriptionPayload(
                            aiSummaryText,
                            reviewsForPrompt,
                            crawledData.getOriginalDescription(),
                            categoryStr,
                            place.getPetFriendly() != null ? place.getPetFriendly() : false
                        );

                    OpenAiDescriptionService.DescriptionResult descriptionResult =
                        openAiDescriptionService.generateDescription(payload).orElse(null);

                    if (descriptionResult == null) {
                        placeResult.put("success", false);
                        placeResult.put("error", "OpenAI description generation failed");
                        results.add(placeResult);
                        failureCount++;
                        continue;
                    }

                    log.info("  ✅ Description generated: {}", descriptionResult.description());
                    log.info("  📊 Cached tokens: {}", descriptionResult.cachedTokens());
                    log.info("  🔑 Keywords: {}", String.join(", ", descriptionResult.keywords()));

                    // Step 4: Generate keywords with embedding service
                    log.info("  🔑 Generating keywords...");
                    String[] keywords = keywordEmbeddingService.generateKeywords(
                        textForAI,
                        categoryStr,
                        place.getPetFriendly() != null ? place.getPetFriendly() : false
                    );

                    // Populate result
                    placeResult.put("success", true);
                    placeResult.put("description", descriptionResult.description());
                    placeResult.put("openaiKeywords", descriptionResult.keywords());
                    placeResult.put("generatedKeywords", List.of(keywords));
                    placeResult.put("cachedTokens", descriptionResult.cachedTokens());
                    placeResult.put("category", categoryStr);
                    placeResult.put("petFriendly", place.getPetFriendly());

                    results.add(placeResult);
                    successCount++;

                    log.info("  ✅ Successfully processed: {}\n", place.getName());

                } catch (Exception e) {
                    log.error("  ❌ Error processing place {}: {}", place.getName(), e.getMessage(), e);
                    placeResult.put("success", false);
                    placeResult.put("error", e.getMessage());
                    results.add(placeResult);
                    failureCount++;
                }
            }

            response.put("success", true);
            response.put("totalProcessed", places.size());
            response.put("successCount", successCount);
            response.put("failureCount", failureCount);
            response.put("results", results);

            log.info("\n🎉 Test completed: {} successful, {} failed", successCount, failureCount);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("❌ Test failed with error: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("error", e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    private String prepareReviewSnippet(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "리뷰 정보 없음";
        }
        int limit = Math.min(reviews.size(), 10);
        return String.join("\n", reviews.subList(0, limit));
    }
}
