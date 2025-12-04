package com.mohe.spring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for reverse geocoding (converting coordinates to addresses)
 * Uses Naver Geocoding API as primary, with fallback options
 */
@Service
public class AddressService {
    
    private static final Logger logger = LoggerFactory.getLogger(AddressService.class);
    private static final Duration CACHE_TIMEOUT = Duration.ofHours(1);
    
    private final WebClient webClient;
    private final String naverClientId;
    private final String naverClientSecret;
    private final String googleApiKey;
    
    // Simple in-memory cache for addresses (1 hour)
    private final Map<String, CacheEntry> addressCache = new ConcurrentHashMap<>();
    
    public AddressService(
            WebClient webClient,
            @Value("${api.naver.client-id:}") String naverClientId,
            @Value("${api.naver.client-secret:}") String naverClientSecret,
            @Value("${api.google.places-api-key:}") String googleApiKey
    ) {
        this.webClient = webClient;
        this.naverClientId = naverClientId;
        this.naverClientSecret = naverClientSecret;
        this.googleApiKey = googleApiKey;
    }
    
    /**
     * Get address information from coordinates
     */
    public AddressInfo getAddressFromCoordinates(double latitude, double longitude) {
        String cacheKey = latitude + "_" + longitude;
        CacheEntry cached = addressCache.get(cacheKey);
        
        if (cached != null && System.currentTimeMillis() - cached.timestamp < CACHE_TIMEOUT.toMillis()) {
            logger.debug("Returning cached address for coordinates: {}, {}", latitude, longitude);
            return cached.addressInfo;
        }
        
        AddressInfo address;
        try {
            if (naverClientId != null && !naverClientId.isBlank() &&
                naverClientSecret != null && !naverClientSecret.isBlank()) {
                logger.info("Attempting to get address from Naver API");
                address = getAddressFromNaver(latitude, longitude);
            } else {
                logger.info("Naver API keys not configured, using fallback address");
                address = createFallbackAddress(latitude, longitude);
            }
        } catch (Exception error) {
            logger.warn("Failed to get address from Naver API, using fallback", error);
            address = createFallbackAddress(latitude, longitude);
        }
        
        // Cache the result
        addressCache.put(cacheKey, new CacheEntry(address, System.currentTimeMillis()));
        cleanupCache();
        
        return address;
    }
    
