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

            // 확장자 추출
            String extension = extractExtensionFromUrl(imageUrl);

            // 파일명 생성 (place/ 폴더에 저장)
            String fileName = "place/" + generateFileName(placeId, placeName, i + 1) + "." + extension;

            try {
                String savedFileName = saveImageToProcessor(imageUrl, fileName);
                if (savedFileName != null) {
                    // ImageProcessor가 반환한 파일명을 그대로 사용
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
     * @param fileName 저장할 파일명 (확장자 포함 필수)
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

            logger.debug("📤 Calling ImageProcessor: url={}, fileName={}", imageUrl, fileName);

            // API 호출
            ResponseEntity<ImageSaveResponse> response = restTemplate.postForEntity(
                    url,
                    entity,
                    ImageSaveResponse.class
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                String savedFileName = response.getBody().getFileName();
                logger.info("✅ ImageProcessor saved: {} (original URL: {})", savedFileName, imageUrl);
                return savedFileName;
            } else {
                logger.error("❌ ImageProcessor returned non-2xx: {} for URL: {}", response.getStatusCode(), imageUrl);
                return null;
            }

        } catch (Exception e) {
            logger.error("❌ Failed to call ImageProcessor API for URL: {} - Error: {}", imageUrl, e.getMessage(), e);
            return null;
        }
    }

    /**
     * 메뉴 이미지 다운로드 및 저장 (ImageProcessor 노드 사용)
     *
     * @param placeId  Place ID
     * @param menuName 메뉴명
     * @param imageUrl 이미지 URL
     * @return 저장된 이미지 경로 (예: /images/menu/123_menuName_1.jpg) 또는 null (실패 시)
     */
    public String saveMenuImage(Long placeId, String menuName, String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            logger.debug("No image URL provided for menu: {}", menuName);
            return null;
        }

        try {
            // 확장자 추출
            String extension = extractExtensionFromUrl(imageUrl);

            // 파일명 생성: menu/{placeId}_{menuName}_{uuid}.{ext}
            String sanitizedMenuName = menuName.replaceAll("[^a-zA-Z0-9가-힣]", "_");
            String uuid = java.util.UUID.randomUUID().toString().substring(0, 8);
            String fileName = "menu/" + placeId + "_" + sanitizedMenuName + "_" + uuid + "." + extension;

            String savedFileName = saveImageToProcessor(imageUrl, fileName);
            if (savedFileName != null) {
                String savedPath = "/images/" + savedFileName;
                logger.info("✅ Saved menu image via ImageProcessor: {} from {}", savedPath, imageUrl);
                return savedPath;
            } else {
                logger.warn("⚠️ Failed to save menu image via ImageProcessor: {}", imageUrl);
                return null;
            }
        } catch (Exception e) {
            logger.error("❌ Error saving menu image via ImageProcessor: {}", imageUrl, e);
            return null;
        }
    }

    /**
     * 장소 이미지 저장 (기존 기능 - 폴더 구조 업데이트)
     *
     * @param placeId   Place ID
     * @param placeName Place 이름
     * @param imageUrls 이미지 URL 목록
     * @return 저장된 이미지 경로 목록
     */
    public List<String> savePlaceImages(Long placeId, String placeName, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            logger.debug("No images to save for place: {}", placeName);
            return List.of();
        }

        List<String> savedPaths = new ArrayList<>();

        for (int i = 0; i < imageUrls.size(); i++) {
            String imageUrl = imageUrls.get(i);
            String extension = extractExtensionFromUrl(imageUrl);

            // 파일명 생성: place/{placeId}_{placeName}_{index}.{ext}
            String sanitizedName = placeName.replaceAll("[^a-zA-Z0-9가-힣]", "_");
            String fileName = "place/" + placeId + "_" + sanitizedName + "_" + (i + 1) + "." + extension;

            try {
                String savedFileName = saveImageToProcessor(imageUrl, fileName);
                if (savedFileName != null) {
                    String savedPath = "/images/" + savedFileName;
                    savedPaths.add(savedPath);
                    logger.debug("✅ Saved place image via ImageProcessor: {} from {}", savedPath, imageUrl);
                } else {
                    logger.warn("⚠️ Failed to save place image via ImageProcessor: {}", imageUrl);
                }
            } catch (Exception e) {
                logger.error("❌ Error saving place image via ImageProcessor: {}", imageUrl, e);
            }
        }

        logger.info("✅ Saved {}/{} place images via ImageProcessor for: {} (id={})",
                savedPaths.size(), imageUrls.size(), placeName, placeId);

        return savedPaths;
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
     *
     * @param placeId Place ID
     * @param placeName Place 이름
     * @param index 이미지 순번
     * @return 파일명 (확장자 제외, 예: "123_카페_1")
     */
    private String generateFileName(Long placeId, String placeName, int index) {
        // 파일명에서 특수문자 제거
        String sanitizedName = placeName.replaceAll("[^a-zA-Z0-9가-힣]", "_");
        return placeId + "_" + sanitizedName + "_" + index;
    }

    /**
     * URL에서 확장자 추출
     *
     * @param url 이미지 URL
     * @return 확장자 (예: "jpg", "png") 또는 기본값 "jpg"
     */
    private String extractExtensionFromUrl(String url) {
        try {
            // URL에서 쿼리 파라미터 제거
            String urlWithoutQuery = url.split("\\?")[0];

            // 마지막 "." 이후 문자열 추출
            int lastDotIndex = urlWithoutQuery.lastIndexOf(".");
            if (lastDotIndex != -1 && lastDotIndex < urlWithoutQuery.length() - 1) {
                String extension = urlWithoutQuery.substring(lastDotIndex + 1).toLowerCase();

                // 유효한 이미지 확장자인지 확인
                if (extension.matches("(jpg|jpeg|png|gif|webp|bmp|svg)")) {
                    return extension;
                }
            }

            // 기본값: jpg
            logger.debug("Could not extract extension from URL: {}, using default: jpg", url);
            return "jpg";
        } catch (Exception e) {
            logger.warn("Error extracting extension from URL: {}, using default: jpg", url);
            return "jpg";
        }
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
