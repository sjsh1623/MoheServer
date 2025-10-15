package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.DistributedPlaceReader;
import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceBusinessHour;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.entity.PlaceReview;
import com.mohe.spring.entity.PlaceSns;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.DistributedJobLockService;
import com.mohe.spring.service.OllamaService;
import com.mohe.spring.service.OpenAiDescriptionService;
import com.mohe.spring.service.crawling.CrawlingService;
import com.mohe.spring.service.image.DistributedImageService;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * Distributed Crawling Job Configuration
 *
 * <p>여러 컴퓨터에서 동시에 실행 가능한 크롤링 배치 작업입니다.</p>
 *
 * <h3>특징</h3>
 * <ul>
 *   <li>락(Lock) 메커니즘으로 중복 처리 방지</li>
 *   <li>자동 청크 할당 (각 워커가 다른 청크 처리)</li>
 *   <li>데드 워커 자동 감지 및 복구</li>
 * </ul>
 *
 * <h3>사용법</h3>
 * <pre>
 * # Mac Mini에서 실행
 * ./gradlew bootRun --args='--spring.batch.job.names=distributedCrawlingJob'
 *
 * # MacBook Pro에서 동시 실행 (다른 청크 자동 할당)
 * ./gradlew bootRun --args='--spring.batch.job.names=distributedCrawlingJob'
 * </pre>
 */
@Configuration
public class DistributedCrawlingJobConfig {

    private final CrawlingService crawlingService;
    private final OllamaService ollamaService;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final DistributedImageService distributedImageService;
    private final PlaceRepository placeRepository;
    private final DistributedJobLockService lockService;

    @Value("${batch.chunk-size:10}")
    private int chunkSize;

    public DistributedCrawlingJobConfig(
        CrawlingService crawlingService,
        OllamaService ollamaService,
        OpenAiDescriptionService openAiDescriptionService,
        DistributedImageService distributedImageService,
        PlaceRepository placeRepository,
        DistributedJobLockService lockService
    ) {
        this.crawlingService = crawlingService;
        this.ollamaService = ollamaService;
        this.openAiDescriptionService = openAiDescriptionService;
        this.distributedImageService = distributedImageService;
        this.placeRepository = placeRepository;
        this.lockService = lockService;
    }

    @Bean
    public Job distributedCrawlingJob(JobRepository jobRepository, Step distributedCrawlingStep) {
        return new JobBuilder("distributedCrawlingJob", jobRepository)
                .start(distributedCrawlingStep)
                .build();
    }

