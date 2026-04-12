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

        // 일반 검색: 키워드 우선 (OpenAI 분석 미사용, 빠른 응답)
        List<Place> places;
        if (latitude != null && longitude != null) {
            places = searchNearbyByRelevance(trimmedQuery, trimmedQuery, latitude, longitude, safeLimit);
        } else {
            // 위치 없으면 기존 방식
            List<Long> descIds = searchByDescriptionEmbedding(trimmedQuery, null, null, safeLimit * 2);
            List<Long> kwIds = searchByKeyword(trimmedQuery, safeLimit);
            LinkedHashSet<Long> merged = new LinkedHashSet<>();
            merged.addAll(descIds);
            merged.addAll(kwIds);
            places = fetchPlaces(merged.stream().limit(safeLimit).collect(Collectors.toList()), safeLimit);
        }

        // 5. DTO 변환 + 프랜차이즈 중복 제거
        List<SimplePlaceDto> placeDtos = deduplicateFranchise(
            places.stream()
                .map(place -> convertToSimplePlaceDto(place, latitude, longitude))
                .collect(Collectors.toList()),
            safeLimit
        );

        long searchTime = System.currentTimeMillis() - startTime;

        return UnifiedSearchResponse.builder()
            .places(placeDtos)
            .totalResults(placeDtos.size())
            .query(trimmedQuery)
            .searchType("keyword")
            .searchTimeMs(searchTime)
            .message(generateSearchMessage(trimmedQuery, placeDtos.size(), null))
            .build();
    }

    /**
     * AI 검색 — 임베딩 우선 (search-results 페이지용)
     */
    public UnifiedSearchResponse searchSemantic(String query, Double latitude, Double longitude, int limit) {
        long startTime = System.currentTimeMillis();
        if (query == null || query.trim().isEmpty()) return createEmptyResponse("검색어를 입력해주세요", startTime);

        String trimmedQuery = query.trim();
        int safeLimit = Math.max(1, Math.min(limit, 50));

        List<Place> places;
        if (latitude != null && longitude != null) {
            places = searchNearbyBySemantic(trimmedQuery, latitude, longitude, safeLimit);
        } else {
            List<Long> descIds = searchByDescriptionEmbedding(trimmedQuery, null, null, safeLimit * 2);
            List<Long> kwIds = searchByKeyword(trimmedQuery, safeLimit);
            LinkedHashSet<Long> merged = new LinkedHashSet<>();
            merged.addAll(descIds);
            merged.addAll(kwIds);
            places = fetchPlaces(merged.stream().limit(safeLimit).collect(Collectors.toList()), safeLimit);
        }

        List<SimplePlaceDto> placeDtos = deduplicateFranchise(
            places.stream().map(p -> convertToSimplePlaceDto(p, latitude, longitude)).collect(Collectors.toList()),
            safeLimit
        );

        return UnifiedSearchResponse.builder()
            .places(placeDtos).totalResults(placeDtos.size()).query(trimmedQuery)
            .searchType("semantic").searchTimeMs(System.currentTimeMillis() - startTime)
            .message(generateSearchMessage(trimmedQuery, placeDtos.size(), null))
            .build();
    }

    /**
     * 임베딩 우선 → 키워드 보충 (AI 검색용)
     */
    private List<Place> searchNearbyBySemantic(String rawQuery, Double lat, Double lon, int limit) {
        LinkedHashSet<Long> mergedIds = new LinkedHashSet<>();

        // 1순위: 임베딩
        float[] queryVector = null;
        String vectorString = null;
        try {
            queryVector = embeddingClient.getEmbedding(rawQuery);
            if (!isZeroVector(queryVector)) vectorString = vectorToString(queryVector);
        } catch (Exception e) {
            System.err.println("Query embedding failed: " + e.getMessage());
        }

        if (vectorString != null) {
            for (double radius : new double[]{5.0, 10.0, 20.0, 30.0, 50.0}) {
                try {
                    List<Object[]> simResults = descEmbeddingRepository.findSimilarPlacesByPrompt(
                        vectorString, lat, lon, radius, limit * 3);
                    for (Object[] row : simResults) {
                        double sim = row[row.length - 1] != null ? ((Number) row[row.length - 1]).doubleValue() : 0;
                        if (sim >= MIN_SIMILARITY) mergedIds.add(((Number) row[0]).longValue());
                    }
                    if (mergedIds.size() >= limit) break;
                } catch (Exception e) {
                    System.err.println("Vector search failed: " + e.getMessage());
                }
            }
        }

        // 2순위: 키워드 보충
        if (mergedIds.size() < limit) {
            List<Long> kwIds = searchByKeyword(rawQuery, limit * 3);
            List<Long> nearbyKwIds = filterIdsByLocation(kwIds, lat, lon, 50.0);
            mergedIds.addAll(nearbyKwIds);
        }

        return fetchPlaces(mergedIds.stream().limit(limit).collect(Collectors.toList()), limit);
    }

    /**
     * 위치 기반 + 벡터 유사도 검색
     * 1. 사용자 질의 임베딩
     * 2. 5km부터 장소 풀 확보 (5km씩 확장)
     * 3. 그 풀 안에서 벡터 유사도 순 정렬
     */
    private static final double MIN_SIMILARITY = 0.20; // 유사도 임계값

    private List<Place> searchNearbyByRelevance(String query, String rawQuery, Double lat, Double lon, int limit) {
        LinkedHashSet<Long> mergedIds = new LinkedHashSet<>();

        // 1순위: 키워드 LIKE (이름/카테고리/설명 직접 매칭 — 고유명사에 정확)
        List<Long> kwIds = searchByKeyword(rawQuery, limit * 3);
        List<Long> nearbyKwIds = filterIdsByLocation(kwIds, lat, lon, 50.0);
        mergedIds.addAll(nearbyKwIds);
        System.out.println("🔍 Keyword match: " + nearbyKwIds.size() + " places");

        // 2순위: 문장 임베딩 (키워드 부족 시 의미적 검색으로 보충)
        if (mergedIds.size() < limit) {
            float[] queryVector = null;
            String vectorString = null;
            try {
                queryVector = embeddingClient.getEmbedding(rawQuery);
                if (!isZeroVector(queryVector)) vectorString = vectorToString(queryVector);
            } catch (Exception e) {
                System.err.println("Query embedding failed: " + e.getMessage());
            }

            if (vectorString != null) {
                for (double radius : new double[]{5.0, 10.0, 20.0, 30.0, 50.0}) {
                    try {
                        List<Object[]> simResults = descEmbeddingRepository.findSimilarPlacesByPrompt(
                            vectorString, lat, lon, radius, limit * 3);

                        for (Object[] row : simResults) {
                            double sim = row[row.length - 1] != null ? ((Number) row[row.length - 1]).doubleValue() : 0;
                            if (sim >= MIN_SIMILARITY) {
                                mergedIds.add(((Number) row[0]).longValue());
                            }
                        }
                        if (mergedIds.size() >= limit) break;
                    } catch (Exception e) {
                        System.err.println("Vector search at " + radius + "km failed: " + e.getMessage());
                    }
                }
            }
            System.out.println("🔍 + Embedding supplement: " + (mergedIds.size() - nearbyKwIds.size()) + " places");
        }

        System.out.println("🔍 Total: " + mergedIds.size() + " places");

        List<Long> finalIds = mergedIds.stream().limit(limit).collect(Collectors.toList());
        return fetchPlaces(finalIds, limit);
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
        LinkedHashSet<Long> results = new LinkedHashSet<>();
        try {
            // 전체 쿼리로 먼저 검색
            Page<Place> fullMatch = placeRepository.searchPlaces(query, PageRequest.of(0, limit));
            fullMatch.getContent().forEach(p -> results.add(p.getId()));

            // 단어별 분리 검색 (2글자 이상)
            if (results.size() < limit) {
                String[] words = query.split("\\s+");
                for (String word : words) {
                    if (word.length() < 2) continue;
                    try {
                        Page<Place> wordMatch = placeRepository.searchPlaces(word, PageRequest.of(0, limit));
                        wordMatch.getContent().forEach(p -> results.add(p.getId()));
                    } catch (Exception ignored) {}
                    if (results.size() >= limit) break;
                }
            }
        } catch (Exception e) {
            System.err.println("Keyword search failed: " + e.getMessage());
        }
        return new ArrayList<>(results);
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
     * ID 리스트를 위치 기반으로 필터 (거리 내 ID만 반환)
     */
    private List<Long> filterIdsByLocation(List<Long> placeIds, Double lat, Double lon, double maxDistKm) {
        if (placeIds.isEmpty()) return List.of();
        List<Place> places = placeRepository.findAllById(placeIds).stream()
            .filter(p -> EmbedStatus.COMPLETED.equals(p.getEmbedStatus()))
            .filter(p -> calculateDistance(lat, lon, p) <= maxDistKm)
            .sorted((a, b) -> Double.compare(calculateDistance(lat, lon, a), calculateDistance(lat, lon, b)))
            .collect(Collectors.toList());
        return places.stream().map(Place::getId).collect(Collectors.toList());
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
     * 프랜차이즈 중복 제거 — 같은 브랜드는 가장 가까운 1개만
     */
    private static final java.util.regex.Pattern BRANCH_SUFFIX = java.util.regex.Pattern.compile(
        "\\s*(\\S+점|\\S+호점|\\S+역점|본점|직영점)$"
    );

    private List<SimplePlaceDto> deduplicateFranchise(List<SimplePlaceDto> places, int limit) {
        LinkedHashMap<String, SimplePlaceDto> brandMap = new LinkedHashMap<>();
        for (SimplePlaceDto p : places) {
            String brand = extractBrand(p.getName());
            if (!brandMap.containsKey(brand)) {
                brandMap.put(brand, p);
            }
        }
        return brandMap.values().stream().limit(limit).collect(Collectors.toList());
    }

    private String extractBrand(String name) {
        if (name == null) return "";
        String cleaned = BRANCH_SUFFIX.matcher(name.trim()).replaceAll("").trim();
        // "본죽&비빔밥cafe" → "본죽" (& 이전만)
        int ampIdx = cleaned.indexOf('&');
        if (ampIdx > 0) cleaned = cleaned.substring(0, ampIdx).trim();
        return cleaned;
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
