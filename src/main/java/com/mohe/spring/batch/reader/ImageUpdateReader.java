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
 * 이미지 업데이트용 Place Reader
 *
 * <p>isCrawled=true인 장소들만 읽어옵니다.
 *
 * <h3>처리 방식</h3>
 * <ul>
 *   <li>Step 1: Place ID만 페이지별로 조회</li>
 *   <li>Step 2: ID로 Place 엔티티 개별 조회</li>
 *   <li>기존 이미지 컬렉션은 강제 로드</li>
 * </ul>
 *
 * <h3>조회 조건</h3>
 * <ul>
 *   <li>isCrawled = true</li>
 *   <li>정렬: ID ASC</li>
 * </ul>
 */
public class ImageUpdateReader implements ItemReader<Place> {

    private static final Logger logger = LoggerFactory.getLogger(ImageUpdateReader.class);

    private final PlaceRepository placeRepository;
    private final int pageSize;

    private List<Long> currentPageIds;
    private int currentIdIndex = 0;
    private int currentPage = 0;
    private boolean hasMorePages = true;

    public ImageUpdateReader(PlaceRepository placeRepository, int pageSize) {
        this.placeRepository = placeRepository;
        this.pageSize = pageSize;
        logger.info("🔧 Image Update Reader initialized with page size: {}", pageSize);
    }

    @Override
    public Place read() throws Exception {
        // Load next page if current page is exhausted
        if (currentPageIds == null || currentIdIndex >= currentPageIds.size()) {
            if (!hasMorePages) {
                logger.info("✅ Image Update Reader finished - no more pages");
                return null; // End of data
            }

            loadNextPageIds();

            if (currentPageIds == null || currentPageIds.isEmpty()) {
                logger.info("✅ Image Update Reader finished - no more data");
                return null; // End of data
            }

            currentIdIndex = 0;
        }

        // Read Place by ID
        Long placeId = currentPageIds.get(currentIdIndex);
        currentIdIndex++;

        Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

        if (place != null) {
            // Force-load images collection to avoid LazyInitializationException
            place.getImages().size();

            logger.debug("📖 Read place: {} (ID: {}) with {} existing images",
                place.getName(), place.getId(), place.getImages().size());
        }

        return place;
    }

    /**
     * Load next page of Place IDs
     */
    private void loadNextPageIds() {
        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by("id").ascending());

        // Query: isCrawled = true
        Page<Long> idsPage = placeRepository.findPlaceIdsForImageUpdate(pageable);

        currentPageIds = new ArrayList<>(idsPage.getContent());
        hasMorePages = idsPage.hasNext();
        currentPage++;

        logger.info("📄 Loaded page {} with {} place IDs (hasMorePages: {})",
            currentPage, currentPageIds.size(), hasMorePages);
    }
}