    @Bean
    public Step distributedCrawlingStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        ItemReader<Place> distributedPlaceReader,
        ItemProcessor<Place, Place> distributedPlaceProcessor,
        ItemWriter<Place> distributedPlaceWriter
    ) {
        return new StepBuilder("distributedCrawlingStep", jobRepository)
                .<Place, Place>chunk(chunkSize, transactionManager)
                .reader(distributedPlaceReader)
                .processor(distributedPlaceProcessor)
                .writer(distributedPlaceWriter)
                .faultTolerant()
                .skip(org.springframework.web.reactive.function.client.WebClientResponseException.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public ItemReader<Place> distributedPlaceReader() {
        return new DistributedPlaceReader(
            placeRepository,
            lockService,
            "distributedCrawlingJob",
            chunkSize
        );
    }

    @Bean
    public ItemProcessor<Place, Place> distributedPlaceProcessor() {
        return place -> {
            try {
                // Get search query
                String searchQuery = place.getRoadAddress();
                if (!place.getDescriptions().isEmpty()) {
                    String savedSearchQuery = place.getDescriptions().get(0).getSearchQuery();
                    if (savedSearchQuery != null && !savedSearchQuery.isEmpty()) {
                        searchQuery = savedSearchQuery;
                    }
                }

                System.out.println("🔍 [" + lockService.getWorkerHostname() + "] Crawling: " +
                    place.getName() + " (id=" + place.getId() + ")");

                CrawledDataDto crawledData = crawlingService.crawlPlaceData(searchQuery, place.getName())
                    .block().getData();

                // Update place with crawled data (same logic as UpdateCrawledDataJobConfig)
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

                // Determine text for Ollama
                String aiSummaryText = "";
                if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                    aiSummaryText = String.join("\n", crawledData.getAiSummary());
                }

                String textForOllama;
                if (aiSummaryText != null && !aiSummaryText.trim().isEmpty()) {
                    textForOllama = aiSummaryText;
                } else if (crawledData.getOriginalDescription() != null &&
                           !crawledData.getOriginalDescription().trim().isEmpty()) {
                    textForOllama = crawledData.getOriginalDescription();
                } else if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                    int reviewCount = Math.min(crawledData.getReviews().size(), 3);
                    textForOllama = String.join("\n", crawledData.getReviews().subList(0, reviewCount));
                } else {
                    textForOllama = null;
                }

                if (textForOllama == null || textForOllama.trim().isEmpty()) {
                    System.err.println("⚠️ No description available for " + place.getName());
                    place.setCrawlerFound(true);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }

                description.setAiSummary(aiSummaryText);
                description.setSearchQuery(searchQuery);

                // Generate Mohe description using OpenAI
                String categoryStr = place.getCategory() != null ?
                    String.join(",", place.getCategory()) : "";
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

                // Fallback if generation failed
                if (moheDescription == null || moheDescription.trim().isEmpty() ||
                    moheDescription.equals("AI 설명을 생성할 수 없습니다.")) {
                    String fallback = textForOllama;
                    if (fallback.length() > 150) {
                        fallback = fallback.substring(0, 147).trim() + "...";
                    }
                    moheDescription = fallback;
                }

                if (moheDescription == null || moheDescription.trim().isEmpty()) {
                    moheDescription = place.getName() + "에 대한 정보입니다.";
                }

                description.setOllamaDescription(moheDescription);
                place.getDescriptions().add(description);

                // Generate keywords
                String[] keywords = ollamaService.generateKeywords(
                    textForOllama,
                    categoryStr,
                    place.getPetFriendly() != null ? place.getPetFriendly() : false
                );

                // Validate keywords
                boolean allDefault = true;
                for (int i = 0; i < keywords.length; i++) {
                    if (!keywords[i].equals("키워드" + (i + 1))) {
                        allDefault = false;
                        break;
                    }
                }

                if (allDefault) {
                    System.err.println("⚠️ Keyword generation failed for " + place.getName());
                    place.setCrawlerFound(true);
                    place.setReady(false);
                    placeRepository.save(place);
                    return null;
                }

                place.setKeyword(Arrays.asList(keywords));

                // Vectorize keywords
                float[] keywordVector = ollamaService.vectorizeKeywords(keywords);
                boolean isDefaultVector = true;
                for (float v : keywordVector) {
                    if (v != 0.0f) {
                        isDefaultVector = false;
                        break;
                    }
                }

                if (isDefaultVector) {
                    System.err.println("⚠️ Vectorization failed for " + place.getName());
                    place.setCrawlerFound(true);
                    place.setReady(false);
                    placeRepository.save(place);
                    return null;
                }

                place.setKeywordVector(Arrays.toString(keywordVector));

                // Download and save images (using distributed image service)
                place.getImages().clear();
                if (crawledData.getImageUrls() != null && !crawledData.getImageUrls().isEmpty()) {
                    List<String> savedImagePaths = distributedImageService.downloadAndSaveImages(
                        place.getId(),
                        place.getName(),
                        crawledData.getImageUrls()
                    );

                    for (int i = 0; i < savedImagePaths.size(); i++) {
                        PlaceImage placeImage = new PlaceImage();
                        placeImage.setPlace(place);
                        placeImage.setUrl(savedImagePaths.get(i));
                        placeImage.setOrderIndex(i + 1);
                        place.getImages().add(placeImage);
                    }
                }

                // Business hours
                place.getBusinessHours().clear();
                if (crawledData.getBusinessHours() != null &&
                    crawledData.getBusinessHours().getWeekly() != null) {
                    for (Map.Entry<String, com.mohe.spring.dto.crawling.WeeklyHoursDto> entry :
                         crawledData.getBusinessHours().getWeekly().entrySet()) {
                        PlaceBusinessHour businessHour = new PlaceBusinessHour();
                        businessHour.setPlace(place);
                        businessHour.setDayOfWeek(entry.getKey());

                        try {
                            if (entry.getValue().getOpen() != null &&
                                !entry.getValue().getOpen().isEmpty()) {
                                businessHour.setOpen(LocalTime.parse(entry.getValue().getOpen()));
                            }
                            if (entry.getValue().getClose() != null &&
                                !entry.getValue().getClose().isEmpty()) {
                                businessHour.setClose(LocalTime.parse(entry.getValue().getClose()));
                            }
                        } catch (Exception e) {
                            System.err.println("Failed to parse time: " + e.getMessage());
                        }

                        businessHour.setDescription(entry.getValue().getDescription());
                        businessHour.setIsOperating(entry.getValue().isOperating());

                        if (crawledData.getBusinessHours().getLastOrderMinutes() != null) {
                            businessHour.setLastOrderMinutes(
                                crawledData.getBusinessHours().getLastOrderMinutes());
                        }

                        place.getBusinessHours().add(businessHour);
                    }
                }

                // SNS
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

                // Reviews
                place.getReviews().clear();
                if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                    int reviewCount = Math.min(crawledData.getReviews().size(), 10);
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
                }

                // Mark as ready
                place.setReady(true);
                place.setCrawlerFound(true);

                System.out.println("✅ [" + lockService.getWorkerHostname() + "] Completed: " +
                    place.getName());

                return place;
            } catch (Exception e) {
                System.err.println("❌ Error processing " + place.getName() + ": " + e.getMessage());
                place.setCrawlerFound(false);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }
        };
    }

    @Bean
    public ItemWriter<Place> distributedPlaceWriter() {
        return chunk -> {
            placeRepository.saveAll(chunk.getItems());
            System.out.println("💾 [" + lockService.getWorkerHostname() + "] Saved " +
                chunk.getItems().size() + " places");
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
