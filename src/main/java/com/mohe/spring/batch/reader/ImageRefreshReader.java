package com.mohe.spring.batch.reader;

import com.mohe.spring.entity.EmbedStatus;
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
 * 이미지 새로고침용 Place Reader
 *
 * <p>다양한 모드로 Place를 읽어옵니다:
 * <ul>
 *   <li>ALL: 모든 장소</li>
 *   <li>NO_IMAGES: 이미지가 없는 장소만</li>
 *   <li>READY_ONLY: embed_status=COMPLETED인 장소만</li>
 *   <li>NOT_READY: embed_status=PENDING인 장소만</li>
 * </ul>
 *
 * <h3>처리 방식</h3>
 * <ul>
 *   <li>Step 1: Place ID만 페이지별로 조회 (메모리 효율)</li>
 *   <li>Step 2: ID로 Place 엔티티 개별 조회</li>
 *   <li>이미지 컬렉션 강제 로드</li>
 * </ul>
 */
public class ImageRefreshReader implements ItemReader<Place> {

    private static final Logger logger = LoggerFactory.getLogger(ImageRefreshReader.class);

    public enum RefreshMode {
        ALL,           // 모든 장소
        NO_IMAGES,     // 이미지가 없는 장소만
        READY_ONLY,    // ready=true인 장소만
        NOT_READY      // ready=false인 장소만
    }

    private final PlaceRepository placeRepository;
    private final int pageSize;
    private final RefreshMode mode;

    private List<Long> currentPageIds;
    private int currentIdIndex = 0;
    private int currentPage = 0;
    private boolean hasMorePages = true;
    private long totalProcessed = 0;

    public ImageRefreshReader(PlaceRepository placeRepository, int pageSize, RefreshMode mode) {
        this.placeRepository = placeRepository;
        this.pageSize = pageSize;
        this.mode = mode;
        logger.info("🔧 Image Refresh Reader initialized - mode: {}, pageSize: {}", mode, pageSize);
    }

    @Override
    public Place read() throws Exception {
        // Load next page if current page is exhausted
        if (currentPageIds == null || currentIdIndex >= currentPageIds.size()) {
            if (!hasMorePages) {
                logger.info("✅ Image Refresh Reader finished - total processed: {}", totalProcessed);
                return null;
            }

            loadNextPageIds();

            if (currentPageIds == null || currentPageIds.isEmpty()) {
                logger.info("✅ Image Refresh Reader finished - no more data (total: {})", totalProcessed);
                return null;
            }

            currentIdIndex = 0;
        }

        // Read Place by ID
        Long placeId = currentPageIds.get(currentIdIndex);
        currentIdIndex++;

        Place place = placeRepository.findByIdWithCollections(placeId).orElse(null);

        if (place != null) {
            // Force-load images collection
            place.getImages().size();
            totalProcessed++;

            if (totalProcessed % 100 == 0) {
                logger.info("📊 Progress: {} places processed", totalProcessed);
            }

            logger.debug("📖 Read place: {} (ID: {}) with {} existing images",
                place.getName(), place.getId(), place.getImages().size());
        }

        return place;
    }

    /**
     * Load next page of Place IDs based on mode
     */
    private void loadNextPageIds() {
        Pageable pageable = PageRequest.of(currentPage, pageSize, Sort.by("id").ascending());

        Page<Long> idsPage = switch (mode) {
            case ALL -> placeRepository.findAllPlaceIdsForImageRefresh(pageable);
            case NO_IMAGES -> placeRepository.findPlaceIdsWithoutImages(pageable);
            case READY_ONLY -> placeRepository.findPlaceIdsByEmbedStatus(EmbedStatus.COMPLETED, pageable);
            case NOT_READY -> placeRepository.findPlaceIdsByEmbedStatus(EmbedStatus.PENDING, pageable);
        };

        currentPageIds = new ArrayList<>(idsPage.getContent());
        hasMorePages = idsPage.hasNext();
        currentPage++;

        logger.info("📄 [{}] Loaded page {} with {} place IDs (hasMorePages: {}, total elements: {})",
            mode, currentPage, currentPageIds.size(), hasMorePages, idsPage.getTotalElements());
    }

    /**
     * Reset reader state for restart
     */
    public void reset() {
        currentPageIds = null;
        currentIdIndex = 0;
        currentPage = 0;
        hasMorePages = true;
        totalProcessed = 0;
        logger.info("🔄 Image Refresh Reader reset");
    }
}
