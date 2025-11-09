package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.UpdateCrawledDataReader;
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
import com.mohe.spring.service.KeywordEmbeddingService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.integration.async.AsyncItemProcessor;
import org.springframework.batch.integration.async.AsyncItemWriter;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

@Configuration
public class UpdateCrawledDataJobConfig {

    private final CrawlingService crawlingService;
    private final KeywordEmbeddingService keywordEmbeddingService;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final ImageService imageService;
    private final PlaceRepository placeRepository;

    @Value("${batch.async.core-pool-size:10}")
    private int corePoolSize;

    @Value("${batch.async.max-pool-size:20}")
    private int maxPoolSize;

    @Value("${batch.async.queue-capacity:100}")
    private int queueCapacity;

    @Value("${batch.chunk-size:20}")
    private int chunkSize;

    public UpdateCrawledDataJobConfig(
        CrawlingService crawlingService,
        KeywordEmbeddingService keywordEmbeddingService,
        OpenAiDescriptionService openAiDescriptionService,
        ImageService imageService,
        PlaceRepository placeRepository
    ) {
        this.crawlingService = crawlingService;
        this.keywordEmbeddingService = keywordEmbeddingService;
        this.openAiDescriptionService = openAiDescriptionService;
        this.imageService = imageService;
        this.placeRepository = placeRepository;
    }

