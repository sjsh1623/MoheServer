package com.mohe.spring.service;

import com.mohe.spring.dto.SimplePlaceDto;
import com.mohe.spring.dto.UnifiedSearchResponse;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceKeywordEmbedding;
import com.mohe.spring.repository.PlaceKeywordEmbeddingRepository;
import com.mohe.spring.repository.PlaceRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 통합 검색 서비스 - 장소/지역/음식/활동 등 의미론적 Embedding 검색 지원
 *
 * 검색 방식:
 * 1. 키워드 검색 (LIKE) - 장소명, 주소
 * 2. Embedding 벡터 검색 - 의미론적 유사도 (음식, 활동, 분위기 등)
 * 3. 하이브리드 검색 - 키워드 + 벡터 결합
 */
@Service
@Transactional(readOnly = true)
public class UnifiedSearchService {

    private final PlaceRepository placeRepository;
    private final PlaceKeywordEmbeddingRepository placeKeywordEmbeddingRepository;
    private final KeywordEmbeddingService keywordEmbeddingService;
    private final PlaceService placeService;

    public UnifiedSearchService(PlaceRepository placeRepository,
                                PlaceKeywordEmbeddingRepository placeKeywordEmbeddingRepository,
                                KeywordEmbeddingService keywordEmbeddingService,
                                PlaceService placeService) {
        this.placeRepository = placeRepository;
        this.placeKeywordEmbeddingRepository = placeKeywordEmbeddingRepository;
        this.keywordEmbeddingService = keywordEmbeddingService;
        this.placeService = placeService;
    }

    /**
     * 통합 검색 - 장소명, 지역명, 음식, 활동 등 모든 검색 지원
     *
     * @param query 검색어 (예: "파스타", "성수동 카페", "데이트 장소", "혼밥")
     * @param latitude 사용자 위도 (선택)
     * @param longitude 사용자 경도 (선택)
     * @param limit 결과 개수
     * @return 통합 검색 결과
     */
    public UnifiedSearchResponse search(String query, Double latitude, Double longitude, int limit) {
        long startTime = System.currentTimeMillis();

        if (query == null || query.trim().isEmpty()) {
            return createEmptyResponse("검색어를 입력해주세요", startTime);
        }

        String trimmedQuery = query.trim();
        int safeLimit = Math.max(1, Math.min(limit, 50));

        // 1. Embedding 벡터 검색 (의미론적 검색)
        List<Long> embeddingPlaceIds = searchByEmbedding(trimmedQuery, safeLimit * 3);

        // 2. 키워드 검색 (장소명, 주소)
        List<Long> keywordPlaceIds = searchByKeyword(trimmedQuery, safeLimit * 2);

        // 3. 결과 병합 (Embedding 결과 우선, 키워드 결과 보조)
        List<Long> mergedPlaceIds = mergeResults(embeddingPlaceIds, keywordPlaceIds, safeLimit * 2);

        // 4. 위치 기반 필터링 및 정렬 (선택)
        List<Place> places;
        if (latitude != null && longitude != null) {
            places = filterAndSortByLocation(mergedPlaceIds, latitude, longitude, safeLimit);
        } else {
            places = fetchPlaces(mergedPlaceIds, safeLimit);
        }

        // 5. DTO 변환
        List<SimplePlaceDto> placeDtos = places.stream()
            .map(place -> convertToSimplePlaceDto(place, latitude, longitude))
            .collect(Collectors.toList());

        long searchTime = System.currentTimeMillis() - startTime;

        return UnifiedSearchResponse.builder()
            .places(placeDtos)
            .totalResults(placeDtos.size())
            .query(trimmedQuery)
            .searchType(determineSearchType(embeddingPlaceIds, keywordPlaceIds))
            .searchTimeMs(searchTime)
            .message(generateSearchMessage(trimmedQuery, placeDtos.size()))
            .build();
    }

