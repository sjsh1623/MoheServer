package com.mohe.spring.service.refresh;

import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.dto.crawling.CrawlingResponse;
import com.mohe.spring.dto.refresh.BatchRefreshResponseDto;
import com.mohe.spring.dto.refresh.PlaceRefreshResponseDto;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceBusinessHour;
import com.mohe.spring.entity.PlaceDescription;
import com.mohe.spring.entity.PlaceImage;
import com.mohe.spring.entity.PlaceMenu;
import com.mohe.spring.entity.PlaceReview;
import com.mohe.spring.entity.PlaceSns;
import com.mohe.spring.repository.PlaceMenuRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.service.crawling.CrawlingService;
import com.mohe.spring.service.image.ImageProcessorService;
import com.mohe.spring.service.image.ImageService;
import com.mohe.spring.service.OpenAiDescriptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 장소 이미지/리뷰 새로고침 서비스
 *
 * <p>특정 장소에 대해 네이버에서 이미지와 리뷰를 새로 크롤링하여 업데이트합니다.</p>
 *
 * <h3>기능</h3>
 * <ul>
 *   <li>이미지: 최대 5장까지 새로 가져와서 저장</li>
 *   <li>리뷰: 중복되지 않는 새로운 리뷰만 추가</li>
 * </ul>
 */
@Service
public class PlaceRefreshService {
    private static final Logger logger = LoggerFactory.getLogger(PlaceRefreshService.class);

    private static final int MAX_IMAGES = 5;
    private static final int MAX_REVIEWS = 10;
    private static final int MAX_MENUS = 50;

    private final PlaceRepository placeRepository;
    private final PlaceMenuRepository placeMenuRepository;
    private final CrawlingService crawlingService;
    private final ImageService imageService;
    private final ImageProcessorService imageProcessorService;
    private final OpenAiDescriptionService openAiDescriptionService;

    public PlaceRefreshService(
            PlaceRepository placeRepository,
            PlaceMenuRepository placeMenuRepository,
            CrawlingService crawlingService,
            ImageService imageService,
            ImageProcessorService imageProcessorService,
            OpenAiDescriptionService openAiDescriptionService) {
        this.placeRepository = placeRepository;
        this.placeMenuRepository = placeMenuRepository;
        this.crawlingService = crawlingService;
        this.imageService = imageService;
        this.imageProcessorService = imageProcessorService;
        this.openAiDescriptionService = openAiDescriptionService;
        logger.info("PlaceRefreshService initialized");
    }

    /**
     * 장소 전체 데이터 새로고침 (이미지, 리뷰, 메뉴, 영업시간)
     *
     * <p>다음 데이터를 새로 크롤링하여 업데이트합니다:</p>
     * <ul>
     *   <li>이미지: 최대 5장</li>
     *   <li>리뷰: 최대 10개</li>
     *   <li>메뉴: 최대 50개 (이미지 포함)</li>
     *   <li>영업시간</li>
     * </ul>
     *
     * @param placeId 장소 ID
     * @return 새로고침 결과
     */
    @Transactional
    public PlaceRefreshResponseDto refreshPlaceData(Long placeId) {
        logger.info("Starting refresh for place ID: {}", placeId);

        // 1. 장소 조회
        Place place = placeRepository.findByIdWithCollections(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));

        // 컬렉션 로드 (LazyInitializationException 방지)
        place.getImages().size();
        place.getReviews().size();
        place.getBusinessHours().size();
        place.getMenus().size();

        // 2. 전체 데이터 크롤링 실행
        String searchQuery = buildSearchQuery(place);
        logger.info("Crawling with query: '{}' for place: '{}'", searchQuery, place.getName());

        CrawlingResponse<CrawledDataDto> response = crawlingService
                .crawlPlaceData(searchQuery, place.getName())
                .block();

        if (response == null || !response.isSuccess() || response.getData() == null) {
            logger.warn("Crawling failed for place ID: {}", placeId);
            return PlaceRefreshResponseDto.builder()
                    .placeId(placeId)
                    .placeName(place.getName())
                    .imageCount(0)
                    .newReviewCount(0)
                    .totalReviewCount(place.getReviews().size())
                    .menuCount(0)
                    .menuWithImageCount(0)
                    .imageUrls(List.of())
                    .newReviews(List.of())
                    .menus(List.of())
                    .message("Crawling failed: " + (response != null ? response.getMessage() : "No response"))
                    .build();
        }

