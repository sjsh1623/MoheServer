package com.mohe.spring.service;

import com.mohe.spring.dto.SimplePlaceDto;
import com.mohe.spring.dto.UnifiedSearchResponse;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.PlaceKeywordEmbedding;
import com.mohe.spring.repository.PlaceDescriptionEmbeddingRepository;
import com.mohe.spring.repository.PlaceKeywordEmbeddingRepository;
import com.mohe.spring.repository.PlaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
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
 * 1. OpenAI 쿼리 분석 - 카테고리, 키워드, 의도 추출
 * 2. Kanana Embedding 벡터 검색 - 의미론적 유사도
 * 3. 키워드 검색 (LIKE) - 장소명, 주소
 * 4. 하이브리드 검색 - 분석 결과 + 벡터 + 키워드 결합
 */
@Service
@Transactional(readOnly = true)
public class UnifiedSearchService {

    private final PlaceRepository placeRepository;
    private final PlaceKeywordEmbeddingRepository placeKeywordEmbeddingRepository;
    private final PlaceDescriptionEmbeddingRepository descEmbeddingRepository;
    private final EmbeddingClient embeddingClient;
    private final KeywordEmbeddingService keywordEmbeddingService;
    private final PlaceService placeService;
    private final OpenAiService openAiService;

    @Autowired
    public UnifiedSearchService(PlaceRepository placeRepository,
                                PlaceKeywordEmbeddingRepository placeKeywordEmbeddingRepository,
                                PlaceDescriptionEmbeddingRepository descEmbeddingRepository,
                                EmbeddingClient embeddingClient,
                                KeywordEmbeddingService keywordEmbeddingService,
                                PlaceService placeService,
                                @Autowired(required = false) OpenAiService openAiService) {
        this.placeRepository = placeRepository;
        this.placeKeywordEmbeddingRepository = placeKeywordEmbeddingRepository;
        this.descEmbeddingRepository = descEmbeddingRepository;
        this.embeddingClient = embeddingClient;
        this.keywordEmbeddingService = keywordEmbeddingService;
        this.placeService = placeService;
        this.openAiService = openAiService;
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

        // 0. OpenAI 쿼리 분석 (카테고리, 키워드, 의도 추출)
        String enrichedQuery = trimmedQuery;
        String category = null;
        String intent = null;

        if (openAiService != null) {
            try {
                System.out.println("🔍 OpenAI 쿼리 분석 시작: " + trimmedQuery);
                OpenAiService.QueryAnalysisResult analysis = openAiService.analyzeSearchQuery(trimmedQuery);
                enrichedQuery = analysis.getEnrichedQuery();
                category = analysis.getCategory();
                intent = analysis.getIntent();
                System.out.println("✅ OpenAI 분석 완료 - 카테고리: " + category + ", 키워드: " + analysis.getKeywords() + ", 의도: " + intent);
            } catch (Exception e) {
                System.err.println("⚠️ OpenAI 분석 실패, 원본 쿼리 사용: " + e.getMessage());
            }
        }

        // 위치 기반 검색: 가까운 곳에서 벡터 유사도로 정렬
        List<Place> places;
        if (latitude != null && longitude != null) {
            places = searchNearbyByRelevance(enrichedQuery, trimmedQuery, latitude, longitude, safeLimit);
        } else {
            // 위치 없으면 기존 방식
            List<Long> descIds = searchByDescriptionEmbedding(enrichedQuery, null, null, safeLimit * 2);
            List<Long> kwIds = searchByKeyword(trimmedQuery, safeLimit);
            LinkedHashSet<Long> merged = new LinkedHashSet<>();
            merged.addAll(descIds);
            merged.addAll(kwIds);
            places = fetchPlaces(merged.stream().limit(safeLimit).collect(Collectors.toList()), safeLimit);
        }

        // 5. DTO 변환
        List<SimplePlaceDto> placeDtos = places.stream()
            .map(place -> convertToSimplePlaceDto(place, latitude, longitude))
            .collect(Collectors.toList());

        long searchTime = System.currentTimeMillis() - startTime;

        String searchType = !places.isEmpty() ? "semantic" : "keyword";
        if (openAiService != null && category != null) {
            searchType = "ai-" + searchType;
        }

        return UnifiedSearchResponse.builder()
            .places(placeDtos)
            .totalResults(placeDtos.size())
            .query(trimmedQuery)
            .searchType(searchType)
            .searchTimeMs(searchTime)
            .message(generateSearchMessage(trimmedQuery, placeDtos.size(), intent))
            .build();
    }

