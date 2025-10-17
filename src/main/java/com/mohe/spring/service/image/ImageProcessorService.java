package com.mohe.spring.service.image;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Image Processor Service
 *
 * <p>Node.js 이미지 프로세서를 통해 이미지를 저장하는 서비스</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>외부 URL의 이미지를 ImageProcessor 노드로 전송하여 저장</li>
 *   <li>자동 확장자 감지 및 추가</li>
 *   <li>이미지 리사이징 지원</li>
 * </ul>
 */
@Service
public class ImageProcessorService {

    private static final Logger logger = LoggerFactory.getLogger(ImageProcessorService.class);

    private final RestTemplate restTemplate;
    private final String imageProcessorUrl;

    public ImageProcessorService(
            RestTemplate restTemplate,
            @Value("${mohe.image-processor.url}") String imageProcessorUrl
    ) {
        this.restTemplate = restTemplate;
        this.imageProcessorUrl = imageProcessorUrl;

        logger.info("🖼️ Image Processor Service initialized");
        logger.info("   Processor URL: {}", imageProcessorUrl);
    }

    /**
     * 이미지 URL 목록을 ImageProcessor로 전송하여 저장
     *
     * @param placeId   Place ID
     * @param placeName Place 이름
     * @param imageUrls 이미지 URL 목록
     * @return 저장된 이미지 경로 목록
     */
    public List<String> saveImages(Long placeId, String placeName, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            logger.debug("No images to save for place: {}", placeName);
            return List.of();
        }

        List<String> savedPaths = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            String fileName = generateFileName(placeId, placeName, i + 1);

            try {
                String savedFileName = saveImageToProcessor(imageUrl, fileName);
                if (savedFileName != null) {
                    // ImageProcessor는 자동으로 확장자를 추가하므로, 반환된 파일명을 사용
                    String savedPath = "/images/" + savedFileName;
                    savedPaths.add(savedPath);
                    logger.debug("✅ Saved via ImageProcessor: {} from {}", savedPath, imageUrl);
                } else {
                    logger.warn("⚠️ Failed to save image via ImageProcessor: {}", imageUrl);
                }
            } catch (Exception e) {
                logger.error("❌ Error saving image via ImageProcessor: {}", imageUrl, e);
            }
        }

        logger.info("✅ Saved {}/{} images via ImageProcessor for: {} (id={})",
                savedPaths.size(), imageUrls.size(), placeName, placeId);

        return savedPaths;
    }

    /**
     * ImageProcessor API를 호출하여 이미지 저장
     *
     * @param imageUrl 다운로드할 이미지 URL
     * @param fileName 저장할 파일명 (확장자 제외)
     * @return 저장된 파일명 (확장자 포함) 또는 null (실패 시)
     */
    private String saveImageToProcessor(String imageUrl, String fileName) {
        try {
            String url = imageProcessorUrl + "/save";

            // 요청 DTO 생성
            ImageSaveRequest request = new ImageSaveRequest(imageUrl, fileName);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ImageSaveRequest> entity = new HttpEntity<>(request, headers);

            // API 호출
            ResponseEntity<ImageSaveResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    ImageSaveResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String savedFileName = response.getBody().getFileName();
                logger.debug("✅ ImageProcessor saved: {}", savedFileName);
                return savedFileName;
            } else {
                logger.error("❌ ImageProcessor returned non-2xx: {}", response.getStatusCode());
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ Failed to call ImageProcessor API: {}", e.getMessage());
            return null;
        }
    }

    /**
     * ImageProcessor 헬스 체크
     *
     * @return 서버 상태 (true: 정상, false: 비정상)
     */
    public boolean checkHealth() {
        try {
            // ImageProcessor는 GET /image 엔드포인트가 있으므로, 간단히 루트를 호출
            ResponseEntity<String> response = restTemplate.getForEntity(
                    imageProcessorUrl,
                    String.class
            );
            boolean healthy = response.getStatusCode().is2xxSuccessful();

            if (healthy) {
                logger.debug("💚 ImageProcessor is healthy: {}", imageProcessorUrl);
            } else {
                logger.warn("💛 ImageProcessor returned non-2xx: {}", response.getStatusCode());
            }

            return healthy;
        } catch (Exception e) {
            logger.error("💔 ImageProcessor is unreachable: {}", imageProcessorUrl);
            return false;
        }
    }

    /**
     * 파일명 생성 (확장자 제외)
     */
    private String generateFileName(Long placeId, String placeName, int index) {
        // 파일명에서 특수문자 제거
        String sanitizedName = placeName.replaceAll("[^a-zA-Z0-9가-힣]", "_");
        // 확장자는 ImageProcessor가 자동으로 추가하므로 제외
        return placeId + "_" + sanitizedName + "_" + index;
    }

    /**
     * ImageProcessor API 요청 DTO
     */
    private static class ImageSaveRequest {
        private String url;
        private String fileName;

        public ImageSaveRequest(String url, String fileName) {
            this.url = url;
            this.fileName = fileName;
        }

        public String getUrl() {
            return url;
        }

        public String getFileName() {
            return fileName;
        }
    }

    /**
     * ImageProcessor API 응답 DTO
     */
    private static class ImageSaveResponse {
        private String message;

        @JsonProperty("fileName")
        private String fileName;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }
    }
}