        CrawledDataDto crawledData = response.getData();

        // 3. 이미지 업데이트
        List<String> savedImageUrls = updateImages(place, crawledData.getImageUrls());

        // 4. 리뷰 업데이트 (중복 체크 후 새 리뷰만 추가)
        List<String> newReviews = updateReviews(place, crawledData.getReviews());

        // 5. 영업시간 업데이트
        updateBusinessHours(place, crawledData);

        // 6. 메뉴 크롤링 및 업데이트
        List<PlaceRefreshResponseDto.MenuDto> savedMenus = new ArrayList<>();
        int menuWithImageCount = 0;

        var menuData = crawlingService.fetchPlaceMenus(place.getName(), place.getRoadAddress());
        if (menuData != null) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> crawledMenus = (List<Map<String, Object>>) menuData.get("menus");
            if (crawledMenus != null && !crawledMenus.isEmpty()) {
                // 기존 메뉴 삭제
                placeMenuRepository.deleteByPlaceId(placeId);
                placeRepository.flush();

                for (int i = 0; i < Math.min(crawledMenus.size(), MAX_MENUS); i++) {
                    Map<String, Object> menuMap = crawledMenus.get(i);
                    String name = (String) menuMap.get("name");
                    String price = (String) menuMap.get("price");
                    String description = (String) menuMap.get("description");
                    String imageUrl = (String) menuMap.get("image_url");
                    Boolean isPopular = menuMap.get("is_popular") != null ? (Boolean) menuMap.get("is_popular") : false;

                    if (name == null || name.isEmpty()) continue;

                    PlaceMenu placeMenu = new PlaceMenu(place, name, price);
                    placeMenu.setDescription(description);
                    placeMenu.setImageUrl(imageUrl);
                    placeMenu.setIsPopular(isPopular);
                    placeMenu.setDisplayOrder(i + 1);

                    // 이미지 다운로드
                    if (imageUrl != null && !imageUrl.isEmpty() && !imageUrl.equals("https://search.pstatic.net/common/")) {
                        String savedImagePath = imageProcessorService.saveMenuImage(placeId, name, imageUrl);
                        if (savedImagePath != null) {
                            placeMenu.setImagePath(savedImagePath);
                            menuWithImageCount++;
                        }
                    }

                    placeMenuRepository.save(placeMenu);

                    savedMenus.add(PlaceRefreshResponseDto.MenuDto.builder()
                            .name(name)
                            .price(price)
                            .description(description)
                            .imageUrl(imageUrl)
                            .imagePath(placeMenu.getImagePath())
                            .isPopular(isPopular)
                            .build());
                }
            }
        }

        // 7. 저장
        placeRepository.saveAndFlush(place);

        logger.info("Refresh completed for place ID: {} - Images: {}, Reviews: {}, Menus: {} (with images: {})",
                placeId, savedImageUrls.size(), newReviews.size(), savedMenus.size(), menuWithImageCount);

        return PlaceRefreshResponseDto.builder()
                .placeId(placeId)
                .placeName(place.getName())
                .imageCount(savedImageUrls.size())
                .newReviewCount(newReviews.size())
                .totalReviewCount(place.getReviews().size())
                .menuCount(savedMenus.size())
                .menuWithImageCount(menuWithImageCount)
                .imageUrls(savedImageUrls)
                .newReviews(newReviews)
                .menus(savedMenus)
                .message("Successfully refreshed place data (images, reviews, menus, business hours)")
                .build();
    }

    /**
     * 이미지만 새로고침
     */
    @Transactional
    public PlaceRefreshResponseDto refreshImages(Long placeId) {
        logger.info("Starting image refresh for place ID: {}", placeId);

        Place place = placeRepository.findByIdWithCollections(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));

        place.getImages().size();

        // 이미지 전용 크롤링 API 사용
        var imageData = crawlingService.fetchPlaceImages(place.getName(), place.getRoadAddress());

        if (imageData == null) {
            logger.warn("Image crawling failed for place ID: {}", placeId);
            return PlaceRefreshResponseDto.builder()
                    .placeId(placeId)
                    .placeName(place.getName())
                    .imageCount(0)
                    .newReviewCount(0)
                    .totalReviewCount(place.getReviews().size())
                    .imageUrls(List.of())
                    .newReviews(List.of())
                    .message("Image crawling failed")
                    .build();
        }

        @SuppressWarnings("unchecked")
        List<String> imageUrls = (List<String>) imageData.get("images");
        List<String> savedImageUrls = updateImages(place, imageUrls);

        placeRepository.saveAndFlush(place);

        logger.info("Image refresh completed for place ID: {} - Images: {}", placeId, savedImageUrls.size());

        return PlaceRefreshResponseDto.builder()
                .placeId(placeId)
                .placeName(place.getName())
                .imageCount(savedImageUrls.size())
                .newReviewCount(0)
                .totalReviewCount(place.getReviews().size())
                .imageUrls(savedImageUrls)
                .newReviews(List.of())
                .message("Successfully refreshed images")
                .build();
    }

    /**
     * 리뷰만 새로고침
     */
    @Transactional
    public PlaceRefreshResponseDto refreshReviews(Long placeId) {
        logger.info("Starting review refresh for place ID: {}", placeId);

        Place place = placeRepository.findByIdWithCollections(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));

        place.getReviews().size();

        String searchQuery = buildSearchQuery(place);
        CrawlingResponse<CrawledDataDto> response = crawlingService
                .crawlPlaceData(searchQuery, place.getName())
                .block();

        if (response == null || !response.isSuccess() || response.getData() == null) {
            logger.warn("Review crawling failed for place ID: {}", placeId);
            return PlaceRefreshResponseDto.builder()
                    .placeId(placeId)
                    .placeName(place.getName())
                    .imageCount(place.getImages().size())
                    .newReviewCount(0)
                    .totalReviewCount(place.getReviews().size())
                    .imageUrls(List.of())
                    .newReviews(List.of())
                    .message("Review crawling failed")
                    .build();
        }

        List<String> newReviews = updateReviews(place, response.getData().getReviews());

        placeRepository.saveAndFlush(place);

        logger.info("Review refresh completed for place ID: {} - New reviews: {}",
                placeId, newReviews.size());

        return PlaceRefreshResponseDto.builder()
                .placeId(placeId)
                .placeName(place.getName())
                .imageCount(place.getImages().size())
                .newReviewCount(newReviews.size())
                .totalReviewCount(place.getReviews().size())
                .imageUrls(List.of())
                .newReviews(newReviews)
                .message("Successfully refreshed reviews")
                .build();
    }

    /**
     * 영업시간만 새로고침
     *
     * @param placeId 장소 ID
     * @return 새로고침 결과
     */
    @Transactional
    public PlaceRefreshResponseDto refreshBusinessHours(Long placeId) {
        logger.info("Starting business hours refresh for place ID: {}", placeId);

        Place place = placeRepository.findByIdWithCollections(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));

        place.getBusinessHours().size();

        String searchQuery = buildSearchQuery(place);
        CrawlingResponse<CrawledDataDto> response = crawlingService
                .crawlPlaceData(searchQuery, place.getName())
                .block();

        if (response == null || !response.isSuccess() || response.getData() == null) {
            logger.warn("Business hours crawling failed for place ID: {}", placeId);
            return PlaceRefreshResponseDto.builder()
                    .placeId(placeId)
                    .placeName(place.getName())
                    .imageCount(place.getImages().size())
                    .newReviewCount(0)
                    .totalReviewCount(place.getReviews().size())
                    .menuCount(0)
                    .menuWithImageCount(0)
                    .imageUrls(List.of())
                    .newReviews(List.of())
                    .menus(List.of())
                    .message("Business hours crawling failed")
                    .build();
        }

        updateBusinessHours(place, response.getData());
        placeRepository.saveAndFlush(place);

        logger.info("Business hours refresh completed for place ID: {} - {} entries",
                placeId, place.getBusinessHours().size());

        return PlaceRefreshResponseDto.builder()
                .placeId(placeId)
                .placeName(place.getName())
                .imageCount(place.getImages().size())
                .newReviewCount(0)
                .totalReviewCount(place.getReviews().size())
                .menuCount(0)
                .menuWithImageCount(0)
                .imageUrls(List.of())
                .newReviews(List.of())
                .menus(List.of())
                .message("Successfully refreshed business hours (" + place.getBusinessHours().size() + " entries)")
                .build();
    }

    /**
     * 메뉴만 새로고침
     *
     * @param placeId 장소 ID
     * @return 새로고침 결과
     */
    @Transactional
    public PlaceRefreshResponseDto refreshMenus(Long placeId) {
        logger.info("Starting menu refresh for place ID: {}", placeId);

        Place place = placeRepository.findByIdWithCollections(placeId)
                .orElseThrow(() -> new IllegalArgumentException("Place not found: " + placeId));

        // 메뉴 크롤링 API 호출
        var menuData = crawlingService.fetchPlaceMenus(place.getName(), place.getRoadAddress());

        if (menuData == null) {
            logger.warn("Menu crawling failed for place ID: {}", placeId);
            return PlaceRefreshResponseDto.builder()
                    .placeId(placeId)
                    .placeName(place.getName())
                    .imageCount(place.getImages().size())
                    .newReviewCount(0)
                    .totalReviewCount(place.getReviews().size())
                    .menuCount(0)
                    .menuWithImageCount(0)
                    .imageUrls(List.of())
                    .newReviews(List.of())
                    .menus(List.of())
                    .message("Menu crawling failed")
                    .build();
        }

        // 메뉴 데이터 추출
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> crawledMenus = (List<Map<String, Object>>) menuData.get("menus");

        if (crawledMenus == null || crawledMenus.isEmpty()) {
            logger.info("No menus found for place ID: {}", placeId);
            return PlaceRefreshResponseDto.builder()
                    .placeId(placeId)
                    .placeName(place.getName())
                    .imageCount(place.getImages().size())
                    .newReviewCount(0)
                    .totalReviewCount(place.getReviews().size())
                    .menuCount(0)
                    .menuWithImageCount(0)
                    .imageUrls(List.of())
                    .newReviews(List.of())
                    .menus(List.of())
                    .message("No menus found")
                    .build();
        }

        // 기존 메뉴 삭제
        placeMenuRepository.deleteByPlaceId(placeId);
        placeRepository.flush();

        // 새 메뉴 저장
        List<PlaceRefreshResponseDto.MenuDto> savedMenus = new ArrayList<>();
        int menuWithImageCount = 0;

        for (int i = 0; i < Math.min(crawledMenus.size(), MAX_MENUS); i++) {
            Map<String, Object> menuMap = crawledMenus.get(i);

            String name = (String) menuMap.get("name");
            String price = (String) menuMap.get("price");
            String description = (String) menuMap.get("description");
            String imageUrl = (String) menuMap.get("image_url");
            Boolean isPopular = menuMap.get("is_popular") != null ? (Boolean) menuMap.get("is_popular") : false;

            if (name == null || name.isEmpty()) {
                continue;
            }

            PlaceMenu placeMenu = new PlaceMenu(place, name, price);
            placeMenu.setDescription(description);
            placeMenu.setImageUrl(imageUrl);
            placeMenu.setIsPopular(isPopular);
            placeMenu.setDisplayOrder(i + 1);

            // 이미지 다운로드 (이미지가 있는 경우)
            if (imageUrl != null && !imageUrl.isEmpty()) {
                String savedImagePath = imageProcessorService.saveMenuImage(placeId, name, imageUrl);
                if (savedImagePath != null) {
                    placeMenu.setImagePath(savedImagePath);
                    menuWithImageCount++;
                }
            }

            placeMenuRepository.save(placeMenu);

            savedMenus.add(PlaceRefreshResponseDto.MenuDto.builder()
                    .name(name)
                    .price(price)
                    .description(description)
                    .imageUrl(imageUrl)
                    .imagePath(placeMenu.getImagePath())
                    .isPopular(isPopular)
                    .build());
        }

        logger.info("Menu refresh completed for place ID: {} - Menus: {}, With images: {}",
                placeId, savedMenus.size(), menuWithImageCount);

        return PlaceRefreshResponseDto.builder()
                .placeId(placeId)
                .placeName(place.getName())
                .imageCount(place.getImages().size())
                .newReviewCount(0)
                .totalReviewCount(place.getReviews().size())
                .menuCount(savedMenus.size())
                .menuWithImageCount(menuWithImageCount)
                .imageUrls(List.of())
                .newReviews(List.of())
                .menus(savedMenus)
                .message("Successfully refreshed menus")
                .build();
    }

    /**
     * 전체 Places 배치 새로고침 (이미지, 리뷰, 메뉴, 영업시간)
     *
     * <p>모든 장소에 대해 새로고침을 수행합니다.</p>
     *
     * @return 배치 새로고침 결과
     */
    public BatchRefreshResponseDto refreshAllPlaces() {
        logger.info("Starting batch refresh for all places");
        long startTime = System.currentTimeMillis();

        List<Place> allPlaces = placeRepository.findAll();
        int totalPlaces = allPlaces.size();
        int successCount = 0;
        int failedCount = 0;

        List<BatchRefreshResponseDto.PlaceRefreshSummary> results = new ArrayList<>();

        for (Place place : allPlaces) {
            try {
                logger.info("[{}/{}] Refreshing place: {} (ID: {})",
                        successCount + failedCount + 1, totalPlaces, place.getName(), place.getId());

                PlaceRefreshResponseDto result = refreshPlaceData(place.getId());

                results.add(BatchRefreshResponseDto.PlaceRefreshSummary.builder()
                        .placeId(place.getId())
                        .placeName(place.getName())
                        .success(true)
                        .imageCount(result.getImageCount())
                        .reviewCount(result.getNewReviewCount())
                        .menuCount(result.getMenuCount())
                        .build());

                successCount++;
                logger.info("✅ [{}/{}] Refreshed place: {} - Images: {}, Reviews: {}, Menus: {}",
                        successCount + failedCount, totalPlaces, place.getName(),
                        result.getImageCount(), result.getNewReviewCount(), result.getMenuCount());

            } catch (Exception e) {
                failedCount++;
                logger.error("❌ [{}/{}] Failed to refresh place: {} (ID: {}) - {}",
                        successCount + failedCount, totalPlaces, place.getName(), place.getId(), e.getMessage());

                results.add(BatchRefreshResponseDto.PlaceRefreshSummary.builder()
                        .placeId(place.getId())
                        .placeName(place.getName())
                        .success(false)
                        .imageCount(0)
                        .reviewCount(0)
                        .menuCount(0)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        logger.info("Batch refresh completed: {} succeeded, {} failed, total time: {}ms",
                successCount, failedCount, elapsedTime);

        return BatchRefreshResponseDto.builder()
                .totalPlaces(totalPlaces)
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .elapsedTimeMs(elapsedTime)
                .message(String.format("Batch refresh completed: %d/%d succeeded", successCount, totalPlaces))
                .build();
    }

    /**
     * 특정 범위의 Places 배치 새로고침
     *
     * <p>지정된 offset과 limit으로 페이지네이션된 장소들에 대해 새로고침을 수행합니다.</p>
     *
     * @param offset 시작 위치
     * @param limit  최대 개수
     * @return 배치 새로고침 결과
     */
    public BatchRefreshResponseDto refreshPlacesBatch(int offset, int limit) {
        logger.info("Starting batch refresh for places: offset={}, limit={}", offset, limit);
        long startTime = System.currentTimeMillis();

        List<Place> places = placeRepository.findAll()
                .stream()
                .skip(offset)
                .limit(limit)
                .collect(Collectors.toList());

        int totalPlaces = places.size();
        int successCount = 0;
        int failedCount = 0;

        List<BatchRefreshResponseDto.PlaceRefreshSummary> results = new ArrayList<>();

        for (int i = 0; i < places.size(); i++) {
            Place place = places.get(i);
            try {
                logger.info("[{}/{}] Refreshing place: {} (ID: {})",
                        i + 1, totalPlaces, place.getName(), place.getId());

                PlaceRefreshResponseDto result = refreshPlaceData(place.getId());

                results.add(BatchRefreshResponseDto.PlaceRefreshSummary.builder()
                        .placeId(place.getId())
                        .placeName(place.getName())
                        .success(true)
                        .imageCount(result.getImageCount())
                        .reviewCount(result.getNewReviewCount())
                        .menuCount(result.getMenuCount())
                        .build());

                successCount++;

            } catch (Exception e) {
                failedCount++;
                logger.error("❌ Failed to refresh place: {} (ID: {}) - {}",
                        place.getName(), place.getId(), e.getMessage());

                results.add(BatchRefreshResponseDto.PlaceRefreshSummary.builder()
                        .placeId(place.getId())
                        .placeName(place.getName())
                        .success(false)
                        .imageCount(0)
                        .reviewCount(0)
                        .menuCount(0)
                        .errorMessage(e.getMessage())
                        .build());
            }
        }

        long elapsedTime = System.currentTimeMillis() - startTime;

        logger.info("Batch refresh completed: {} succeeded, {} failed, total time: {}ms",
                successCount, failedCount, elapsedTime);

        return BatchRefreshResponseDto.builder()
                .totalPlaces(totalPlaces)
                .successCount(successCount)
                .failedCount(failedCount)
                .results(results)
                .elapsedTimeMs(elapsedTime)
                .message(String.format("Batch refresh completed: %d/%d succeeded (offset=%d, limit=%d)",
                        successCount, totalPlaces, offset, limit))
                .build();
    }

    /**
     * 검색 쿼리 생성
     */
    private String buildSearchQuery(Place place) {
        StringBuilder query = new StringBuilder();

        if (place.getRoadAddress() != null && !place.getRoadAddress().isEmpty()) {
            // 주소에서 시/구/동 추출
            String address = place.getRoadAddress();
            String[] parts = address.split(" ");
            if (parts.length >= 2) {
                query.append(parts[0]).append(" ").append(parts[1]);
            } else {
                query.append(address);
            }
        }

        return query.toString().trim();
    }

    /**
     * 이미지 업데이트 - 기존 이미지 삭제 후 새로 저장
     */
    private List<String> updateImages(Place place, List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) {
            logger.info("No images to update for place: {}", place.getName());
            return List.of();
        }

        // 최대 5장만
        List<String> limitedUrls = imageUrls.stream()
                .limit(MAX_IMAGES)
                .collect(Collectors.toList());

        // 기존 이미지 삭제
        place.getImages().clear();

        // 새 이미지 저장
        List<String> savedPaths = imageService.downloadAndSaveImages(
                place.getId(),
                place.getName(),
                limitedUrls
        );

        // PlaceImage 엔티티 생성
        for (int i = 0; i < savedPaths.size(); i++) {
            PlaceImage placeImage = new PlaceImage();
            placeImage.setPlace(place);
            placeImage.setUrl(savedPaths.get(i));
            placeImage.setOrderIndex(i + 1);
            place.getImages().add(placeImage);
        }

        logger.info("Updated {} images for place: {}", savedPaths.size(), place.getName());
        return savedPaths;
    }

    /**
     * 리뷰 업데이트 - 중복 체크 후 새 리뷰만 추가
     */
    private List<String> updateReviews(Place place, List<String> crawledReviews) {
        if (crawledReviews == null || crawledReviews.isEmpty()) {
            logger.info("No reviews to update for place: {}", place.getName());
            return List.of();
        }

        // 기존 리뷰 텍스트 수집 (중복 체크용)
        Set<String> existingReviewTexts = place.getReviews().stream()
                .map(PlaceReview::getReviewText)
                .filter(text -> text != null && !text.isEmpty())
                .map(this::normalizeText)
                .collect(Collectors.toSet());

        List<String> newReviewTexts = new ArrayList<>();
        int currentMaxIndex = place.getReviews().stream()
                .mapToInt(r -> r.getOrderIndex() != null ? r.getOrderIndex() : 0)
                .max()
                .orElse(0);

        for (String reviewText : crawledReviews) {
            if (reviewText == null || reviewText.trim().isEmpty()) {
                continue;
            }

            String sanitizedText = sanitizeText(reviewText);
            String normalizedText = normalizeText(sanitizedText);

            // 중복 체크
            if (existingReviewTexts.contains(normalizedText)) {
                logger.debug("Skipping duplicate review: {}", sanitizedText.substring(0, Math.min(50, sanitizedText.length())));
                continue;
            }

            // 최대 리뷰 수 체크
            if (place.getReviews().size() >= MAX_REVIEWS) {
                logger.info("Max reviews ({}) reached for place: {}", MAX_REVIEWS, place.getName());
                break;
            }

            // 새 리뷰 추가
            PlaceReview review = new PlaceReview();
            review.setPlace(place);
            review.setReviewText(sanitizedText);
            review.setOrderIndex(++currentMaxIndex);
            place.getReviews().add(review);

            existingReviewTexts.add(normalizedText);
            newReviewTexts.add(sanitizedText);
        }

        logger.info("Added {} new reviews for place: {}", newReviewTexts.size(), place.getName());
        return newReviewTexts;
    }

    /**
     * 텍스트 정규화 (중복 체크용)
     */
    private String normalizeText(String text) {
        if (text == null) return "";
        return text.toLowerCase()
                .replaceAll("\\s+", " ")
                .trim();
    }

    /**
     * 텍스트 정제 (특수문자 제거)
     */
    private String sanitizeText(String text) {
        if (text == null) return "";
        return text
                .replaceAll("\\x00", "") // NULL byte 제거
                .replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "") // 제어 문자 제거
                .trim();
    }

    /**
     * 리뷰 추가 (기존 clear 후 호출)
     */
    private List<String> addReviews(Place place, List<String> crawledReviews) {
        if (crawledReviews == null || crawledReviews.isEmpty()) {
            return List.of();
        }

        List<String> addedReviews = new ArrayList<>();
        int reviewCount = Math.min(crawledReviews.size(), MAX_REVIEWS);

        for (int i = 0; i < reviewCount; i++) {
            String reviewText = crawledReviews.get(i);
            if (reviewText == null || reviewText.trim().isEmpty()) continue;

            String sanitized = sanitizeText(reviewText);
            PlaceReview review = new PlaceReview();
            review.setPlace(place);
            review.setReviewText(sanitized);
            review.setOrderIndex(i + 1);
            place.getReviews().add(review);
            addedReviews.add(sanitized);
        }

        logger.info("Added {} reviews for place: {}", addedReviews.size(), place.getName());
        return addedReviews;
    }

    /**
     * 영업시간 업데이트
     */
    private void updateBusinessHours(Place place, CrawledDataDto crawledData) {
        place.getBusinessHours().clear();

        if (crawledData.getBusinessHours() != null && crawledData.getBusinessHours().getWeekly() != null) {
            for (Map.Entry<String, com.mohe.spring.dto.crawling.WeeklyHoursDto> entry :
                    crawledData.getBusinessHours().getWeekly().entrySet()) {

                PlaceBusinessHour businessHour = new PlaceBusinessHour();
                businessHour.setPlace(place);
                businessHour.setDayOfWeek(entry.getKey());

                try {
                    if (entry.getValue().getOpen() != null && !entry.getValue().getOpen().isEmpty()) {
                        businessHour.setOpen(LocalTime.parse(entry.getValue().getOpen()));
                    }
                    if (entry.getValue().getClose() != null && !entry.getValue().getClose().isEmpty()) {
                        businessHour.setClose(LocalTime.parse(entry.getValue().getClose()));
                    }
                } catch (Exception e) {
                    logger.warn("Failed to parse business hours for {}: {}", place.getName(), e.getMessage());
                }

                businessHour.setDescription(sanitizeText(entry.getValue().getDescription()));
                businessHour.setIsOperating(entry.getValue().isOperating());

                if (crawledData.getBusinessHours().getLastOrderMinutes() != null) {
                    businessHour.setLastOrderMinutes(crawledData.getBusinessHours().getLastOrderMinutes());
                }

                place.getBusinessHours().add(businessHour);
            }
            logger.info("Updated {} business hours for place: {}", place.getBusinessHours().size(), place.getName());
        }
    }

    /**
     * SNS URL 업데이트
     */
    private void updateSnsUrls(Place place, CrawledDataDto crawledData) {
        place.getSns().clear();

        if (crawledData.getSnsUrls() != null && !crawledData.getSnsUrls().isEmpty()) {
            for (Map.Entry<String, String> entry : crawledData.getSnsUrls().entrySet()) {
                if (entry.getValue() != null && !entry.getValue().isEmpty()) {
                    PlaceSns sns = new PlaceSns();
                    sns.setPlace(place);
                    sns.setPlatform(entry.getKey());
                    sns.setUrl(entry.getValue());
                    place.getSns().add(sns);
                }
            }
            logger.info("Updated {} SNS URLs for place: {}", place.getSns().size(), place.getName());
        }
    }

    /**
     * 설명 업데이트 (원본 + AI 요약 + OpenAI Mohe 설명)
     */
    private void updateDescriptions(Place place, CrawledDataDto crawledData) {
        place.getDescriptions().clear();

        PlaceDescription description = new PlaceDescription();
        description.setPlace(place);
        description.setOriginalDescription(sanitizeText(crawledData.getOriginalDescription()));

        // AI Summary
        String aiSummaryText = "";
        if (crawledData.getAiSummary() != null && !crawledData.getAiSummary().isEmpty()) {
            aiSummaryText = String.join("\n", crawledData.getAiSummary());
        }
        description.setAiSummary(sanitizeText(aiSummaryText));
        description.setSearchQuery(buildSearchQuery(place));

        // OpenAI Mohe 설명 생성
        String textForDescription = aiSummaryText;
        if (textForDescription == null || textForDescription.isEmpty()) {
            textForDescription = crawledData.getOriginalDescription();
        }
        if ((textForDescription == null || textForDescription.isEmpty()) && crawledData.getReviews() != null) {
            int count = Math.min(crawledData.getReviews().size(), 3);
            textForDescription = String.join("\n", crawledData.getReviews().subList(0, count));
        }

        if (textForDescription != null && !textForDescription.isEmpty()) {
            try {
                String categoryStr = place.getCategory() != null ? String.join(",", place.getCategory()) : "";
                String reviewsForPrompt = crawledData.getReviews() != null ?
                        String.join("\n", crawledData.getReviews().subList(0, Math.min(10, crawledData.getReviews().size()))) : "";

                OpenAiDescriptionService.DescriptionPayload payload = new OpenAiDescriptionService.DescriptionPayload(
                        aiSummaryText,
                        reviewsForPrompt,
                        crawledData.getOriginalDescription(),
                        categoryStr,
                        place.getPetFriendly() != null ? place.getPetFriendly() : false
                );

                OpenAiDescriptionService.DescriptionResult result = openAiDescriptionService.generateDescription(payload)
                        .orElse(null);

                if (result != null && result.description() != null && !result.description().isEmpty()) {
                    description.setMoheDescription(sanitizeText(result.description()));
                    if (result.keywords() != null && result.keywords().size() == 9) {
                        place.setKeyword(result.keywords());
                    }
                    logger.info("Generated OpenAI description for place: {}", place.getName());
                } else {
                    // Fallback
                    String fallback = textForDescription.length() > 150 ?
                            textForDescription.substring(0, 147) + "..." : textForDescription;
                    description.setMoheDescription(sanitizeText(fallback));
                    logger.warn("OpenAI generation failed, using fallback for place: {}", place.getName());
                }
            } catch (Exception e) {
                logger.error("Error generating OpenAI description for place {}: {}", place.getName(), e.getMessage());
                description.setMoheDescription(place.getName() + "에 대한 정보입니다.");
            }
        } else {
            description.setMoheDescription(place.getName() + "에 대한 정보입니다.");
        }

        place.getDescriptions().add(description);
    }
}
