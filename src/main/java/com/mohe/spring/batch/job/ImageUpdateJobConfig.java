package com.mohe.spring.batch.job;

import com.mohe.spring.batch.reader.ImageUpdateReader;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.repository.PlaceImageRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.crawling.CrawlingService;
import com.mohe.spring.service.image.ImageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 이미지 업데이트 전용 배치 Job
 *
 * <p>crawlerFound=true이고 ready=false인 장소들의 이미지만 다시 크롤링하여 업데이트합니다.
 *
 * <h3>처리 흐름</h3>
 * <ol>
 *   <li>Reader: crawlerFound=true AND (ready=false OR ready IS NULL)인 Place 조회 (ID 순서)</li>
 *   <li>Processor: 새로운 이미지 API 호출하여 이미지 가져오기</li>
 *   <li>Writer: 기존 이미지 삭제 후 새 이미지 저장, ready=true로 업데이트</li>
 * </ol>
 *
 * <h3>API 엔드포인트</h3>
 * <ul>
 *   <li>POST /api/v1/place/images - 이미지만 크롤링</li>
 *   <li>Input: {"searchQuery": "...", "placeName": "..."}</li>
 *   <li>Output: {"images": [...]} (최대 5개)</li>
 * </ul>
 */
@Configuration
public class ImageUpdateJobConfig {

    private static final Logger logger = LoggerFactory.getLogger(ImageUpdateJobConfig.class);

    private final CrawlingService crawlingService;
    private final ImageService imageService;
    private final PlaceRepository placeRepository;
    private final PlaceImageRepository placeImageRepository;

    public ImageUpdateJobConfig(
        CrawlingService crawlingService,
        ImageService imageService,
        PlaceRepository placeRepository,
        PlaceImageRepository placeImageRepository
    ) {
        this.crawlingService = crawlingService;
        this.imageService = imageService;
        this.placeRepository = placeRepository;
        this.placeImageRepository = placeImageRepository;
    }

    @Bean
    public Job imageUpdateJob(JobRepository jobRepository, Step imageUpdateStep) {
        return new JobBuilder("imageUpdateJob", jobRepository)
                .start(imageUpdateStep)
                .build();
    }

    @Bean
    public Step imageUpdateStep(
        JobRepository jobRepository,
        PlatformTransactionManager transactionManager,
        ItemReader<Place> imageUpdateReader,
        ItemProcessor<Place, Place> imageUpdateProcessor,
        ItemWriter<Place> imageUpdateWriter
    ) {
        return new StepBuilder("imageUpdateStep", jobRepository)
                .<Place, Place>chunk(10, transactionManager)
                .reader(imageUpdateReader)
                .processor(imageUpdateProcessor)
                .writer(imageUpdateWriter)
                .faultTolerant()
                .skip(Exception.class)
                .skipLimit(Integer.MAX_VALUE)
                .build();
    }

    @Bean
    public ItemReader<Place> imageUpdateReader() {
        return new ImageUpdateReader(placeRepository, 10);
    }

    @Bean
    public ItemProcessor<Place, Place> imageUpdateProcessor() {
        return place -> {
            try {
                logger.info("🖼️ Fetching images for place: {} (ID: {})", place.getName(), place.getId());

                // 이미지 API 호출
                Map<String, Object> imageData = crawlingService.fetchPlaceImages(
                    place.getName(),
                    place.getRoadAddress()
                );

                if (imageData != null && imageData.containsKey("images")) {
                    @SuppressWarnings("unchecked")
                    List<String> imageUrls = (List<String>) imageData.get("images");

                    if (imageUrls != null && !imageUrls.isEmpty()) {
                        logger.info("✅ Fetched {} images for place: {}", imageUrls.size(), place.getName());

                        // Place 객체에 이미지 URL 리스트 저장 (Writer에서 처리)
                        place.setImageUrls(imageUrls);
                        return place;
                    } else {
                        logger.warn("⚠️ No images found for place: {} (ID: {}). Place will remain ready=false",
                            place.getName(), place.getId());
                    }
                } else {
                    logger.warn("⚠️ Invalid image data response for place: {} (ID: {}). Place will remain ready=false",
                        place.getName(), place.getId());
                }

                return null; // Skip this place - ready will remain false
            } catch (Exception e) {
                logger.error("❌ Error fetching images for place: {} (ID: {}) - {}. Place will remain ready=false",
                    place.getName(), place.getId(), e.getMessage());
                return null; // Skip this place - ready will remain false
            }
        };
    }

    @Bean
    public ItemWriter<Place> imageUpdateWriter() {
        return places -> {
            for (Place place : places) {
                if (place == null || place.getImageUrls() == null || place.getImageUrls().isEmpty()) {
                    continue;
                }

                try {
                    logger.info("💾 Updating images for place: {} (ID: {})", place.getName(), place.getId());

                    // 1. 기존 이미지 삭제
                    List<PlaceImage> existingImages = placeImageRepository.findByPlaceIdOrderByOrderIndexAsc(place.getId());
                    if (!existingImages.isEmpty()) {
                        placeImageRepository.deleteAll(existingImages);
                        logger.info("🗑️ Deleted {} existing images for place: {}", existingImages.size(), place.getName());
                    }

                    // 2. 새 이미지 저장
                    List<PlaceImage> newImages = new ArrayList<>();
                    List<String> imageUrls = place.getImageUrls();

                    // 최대 5개 이미지만 처리
                    List<String> imageUrlsToSave = imageUrls.subList(0, Math.min(imageUrls.size(), 5));

                    try {
                        // 이미지 다운로드 및 저장 (일괄 처리)
                        List<String> savedImagePaths = imageService.downloadAndSaveImages(
                            place.getId(),
                            place.getName(),
                            imageUrlsToSave
                        );

                        if (savedImagePaths != null && !savedImagePaths.isEmpty()) {
                            for (int i = 0; i < savedImagePaths.size(); i++) {
                                PlaceImage placeImage = new PlaceImage();
                                placeImage.setPlace(place);
                                placeImage.setUrl(savedImagePaths.get(i));
                                placeImage.setOrderIndex(i);
                                newImages.add(placeImage);
                            }
                        }
                    } catch (Exception e) {
                        logger.error("❌ Failed to save images for place: {} (ID: {}) - {}. ready will remain false",
                            place.getName(), place.getId(), e.getMessage());
                    }

                    if (!newImages.isEmpty()) {
                        placeImageRepository.saveAll(newImages);
                        logger.info("✅ Saved {} new images for place: {}", newImages.size(), place.getName());

                        // 3. Mark place as ready=true ONLY after successful image update
                        place.setReady(true);
                        placeRepository.save(place);
                        logger.info("✅ Marked place as ready=true: {} (ID: {})", place.getName(), place.getId());
                    } else {
                        logger.warn("⚠️ No images saved for place: {} (ID: {}). ready remains false",
                            place.getName(), place.getId());
                    }

                } catch (Exception e) {
                    logger.error("❌ Error updating images for place: {} (ID: {}) - {}. ready remains false",
                        place.getName(), place.getId(), e.getMessage(), e);
                }
            }
        };
    }
}
