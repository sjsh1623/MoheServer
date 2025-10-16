package com.mohe.spring.controller;

import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.OpenAiDescriptionService;
import com.mohe.spring.service.OllamaService;
import com.mohe.spring.service.crawling.CrawlingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/test")
@Tag(name = "OpenAI Cache Test", description = "OpenAI 캐시 및 description 생성 테스트 API")
public class OpenAiCacheTestController {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCacheTestController.class);

    private final PlaceRepository placeRepository;
    private final CrawlingService crawlingService;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final OllamaService ollamaService;

    public OpenAiCacheTestController(
        PlaceRepository placeRepository,
        CrawlingService crawlingService,
        OpenAiDescriptionService openAiDescriptionService,
        OllamaService ollamaService
    ) {
        this.placeRepository = placeRepository;
        this.crawlingService = crawlingService;
        this.openAiDescriptionService = openAiDescriptionService;
        this.ollamaService = ollamaService;
    }

    @GetMapping("/ping")
    @Operation(summary = "핑 테스트", description = "컨트롤러 라우팅 테스트")
    public ResponseEntity<String> ping() {
        log.info("🏓 Ping endpoint called!");
        return ResponseEntity.ok("pong");
    }

    @PostMapping("/openai-cache")
    @Operation(
        summary = "OpenAI 캐시 테스트",
        description = "5개 장소에 대해 크롤링 → OpenAI description 생성을 수행하고 캐시 동작을 테스트합니다. " +
                     "첫 요청은 프롬프트 캐시 등록, 이후 요청은 캐시된 프롬프트 재사용을 확인할 수 있습니다."
    )
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "테스트 성공")
    public ResponseEntity<TestResult> testOpenAiCache() {
        log.info("🧪 Starting OpenAI cache test with 5 places...");

        // 1. Get 5 places that need processing (not ready)
        List<Place> places = placeRepository.findTop5ByReadyFalseOrReadyIsNull();

        if (places.isEmpty()) {
            log.warn("⚠️ No places available for testing");
            return ResponseEntity.ok(
                new TestResult(0, new ArrayList<>(), "No places available for testing")
            );
        }

        log.info("📋 Found {} places to process", places.size());

        List<PlaceTestResult> results = new ArrayList<>();
        int successCount = 0;

        for (Place place : places) {
            PlaceTestResult result = processPlace(place);
            results.add(result);
            if (result.success) {
                successCount++;
            }
        }

        TestResult testResult = new TestResult(
            successCount,
            results,
            String.format("Processed %d places, %d succeeded", places.size(), successCount)
        );

        log.info("✅ OpenAI cache test completed: {}/{} succeeded", successCount, places.size());

        return ResponseEntity.ok(testResult);
    }

    private PlaceTestResult processPlace(Place place) {
        PlaceTestResult result = new PlaceTestResult();
        result.placeName = place.getName();
        result.placeId = place.getId();

        try {
            // Step 1: Crawling
            log.info("🔍 Starting crawl for '{}'", place.getName());
            String searchQuery = place.getRoadAddress();

            var crawlingResponse = crawlingService.crawlPlaceData(searchQuery, place.getName())
                .block();

            // Check if crawling was successful
            if (crawlingResponse == null || !crawlingResponse.isSuccess() || crawlingResponse.getData() == null) {
                String errorMsg = crawlingResponse != null ? crawlingResponse.getMessage() : "Crawler returned null response";
                throw new RuntimeException("Crawling failed: " + errorMsg);
            }

            CrawledDataDto crawledData = crawlingResponse.getData();

            log.info("📥 Crawl completed for '{}'", place.getName());
            log.info("=" .repeat(80));
            log.info("CRAWLED DATA for '{}':", place.getName());
            log.info("  Original Description: {}", crawledData.getOriginalDescription());
            log.info("  AI Summary: {}", crawledData.getAiSummary());
            log.info("  Review Count: {}", crawledData.getReviews() != null ? crawledData.getReviews().size() : 0);
            log.info("  Parking Available: {}", crawledData.isParkingAvailable());
            log.info("  Pet Friendly: {}", crawledData.isPetFriendly());
            log.info("=" .repeat(80));

            // Check if we have enough data
            String aiSummaryText = "";
            if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                aiSummaryText = String.join("\n", crawledData.getAiSummary());
            }

            String textForProcessing;
            if (aiSummaryText != null && !aiSummaryText.trim().isEmpty()) {
                textForProcessing = aiSummaryText;
                log.info("✅ Using AI summary for '{}'", place.getName());
            } else if (crawledData.getOriginalDescription() != null && !crawledData.getOriginalDescription().trim().isEmpty()) {
                textForProcessing = crawledData.getOriginalDescription();
                log.info("⚠️ No AI summary for '{}', using original description", place.getName());
            } else if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                int reviewCount = Math.min(crawledData.getReviews().size(), 3);
                textForProcessing = String.join("\n", crawledData.getReviews().subList(0, reviewCount));
                log.info("⚠️ No AI summary or description for '{}', using reviews", place.getName());
            } else {
                throw new RuntimeException("No data available for processing");
            }

            result.crawledData = new CrawledDataSummary(
                crawledData.getOriginalDescription(),
                aiSummaryText,
                crawledData.getReviews() != null ? crawledData.getReviews().size() : 0,
                crawledData.isParkingAvailable(),
                crawledData.isPetFriendly()
            );

            // Step 2: Generate OpenAI description
            log.info("🤖 Generating OpenAI description for '{}'", place.getName());

            String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
            String reviewsForPrompt = prepareReviewSnippet(crawledData.getReviews());

            OpenAiDescriptionService.DescriptionPayload payload =
                new OpenAiDescriptionService.DescriptionPayload(
                    aiSummaryText,
                    reviewsForPrompt,
                    crawledData.getOriginalDescription(),
                    categoryStr,
                    crawledData.isPetFriendly()
                );

            OpenAiDescriptionService.DescriptionResult descriptionResult =
                openAiDescriptionService.generateDescription(payload)
                    .orElseThrow(() -> new RuntimeException("OpenAI description generation failed"));

            result.openAiResult = new OpenAiResultSummary(
                descriptionResult.description(),
                descriptionResult.keywords(),
                descriptionResult.cachedTokens()
            );

            log.info("=" .repeat(80));
            log.info("OPENAI RESULT for '{}':", place.getName());
            log.info("  Cached Tokens: {}", descriptionResult.cachedTokens());
            log.info("  Description: {}", descriptionResult.description());
            log.info("  Keywords: {}", String.join(", ", descriptionResult.keywords()));
            if (descriptionResult.cachedTokens() > 0) {
                log.info("  🎯 CACHE HIT! {} tokens were reused from cache", descriptionResult.cachedTokens());
            } else {
                log.info("  📝 CACHE MISS - This is the first request, prompt cache registered");
            }
            log.info("=" .repeat(80));

            // Step 3: Generate keywords using Ollama
            log.info("🔑 Generating Ollama keywords for '{}'", place.getName());
            String[] keywords = ollamaService.generateKeywords(
                textForProcessing,
                categoryStr,
                crawledData.isPetFriendly()
            );
            result.ollamaKeywords = Arrays.asList(keywords);

            log.info("=" .repeat(80));
            log.info("OLLAMA KEYWORDS for '{}':", place.getName());
            log.info("  Keywords: {}", String.join(", ", keywords));
            log.info("=" .repeat(80));

            log.info("✅ Successfully processed '{}' - ALL STEPS COMPLETED", place.getName());

            result.success = true;
            result.message = "Successfully processed";

        } catch (Exception e) {
            log.error("❌ Failed to process '{}': {}", place.getName(), e.getMessage(), e);
            result.success = false;
            result.message = "Error: " + e.getMessage();
        }

        return result;
    }

    private String prepareReviewSnippet(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "리뷰 정보 없음";
        }
        int limit = Math.min(reviews.size(), 10);
        return String.join("\n", reviews.subList(0, limit));
    }

    // DTOs for response
    public static class TestResult {
        public int successCount;
        public List<PlaceTestResult> results;
        public String message;

        public TestResult(int successCount, List<PlaceTestResult> results, String message) {
            this.successCount = successCount;
            this.results = results;
            this.message = message;
        }
    }

    public static class PlaceTestResult {
        public Long placeId;
        public String placeName;
        public boolean success;
        public String message;
        public CrawledDataSummary crawledData;
        public OpenAiResultSummary openAiResult;
        public List<String> ollamaKeywords;
    }

    public static class CrawledDataSummary {
        public String originalDescription;
        public String aiSummary;
        public int reviewCount;
        public boolean parkingAvailable;
        public boolean petFriendly;

        public CrawledDataSummary(String originalDescription, String aiSummary, int reviewCount,
                                 boolean parkingAvailable, boolean petFriendly) {
            this.originalDescription = originalDescription;
            this.aiSummary = aiSummary;
            this.reviewCount = reviewCount;
            this.parkingAvailable = parkingAvailable;
            this.petFriendly = petFriendly;
        }
    }

    public static class OpenAiResultSummary {
        public String description;
        public List<String> keywords;
        public int cachedTokens;

        public OpenAiResultSummary(String description, List<String> keywords, int cachedTokens) {
            this.description = description;
            this.keywords = keywords;
            this.cachedTokens = cachedTokens;
        }
    }
}
