package com.mohe.spring.batch.job;

import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceBusinessHour;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.entity.PlaceSns;
import com.mohe.spring.repository.PlaceRepository;
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
    private final ImageService imageService;
    private final PlaceRepository placeRepository;

    public UpdateCrawledDataJobConfig(CrawlingService crawlingService, OllamaService ollamaService, ImageService imageService, PlaceRepository placeRepository) {
        this.crawlingService = crawlingService;
        this.ollamaService = ollamaService;
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
        reader.setMethodName("findByReady");
        reader.setArguments(List.of(false));
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

            // Check if AI summary is available, otherwise use original description
            String aiSummaryText = "";
            if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                aiSummaryText = String.join("\n", crawledData.getAiSummary());
            }

            // If AI summary is empty, fall back to original description
            String textForOllama;
            if (aiSummaryText.trim().isEmpty()) {
                textForOllama = crawledData.getOriginalDescription();
                System.out.println("⚠️ No AI summary for '" + place.getName() + "', using original description instead");
            } else {
                textForOllama = aiSummaryText;
            }

            // Validate that we have some text to work with
            if (textForOllama == null || textForOllama.trim().isEmpty()) {
                System.err.println("Skipping place '" + place.getName() + "' - no description text available (both AI summary and original description are empty)");
                return null;
            }

            description.setAiSummary(aiSummaryText);
            description.setSearchQuery(searchQuery);

            // Generate Mohe description using Ollama
            String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
            System.out.println("🤖 Generating Ollama description for '" + place.getName() + "'...");
            String moheDescription = ollamaService.generateMoheDescription(
                textForOllama,
                categoryStr,
                place.getPetFriendly() != null ? place.getPetFriendly() : false
            );
            System.out.println("✅ Ollama description generated for '" + place.getName() + "'");

            // CRITICAL: ollama_description must NEVER be empty
            // If Ollama generation failed, use the original text as fallback
            if (moheDescription == null || moheDescription.trim().isEmpty() || moheDescription.equals("AI 설명을 생성할 수 없습니다.")) {
                System.err.println("⚠️ Ollama description generation failed for '" + place.getName() + "', using fallback description");

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
                System.err.println("Skipping place '" + place.getName() + "' - Ollama keyword generation failed (all default)");
                return null; // Skip this item - don't save to DB
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
                System.err.println("Skipping place '" + place.getName() + "' - Ollama vectorization failed (default empty vector)");
                return null; // Skip this item - don't save to DB
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
                // 404 에러 = 크롤러에서 해당 장소를 찾을 수 없음 -> crawler_found = false
                if (e.getStatusCode().value() == 404) {
                    System.err.println("Skipping place '" + place.getName() + "' - not found by crawler (404)");
                    place.setCrawlerFound(false);
                    placeRepository.save(place);
                } else {
                    // 다른 HTTP 에러 (500, 503 등) = 크롤링 서버 문제 -> crawler_found = null (변경하지 않음)
                    System.err.println("Skipping place '" + place.getName() + "' due to crawler server error: " + e.getStatusCode());
                }
                return null; // Returning null will skip this item
            } catch (Exception e) {
                // 기타 예외 (connection refused, timeout 등) = 크롤링 서버 죽음 -> crawler_found = null (변경하지 않음)
                System.err.println("Skipping place '" + place.getName() + "' due to error: " + e.getMessage());
                e.printStackTrace();
                return null; // Returning null will skip this item
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
}