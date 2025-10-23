package com.mohe.spring.batch.reader;

import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import org.springframework.batch.item.ItemReader;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Custom ItemReader for Vector Embedding batch processing
 *
 * Uses two-step query pattern to avoid LazyInitializationException:
 * 1. Load Place IDs page-by-page
 * 2. Fetch individual Place entities with collections
 *
 * Conditions: crawler_found = true, ready = false, mohe_description IS NOT NULL
 */
@Component
public class VectorEmbeddingReader implements ItemReader<Place> {

    private final PlaceRepository placeRepository;

    // Pagination state
    private List<Long> currentPageIds;
    private int currentIdIndex;
    private int currentPage;
    private boolean hasMorePages;
    private final int pageSize;

    // Initialization flag
    private boolean initialized;

    public VectorEmbeddingReader(PlaceRepository placeRepository) {
        this.placeRepository = placeRepository;
        this.pageSize = 10; // Load 10 IDs at a time
        this.currentPage = 0;
        this.currentIdIndex = 0;
        this.currentPageIds = new ArrayList<>();
        this.hasMorePages = true;
        this.initialized = false;
    }

    @Override
    public Place read() throws Exception {
        // Initialize on first read
        if (!initialized) {
            loadNextPageIds();
            initialized = true;
        }

        // If current page is exhausted, load next page
        if (currentIdIndex >= currentPageIds.size()) {
            if (!hasMorePages) {
                return null; // End of data
            }
            loadNextPageIds();
            if (currentPageIds.isEmpty()) {
                return null; // No more data
            }
        }

        // Get current place ID
        Long placeId = currentPageIds.get(currentIdIndex);
        currentIdIndex++;

        // Step 2: Fetch full entity with collections
        // Use findByIdWithCollections which eagerly loads descriptions
        Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

        if (place != null) {
            // Force-load other collections within session to prevent LazyInitializationException
            // Even though we only need descriptions, force-load keyword field
            if (place.getKeyword() != null) {
                place.getKeyword().size(); // Touch collection to initialize
            }

            // Descriptions are already loaded by @EntityGraph, but ensure they're accessible
            if (place.getDescriptions() != null) {
                place.getDescriptions().size(); // Touch collection to initialize
            }
        }

        return place;
    }

    /**
     * Load next page of Place IDs
     * Uses findPlacesForVectorEmbedding which returns places where:
     * - crawler_found = true
     * - ready = false
     * - mohe_description IS NOT NULL
     */
    private void loadNextPageIds() {
        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by("id").ascending());
        Page<Place> placesPage = placeRepository.findPlacesForVectorEmbedding(pageable);

        // Extract IDs from places
        currentPageIds = placesPage.getContent().stream()
            .map(Place::getId)
            .toList();

        currentIdIndex = 0;
        hasMorePages = placesPage.hasNext();
        currentPage++;

        System.out.println("[VectorEmbeddingReader] Loaded " + currentPageIds.size() + " place IDs (page " + currentPage + ")");
    }
}
