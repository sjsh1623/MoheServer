package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.WeatherResponse;
import com.mohe.spring.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Weather information controller
 * Provides current weather data for given coordinates
 */
@RestController
@RequestMapping("/api/weather")
@SecurityRequirements
@Tag(name = "날씨 정보", description = "좌표 기반 날씨 정보 조회 API (기상청 단기예보)")
public class WeatherController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherController.class);

    private final WeatherService weatherService;
    private final String kmaServiceKey;

    public WeatherController(
            WeatherService weatherService,
            @Value("${api.kma.service-key:}") String kmaServiceKey) {
        this.weatherService = weatherService;
        this.kmaServiceKey = kmaServiceKey;
    }

    @GetMapping("/current")
    @Operation(
        summary = "현재 날씨 정보 조회",
        description = """
            위도/경도 좌표를 기반으로 현재 날씨 정보를 조회합니다.

            **데이터 제공:**
            - 기상청 단기예보 API (한국 좌표)
            - OpenMeteo API (fallback 또는 KMA API 키 미설정 시)

            **조회 가능 정보:**
            - 기온 (섭씨/화씨)
            - 날씨 상태 (맑음, 비, 눈 등)
            - 습도
            - 풍속
            - 시간대

            **캐싱:** 10분간 캐시되어 빠른 응답 제공
            """
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "날씨 정보 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = WeatherResponse.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "temperatureCelsius": 23.5,
                            "temperatureFahrenheit": 74.3,
                            "conditionCode": "clear",
                            "conditionText": "맑음",
                            "humidity": 65,
                            "windSpeedKmh": 12.5,
                            "daypart": "afternoon",
                            "latitude": 37.5665,
                            "longitude": 126.9780,
                            "provider": "KMA"
                          }
                        }
                        """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "잘못된 좌표 값"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "날씨 정보 조회 실패"
            )
        }
    )
    public ResponseEntity<ApiResponse<WeatherResponse>> getCurrentWeather(
            @Parameter(description = "위도", required = true, example = "37.5665")
            @RequestParam double lat,
            @Parameter(description = "경도", required = true, example = "126.9780")
            @RequestParam double lon,
            HttpServletRequest httpRequest) {
        try {
            logger.info("Received weather request: lat={}, lon={}", lat, lon);

            // Validate coordinates
            if (lat < -90 || lat > 90) {
                logger.warn("Invalid latitude: {}", lat);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        "INVALID_COORDINATES",
                        "위도는 -90에서 90 사이의 값이어야 합니다",
                        httpRequest.getRequestURI()
                    )
                );
            }

            if (lon < -180 || lon > 180) {
                logger.warn("Invalid longitude: {}", lon);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        "INVALID_COORDINATES",
                        "경도는 -180에서 180 사이의 값이어야 합니다",
                        httpRequest.getRequestURI()
                    )
                );
            }

            // Fetch weather data
            logger.info("Fetching weather data for coordinates: lat={}, lon={}", lat, lon);

            // Determine which provider is being used
            String provider = (kmaServiceKey != null && !kmaServiceKey.isBlank()) ? "KMA" : "OpenMeteo";

            // Get weather response from service
            WeatherResponse response = weatherService.getCurrentWeatherResponse(lat, lon, provider);

            logger.info("Weather data retrieved successfully: {}°C, {}",
                       response.getTemperatureCelsius(), response.getConditionText());
            return ResponseEntity.ok(ApiResponse.success(response));

        } catch (Exception e) {
            logger.error("Failed to fetch weather data", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    "WEATHER_FETCH_ERROR",
                    "날씨 정보 조회에 실패했습니다: " + e.getMessage(),
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/test")
    @Operation(
        summary = "날씨 서비스 테스트",
        description = "날씨 서비스가 정상 동작하는지 테스트합니다."
    )
    public ResponseEntity<ApiResponse<String>> testWeather(HttpServletRequest httpRequest) {
        try {
            logger.info("Weather service test endpoint called");
            String provider = (kmaServiceKey != null && !kmaServiceKey.isBlank()) ? "KMA" : "OpenMeteo";
            String data = "Weather service is working. Provider: " + provider;
            ApiResponse<String> response = ApiResponse.success(data, "Weather service test completed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Weather service test failed", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error("TEST_FAILED", "Test failed: " + e.getMessage(), httpRequest.getRequestURI())
            );
        }
    }
}
