package com.mohe.spring.batch.job;

import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceBusinessHour;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.entity.PlaceReview;
import com.mohe.spring.entity.PlaceSns;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.OpenAiDescriptionService;
import com.mohe.spring.service.crawling.CrawlingService;
import com.mohe.spring.service.image.ImageService;
import com.mohe.spring.service.OllamaService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.data.RepositoryItemReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Configuration
public class UpdateCrawledDataJobConfig {

    private final CrawlingService crawlingService;
    private final OllamaService ollamaService;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final ImageService imageService;
    private final PlaceRepository placeRepository;

    public UpdateCrawledDataJobConfig(
        CrawlingService crawlingService,
        OllamaService ollamaService,
        OpenAiDescriptionService openAiDescriptionService,
        ImageService imageService,
        PlaceRepository placeRepository
    ) {
        this.crawlingService = crawlingService;
        this.ollamaService = ollamaService;
        this.openAiDescriptionService = openAiDescriptionService;
        this.imageService = imageService;
        this.placeRepository = placeRepository;
    }

    @Bean
    public Job updateCrawledDataJob(JobRepository jobRepository, Step updateCrawledDataStep) {
        return new JobBuilder("updateCrawledDataJob", jobRepository)
                .start(updateCrawledDataStep)
                .build();
    }

