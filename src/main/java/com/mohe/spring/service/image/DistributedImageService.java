package com.mohe.spring.service.image;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Distributed Image Service
 *
 * <p>로컬 저장과 원격 저장을 자동으로 선택하는 통합 이미지 서비스</p>
 *
 * <h3>동작 방식</h3>
 * <ul>
 *   <li><b>Mac Mini (이미지 서버):</b> 로컬 저장 사용</li>
 *   <li><b>MacBook Pro (워커):</b> 원격 저장 사용 (Mac Mini로 전송)</li>
 *   <li><b>기타 워커:</b> 원격 저장 사용</li>
 * </ul>
 */
@Service
public class DistributedImageService {

    private static final Logger logger = LoggerFactory.getLogger(DistributedImageService.class);

    private final ImageService localImageService;
    private final RemoteImageService remoteImageService;

    public DistributedImageService(
            ImageService localImageService,
            RemoteImageService remoteImageService
    ) {
        this.localImageService = localImageService;
        this.remoteImageService = remoteImageService;

        logger.info("🖼️ Distributed Image Service initialized");
        logger.info("   Remote storage: {}", remoteImageService.isUsingRemoteStorage() ? "ENABLED" : "DISABLED");

        if (remoteImageService.isUsingRemoteStorage()) {
            logger.info("   Image server: {}", remoteImageService.getImageServerUrl());

            // 이미지 서버 헬스 체크
            boolean healthy = remoteImageService.checkServerHealth();
            if (healthy) {
                logger.info("   ✅ Image server is reachable");
            } else {
                logger.warn("   ⚠️ Image server is NOT reachable - will use local storage as fallback");
            }
        }
    }

    /**
     * 이미지 다운로드 및 저장
     *
     * <p>설정에 따라 로컬 또는 원격 저장소에 저장합니다.</p>
     *
     * @param placeId   Place ID
     * @param placeName Place 이름
     * @param imageUrls 이미지 URL 목록
     * @return 저장된 이미지 경로 목록
     */
    public List<String> downloadAndSaveImages(Long placeId, String placeName, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            logger.debug("No images to save for place: {}", placeName);
            return List.of();
        }

        // 원격 저장소 사용 설정인 경우
        if (remoteImageService.isUsingRemoteStorage()) {
            logger.info("📡 Using remote image storage for: {} ({})", placeName, placeId);

            List<String> remotePaths = remoteImageService.uploadImagesToRemoteServer(
                    placeId, placeName, imageUrls
            );

            if (!remotePaths.isEmpty()) {
                return remotePaths;
            } else {
                logger.warn("⚠️ Remote upload failed, falling back to local storage");
                // Fallback to local storage
            }
        }

        // 로컬 저장소 사용
        logger.info("💾 Using local image storage for: {} ({})", placeName, placeId);
        return localImageService.downloadAndSaveImages(placeId, placeName, imageUrls);
    }

    /**
     * 이미지 삭제
     *
     * @param placeId Place ID
     * @return 삭제 성공 여부
     */
    public boolean deleteImages(Long placeId) {
        if (remoteImageService.isUsingRemoteStorage()) {
            return remoteImageService.deleteImagesFromRemoteServer(placeId);
        } else {
            // 로컬 삭제는 ImageService에 구현 필요
            logger.warn("Local image deletion not implemented yet");
            return false;
        }
    }

    /**
     * 원격 스토리지 사용 여부
     */
    public boolean isUsingRemoteStorage() {
        return remoteImageService.isUsingRemoteStorage();
    }
}
