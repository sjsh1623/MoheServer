package com.mohe.spring.service.livemode;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.dto.crawling.CrawlingResponse;
import com.mohe.spring.dto.embedding.EmbeddingResponse;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceBusinessHour;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.entity.PlaceReview;
import com.mohe.spring.entity.PlaceSns;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.EmbeddingClient;
import com.mohe.spring.service.KeywordEmbeddingSaveService;
import com.mohe.spring.service.OpenAiDescriptionService;
import com.mohe.spring.service.crawling.CrawlingService;
import com.mohe.spring.service.image.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Live Mode 서비스
 * 사용자 조회 시점에 ready=false인 장소를 실시간으로 처리합니다.
 *
 * 처리 과정:
 * 1. 크롤링 + AI 요약 + 이미지 저장 (UpdateCrawledDataJob 로직)
 * 2. 벡터화 (VectorEmbeddingJob 로직)
 * 3. ready=true 설정
 */
@Service
@ConditionalOnProperty(name = "live.mode.enabled", havingValue = "true")
public class LiveModeService {

    private static final Logger logger = LoggerFactory.getLogger(LiveModeService.class);

    private final CrawlingService crawlingService;
    private final OpenAiDescriptionService openAiDescriptionService;
    private final ImageService imageService;
    private final EmbeddingClient embeddingClient;
    private final KeywordEmbeddingSaveService embeddingSaveService;
    private final PlaceRepository placeRepository;

    @Value("${live.mode.timeout:120000}")
    private long liveModeTimeout;

    // Caffeine Cache: 중복 처리 방지
    private final Cache<Long, ProcessingStatus> processingCache;

    public LiveModeService(
        CrawlingService crawlingService,
        OpenAiDescriptionService openAiDescriptionService,
        ImageService imageService,
        EmbeddingClient embeddingClient,
        KeywordEmbeddingSaveService embeddingSaveService,
        PlaceRepository placeRepository,
        @Value("${live.mode.cache.ttl:3600}") int cacheTtl,
        @Value("${live.mode.cache.max-size:1000}") long cacheMaxSize
    ) {
        this.crawlingService = crawlingService;
        this.openAiDescriptionService = openAiDescriptionService;
        this.imageService = imageService;
        this.embeddingClient = embeddingClient;
        this.embeddingSaveService = embeddingSaveService;
        this.placeRepository = placeRepository;

        // Caffeine Cache 초기화
        this.processingCache = Caffeine.newBuilder()
            .expireAfterWrite(cacheTtl, TimeUnit.SECONDS)
            .maximumSize(cacheMaxSize)
            .build();

        logger.info("🚀 LiveModeService initialized - timeout: {}ms, cache TTL: {}s, max size: {}",
            liveModeTimeout, cacheTtl, cacheMaxSize);
    }

    /**
     * 실시간으로 장소 데이터를 완전히 처리
     * @param place 처리할 Place 엔티티
     * @return 처리 완료된 Place (실패 시 원본 반환)
     */
    public Place processPlaceRealtime(Place place) {
        if (place == null || place.getId() == null) {
            return place;
        }

        // 이미 ready=true면 처리 불필요
        if (Boolean.TRUE.equals(place.getReady())) {
            return place;
        }

        // 캐시 체크: 이미 처리 중이거나 완료된 경우
        ProcessingStatus cachedStatus = processingCache.getIfPresent(place.getId());
        if (cachedStatus == ProcessingStatus.IN_PROGRESS) {
            logger.info("⏳ Place {} is already being processed by another request", place.getName());
            return place; // 처리 중이므로 원본 반환
        } else if (cachedStatus == ProcessingStatus.COMPLETED) {
            logger.info("✅ Place {} already processed (cached), fetching from DB", place.getName());
            return placeRepository.findById(place.getId()).orElse(place);
        }

        // 처리 시작 - 캐시에 IN_PROGRESS 등록
        processingCache.put(place.getId(), ProcessingStatus.IN_PROGRESS);
        logger.info("🎬 Starting real-time processing for place: {} (ID: {})", place.getName(), place.getId());

        try {
            // CompletableFuture로 비동기 처리 + 타임아웃 적용
            CompletableFuture<Place> future = CompletableFuture.supplyAsync(() -> {
                try {
                    return performFullProcessing(place);
                } catch (Exception e) {
                    logger.error("❌ Real-time processing failed for place: {}", place.getName(), e);
                    processingCache.put(place.getId(), ProcessingStatus.FAILED);
                    return null;
                }
            });

            // 타임아웃 적용
            Place result = future.get(liveModeTimeout, TimeUnit.MILLISECONDS);

            if (result != null) {
                processingCache.put(place.getId(), ProcessingStatus.COMPLETED);
                logger.info("✅ Real-time processing completed for place: {} (ready=true)", result.getName());
                return result;
            } else {
                logger.warn("⚠️ Real-time processing returned null for place: {}", place.getName());
                processingCache.invalidate(place.getId());
                return place;
            }

        } catch (TimeoutException e) {
            logger.warn("⏱️ Live mode timeout ({} ms) for place: {} - returning partial data",
                liveModeTimeout, place.getName());
            processingCache.invalidate(place.getId());
            return place; // 타임아웃 시 부분 데이터 반환
        } catch (Exception e) {
            logger.error("❌ Live mode processing error for place: {}", place.getName(), e);
            processingCache.invalidate(place.getId());
            return place;
        }
    }