    @Bean
    public Step updateCrawledDataStep(JobRepository jobRepository, PlatformTransactionManager transactionManager, ItemReader<Place> placeReader, ItemProcessor<Place, Place> placeProcessor, ItemWriter<Place> placeWriter) {
        return new StepBuilder("updateCrawledDataStep", jobRepository)
                .<Place, Place>chunk(10, transactionManager)
                .reader(placeReader)
                .processor(placeProcessor)
                .writer(placeWriter)
                .faultTolerant()
                .skip(org.springframework.web.reactive.function.client.WebClientResponseException.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public RepositoryItemReader<Place> placeReader() {
        RepositoryItemReader<Place> reader = new RepositoryItemReader<>();
        reader.setRepository(placeRepository);
        reader.setMethodName("findPlacesForBatchProcessing");
        reader.setArguments(List.of()); // No arguments needed - query handles filtering
        reader.setPageSize(10);
        reader.setSort(Map.of("id", Sort.Direction.ASC));
        return reader;
    }

    @Bean
    public ItemProcessor<Place, Place> placeProcessor() {
        return place -> {
            try {
                // Get search query from PlaceDescription if available
                String searchQuery = place.getRoadAddress();
                if (!place.getDescriptions().isEmpty()) {
                    String savedSearchQuery = place.getDescriptions().get(0).getSearchQuery();
                    if (savedSearchQuery != null && !savedSearchQuery.isEmpty()) {
                        searchQuery = savedSearchQuery;
                    }
                }

                System.out.println("🔍 Starting crawl for '" + place.getName() + "' with query: '" + searchQuery + "'");
                CrawledDataDto crawledData = crawlingService.crawlPlaceData(searchQuery, place.getName()).block().getData();
                System.out.println("📥 Crawl response received for '" + place.getName() + "'");

            // Update Place entity with crawled data
            try {
                place.setReviewCount(Integer.parseInt(crawledData.getReviewCount()));
            } catch (NumberFormatException e) {
                place.setReviewCount(0);
            }
            place.setParkingAvailable(crawledData.isParkingAvailable());
            place.setPetFriendly(crawledData.isPetFriendly());

            // Clear and create new PlaceDescription
            place.getDescriptions().clear();
            PlaceDescription description = new PlaceDescription();
            description.setPlace(place);
            description.setOriginalDescription(crawledData.getOriginalDescription());

            // Check if AI summary is available, otherwise use original description, then reviews
            String aiSummaryText = "";
            if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                aiSummaryText = String.join("\n", crawledData.getAiSummary());
            }

            // Fallback logic: AI summary -> original description -> reviews
            String textForOllama;
            if (aiSummaryText != null && !aiSummaryText.trim().isEmpty()) {
                textForOllama = aiSummaryText;
                System.out.println("✅ Using AI summary for '" + place.getName() + "'");
            } else if (crawledData.getOriginalDescription() != null && !crawledData.getOriginalDescription().trim().isEmpty()) {
                textForOllama = crawledData.getOriginalDescription();
                System.out.println("⚠️ No AI summary for '" + place.getName() + "', using original description instead");
            } else if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                // Use first 3 reviews as description source
                int reviewCount = Math.min(crawledData.getReviews().size(), 3);
                textForOllama = String.join("\n", crawledData.getReviews().subList(0, reviewCount));
                System.out.println("⚠️ No AI summary or original description for '" + place.getName() + "', using reviews instead");
            } else {
                textForOllama = null;
            }

            // Validate that we have some text to work with
            if (textForOllama == null || textForOllama.trim().isEmpty()) {
                System.err.println("⚠️ Lack of information for '" + place.getName() + "' - no description text available (AI summary, original description, and reviews are all empty)");
                // Crawling succeeded but lack of information -> crawler_found = true, ready = false
                place.setCrawlerFound(true);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }

            description.setAiSummary(aiSummaryText);
            description.setSearchQuery(searchQuery);

            // Generate Mohe description using OpenAI (pass reviews for context)
            String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
            System.out.println("🤖 Generating OpenAI description for '" + place.getName() + "'...");

            String reviewsForPrompt = prepareReviewSnippet(crawledData.getReviews());
            OpenAiDescriptionService.DescriptionPayload payload =
                new OpenAiDescriptionService.DescriptionPayload(
                    aiSummaryText,
                    reviewsForPrompt,
                    crawledData.getOriginalDescription(),
                    categoryStr,
                    place.getPetFriendly() != null ? place.getPetFriendly() : false
                );

            String moheDescription = openAiDescriptionService.generateDescription(payload)
                .map(OpenAiDescriptionService.DescriptionResult::description)
                .orElse(null);
            System.out.println("✅ OpenAI description generated for '" + place.getName() + "': " + moheDescription);

            // CRITICAL: ollama_description must NEVER be empty
            // If Ollama generation failed, use the original text as fallback
            if (moheDescription == null || moheDescription.trim().isEmpty() || moheDescription.equals("AI 설명을 생성할 수 없습니다.")) {
                System.err.println("⚠️ OpenAI description generation failed for '" + place.getName() + "', using fallback description");

                // Use original description as fallback, truncate to reasonable length if needed
                String fallbackDescription = textForOllama;
                if (fallbackDescription.length() > 150) {
                    // Try to find a good sentence boundary
                    int lastPeriod = Math.max(fallbackDescription.substring(0, 150).lastIndexOf('.'),
                                            fallbackDescription.substring(0, 150).lastIndexOf('!'));
                    lastPeriod = Math.max(lastPeriod, fallbackDescription.substring(0, 150).lastIndexOf('?'));

                    if (lastPeriod > 50) {
                        fallbackDescription = fallbackDescription.substring(0, lastPeriod + 1).trim();
                    } else {
                        // Just truncate at 150 chars and add ellipsis
                        fallbackDescription = fallbackDescription.substring(0, 147).trim() + "...";
                    }
                }
                moheDescription = fallbackDescription;
            }

            // Double-check: This should NEVER happen, but as a last resort
            if (moheDescription == null || moheDescription.trim().isEmpty()) {
                moheDescription = place.getName() + "에 대한 정보입니다.";
                System.err.println("🚨 CRITICAL: Using minimal fallback for '" + place.getName() + "'");
            }

            description.setOllamaDescription(moheDescription);
            place.getDescriptions().add(description);

            // 📝 Log Mohe AI description before saving to database
            System.out.println("📝 Mohe AI Summary for '" + place.getName() + "' (will be saved to DB):");
            System.out.println("   " + moheDescription);

            // Generate and set keywords using Ollama (use the same text we used for description)
            System.out.println("🔑 Generating keywords for '" + place.getName() + "'...");
            String[] keywords = ollamaService.generateKeywords(
                textForOllama,
                categoryStr,
                place.getPetFriendly() != null ? place.getPetFriendly() : false
            );
            System.out.println("✅ Keywords generated for '" + place.getName() + "': " + String.join(", ", keywords));

            // Validate keywords - check if all are default placeholders
            boolean allKeywordsAreDefault = true;
            for (int i = 0; i < keywords.length; i++) {
                if (!keywords[i].equals("키워드" + (i + 1))) {
                    allKeywordsAreDefault = false;
                    break;
                }
            }

            if (allKeywordsAreDefault) {
                System.err.println("⚠️ AI issue for '" + place.getName() + "' - Ollama keyword generation failed (all default)");
                // Crawling succeeded but AI issue -> crawler_found = true, ready = false
                place.setCrawlerFound(true);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }

            place.setKeyword(Arrays.asList(keywords));

            // Vectorize keywords using Ollama embedding
            System.out.println("🧮 Vectorizing keywords for '" + place.getName() + "'...");
            float[] keywordVector = ollamaService.vectorizeKeywords(keywords);
            System.out.println("✅ Vector generated for '" + place.getName() + "' (dimension: " + keywordVector.length + ")");

            // Validate vector - check if it's the default empty vector
            boolean isDefaultVector = true;
            for (float v : keywordVector) {
                if (v != 0.0f) {
                    isDefaultVector = false;
                    break;
                }
            }

            if (isDefaultVector) {
                System.err.println("⚠️ AI issue for '" + place.getName() + "' - Ollama vectorization failed (default empty vector)");
                // Crawling succeeded but AI issue -> crawler_found = true, ready = false
                place.setCrawlerFound(true);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }

            place.setKeywordVector(Arrays.toString(keywordVector));

            // Download and save images
            place.getImages().clear();
            if (crawledData.getImageUrls() != null && !crawledData.getImageUrls().isEmpty()) {
                System.out.println("📸 Downloading " + crawledData.getImageUrls().size() + " images for '" + place.getName() + "'...");
                List<String> savedImagePaths = imageService.downloadAndSaveImages(
                    place.getId(),
                    place.getName(),
                    crawledData.getImageUrls()
                );
                System.out.println("✅ Saved " + savedImagePaths.size() + " images for '" + place.getName() + "'");

                for (int i = 0; i < savedImagePaths.size(); i++) {
                    PlaceImage placeImage = new PlaceImage();
                    placeImage.setPlace(place);
                    placeImage.setUrl(savedImagePaths.get(i));
                    placeImage.setOrderIndex(i + 1);
                    place.getImages().add(placeImage);
                }
            }

            // Create and set PlaceBusinessHours
            place.getBusinessHours().clear();
            if (crawledData.getBusinessHours() != null && crawledData.getBusinessHours().getWeekly() != null) {
                for (Map.Entry<String, com.mohe.spring.dto.crawling.WeeklyHoursDto> entry : crawledData.getBusinessHours().getWeekly().entrySet()) {
                    PlaceBusinessHour businessHour = new PlaceBusinessHour();
                    businessHour.setPlace(place);
                    businessHour.setDayOfWeek(entry.getKey());

                    // Parse time strings safely
                    try {
                        if (entry.getValue().getOpen() != null && !entry.getValue().getOpen().isEmpty()) {
                            businessHour.setOpen(LocalTime.parse(entry.getValue().getOpen()));
                        }
                        if (entry.getValue().getClose() != null && !entry.getValue().getClose().isEmpty()) {
                            businessHour.setClose(LocalTime.parse(entry.getValue().getClose()));
                        }
                    } catch (Exception e) {
                        // Log and continue if time parsing fails
                        System.err.println("Failed to parse business hours for " + place.getName() + ": " + e.getMessage());
                    }

                    businessHour.setDescription(entry.getValue().getDescription());
                    businessHour.setIsOperating(entry.getValue().isOperating());

                    // Set last order minutes from the parent business_hours object
                    if (crawledData.getBusinessHours().getLastOrderMinutes() != null) {
                        businessHour.setLastOrderMinutes(crawledData.getBusinessHours().getLastOrderMinutes());
                    }

                    place.getBusinessHours().add(businessHour);
                }
            }

            // Create and set PlaceSns
            place.getSns().clear();
            if (crawledData.getSnsUrls() != null && !crawledData.getSnsUrls().isEmpty()) {
                for (Map.Entry<String, String> entry : crawledData.getSnsUrls().entrySet()) {
                    if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                        PlaceSns sns = new PlaceSns();
                        sns.setPlace(place);
                        sns.setPlatform(entry.getKey());
                        sns.setUrl(entry.getValue());
                        place.getSns().add(sns);
                    }
                }
            }

            // Create and set PlaceReview (save up to 10 reviews)
            place.getReviews().clear();
            if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                // Limit to 10 reviews
                int reviewCount = Math.min(crawledData.getReviews().size(), 10);
                System.out.println("💬 Saving " + reviewCount + " reviews for '" + place.getName() + "'");

                for (int i = 0; i < reviewCount; i++) {
                    String reviewText = crawledData.getReviews().get(i);
                    if (reviewText != null && !reviewText.trim().isEmpty()) {
                        PlaceReview review = new PlaceReview();
                        review.setPlace(place);
                        review.setReviewText(reviewText);
                        review.setOrderIndex(i + 1);
                        place.getReviews().add(review);
                    }
                }
                System.out.println("✅ Saved " + place.getReviews().size() + " reviews for '" + place.getName() + "'");
            }

                // Mark place as ready and crawler_found as true after successful processing
                place.setReady(true);
                place.setCrawlerFound(true);

                // ✅ Success logging
                System.out.println("✅ Successfully crawled '" + place.getName() + "' - " +
                    "Reviews: " + place.getReviewCount() + ", " +
                    "Images: " + place.getImages().size() + ", " +
                    "Keywords: " + String.join(", ", place.getKeyword()) + ", " +
                    "Parking: " + (place.getParkingAvailable() != null ? place.getParkingAvailable() : "Unknown") + ", " +
                    "Pet-friendly: " + (place.getPetFriendly() != null ? place.getPetFriendly() : "Unknown"));

                return place;
            } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
                // HTTP 에러 = 크롤링 실패 -> crawler_found = false, ready = false
                if (e.getStatusCode().value() == 404) {
                    System.err.println("❌ Crawling failed for '" + place.getName() + "' - not found by crawler (404)");
                } else {
                    System.err.println("❌ Crawling failed for '" + place.getName() + "' - crawler server error: " + e.getStatusCode());
                }
                place.setCrawlerFound(false);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            } catch (Exception e) {
                // 기타 예외 (connection refused, timeout 등) = 크롤링 실패 -> crawler_found = false, ready = false
                System.err.println("❌ Crawling failed for '" + place.getName() + "' due to error: " + e.getMessage());
                e.printStackTrace();
                place.setCrawlerFound(false);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<Place> placeWriter() {
        return chunk -> {
            // Spring Batch 5.x uses Chunk instead of List
            placeRepository.saveAll(chunk.getItems());
            // Log batch write success
            System.out.println("💾 Saved batch of " + chunk.getItems().size() + " places to database");
        };
    }

    private String prepareReviewSnippet(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "리뷰 정보 없음";
        }
        int limit = Math.min(reviews.size(), 10);
        return String.join("\n", reviews.subList(0, limit));
    }
}
