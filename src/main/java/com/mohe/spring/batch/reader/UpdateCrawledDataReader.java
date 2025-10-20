package com.mohe.spring.batch.reader;

import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom ItemReader for UpdateCrawledDataJob
 *
 * Solves the Hibernate HHH90003004 warning issue:
 * - Cannot use multiple collection FETCH JOINs with pagination
 * - Solution: Two-step query approach with page-by-page loading
 *   1. Query Place IDs page by page (not all at once)
 *   2. Load full Place entities with collections for current page
 */
public class UpdateCrawledDataReader implements ItemReader<Place> {

    private static final Logger logger = LoggerFactory.getLogger(UpdateCrawledDataReader.class);

    private final PlaceRepository placeRepository;
    private final int pageSize;

    private List<Long> currentPageIds;
    private int currentIdIndex = 0;
    private int currentPage = 0;
    private boolean hasMorePages = true;

    public UpdateCrawledDataReader(PlaceRepository placeRepository, int pageSize) {
        this.placeRepository = placeRepository;
        this.pageSize = pageSize;
    }

    /**
     * Load next page of Place IDs
     * Only loads one page at a time to avoid memory issues
     */
    private void loadNextPageIds() {
        if (!hasMorePages) {
            return;
        }

        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by("id").ascending());
        Page<Long> idsPage = placeRepository.findPlaceIdsForBatchProcessing(pageable);

        currentPageIds = new ArrayList<>(idsPage.getContent());
        currentIdIndex = 0;
        hasMorePages = idsPage.hasNext();
        currentPage++;

        if (!currentPageIds.isEmpty()) {
            logger.info("📄 Loaded page {}: {} IDs (hasNext: {})", currentPage, currentPageIds.size(), hasMorePages);
        }
    }

    /**
     * Read next Place entity with all collections eagerly loaded
     * Loads IDs page by page to avoid memory issues
     */
    @Override
    public Place read() throws Exception {
        // If current page is empty or exhausted, load next page
        if (currentPageIds == null || currentIdIndex >= currentPageIds.size()) {
            if (!hasMorePages && currentPageIds != null && currentIdIndex >= currentPageIds.size()) {
                // All pages processed
                logger.info("✅ All places read successfully");
                return null;
            }

            loadNextPageIds();

            // If no IDs in new page, we're done
            if (currentPageIds.isEmpty()) {
                logger.info("✅ All places read successfully");
                return null;
            }
        }

        // Get next ID from current page
        Long placeId = currentPageIds.get(currentIdIndex);
        currentIdIndex++;

        // Load Place with descriptions (to avoid MultipleBagFetchException)
        Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

        if (place != null) {
            // Force-load other collections to avoid LazyInitializationException
            // Hibernate will use separate queries for each collection
            place.getImages().size();
            place.getBusinessHours().size();
            place.getSns().size();
            place.getReviews().size();

            logger.info("📖 Reading Place {}/{} in page {}: ID={}, Name='{}'",
                currentIdIndex, currentPageIds.size(), currentPage, place.getId(), place.getName());
        } else {
            logger.warn("⚠️ Place with ID={} not found", placeId);
        }

        return place;
    }
}
