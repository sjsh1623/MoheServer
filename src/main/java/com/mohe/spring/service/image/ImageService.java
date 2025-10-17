package com.mohe.spring.service.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Image Service
 *
 * <p>이미지 저장을 ImageProcessor 노드에 위임하는 서비스</p>
 *
 * <h3>변경 사항</h3>
 * <ul>
 *   <li>기존: 로컬 파일 시스템에 직접 저장</li>
 *   <li>현재: ImageProcessor 노드를 통해 저장 (자동 확장자 감지, 리사이징 지원)</li>
 * </ul>
 */
@Service
public class ImageService {
    private static final Logger logger = LoggerFactory.getLogger(ImageService.class);

    private final ImageProcessorService imageProcessorService;

    public ImageService(ImageProcessorService imageProcessorService) {
        this.imageProcessorService = imageProcessorService;
        logger.info("🖼️ ImageService initialized (using ImageProcessor)");
    }

    /**
     * 이미지 다운로드 및 저장 (ImageProcessor 노드 사용)
     *
     * @param placeId   Place ID
     * @param placeName Place 이름
     * @param imageUrls 이미지 URL 목록
     * @return 저장된 이미지 경로 목록
     */
    public List<String> downloadAndSaveImages(Long placeId, String placeName, List<String> imageUrls) {
        return imageProcessorService.saveImages(placeId, placeName, imageUrls);
    }
}