    /**
     * 비동기 병렬 처리를 위한 TaskExecutor
     * - corePoolSize: 최소 스레드 풀 크기 (기본 10)
     * - maxPoolSize: 최대 스레드 풀 크기 (기본 20)
     * - queueCapacity: 대기 큐 크기 (기본 100)
     */
    @Bean(name = "batchTaskExecutor")
    public TaskExecutor batchTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(corePoolSize);
        executor.setMaxPoolSize(maxPoolSize);
        executor.setQueueCapacity(queueCapacity);
        executor.setThreadNamePrefix("batch-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        System.out.println("🚀 Batch TaskExecutor initialized: core=" + corePoolSize +
                         ", max=" + maxPoolSize + ", queue=" + queueCapacity);
        return executor;
    }

    @Bean
    public Job updateCrawledDataJob(JobRepository jobRepository, Step updateCrawledDataStep) {
        return new JobBuilder("updateCrawledDataJob", jobRepository)
                .start(updateCrawledDataStep)
                .build();
    }

    @Bean
    public Step updateCrawledDataStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Place> placeReader,
            ItemProcessor<Place, Place> placeProcessor,
            ItemWriter<Place> placeWriter,
            TaskExecutor batchTaskExecutor
    ) {
        // AsyncItemProcessor 설정
        AsyncItemProcessor<Place, Place> asyncItemProcessor = new AsyncItemProcessor<>();
        asyncItemProcessor.setDelegate(placeProcessor);
        asyncItemProcessor.setTaskExecutor(batchTaskExecutor);

        // AsyncItemWriter 설정
        AsyncItemWriter<Place> asyncItemWriter = new AsyncItemWriter<>();
        asyncItemWriter.setDelegate(placeWriter);

        try {
            asyncItemProcessor.afterPropertiesSet();
            asyncItemWriter.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize async processors", e);
        }

        System.out.println("🔧 Async batch step configured: chunkSize=" + chunkSize);

        return new StepBuilder("updateCrawledDataStep", jobRepository)
                .<Place, Future<Place>>chunk(chunkSize, transactionManager)
                .reader(placeReader)
                .processor(asyncItemProcessor)
                .writer(asyncItemWriter)
                .faultTolerant()
                .skip(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
                .skip(org.hibernate.StaleStateException.class)
                .skip(org.springframework.dao.OptimisticLockingFailureException.class)
                .skipLimit(Integer.MAX_VALUE)
                .noRollback(org.springframework.orm.ObjectOptimisticLockingFailureException.class)
                .noRollback(org.hibernate.StaleStateException.class)
                .build();
    }

    @Bean
    public ItemReader<Place> placeReader() {
        // Use custom reader to avoid Hibernate HHH90003004 warning
        // Two-step approach: 1) Load IDs with pagination, 2) Load entities with collections
        return new UpdateCrawledDataReader(placeRepository, 10);
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
                var response = crawlingService.crawlPlaceData(searchQuery, place.getName()).block();

                if (response == null || response.getData() == null) {
                    System.err.println("❌ Crawling failed for '" + place.getName() + "' - null response from crawler");
                    place.setCrawlerFound(false);
                    place.setReady(false);
                    // Don't save here - will be saved by writer
                    return place;  // Return place so writer can save it
                }

                CrawledDataDto crawledData = response.getData();
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
            description.setOriginalDescription(sanitizeText(crawledData.getOriginalDescription()));

            // Check if AI summary is available, otherwise use original description, then reviews
            String aiSummaryText = "";
            if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                aiSummaryText = String.join("\n", crawledData.getAiSummary());
            }

            // Fallback logic: AI summary -> original description -> reviews
            String textForKeywords;
            if (aiSummaryText != null && !aiSummaryText.trim().isEmpty()) {
                textForKeywords = aiSummaryText;
                System.out.println("✅ Using AI summary for '" + place.getName() + "'");
            } else if (crawledData.getOriginalDescription() != null && !crawledData.getOriginalDescription().trim().isEmpty()) {
                textForKeywords = crawledData.getOriginalDescription();
                System.out.println("⚠️ No AI summary for '" + place.getName() + "', using original description instead");
            } else if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                // Use first 3 reviews as description source
                int reviewCount = Math.min(crawledData.getReviews().size(), 3);
                textForKeywords = String.join("\n", crawledData.getReviews().subList(0, reviewCount));
                System.out.println("⚠️ No AI summary or original description for '" + place.getName() + "', using reviews instead");
            } else {
                textForKeywords = null;
            }

            // Validate that we have some text to work with
            if (textForKeywords == null || textForKeywords.trim().isEmpty()) {
                System.err.println("⚠️ Lack of information for '" + place.getName() + "' - no description text available (AI summary, original description, and reviews are all empty)");
                // Crawling succeeded but lack of information -> crawler_found = true, ready = false
                place.setCrawlerFound(true);
                place.setReady(false);
                // Don't save here - will be saved by writer
                return place;  // Return place so writer can save it
            }

            description.setAiSummary(sanitizeText(aiSummaryText));
            description.setSearchQuery(sanitizeText(searchQuery));

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

            OpenAiDescriptionService.DescriptionResult descriptionResult = openAiDescriptionService.generateDescription(payload)
                .orElse(null);

            String moheDescription = descriptionResult != null ? descriptionResult.description() : null;
            List<String> keywords = descriptionResult != null ? descriptionResult.keywords() : List.of();

            System.out.println("✅ OpenAI description generated for '" + place.getName() + "': " + moheDescription);
            System.out.println("✅ OpenAI keywords extracted for '" + place.getName() + "': " + String.join(", ", keywords));

            // CRITICAL: mohe_description must NEVER be empty
            // If OpenAI generation failed, use the original text as fallback
            if (moheDescription == null || moheDescription.trim().isEmpty() || moheDescription.equals("AI 설명을 생성할 수 없습니다.")) {
                System.err.println("⚠️ OpenAI description generation failed for '" + place.getName() + "', using fallback description");

                // Use original description as fallback, truncate to reasonable length if needed
                String fallbackDescription = textForKeywords;
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

            description.setMoheDescription(sanitizeText(moheDescription));
            place.getDescriptions().add(description);

            // 📝 Log Mohe AI description before saving to database
            System.out.println("📝 Mohe AI Summary for '" + place.getName() + "' (will be saved to DB):");
            System.out.println("   " + moheDescription);

            // Validate keywords from OpenAI response - check if empty or invalid
            if (keywords.isEmpty() || keywords.size() != 9) {
                System.err.println("⚠️ AI issue for '" + place.getName() + "' - Keyword extraction failed (expected 9, got " + keywords.size() + ")");
                // Fallback: use basic keywords from category and place name
                System.out.println("🔑 Using fallback keywords for '" + place.getName() + "'...");
                List<String> fallbackKeywords = new ArrayList<>();
                if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                    fallbackKeywords.addAll(place.getCategory());
                }
                // Pad with generic keywords if needed
                while (fallbackKeywords.size() < 9) {
                    fallbackKeywords.add("장소");
                }
                keywords = fallbackKeywords.subList(0, 9);
                System.out.println("✅ Fallback keywords generated for '" + place.getName() + "': " + String.join(", ", keywords));

                // Validate fallback keywords - check if all are default placeholders
                boolean allKeywordsAreDefault = true;
                for (int i = 0; i < keywords.size(); i++) {
                    if (!keywords.get(i).equals("장소")) {
                        allKeywordsAreDefault = false;
                        break;
                    }
                }

                if (allKeywordsAreDefault) {
                    System.err.println("⚠️ AI issue for '" + place.getName() + "' - Fallback keyword generation also failed (all default)");
                    // Crawling succeeded but AI issue -> crawler_found = true, ready = false
                    place.setCrawlerFound(true);
                    place.setReady(false);
                    // Don't save here - will be saved by writer
                    return place;  // Return place so writer can save it
                }
            }

            place.setKeyword(keywords);

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

                    businessHour.setDescription(sanitizeText(entry.getValue().getDescription()));
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
                        // Sanitize review text to remove NULL bytes and other invalid characters
                        String sanitizedReviewText = sanitizeText(reviewText);
                        if (sanitizedReviewText != null && !sanitizedReviewText.trim().isEmpty()) {
                            PlaceReview review = new PlaceReview();
                            review.setPlace(place);
                            review.setReviewText(sanitizedReviewText);
                            review.setOrderIndex(i + 1);
                            place.getReviews().add(review);
                        }
                    }
                }
                System.out.println("✅ Saved " + place.getReviews().size() + " reviews for '" + place.getName() + "'");
            }

                // Mark place as crawler_found=true (description generated), but ready=false (vectorization not done yet)
                place.setCrawlerFound(true);
                place.setReady(false);

                // ✅ Success logging
                System.out.println("✅ Successfully crawled '" + place.getName() + "' - " +
                    "Reviews: " + place.getReviewCount() + ", " +
                    "Images: " + place.getImages().size() + ", " +
                    "Keywords: " + String.join(", ", place.getKeyword()) + ", " +
                    "Parking: " + (place.getParkingAvailable() != null ? place.getParkingAvailable() : "Unknown") + ", " +
                    "Pet-friendly: " + (place.getPetFriendly() != null ? place.getPetFriendly() : "Unknown") + ", " +
                    "crawler_found=true, ready=false (awaiting vectorization)");

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
                // Writer가 저장 처리
                return place;
            } catch (Exception e) {
                // 기타 예외 (connection refused, timeout 등) = 크롤링 실패 -> crawler_found = false, ready = false
                System.err.println("❌ Crawling failed for '" + place.getName() + "' due to error: " + e.getMessage());
                e.printStackTrace();
                place.setCrawlerFound(false);
                place.setReady(false);
                // Writer가 저장 처리
                return place;
            }
        };
    }

    @Bean
    public ItemWriter<Place> placeWriter() {
        return chunk -> {
            System.out.println("📝 Starting to save batch of " + chunk.getItems().size() + " places...");
            int savedCount = 0;

            for (Place place : chunk.getItems()) {
                // Refresh entity from DB to get managed state
                Place freshPlace = placeRepository.findById(place.getId())
                    .orElseThrow(() -> new IllegalStateException("Place not found: " + place.getId()));

                // Clear existing collections to avoid orphan removal conflicts
                freshPlace.getDescriptions().clear();
                freshPlace.getImages().clear();
                freshPlace.getBusinessHours().clear();
                freshPlace.getSns().clear();
                freshPlace.getReviews().clear();

                // Flush to ensure orphans are deleted before adding new items
                placeRepository.flush();

                // Update fields with new data
                updatePlaceFields(freshPlace, place);

                // Save the updated entity
                placeRepository.saveAndFlush(freshPlace);
                savedCount++;

                System.out.println("💾 [" + savedCount + "/" + chunk.getItems().size() + "] Saved place '" + freshPlace.getName() +
                    "' (ID: " + freshPlace.getId() + ", crawler_found=" + freshPlace.getCrawlerFound() +
                    ", ready=" + freshPlace.getReady() + ") to database");
            }

            System.out.println("✅ Successfully saved batch: " + savedCount + "/" + chunk.getItems().size() + " places written to database");
        };
    }

    /**
     * Update fresh entity with data from processed entity
     */
    private void updatePlaceFields(Place target, Place source) {
        target.setName(source.getName());
        target.setLatitude(source.getLatitude());
        target.setLongitude(source.getLongitude());
        target.setRoadAddress(source.getRoadAddress());
        target.setWebsiteUrl(source.getWebsiteUrl());
        target.setRating(source.getRating());
        target.setReviewCount(source.getReviewCount());
        target.setCategory(source.getCategory());
        target.setKeyword(source.getKeyword());
        target.setParkingAvailable(source.getParkingAvailable());
        target.setPetFriendly(source.getPetFriendly());
        target.setReady(source.getReady());
        target.setCrawlerFound(source.getCrawlerFound());

        // Copy collections
        if (source.getDescriptions() != null) {
            source.getDescriptions().forEach(desc -> {
                desc.setPlace(target);
                target.getDescriptions().add(desc);
            });
        }
        if (source.getImages() != null) {
            source.getImages().forEach(img -> {
                img.setPlace(target);
                target.getImages().add(img);
            });
        }
        if (source.getBusinessHours() != null) {
            source.getBusinessHours().forEach(hour -> {
                hour.setPlace(target);
                target.getBusinessHours().add(hour);
            });
        }
        if (source.getSns() != null) {
            source.getSns().forEach(s -> {
                s.setPlace(target);
                target.getSns().add(s);
            });
        }
        if (source.getReviews() != null) {
            source.getReviews().forEach(review -> {
                review.setPlace(target);
                target.getReviews().add(review);
            });
        }
    }

    private String prepareReviewSnippet(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "리뷰 정보 없음";
        }
        int limit = Math.min(reviews.size(), 10);
        return String.join("\n", reviews.subList(0, limit));
    }

    /**
     * Sanitize text to remove NULL bytes and other invalid characters for PostgreSQL
     * PostgreSQL does not allow NULL bytes (0x00) in UTF-8 strings
     *
     * @param text Input text that may contain invalid characters
     * @return Sanitized text safe for PostgreSQL storage
     */
    private String sanitizeText(String text) {
        if (text == null) {
            return null;
        }

        // Remove NULL bytes (0x00) which PostgreSQL rejects
        // Also remove other control characters that might cause issues
        String sanitized = text.replace("\u0000", "")  // NULL byte
                              .replace("\u0001", "")  // Start of heading
                              .replace("\u0002", "")  // Start of text
                              .replace("\u0003", "")  // End of text
                              .replace("\u0004", "")  // End of transmission
                              .replace("\u0005", "")  // Enquiry
                              .replace("\u0006", "")  // Acknowledge
                              .replace("\u0007", "")  // Bell
                              .replace("\u0008", "")  // Backspace
                              .replace("\u000B", "")  // Vertical tab
                              .replace("\u000C", "")  // Form feed
                              .replace("\u000E", "")  // Shift out
                              .replace("\u000F", "")  // Shift in
                              .replace("\u0010", "")  // Data link escape
                              .replace("\u0011", "")  // Device control 1
                              .replace("\u0012", "")  // Device control 2
                              .replace("\u0013", "")  // Device control 3
                              .replace("\u0014", "")  // Device control 4
                              .replace("\u0015", "")  // Negative acknowledge
                              .replace("\u0016", "")  // Synchronous idle
                              .replace("\u0017", "")  // End of transmission block
                              .replace("\u0018", "")  // Cancel
                              .replace("\u0019", "")  // End of medium
                              .replace("\u001A", "")  // Substitute
                              .replace("\u001B", "")  // Escape
                              .replace("\u001C", "")  // File separator
                              .replace("\u001D", "")  // Group separator
                              .replace("\u001E", "")  // Record separator
                              .replace("\u001F", ""); // Unit separator

        return sanitized;
    }
}