    /**
     * Get address using Naver Reverse Geocoding API
     */
    private AddressInfo getAddressFromNaver(double latitude, double longitude) {
        logger.info("Getting address from Naver for coordinates: {}, {}", latitude, longitude);
        logger.debug("Using Naver API credentials - Client ID: {}", naverClientId != null ? naverClientId.substring(0, 4) + "..." : "null");

        try {
            String coords = longitude + "," + latitude; // Naver uses lon,lat format
            String apiUrl = "https://naveropenapi.apigw.ntruss.com/map-reversegeocode/v2/gc?coords=" + coords + "&sourcecrs=epsg:4326&output=json&orders=roadaddr";
            logger.debug("Naver API request URL: {}", apiUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = webClient.get()
                .uri(apiUrl)
                .header("X-NCP-APIGW-API-KEY-ID", naverClientId)
                .header("X-NCP-APIGW-API-KEY", naverClientSecret)
                .retrieve()
                .bodyToMono(Map.class)
                .retryWhen(
                    Retry.backoff(3, Duration.ofSeconds(1))
                        .maxBackoff(Duration.ofSeconds(5))
                        .filter(throwable -> throwable instanceof WebClientResponseException)
                )
                .block(Duration.ofSeconds(3));

            if (response == null) {
                logger.error("❌ Empty response from Naver Geocoding API");
                throw new RuntimeException("Empty response from Naver Geocoding");
            }

            // Check for API error response
            if (response.containsKey("error")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> error = (Map<String, Object>) response.get("error");
                String errorCode = (String) error.get("errorCode");
                String errorMessage = (String) error.get("message");
                String errorDetails = (String) error.get("details");

                logger.error("❌ Naver API Error - Code: {}, Message: {}, Details: {}",
                    errorCode, errorMessage, errorDetails);
                logger.error("💡 해결 방법:");
                logger.error("   1. Naver Cloud Platform Console 접속: https://console.ncloud.com");
                logger.error("   2. AI·NAVER API > Application 메뉴에서 '{}' 확인", naverClientId);
                logger.error("   3. 서비스 선택 탭에서 'Reverse Geocoding' 체크 여부 확인");
                logger.error("   4. 자세한 가이드: /Users/andrewlim/Desktop/Mohe/NAVER_API_SETUP_GUIDE.md");

                if ("200".equals(errorCode)) {
                    throw new RuntimeException("Naver API Authentication Failed - API 키를 확인하세요. 가이드: NAVER_API_SETUP_GUIDE.md");
                } else if ("429".equals(errorCode)) {
                    throw new RuntimeException("Naver API Quota Exceeded - Reverse Geocoding 서비스를 활성화하세요. 가이드: NAVER_API_SETUP_GUIDE.md");
                } else {
                    throw new RuntimeException("Naver API Error: " + errorMessage);
                }
            }

            logger.info("✅ Successfully received response from Naver API");
            return parseNaverResponse(response, latitude, longitude);
        } catch (Exception error) {
            logger.error("❌ Failed to get address from Naver: {}", error.getMessage());
            logger.error("🔍 Falling back to approximate location based on coordinates");
            throw error;
        }
    }
    
    /**
     * Parse Naver Reverse Geocoding response
     */
    @SuppressWarnings("unchecked")
    private AddressInfo parseNaverResponse(Map<String, Object> response, double latitude, double longitude) {
        try {
            Map<String, Object> status = (Map<String, Object>) response.get("status");
            Integer statusCode = status != null ? (Integer) status.get("code") : null;
            
            if (statusCode == null || statusCode != 0) {
                logger.warn("Naver API returned non-zero status: {}", statusCode);
                return createFallbackAddress(latitude, longitude);
            }
            
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null || results.isEmpty()) {
                logger.warn("No results from Naver API");
                return createFallbackAddress(latitude, longitude);
            }
            
            Map<String, Object> result = results.get(0);
            Map<String, Object> region = (Map<String, Object>) result.get("region");
            Map<String, Object> land = (Map<String, Object>) result.get("land");
            
            // Extract address components
            String sido = extractRegionName(region, "area1");
            String sigungu = extractRegionName(region, "area2");
            String dong = extractRegionName(region, "area3");
            String roadName = land != null ? (String) land.get("name") : "";
            String buildingNumber = land != null ? (String) land.get("number1") : "";
            
            // Create formatted address
            StringBuilder fullAddressBuilder = new StringBuilder();
            if (!sido.isEmpty()) fullAddressBuilder.append(sido);
            if (!sigungu.isEmpty()) {
                if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(" ");
                fullAddressBuilder.append(sigungu);
            }
            if (!dong.isEmpty()) {
                if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(" ");
                fullAddressBuilder.append(dong);
            }
            if (!roadName.isEmpty()) {
                if (fullAddressBuilder.length() > 0) fullAddressBuilder.append(" ");
                fullAddressBuilder.append(roadName);
                if (!buildingNumber.isEmpty()) {
                    fullAddressBuilder.append(" ").append(buildingNumber);
                }
            }
            String fullAddress = fullAddressBuilder.toString();
            
            StringBuilder shortAddressBuilder = new StringBuilder();
            if (!sigungu.isEmpty()) shortAddressBuilder.append(sigungu);
            if (!dong.isEmpty()) {
                if (shortAddressBuilder.length() > 0) shortAddressBuilder.append(" ");
                shortAddressBuilder.append(dong);
            }
            String shortAddress = shortAddressBuilder.toString();
            
            AddressInfo fallback = createFallbackAddress(latitude, longitude);
            
            return new AddressInfo(
                !fullAddress.isEmpty() ? fullAddress : fallback.getFullAddress(),
                !shortAddress.isEmpty() ? shortAddress : fallback.getShortAddress(),
                sido,
                sigungu,
                dong,
                roadName,
                buildingNumber,
                latitude,
                longitude
            );
        } catch (Exception error) {
            logger.error("Failed to parse Naver response", error);
            return createFallbackAddress(latitude, longitude);
        }
    }
    
    @SuppressWarnings("unchecked")
    private String extractRegionName(Map<String, Object> region, String areaKey) {
        if (region == null) return "";
        Map<String, Object> area = (Map<String, Object>) region.get(areaKey);
        if (area == null) return "";
        String name = (String) area.get("name");
        return name != null ? name : "";
    }
    
