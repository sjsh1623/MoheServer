package com.mohe.spring.service;

import com.mohe.spring.dto.embedding.BatchEmbeddingResult;
import com.mohe.spring.dto.embedding.EmbeddingResponse;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceKeywordEmbedding;
import com.mohe.spring.repository.PlaceKeywordEmbeddingRepository;
import com.mohe.spring.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Service for batch processing keyword embeddings for places
 * Processes places where crawler_found = true and generates vector embeddings for their keywords
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmbeddingBatchService {

    private final PlaceRepository placeRepository;
    private final PlaceKeywordEmbeddingRepository embeddingRepository;
    private final EmbeddingClient embeddingClient;

    private static final int BATCH_SIZE = 9; // Process 9 places at a time
    private static final int MAX_KEYWORDS_PER_PLACE = 9; // Maximum 9 keywords per place

    /**
     * Run the batch embedding process for all eligible places
     * Eligible: crawler_found = true
     *
     * @return BatchEmbeddingResult containing processing statistics
     */
    public BatchEmbeddingResult runBatchEmbedding() {
        log.info("[INFO] Start embedding batch process");
        long startTime = System.currentTimeMillis();

        BatchEmbeddingResult.BatchEmbeddingResultBuilder resultBuilder = BatchEmbeddingResult.builder();

        try {
            // Fetch places where crawler_found = true
            log.info("[INFO] Fetching places where crawler_found = true");
            List<Place> eligiblePlaces = fetchEligiblePlaces();

            log.info("[INFO] Found {} eligible places for embedding", eligiblePlaces.size());

            int totalPlaces = 0;
            int successfulPlaces = 0;
            int failedPlaces = 0;
            int skippedPlaces = 0;
            int totalEmbeddings = 0;

            // Process places in batches of BATCH_SIZE
            for (int i = 0; i < eligiblePlaces.size(); i += BATCH_SIZE) {
                int endIndex = Math.min(i + BATCH_SIZE, eligiblePlaces.size());
                List<Place> batch = eligiblePlaces.subList(i, endIndex);

                log.info("[INFO] Processing batch {}-{} of {} places",
                    i + 1, endIndex, eligiblePlaces.size());

                for (Place place : batch) {
                    totalPlaces++;

                    try {
                        int embeddingsCreated = processPlace(place);

                        if (embeddingsCreated > 0) {
                            successfulPlaces++;
                            totalEmbeddings += embeddingsCreated;
                            log.info("[INFO] Successfully processed place_id={} ({} keywords embedded)",
                                place.getId(), embeddingsCreated);
                        } else {
                            skippedPlaces++;
                            log.info("[INFO] Skipped place_id={} (no keywords)", place.getId());
                        }

                    } catch (Exception e) {
                        failedPlaces++;
                        log.error("[ERROR] Failed to process place_id={}: {}",
                            place.getId(), e.getMessage(), e);
                    }
                }

                // Log progress
                double progress = (double) totalPlaces / eligiblePlaces.size() * 100;
                log.info("[INFO] Progress: {}/{} places ({:.1f}%) - Success: {}, Failed: {}, Skipped: {}",
                    totalPlaces, eligiblePlaces.size(), progress,
                    successfulPlaces, failedPlaces, skippedPlaces);
            }

            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            log.info("✅ Embedding batch process completed successfully");
            log.info("[INFO] Total: {} | Success: {} | Failed: {} | Skipped: {} | Embeddings: {} | Time: {}ms",
                totalPlaces, successfulPlaces, failedPlaces, skippedPlaces,
                totalEmbeddings, processingTime);

            return resultBuilder
                .totalPlaces(totalPlaces)
                .successfulPlaces(successfulPlaces)
                .failedPlaces(failedPlaces)
                .skippedPlaces(skippedPlaces)
                .totalEmbeddings(totalEmbeddings)
                .processingTimeMs(processingTime)
                .build();

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            long processingTime = endTime - startTime;

            log.error("[ERROR] Batch embedding process failed: {}", e.getMessage(), e);

            return resultBuilder
                .totalPlaces(0)
                .successfulPlaces(0)
                .failedPlaces(0)
                .skippedPlaces(0)
                .totalEmbeddings(0)
                .processingTimeMs(processingTime)
                .errorMessage(e.getMessage())
                .build();
        }
    }

    /**
     * Fetch all places where crawler_found = true
     * Uses two-step query pattern to avoid LazyInitializationException:
     * Step 1: Fetch IDs with pagination
     * Step 2: Fetch individual Place entities by ID
     *
     * @Transactional ensures session remains open during fetch
     */
    @Transactional(readOnly = true)
    protected List<Place> fetchEligiblePlaces() {
        log.debug("[DEBUG] Fetching eligible place IDs where crawler_found = true");

        // Step 1: Fetch all place IDs in pages
        List<Long> allPlaceIds = new ArrayList<>();
        int pageNumber = 0;
        int pageSize = 100;

        Page<Long> idsPage;
        do {
            Pageable pageable = PageRequest.of(pageNumber, pageSize);
            idsPage = placeRepository.findPlaceIdsForKeywordEmbedding(pageable);
            allPlaceIds.addAll(idsPage.getContent());
            pageNumber++;

            log.debug("[DEBUG] Fetched {} place IDs (page {})", idsPage.getContent().size(), pageNumber);
        } while (idsPage.hasNext());

        log.debug("[DEBUG] Total {} eligible place IDs found", allPlaceIds.size());

        // Step 2: Fetch individual Place entities by ID
        // This ensures each Place is loaded in a fresh session
        List<Place> allPlaces = new ArrayList<>();
        for (Long placeId : allPlaceIds) {
            Optional<Place> placeOpt = placeRepository.findByIdForKeywordEmbedding(placeId);
            if (placeOpt.isPresent()) {
                Place place = placeOpt.get();

                // Force initialization of keyword field within transaction
                // This prevents LazyInitializationException later
                if (place.getKeyword() != null) {
                    place.getKeyword().size(); // Touch the collection to initialize it
                }

                allPlaces.add(place);
            }
        }

        log.debug("[DEBUG] Loaded {} Place entities", allPlaces.size());
        return allPlaces;
    }

    /**
     * Process a single place: generate embeddings for its keywords and save to database
     * Each place is processed in its own transaction to avoid rollback of entire batch
     *
     * @param place The place to process
     * @return Number of embeddings created
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int processPlace(Place place) {
        log.debug("[DEBUG] Processing place_id={} (keywords: {})", place.getId(),
            place.getKeyword() != null ? place.getKeyword().size() : 0);

        // Skip if no keywords
        List<String> keywords = place.getKeyword();
        if (keywords == null || keywords.isEmpty()) {
            log.debug("[DEBUG] Skipping place_id={} - no keywords", place.getId());
            return 0;
        }

        // Take only first MAX_KEYWORDS_PER_PLACE keywords
        List<String> keywordsToProcess = keywords.size() > MAX_KEYWORDS_PER_PLACE
            ? keywords.subList(0, MAX_KEYWORDS_PER_PLACE)
            : keywords;

        log.debug("[DEBUG] Processing {} keywords for place_id={}", keywordsToProcess.size(), place.getId());
        log.info("[INFO] 🔑 Keywords to embed: {}", keywordsToProcess);

        try {
            // Call embedding service
            log.info("[INFO] 🚀 Calling embedding service for place_id={} with {} keywords",
                place.getId(), keywordsToProcess.size());

            EmbeddingResponse response = embeddingClient.getEmbeddings(keywordsToProcess);

            // Log raw response for debugging
            log.info("[INFO] 📦 Embedding Response for place_id={}: hasValidEmbeddings={}, embeddingCount={}",
                place.getId(), response.hasValidEmbeddings(), response.getEmbeddingCount());

            if (response.getEmbeddings() != null) {
                log.info("[INFO] 📊 Raw embeddings size: {}", response.getEmbeddings().size());
                for (int i = 0; i < Math.min(3, response.getEmbeddings().size()); i++) {
                    List<Double> emb = response.getEmbeddings().get(i);
                    log.info("[INFO]   - Embedding[{}] dimension: {}, first 5 values: {}",
                        i, emb != null ? emb.size() : "null",
                        emb != null && emb.size() >= 5 ? emb.subList(0, 5) : "N/A");
                }
            } else {
                log.error("[ERROR] ❌ Embeddings field is NULL in response for place_id={}", place.getId());
            }

            // Validate response
            if (!response.hasValidEmbeddings()) {
                log.warn("[WARN] No embeddings returned for place_id={}", place.getId());
                return 0;
            }

            List<float[]> embeddings = response.getEmbeddingsAsFloatArrays();
            log.info("[INFO] 🔄 Converted to float arrays: {} embeddings", embeddings.size());

            if (embeddings.size() != keywordsToProcess.size()) {
                log.warn("[WARN] Embedding count mismatch for place_id={}: expected {}, got {}",
                    place.getId(), keywordsToProcess.size(), embeddings.size());
                // Continue with available embeddings
            }

            // Save embeddings to database
            int embeddingsSaved = 0;
            for (int i = 0; i < Math.min(keywordsToProcess.size(), embeddings.size()); i++) {
                String keyword = keywordsToProcess.get(i);
                float[] embedding = embeddings.get(i);

                log.info("[INFO] 💾 Saving embedding for place_id={}, keyword='{}', dimension={}",
                    place.getId(), keyword, embedding.length);

                PlaceKeywordEmbedding embeddingEntity = new PlaceKeywordEmbedding(
                    place.getId(),
                    keyword,
                    embedding
                );

                PlaceKeywordEmbedding savedEntity = embeddingRepository.save(embeddingEntity);
                log.info("[INFO] ✅ Saved embedding with id={} for place_id={}, keyword='{}'",
                    savedEntity.getId(), place.getId(), keyword);

                embeddingsSaved++;
            }

            log.debug("[DEBUG] Successfully embedded {} keywords for place_id={}", embeddingsSaved, place.getId());
            log.info("[INFO] 🎉 Total saved {} embeddings to DB for place_id={}", embeddingsSaved, place.getId());

            return embeddingsSaved;

        } catch (EmbeddingClient.EmbeddingServiceException e) {
            log.error("[ERROR] Embedding service error for place_id={}: {}", place.getId(), e.getMessage());
            throw e;

        } catch (Exception e) {
            log.error("[ERROR] Unexpected error processing place_id={}: {}", place.getId(), e.getMessage(), e);
            throw e;
        }
    }

    /**
     * Delete all embeddings for a specific place
     *
     * @param placeId The place ID
     */
    @Transactional
    public void deleteEmbeddingsForPlace(Long placeId) {
        log.info("[INFO] Deleting embeddings for place_id={}", placeId);
        embeddingRepository.deleteByPlaceId(placeId);
    }

    /**
     * Get embedding statistics
     */
    public String getEmbeddingStats() {
        long totalEmbeddings = embeddingRepository.count();
        List<Long> placeIds = embeddingRepository.findDistinctPlaceIds();
        long placesWithEmbeddings = placeIds.size();

        return String.format(
            "Total embeddings: %d | Places with embeddings: %d",
            totalEmbeddings, placesWithEmbeddings
        );
    }

    /**
     * Check if embedding service is available
     */
    public boolean isEmbeddingServiceAvailable() {
        return embeddingClient.isServiceAvailable();
    }
}
