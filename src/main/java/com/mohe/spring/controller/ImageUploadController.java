package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Image Upload Controller
 *
 * <p>분산 환경에서 이미지를 중앙 집중식으로 저장하기 위한 API</p>
 *
 * <h3>사용 시나리오</h3>
 * <ul>
 *   <li>Mac Mini: 이미지 저장 서버 (이 API 실행)</li>
 *   <li>MacBook Pro: 크롤링 후 Mac Mini로 이미지 전송</li>
 *   <li>기타 워커: 동일하게 Mac Mini로 이미지 전송</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/images")
@Tag(name = "Image Upload", description = "이미지 중앙 집중식 저장 API")
public class ImageUploadController {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadController.class);

    private final Path imageStorageLocation;

    public ImageUploadController(@Value("${mohe.image.storage-path:/images}") String storagePath) {
        this.imageStorageLocation = Paths.get(storagePath);
        try {
            Files.createDirectories(this.imageStorageLocation);
            logger.info("📁 Image storage initialized: {}", imageStorageLocation.toAbsolutePath());
        } catch (Exception ex) {
            throw new RuntimeException("Could not create image storage directory", ex);
        }
    }

    /**
     * 파일 직접 업로드
     *
     * <p>MultipartFile로 이미지를 직접 업로드합니다.</p>
     *
     * @param placeId   Place ID
     * @param placeName Place 이름
     * @param files     업로드할 이미지 파일들
     * @return 저장된 이미지 경로 목록
     */
    @PostMapping("/upload")
    @Operation(summary = "이미지 직접 업로드", description = "MultipartFile로 이미지를 직접 업로드합니다")
    public ResponseEntity<ApiResponse<List<String>>> uploadImages(
            @RequestParam("placeId") Long placeId,
            @RequestParam("placeName") String placeName,
            @RequestParam("files") List<MultipartFile> files
    ) {
        logger.info("📤 Image upload request: placeId={}, placeName={}, count={}",
                placeId, placeName, files.size());

        List<String> savedPaths = new ArrayList<>();

        for (int i = 0; i < files.size(); i++) {
            MultipartFile file = files.get(i);
            String fileName = generateFileName(placeId, placeName, i + 1);

            try {
                Path targetPath = imageStorageLocation.resolve(fileName);
                Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
                String savedPath = "/images/" + fileName;
                savedPaths.add(savedPath);
                logger.debug("✅ Saved: {}", savedPath);
            } catch (IOException e) {
                logger.error("❌ Failed to save image: {}", fileName, e);
            }
        }

        logger.info("✅ Upload complete: {}/{} images saved", savedPaths.size(), files.size());
        return ResponseEntity.ok(ApiResponse.success(savedPaths));
    }

    /**
     * URL에서 이미지 다운로드 및 저장
     *
     * <p>이미지 URL 목록을 받아서 다운로드하고 저장합니다.
     * 분산 워커에서 크롤링한 이미지를 Mac Mini로 전송할 때 사용합니다.</p>
     *
     * @param request 요청 데이터 (placeId, placeName, imageUrls)
     * @return 저장된 이미지 경로 목록
     */
    @PostMapping("/upload-from-urls")
    @Operation(summary = "URL에서 이미지 다운로드", description = "이미지 URL 목록을 받아서 다운로드하고 저장합니다")
    public ResponseEntity<ApiResponse<List<String>>> uploadFromUrls(
            @RequestBody ImageUploadRequest request
    ) {
        logger.info("📥 Image download request: placeId={}, placeName={}, urls={}",
                request.getPlaceId(), request.getPlaceName(), request.getImageUrls().size());

        List<String> savedPaths = new ArrayList<>();

        for (int i = 0; i < request.getImageUrls().size(); i++) {
            String imageUrl = request.getImageUrls().get(i);
            String fileName = generateFileName(request.getPlaceId(), request.getPlaceName(), i + 1);

            try (InputStream in = new URL(imageUrl).openStream()) {
                Path targetPath = imageStorageLocation.resolve(fileName);
                Files.copy(in, targetPath, StandardCopyOption.REPLACE_EXISTING);
                String savedPath = "/images/" + fileName;
                savedPaths.add(savedPath);
                logger.debug("✅ Downloaded and saved: {} from {}", savedPath, imageUrl);
            } catch (IOException e) {
                logger.error("❌ Failed to download image from: {}", imageUrl, e);
            }
        }

        logger.info("✅ Download complete: {}/{} images saved",
                savedPaths.size(), request.getImageUrls().size());
        return ResponseEntity.ok(ApiResponse.success(savedPaths));
    }

    /**
     * 이미지 삭제
     *
     * @param placeId Place ID (해당 Place의 모든 이미지 삭제)
     * @return 삭제된 이미지 개수
     */
    @DeleteMapping("/{placeId}")
    @Operation(summary = "이미지 삭제", description = "특정 Place의 모든 이미지를 삭제합니다")
    public ResponseEntity<ApiResponse<Integer>> deleteImages(@PathVariable Long placeId) {
        logger.info("🗑️ Image delete request: placeId={}", placeId);

        try {
            int deletedCount = 0;
            String prefix = placeId + "_";

            // 해당 Place의 모든 이미지 찾아서 삭제
            Files.list(imageStorageLocation)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .forEach(path -> {
                        try {
                            Files.delete(path);
                            logger.debug("🗑️ Deleted: {}", path.getFileName());
                        } catch (IOException e) {
                            logger.error("❌ Failed to delete: {}", path.getFileName(), e);
                        }
                    });

            logger.info("✅ Deleted {} images for placeId={}", deletedCount, placeId);
            return ResponseEntity.ok(ApiResponse.success(deletedCount));
        } catch (IOException e) {
            logger.error("❌ Failed to list images for deletion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("IMAGE_DELETE_FAILED",
                            "Failed to delete images", "/api/images/" + placeId));
        }
    }

    /**
     * 헬스 체크
     */
    @GetMapping("/health")
    @Operation(summary = "헬스 체크", description = "이미지 업로드 서비스 상태 확인")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "status", "UP",
                "storageLocation", imageStorageLocation.toAbsolutePath().toString(),
                "writable", Files.isWritable(imageStorageLocation)
        )));
    }

    /**
     * 파일명 생성
     */
    private String generateFileName(Long placeId, String placeName, int index) {
        // 파일명에서 특수문자 제거
        String sanitizedName = placeName.replaceAll("[^a-zA-Z0-9가-힣]", "_");
        return placeId + "_" + sanitizedName + "_" + index + ".jpeg";
    }

    /**
     * 이미지 업로드 요청 DTO
     */
    public static class ImageUploadRequest {
        private Long placeId;
        private String placeName;
        private List<String> imageUrls;

        // Getters and Setters
        public Long getPlaceId() {
            return placeId;
        }

        public void setPlaceId(Long placeId) {
            this.placeId = placeId;
        }

        public String getPlaceName() {
            return placeName;
        }

        public void setPlaceName(String placeName) {
            this.placeName = placeName;
        }

        public List<String> getImageUrls() {
            return imageUrls;
        }

        public void setImageUrls(List<String> imageUrls) {
            this.imageUrls = imageUrls;
        }
    }
}