    /**
     * Embedding 벡터 검색 - 의미론적 유사도 기반
     */
    private List<Long> searchByEmbedding(String query, int limit) {
        try {
            // 검색어를 벡터로 변환
            float[] queryVector = keywordEmbeddingService.vectorizeKeywords(new String[]{query});

            // 영벡터 체크 (임베딩 서비스 실패 시)
            if (isZeroVector(queryVector)) {
                System.out.println("Embedding search skipped: zero vector returned for query '" + query + "'");
                return List.of();
            }

            // pgvector 유사도 검색
            String vectorString = vectorToString(queryVector);
            List<PlaceKeywordEmbedding> similarEmbeddings =
                placeKeywordEmbeddingRepository.findSimilarByEmbedding(vectorString, limit * 2);

            // place_id 추출 (중복 제거, 순서 유지)
            return similarEmbeddings.stream()
                .map(PlaceKeywordEmbedding::getPlaceId)
                .distinct()
                .limit(limit)
                .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Embedding search failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 키워드 검색 - 장소명, 주소 LIKE 검색
     */
    private List<Long> searchByKeyword(String query, int limit) {
        try {
            Page<Place> searchResults = placeRepository.searchPlaces(query, PageRequest.of(0, limit));
            return searchResults.getContent().stream()
                .map(Place::getId)
                .collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Keyword search failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 검색 결과 병합 - Embedding 결과 우선
     */
    private List<Long> mergeResults(List<Long> embeddingIds, List<Long> keywordIds, int limit) {
        LinkedHashSet<Long> merged = new LinkedHashSet<>();

        // Embedding 결과 우선 추가
        merged.addAll(embeddingIds);

        // 키워드 결과 추가 (중복 제외)
        merged.addAll(keywordIds);

        return merged.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 위치 기반 필터링 및 정렬
     */
    private List<Place> filterAndSortByLocation(List<Long> placeIds, Double latitude, Double longitude, int limit) {
        if (placeIds.isEmpty()) {
            return List.of();
        }

        List<Place> places = placeRepository.findAllById(placeIds).stream()
            .filter(place -> EmbedStatus.COMPLETED.equals(place.getEmbedStatus()))
            .collect(Collectors.toList());

        // 거리 계산 및 정렬
        places.sort((p1, p2) -> {
            double dist1 = calculateDistance(latitude, longitude, p1);
            double dist2 = calculateDistance(latitude, longitude, p2);
            return Double.compare(dist1, dist2);
        });

        return places.stream()
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 장소 조회 (위치 없이)
     */
    private List<Place> fetchPlaces(List<Long> placeIds, int limit) {
        if (placeIds.isEmpty()) {
            return List.of();
        }

        // ID 순서 유지하면서 조회
        Map<Long, Place> placeMap = placeRepository.findAllById(placeIds).stream()
            .filter(place -> EmbedStatus.COMPLETED.equals(place.getEmbedStatus()))
            .collect(Collectors.toMap(Place::getId, p -> p));

        return placeIds.stream()
            .filter(placeMap::containsKey)
            .map(placeMap::get)
            .limit(limit)
            .collect(Collectors.toList());
    }

    /**
     * 거리 계산 (Haversine)
     */
    private double calculateDistance(Double latitude, Double longitude, Place place) {
        if (latitude == null || longitude == null) {
            return Double.MAX_VALUE;
        }

        BigDecimal placeLat = place.getLatitude();
        BigDecimal placeLon = place.getLongitude();

        if (placeLat == null || placeLon == null) {
            return Double.MAX_VALUE;
        }

        final double earthRadius = 6371.0; // km
        double dLat = Math.toRadians(placeLat.doubleValue() - latitude);
        double dLon = Math.toRadians(placeLon.doubleValue() - longitude);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
            + Math.cos(Math.toRadians(latitude)) * Math.cos(Math.toRadians(placeLat.doubleValue()))
            * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }

    /**
     * SimplePlaceDto 변환
     */
    private SimplePlaceDto convertToSimplePlaceDto(Place place, Double latitude, Double longitude) {
        List<String> imageUrls = placeService.getImageUrls(place.getId());
        String primaryImage = imageUrls.isEmpty() ? null : imageUrls.get(0);

        String fullAddress = place.getRoadAddress();
        String shortAddress = extractShortAddress(fullAddress);

        SimplePlaceDto dto = new SimplePlaceDto(
            place.getId().toString(),
            place.getName(),
            place.getCategory() != null && !place.getCategory().isEmpty() ? place.getCategory().get(0) : "기타",
            place.getRating() != null ? place.getRating().doubleValue() : 4.0,
            shortAddress,
            primaryImage
        );

        dto.setReviewCount(place.getReviewCount() != null ? place.getReviewCount() : 0);
        dto.setAddress(fullAddress);
        dto.setShortAddress(shortAddress);
        dto.setFullAddress(fullAddress);
        dto.setImages(imageUrls);
        dto.setIsBookmarked(false);
        dto.setIsDemo(false);

        // 거리 계산
        if (latitude != null && longitude != null) {
            double distance = calculateDistance(latitude, longitude, place);
            if (distance != Double.MAX_VALUE) {
                dto.setDistance(Math.round(distance * 10.0) / 10.0);
            }
        }

        // mohe_description
        String moheDescription = place.getDescriptions().stream()
            .filter(desc -> desc.getMoheDescription() != null && !desc.getMoheDescription().isEmpty())
            .map(desc -> desc.getMoheDescription())
            .findFirst()
            .orElse(null);
        dto.setDescription(moheDescription);

        return dto;
    }

    /**
     * 주소 축약 (구+동)
     */
    private String extractShortAddress(String fullAddress) {
        if (fullAddress == null || fullAddress.isBlank()) {
            return "";
        }

        try {
            String address = fullAddress.replaceFirst(
                "^(서울특별시|부산광역시|대구광역시|인천광역시|광주광역시|대전광역시|울산광역시|세종특별자치시|경기도|강원도|충청북도|충청남도|전라북도|전라남도|경상북도|경상남도|제주특별자치도)\\s*",
                ""
            );

            String[] parts = address.split("\\s+");

            if (parts.length >= 2) {
                String district = parts[0];
                String neighborhood = parts[1];

                if (parts.length >= 3 && district.endsWith("시") && (parts[1].endsWith("구") || parts[1].endsWith("군"))) {
                    return parts[1] + " " + parts[2];
                }

                return district + " " + neighborhood;
            }

            return parts.length > 0 ? parts[0] : "";

        } catch (Exception e) {
            return "";
        }
    }

    /**
     * float[] → pgvector 문자열 변환
     */
    private String vectorToString(float[] vector) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < vector.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(vector[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * 영벡터 체크
     */
    private boolean isZeroVector(float[] vector) {
        if (vector == null || vector.length == 0) {
            return true;
        }
        for (float v : vector) {
            if (v != 0.0f) {
                return false;
            }
        }
        return true;
    }

    /**
     * 검색 타입 결정
     */
    private String determineSearchType(List<Long> embeddingIds, List<Long> keywordIds) {
        boolean hasEmbedding = !embeddingIds.isEmpty();
        boolean hasKeyword = !keywordIds.isEmpty();

        if (hasEmbedding && hasKeyword) {
            return "hybrid";
        } else if (hasEmbedding) {
            return "embedding";
        } else if (hasKeyword) {
            return "keyword";
        } else {
            return "none";
        }
    }

    /**
     * 검색 메시지 생성
     */
    private String generateSearchMessage(String query, int resultCount) {
        if (resultCount == 0) {
            return "'" + query + "'에 대한 검색 결과가 없습니다.";
        }
        return "'" + query + "' 검색 결과 " + resultCount + "개를 찾았습니다.";
    }

    /**
     * 빈 응답 생성
     */
    private UnifiedSearchResponse createEmptyResponse(String message, long startTime) {
        long searchTime = System.currentTimeMillis() - startTime;
        return UnifiedSearchResponse.builder()
            .places(List.of())
            .totalResults(0)
            .query("")
            .searchType("none")
            .searchTimeMs(searchTime)
            .message(message)
            .build();
    }
}
