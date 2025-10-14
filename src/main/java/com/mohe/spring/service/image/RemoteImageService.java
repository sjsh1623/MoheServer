package com.mohe.spring.service.image;

import com.mohe.spring.controller.ImageUploadController;
import com.mohe.spring.dto.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;

/**
 * Remote Image Service
 *
 * <p>이미지를 원격 서버(Mac Mini)로 전송하는 서비스</p>
 *
 * <h3>사용 시나리오</h3>
 * <ul>
 *   <li>MacBook Pro에서 크롤링</li>
 *   <li>이미지 URL 수집</li>
 *   <li>Mac Mini의 이미지 서버로 URL 전송</li>
 *   <li>Mac Mini가 이미지 다운로드 및 저장</li>
 * </ul>
 */
@Service
public class RemoteImageService {

    private static final Logger logger = LoggerFactory.getLogger(RemoteImageService.class);

    private final RestTemplate restTemplate;
    private final String imageServerUrl;
    private final boolean useRemoteStorage;

    public RemoteImageService(
            RestTemplate restTemplate,
            @Value("${mohe.image.server-url:}") String imageServerUrl,
            @Value("${mohe.image.use-remote:false}") boolean useRemoteStorage
    ) {
        this.restTemplate = restTemplate;
        this.imageServerUrl = imageServerUrl;
        this.useRemoteStorage = useRemoteStorage;

        if (useRemoteStorage && (imageServerUrl == null || imageServerUrl.isEmpty())) {
            logger.warn("⚠️ Remote image storage is enabled but server URL is not configured!");
        } else if (useRemoteStorage) {
            logger.info("📡 Remote Image Service initialized");
            logger.info("   Server URL: {}", imageServerUrl);
        } else {
            logger.info("💾 Using local image storage (remote disabled)");
        }
    }

    /**
     * 원격 서버로 이미지 URL 전송
     *
     * @param placeId   Place ID
     * @param placeName Place 이름
     * @param imageUrls 이미지 URL 목록
     * @return 저장된 이미지 경로 목록
     */
    public List<String> uploadImagesToRemoteServer(Long placeId, String placeName, List<String> imageUrls) {
        if (!useRemoteStorage || imageServerUrl == null || imageServerUrl.isEmpty()) {
            logger.warn("⚠️ Remote storage not configured, returning empty list");
            return List.of();
        }

        if (imageUrls == null || imageUrls.isEmpty()) {
            logger.debug("No images to upload for place: {}", placeName);
            return List.of();
        }

        try {
            logger.info("📤 Uploading {} images to remote server for: {} (id={})",
                    imageUrls.size(), placeName, placeId);

            // 요청 DTO 생성
            ImageUploadController.ImageUploadRequest request = new ImageUploadController.ImageUploadRequest();
            request.setPlaceId(placeId);
            request.setPlaceName(placeName);
            request.setImageUrls(imageUrls);

            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<ImageUploadController.ImageUploadRequest> entity = new HttpEntity<>(request, headers);

            // API 호출
            String url = imageServerUrl + "/api/images/upload-from-urls";
            var response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<ApiResponse<List<String>>>() {}
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<String> savedPaths = response.getBody().getData();
                logger.info("✅ Successfully uploaded {}/{} images to remote server",
                        savedPaths.size(), imageUrls.size());
                return savedPaths;
            } else {
                logger.error("❌ Failed to upload images: HTTP {}", response.getStatusCode());
                return List.of();
            }

        } catch (Exception e) {
            logger.error("❌ Error uploading images to remote server: {}", e.getMessage(), e);
            return List.of();
        }
    }

    /**
     * 이미지 서버 헬스 체크
     *
     * @return 서버 상태 (true: 정상, false: 비정상)
     */
    public boolean checkServerHealth() {
        if (!useRemoteStorage || imageServerUrl == null || imageServerUrl.isEmpty()) {
            return false;
        }

        try {
            String url = imageServerUrl + "/api/images/health";
            var response = restTemplate.getForEntity(url, String.class);
            boolean healthy = response.getStatusCode().is2xxSuccessful();

            if (healthy) {
                logger.debug("💚 Image server is healthy: {}", imageServerUrl);
            } else {
                logger.warn("💛 Image server returned non-2xx: {}", response.getStatusCode());
            }

            return healthy;
        } catch (Exception e) {
            logger.error("💔 Image server is unreachable: {}", imageServerUrl, e);
            return false;
        }
    }

    /**
     * 원격 이미지 삭제
     *
     * @param placeId Place ID
     * @return 삭제 성공 여부
     */
    public boolean deleteImagesFromRemoteServer(Long placeId) {
        if (!useRemoteStorage || imageServerUrl == null || imageServerUrl.isEmpty()) {
            return false;
        }

        try {
            logger.info("🗑️ Deleting images from remote server for placeId: {}", placeId);

            String url = imageServerUrl + "/api/images/" + placeId;
            restTemplate.delete(url);

            logger.info("✅ Successfully deleted images from remote server");
            return true;
        } catch (Exception e) {
            logger.error("❌ Error deleting images from remote server: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * 원격 스토리지 사용 여부
     */
    public boolean isUsingRemoteStorage() {
        return useRemoteStorage;
    }

    /**
     * 이미지 서버 URL 조회
     */
    public String getImageServerUrl() {
        return imageServerUrl;
    }
}