    /**
     * Create fallback address when API calls fail
     */
    private AddressInfo createFallbackAddress(double latitude, double longitude) {
        // Simple geographic approximation for major Korean cities
        ApproximateLocation approximateLocation = getApproximateLocation(latitude, longitude);
        
        if (approximateLocation != null) {
            return new AddressInfo(
                approximateLocation.getFull(),
                approximateLocation.getShort(),
                approximateLocation.getSido(),
                approximateLocation.getSigungu(),
                approximateLocation.getDong(),
                "",
                "",
                latitude,
                longitude
            );
        } else {
            String formattedCoords = String.format("위도 %.4f, 경도 %.4f", latitude, longitude);
            return new AddressInfo(
                formattedCoords,
                formattedCoords,
                "",
                "",
                "",
                "",
                "",
                latitude,
                longitude
            );
        }
    }
    
    /**
     * Approximate Korean location based on coordinates
     */
    private ApproximateLocation getApproximateLocation(double lat, double lon) {
        // Seoul area (37.4-37.7, 126.7-127.2)
        if (lat >= 37.4 && lat <= 37.7 && lon >= 126.7 && lon <= 127.2) {
            if (lat >= 37.6 && lon <= 126.9) {
                return new ApproximateLocation("서울특별시 강서구", "강서구", "서울특별시", "강서구", "");
            } else if (lat >= 37.6 && lon >= 127.0) {
                return new ApproximateLocation("서울특별시 강북구", "강북구", "서울특별시", "강북구", "");
            } else if (lat >= 37.5 && lat < 37.6 && lon <= 126.9) {
                return new ApproximateLocation("서울특별시 마포구", "마포구", "서울특별시", "마포구", "");
            } else if (lat >= 37.5 && lat < 37.6 && lon >= 127.0) {
                return new ApproximateLocation("서울특별시 강남구", "강남구", "서울특별시", "강남구", "");
            } else {
                return new ApproximateLocation("서울특별시 중구", "중구", "서울특별시", "중구", "");
            }
        }
        
        // Gyeonggi-do area (37.0-37.6, 126.8-127.6)  
        if (lat >= 37.0 && lat <= 37.6 && lon >= 126.8 && lon <= 127.6) {
            if (lat >= 37.2 && lat <= 37.4 && lon >= 127.0 && lon <= 127.4) {
                return new ApproximateLocation("경기도 용인시 기흥구", "용인시 기흥구", "경기도", "용인시", "보정동");
            } else if (lat >= 37.3 && lat <= 37.5 && lon >= 127.0 && lon <= 127.3) {
                return new ApproximateLocation("경기도 성남시 분당구", "성남시 분당구", "경기도", "성남시", "분당동");
            } else if (lat >= 37.4 && lat <= 37.6 && lon >= 126.9 && lon <= 127.2) {
                return new ApproximateLocation("경기도 고양시 일산서구", "고양시 일산서구", "경기도", "고양시", "일산동");
            } else {
                return new ApproximateLocation("경기도", "경기도", "경기도", "", "");
            }
        }
        
        // Incheon area (37.2-37.6, 126.4-126.9)
        if (lat >= 37.2 && lat <= 37.6 && lon >= 126.4 && lon <= 126.9) {
            return new ApproximateLocation("인천광역시 연수구", "연수구", "인천광역시", "연수구", "");
        }
        
        return null;
    }
    
    /**
     * Clean up expired cache entries
     */
    private void cleanupCache() {
        long now = System.currentTimeMillis();
        addressCache.entrySet().removeIf(entry -> now - entry.getValue().timestamp > CACHE_TIMEOUT.toMillis());
    }
    
    private static class CacheEntry {
        final AddressInfo addressInfo;
        final long timestamp;
        
        CacheEntry(AddressInfo addressInfo, long timestamp) {
            this.addressInfo = addressInfo;
            this.timestamp = timestamp;
        }
    }
}


/**
 * Internal data class for approximate location mapping
 */
class ApproximateLocation {
    private final String full;
    private final String shortForm;
    private final String sido;
    private final String sigungu;
    private final String dong;
    
    public ApproximateLocation(String full, String shortForm, String sido, String sigungu, String dong) {
        this.full = full;
        this.shortForm = shortForm;
        this.sido = sido;
        this.sigungu = sigungu;
        this.dong = dong;
    }
    
    // Getters
    public String getFull() { return full; }
    public String getShort() { return shortForm; }
    public String getSido() { return sido; }
    public String getSigungu() { return sigungu; }
    public String getDong() { return dong; }
}