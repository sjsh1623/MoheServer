package com.mohe.spring.batch.writer;

import com.mohe.spring.entity.Place;
import com.mohe.spring.service.PlaceDataCollectionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

/**
 * 장소 데이터를 DB에 저장하는 Writer
 */
@Component
public class PlaceDataWriter implements ItemWriter<Place> {

    private static final Logger logger = LoggerFactory.getLogger(PlaceDataWriter.class);

    private final PlaceDataCollectionService placeDataCollectionService;

    public PlaceDataWriter(PlaceDataCollectionService placeDataCollectionService) {
        this.placeDataCollectionService = placeDataCollectionService;
    }

    @Override
    public void write(Chunk<? extends Place> chunk) throws Exception {
        logger.info("💾 Writing {} places to database", chunk.size());

        for (Place place : chunk) {
            try {
                placeDataCollectionService.savePlace(place);
                logger.info("✅ Saved place: {} (ID: {})", place.getName(), place.getId());
            } catch (Exception e) {
                logger.error("❌ Failed to save place: {}", place.getName(), e);
            }
        }

        logger.info("✅ Batch write completed: {} places saved", chunk.size());
    }
}