    /**
     * 문장 임베딩 벡터 검색 - 프롬프트형 질의에 최적
     * place_description_embeddings의 mohe_description 문장과 cosine 유사도
     */
    /**
     * 위치 기반 + 벡터 유사도 검색
     * 1. 사용자 질의 임베딩
     * 2. 5km부터 장소 풀 확보 (5km씩 확장)
     * 3. 그 풀 안에서 벡터 유사도 순 정렬
     */
    private static final double MIN_SIMILARITY = 0.25; // 유사도 임계값 — 이 이하는 관련 없는 결과

    private List<Place> searchNearbyByRelevance(String query, String rawQuery, Double lat, Double lon, int limit) {
        // 사용자 질의 임베딩
        float[] queryVector = null;
        String vectorString = null;
        try {
            queryVector = embeddingClient.getEmbedding(query);
            if (!isZeroVector(queryVector)) vectorString = vectorToString(queryVector);
        } catch (Exception e) {
            System.err.println("Query embedding failed: " + e.getMessage());
        }

        // 1km부터 확장하며 근처 장소에서 벡터 유사도 검색
        for (double radius = 1.0; radius <= 50.0; radius += (radius < 5 ? 1.0 : radius < 15 ? 2.0 : 5.0)) {
            if (vectorString != null) {
                try {
                    // SQL: 거리 필터 + 벡터 유사도 정렬 (한번에)
                    List<Object[]> simResults = descEmbeddingRepository.findSimilarPlacesByPrompt(
                        vectorString, lat, lon, radius, limit * 3);

                    if (!simResults.isEmpty()) {
                        // 유사도 임계값 이상만 필터
                        List<Object[]> filtered = simResults.stream()
                            .filter(row -> {
                                double sim = row[row.length - 1] != null ? ((Number) row[row.length - 1]).doubleValue() : 0;
                                return sim >= MIN_SIMILARITY;
                            })
                            .collect(Collectors.toList());

                        if (!filtered.isEmpty()) {
                            List<Long> ids = filtered.stream()
                                .map(row -> ((Number) row[0]).longValue())
                                .distinct().limit(limit * 2).collect(Collectors.toList());

                            List<Place> places = placeRepository.findAllById(ids);
                            Map<Long, Place> map = places.stream()
                                .collect(Collectors.toMap(Place::getId, p -> p, (a, b) -> a));

                            List<Place> result = ids.stream()
                                .filter(map::containsKey).map(map::get).collect(Collectors.toList());

                            // 랜덤성
                            if (result.size() > limit) {
                                List<Place> pool = new ArrayList<>(result.subList(0, Math.min(result.size(), limit * 2)));
                                Collections.shuffle(pool);
                                result = pool.subList(0, Math.min(pool.size(), limit));
                            }
                            return result;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Vector search at " + radius + "km failed: " + e.getMessage());
                }
            }

            // 벡터 없으면 LIKE 검색으로 fallback (5km 이상부터)
            if (vectorString == null && radius >= 5.0) break;
        }

        // fallback: 키워드 LIKE 검색 + 거리 필터
        List<Long> kwIds = searchByKeyword(rawQuery, limit * 3);
        return filterAndSortByLocation(kwIds, lat, lon, limit);
    }

    private List<Long> searchByDescriptionEmbedding(String query, Double latitude, Double longitude, int limit) {
        try {
            float[] queryVector = embeddingClient.getEmbedding(query);
            if (isZeroVector(queryVector)) return List.of();

            String vectorString = vectorToString(queryVector);

            if (latitude != null && longitude != null) {
                // 점진적 확장: 5km → 10km → 15km → 20km → 30km → 50km
                for (double radius : new double[]{5.0, 10.0, 15.0, 20.0, 30.0, 50.0}) {
                    List<Object[]> results = descEmbeddingRepository.findSimilarPlacesByPrompt(
                        vectorString, latitude, longitude, radius, limit);
                    if (results.size() >= Math.min(limit, 3)) {
                        return results.stream()
                            .map(row -> ((Number) row[0]).longValue())
                            .distinct().collect(Collectors.toList());
                    }
                }
            }

            // 위치 없거나 결과 부족 시 전체 검색
            List<Object[]> results = descEmbeddingRepository.findSimilarPlaces(vectorString, limit);
            return results.stream()
                .map(row -> ((Number) row[0]).longValue())
                .distinct().collect(Collectors.toList());
        } catch (Exception e) {
            System.err.println("Description embedding search failed: " + e.getMessage());
            return List.of();
        }
    }

    /**
     * 키워드 Embedding 벡터 검색 - 카테고리형 유사도 기반
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

        // 점진적 거리 확장: 5km씩
        for (double radius : new double[]{5.0, 10.0, 15.0, 20.0, 30.0, 50.0}) {
            List<Place> nearby = places.stream()
                .filter(p -> calculateDistance(latitude, longitude, p) <= radius)
                .sorted((p1, p2) -> Double.compare(
                    calculateDistance(latitude, longitude, p1),
                    calculateDistance(latitude, longitude, p2)))
                .limit(limit)
                .collect(Collectors.toList());

            if (nearby.size() >= Math.min(limit, 3)) {
                return nearby;
            }
        }

        // 거리 무관 가까운 순
        places.sort((p1, p2) -> Double.compare(
            calculateDistance(latitude, longitude, p1),
            calculateDistance(latitude, longitude, p2)));
        return places.stream().limit(limit).collect(Collectors.toList());
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
        return generateSearchMessage(query, resultCount, null);
    }

    /**
     * 검색 메시지 생성 (의도 포함)
     */
    private static final List<String> FOUND_TEMPLATES = List.of(
        "'%s'에 딱 맞는 곳을 %d개 찾았어요!",
        "'%s' 관련 장소 %d곳을 골라봤어요.",
        "'%s'에 어울리는 곳들이에요!",
        "%d곳을 찾았어요. '%s' 분위기에 딱이에요!",
        "'%s' 느낌의 장소를 추천해드려요.",
        "이런 곳은 어때요? '%s'에 맞는 %d곳이에요."
    );

    private static final List<String> NOT_FOUND_TEMPLATES = List.of(
        "'%s'에 맞는 곳을 아직 못 찾았어요. 다른 키워드로 해볼까요?",
        "'%s' 관련 장소가 주변에 없네요. 조금 다르게 검색해볼까요?",
        "아쉽지만 '%s' 결과가 없어요. 다른 분위기를 알려주세요!"
    );

    private final Random messageRandom = new Random();

    private String generateSearchMessage(String query, int resultCount, String intent) {
        if (resultCount == 0) {
            String tmpl = NOT_FOUND_TEMPLATES.get(messageRandom.nextInt(NOT_FOUND_TEMPLATES.size()));
            return String.format(tmpl, query);
        }
        if (intent != null && !intent.isEmpty() && !intent.equals(query)) {
            return intent + " - " + resultCount + "곳을 찾았어요!";
        }
        String tmpl = FOUND_TEMPLATES.get(messageRandom.nextInt(FOUND_TEMPLATES.size()));
        return String.format(tmpl, query, resultCount);
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
