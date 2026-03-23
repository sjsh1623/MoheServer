package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.service.GovernmentApiService;
import com.mohe.spring.service.GovernmentApiService.ReverseGeocodeResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

/**
 * 위치 관련 API
 * - 역지오코딩: 좌표 → 행정구역 주소 (Vworld API)
 * - 사용자 위치 등록: 40km 내 지역 크롤링 우선순위 큐 삽입
 */
@RestController
@RequestMapping("/api/location")
public class LocationController {

    private static final Logger logger = LoggerFactory.getLogger(LocationController.class);

    private final GovernmentApiService governmentApiService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${batch.collector.url:http://localhost:4001}")
    private String batchCollectorUrl;

    public LocationController(GovernmentApiService governmentApiService) {
        this.governmentApiService = governmentApiService;
    }

    /**
     * 좌표 → 주소 역지오코딩
     * GET /api/location/reverse-geocode?lat=37.5665&lng=126.9780
     */
    @GetMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<ReverseGeocodeResult>> reverseGeocode(
            @RequestParam double lat,
            @RequestParam double lng) {

        if (lat < 33.0 || lat > 43.0 || lng < 124.0 || lng > 132.0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "INVALID_COORDINATES",
                            "한국 범위를 벗어난 좌표입니다.",
                            "/api/location/reverse-geocode"
                    ));
        }

        ReverseGeocodeResult result = governmentApiService.reverseGeocode(lat, lng);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /**
     * 사용자 현재 위치 등록 → 40km 내 지역 크롤링 우선순위 큐에 삽입
     * POST /api/location/register-user-area?lat=37.2411&lng=127.1793
     *
     * 비동기로 처리 (크롤러 응답을 기다리지 않음)
     */
    @PostMapping("/register-user-area")
    public ResponseEntity<ApiResponse<Map<String, Object>>> registerUserArea(
            @RequestParam double lat,
            @RequestParam double lng) {

        if (lat < 33.0 || lat > 43.0 || lng < 124.0 || lng > 132.0) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(
                            "INVALID_COORDINATES",
                            "한국 범위를 벗어난 좌표입니다.",
                            "/api/location/register-user-area"
                    ));
        }

        // 비동기로 배치 콜렉터에 전달 (응답을 기다리지 않음)
        Thread.ofVirtual().start(() -> callBatchCollectorUserArea(lat, lng));

        return ResponseEntity.ok(ApiResponse.success(
                Map.of("lat", lat, "lng", lng, "radius_km", 40),
                "주변 지역 크롤링 우선순위 큐에 등록 중"
        ));
    }

    private void callBatchCollectorUserArea(double lat, double lng) {
        try {
            String body = String.format("{\"lat\":%s,\"lng\":%s,\"radius_km\":40}", lat, lng);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchCollectorUrl + "/api/batch/user-area"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                JsonNode json = objectMapper.readTree(response.body());
                int count = json.path("data").path("affected_count").asInt();
                logger.info("User area registered: {}개 지역 우선순위 큐 삽입 (lat={}, lng={})", count, lat, lng);
            } else {
                logger.warn("Batch collector returned {}: {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.warn("Failed to notify batch collector for user area (lat={}, lng={}): {}", lat, lng, e.getMessage());
        }
    }
}