    /**
     * 전체 처리 파이프라인 수행
     * Step 1: 크롤링 + AI 요약 + 이미지
     * Step 2: 벡터화
     * Step 3: ready=true 설정
     */
    @Transactional
    protected Place performFullProcessing(Place place) {
        long startTime = System.currentTimeMillis();

        // Step 1: 크롤링 + AI 요약 + 이미지 저장
        Place processedPlace = performCrawlingAndAI(place);
        if (processedPlace == null) {
            logger.error("❌ Step 1 (Crawling + AI) failed for place: {}", place.getName());
            return null;
        }

        // Step 2: 벡터화
        boolean vectorized = performVectorization(processedPlace);
        if (!vectorized) {
            logger.error("❌ Step 2 (Vectorization) failed for place: {}", processedPlace.getName());
            return null;
        }

        // Step 3: ready=true 설정 및 저장
        processedPlace.setReady(true);

        // 배치와 동일한 저장 로그
        logger.info("📝 Starting to save place '{}'...", processedPlace.getName());
        try {
            Place savedPlace = placeRepository.saveAndFlush(processedPlace);
            logger.info("💾 Saved place '{}' (ID: {}, crawler_found={}, ready={}) to database",
                savedPlace.getName(),
                savedPlace.getId(),
                savedPlace.getCrawlerFound(),
                savedPlace.getReady());

            long elapsedTime = System.currentTimeMillis() - startTime;
            logger.info("✅ Successfully saved and completed processing for '{}' in {} ms",
                savedPlace.getName(), elapsedTime);

            return savedPlace;
        } catch (Exception e) {
            logger.error("❌ Failed to save place '{}': {}", processedPlace.getName(), e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Step 1: 크롤링 + AI 요약 + 이미지 저장
     * (UpdateCrawledDataJobConfig의 placeProcessor 로직 재사용)
     */
    @Transactional
    protected Place performCrawlingAndAI(Place place) {
        try {
            // Get search query from PlaceDescription if available
            String searchQuery = place.getRoadAddress();
            if (!place.getDescriptions().isEmpty()) {
                String savedSearchQuery = place.getDescriptions().get(0).getSearchQuery();
                if (savedSearchQuery != null && !savedSearchQuery.isEmpty()) {
                    searchQuery = savedSearchQuery;
                }
            }

            logger.info("🔍 Starting crawl for '{}' with query: '{}'", place.getName(), searchQuery);
            CrawlingResponse<CrawledDataDto> response = crawlingService.crawlPlaceData(searchQuery, place.getName()).block();

            if (response == null || response.getData() == null) {
                logger.error("❌ Crawling failed for '{}' - null response from crawler", place.getName());
                place.setCrawlerFound(false);
                place.setReady(false);
                placeRepository.save(place);
                logger.info("💾 Saved place '{}' with crawler_found=false to database", place.getName());
                return null;
            }

            CrawledDataDto crawledData = response.getData();
            logger.info("📥 Crawl response received for '{}'", place.getName());

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

            // Check if AI summary is available
            String aiSummaryText = "";
            if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
                aiSummaryText = String.join("\n", crawledData.getAiSummary());
            }

            // Fallback logic: AI summary -> original description -> reviews
            String textForKeywords;
            if (aiSummaryText != null && !aiSummaryText.trim().isEmpty()) {
                textForKeywords = aiSummaryText;
            } else if (crawledData.getOriginalDescription() != null && !crawledData.getOriginalDescription().trim().isEmpty()) {
                textForKeywords = crawledData.getOriginalDescription();
            } else if (crawledData.getReviews() != null && !crawledData.getReviews().isEmpty()) {
                int reviewCount = Math.min(crawledData.getReviews().size(), 3);
                textForKeywords = String.join("\n", crawledData.getReviews().subList(0, reviewCount));
            } else {
                textForKeywords = null;
            }

            // Validate that we have some text to work with
            if (textForKeywords == null || textForKeywords.trim().isEmpty()) {
                logger.error("⚠️ Lack of information for '{}'", place.getName());
                place.setCrawlerFound(true);
                place.setReady(false);
                placeRepository.save(place);
                return null;
            }

            description.setAiSummary(sanitizeText(aiSummaryText));
            description.setSearchQuery(sanitizeText(searchQuery));

            // Generate Mohe description using OpenAI
            String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
            logger.info("🤖 Generating OpenAI description for '{}'...", place.getName());

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

            logger.info("✅ OpenAI description generated for '{}'", place.getName());

            // CRITICAL: mohe_description must NEVER be empty
            if (moheDescription == null || moheDescription.trim().isEmpty() || moheDescription.equals("AI 설명을 생성할 수 없습니다.")) {
                logger.warn("⚠️ OpenAI description generation failed, using fallback");
                String fallbackDescription = textForKeywords;
                if (fallbackDescription.length() > 150) {
                    int lastPeriod = Math.max(fallbackDescription.substring(0, 150).lastIndexOf('.'),
                                            fallbackDescription.substring(0, 150).lastIndexOf('!'));
                    lastPeriod = Math.max(lastPeriod, fallbackDescription.substring(0, 150).lastIndexOf('?'));

                    if (lastPeriod > 50) {
                        fallbackDescription = fallbackDescription.substring(0, lastPeriod + 1).trim();
                    } else {
                        fallbackDescription = fallbackDescription.substring(0, 147).trim() + "...";
                    }
                }
                moheDescription = fallbackDescription;
            }

            if (moheDescription == null || moheDescription.trim().isEmpty()) {
                moheDescription = place.getName() + "에 대한 정보입니다.";
            }

            description.setMoheDescription(sanitizeText(moheDescription));
            place.getDescriptions().add(description);

            // Validate keywords
            if (keywords.isEmpty() || keywords.size() != 9) {
                logger.warn("⚠️ Keyword extraction issue, using fallback");
                List<String> fallbackKeywords = new ArrayList<>();
                if (place.getCategory() != null && !place.getCategory().isEmpty()) {
                    fallbackKeywords.addAll(place.getCategory());
                }
                while (fallbackKeywords.size() < 9) {
                    fallbackKeywords.add("장소");
                }
                keywords = fallbackKeywords.subList(0, 9);
            }

            place.setKeyword(keywords);

            // Download and save images
            place.getImages().clear();
            if (crawledData.getImageUrls() != null && !crawledData.getImageUrls().isEmpty()) {
                logger.info("📸 Downloading {} images...", crawledData.getImageUrls().size());
                List<String> savedImagePaths = imageService.downloadAndSaveImages(
                    place.getId(),
                    place.getName(),
                    crawledData.getImageUrls()
                );
                logger.info("✅ Saved {} images", savedImagePaths.size());

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

                    try {
                        if (entry.getValue().getOpen() != null && !entry.getValue().getOpen().isEmpty()) {
                            businessHour.setOpen(LocalTime.parse(entry.getValue().getOpen()));
                        }
                        if (entry.getValue().getClose() != null && !entry.getValue().getClose().isEmpty()) {
                            businessHour.setClose(LocalTime.parse(entry.getValue().getClose()));
                        }
                    } catch (Exception e) {
                        logger.error("Failed to parse business hours: {}", e.getMessage());
                    }

                    businessHour.setDescription(sanitizeText(entry.getValue().getDescription()));
                    businessHour.setIsOperating(entry.getValue().isOperating());

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
                int reviewCount = Math.min(crawledData.getReviews().size(), 10);
                for (int i = 0; i < reviewCount; i++) {
                    String reviewText = crawledData.getReviews().get(i);
                    if (reviewText != null && !reviewText.trim().isEmpty()) {
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
            }

            place.setCrawlerFound(true);
            place.setReady(false); // Will be set to true after vectorization

            // ✅ Success logging (배치와 동일)
            logger.info("✅ Successfully crawled '{}' - " +
                "Reviews: {}, " +
                "Images: {}, " +
                "Keywords: {}, " +
                "Parking: {}, " +
                "Pet-friendly: {}, " +
                "crawler_found=true, ready=false (awaiting vectorization)",
                place.getName(),
                place.getReviewCount(),
                place.getImages().size(),
                String.join(", ", place.getKeyword()),
                place.getParkingAvailable() != null ? place.getParkingAvailable() : "Unknown",
                place.getPetFriendly() != null ? place.getPetFriendly() : "Unknown");

            return place;

        } catch (org.springframework.web.reactive.function.client.WebClientResponseException e) {
            // HTTP 에러 = 크롤링 실패 -> crawler_found = false, ready = false
            if (e.getStatusCode().value() == 404) {
                logger.error("❌ Crawling failed for '{}' - not found by crawler (404)", place.getName());
            } else {
                logger.error("❌ Crawling failed for '{}' - crawler server error: {}", place.getName(), e.getStatusCode());
            }
            place.setCrawlerFound(false);
            place.setReady(false);
            placeRepository.save(place);
            return null;
        } catch (Exception e) {
            // 기타 예외 (connection refused, timeout 등) = 크롤링 실패 -> crawler_found = false, ready = false
            logger.error("❌ Crawling failed for '{}' due to error: {}", place.getName(), e.getMessage());
            e.printStackTrace();
            place.setCrawlerFound(false);
            place.setReady(false);
            placeRepository.save(place);
            return null;
        }
    }

    /**
     * Step 2: 벡터화
     * (VectorEmbeddingJobConfig의 vectorEmbeddingProcessor 로직 재사용)
     */
    @Transactional
    protected boolean performVectorization(Place place) {
        try {
            // Check if place has mohe_description
            if (place.getDescriptions().isEmpty()) {
                logger.error("⚠️ No description found for '{}'", place.getName());
                return false;
            }

            String moheDescription = place.getDescriptions().get(0).getMoheDescription();
            if (moheDescription == null || moheDescription.trim().isEmpty()) {
                logger.error("⚠️ Empty mohe_description for '{}'", place.getName());
                return false;
            }

            logger.info("🧮 Starting vectorization for '{}'...", place.getName());

            // Get keywords
            List<String> existingKeywords = place.getKeyword();
            if (existingKeywords == null || existingKeywords.isEmpty()) {
                logger.error("⚠️ No keywords found for '{}'", place.getName());
                return false;
            }

            List<String> keywordsToProcess = existingKeywords.size() > 9
                ? existingKeywords.subList(0, 9)
                : existingKeywords;

            logger.info("🔑 Processing {} keywords for '{}'", keywordsToProcess.size(), place.getName());

            // Delete existing embeddings
            embeddingSaveService.deleteEmbeddingsForPlace(place.getId());

            // Call embedding service
            EmbeddingResponse response = embeddingClient.getEmbeddings(keywordsToProcess);

            if (!response.hasValidEmbeddings()) {
                logger.error("⚠️ No valid embeddings returned for '{}'", place.getName());
                return false;
            }

            List<float[]> embeddings = response.getEmbeddingsAsFloatArrays();
            logger.info("✅ Received {} embeddings for '{}'", embeddings.size(), place.getName());

            // Validate embeddings
            int validEmbeddings = 0;
            for (float[] embedding : embeddings) {
                boolean isNonZero = false;
                for (float v : embedding) {
                    if (v != 0.0f) {
                        isNonZero = true;
                        break;
                    }
                }
                if (isNonZero) validEmbeddings++;
            }

            if (validEmbeddings == 0) {
                logger.error("⚠️ All embeddings are zero vectors for '{}'", place.getName());
                return false;
            }

            // Save embeddings
            int savedCount = embeddingSaveService.saveEmbeddings(
                place.getId(),
                keywordsToProcess,
                embeddings
            );

            logger.info("💾 Saved {} embeddings for place_id={}", savedCount, place.getId());

            // ✅ Success logging (배치와 동일)
            logger.info("✅ Successfully vectorized '{}' - " +
                "Keywords: {}, " +
                "Vector dimension: 1792, " +
                "Saved {} embeddings, " +
                "ready=true",
                place.getName(),
                String.join(", ", keywordsToProcess),
                savedCount);

            return true;
        } catch (Exception e) {
            logger.error("❌ Vectorization failed for '{}' due to error: {}", place.getName(), e.getMessage());
            e.printStackTrace();
            // Keep crawler_found=true, ready=false
            placeRepository.save(place);
            return false;
        }
    }

    private String prepareReviewSnippet(List<String> reviews) {
        if (reviews == null || reviews.isEmpty()) {
            return "리뷰 정보 없음";
        }
        int limit = Math.min(reviews.size(), 10);
        return String.join("\n", reviews.subList(0, limit));
    }

    private String sanitizeText(String text) {
        if (text == null) {
            return null;
        }

        return text.replace("\u0000", "")
                  .replace("\u0001", "")
                  .replace("\u0002", "")
                  .replace("\u0003", "")
                  .replace("\u0004", "")
                  .replace("\u0005", "")
                  .replace("\u0006", "")
                  .replace("\u0007", "")
                  .replace("\u0008", "")
                  .replace("\u000B", "")
                  .replace("\u000C", "")
                  .replace("\u000E", "")
                  .replace("\u000F", "")
                  .replace("\u0010", "")
                  .replace("\u0011", "")
                  .replace("\u0012", "")
                  .replace("\u0013", "")
                  .replace("\u0014", "")
                  .replace("\u0015", "")
                  .replace("\u0016", "")
                  .replace("\u0017", "")
                  .replace("\u0018", "")
                  .replace("\u0019", "")
                  .replace("\u001A", "")
                  .replace("\u001B", "")
                  .replace("\u001C", "")
                  .replace("\u001D", "")
                  .replace("\u001E", "")
                  .replace("\u001F", "");
    }
}
