package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.service.AddressService;
import com.mohe.spring.service.AddressInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
// Import removed - using fully qualified name for ApiResponse to avoid conflict
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/address")
@SecurityRequirements
@Tag(name = "주소 정보", description = "좌표를 주소로 변환하는 API")
public class AddressController {

    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);
    private final AddressService addressService;
    private final com.mohe.spring.config.LocationProperties locationProperties;

    public AddressController(AddressService addressService, com.mohe.spring.config.LocationProperties locationProperties) {
        this.addressService = addressService;
        this.locationProperties = locationProperties;
    }

    @GetMapping("/reverse")
    @Operation(
        summary = "좌표를 주소로 변환",
        description = """
            위도/경도 좌표를 한국 주소로 변환합니다.

            - ENV Mock 위치가 설정되어 있으면 해당 좌표 사용 (파라미터 무시)
            - Naver Reverse Geocoding API 우선 사용
            - API 실패 시 좌표 형식으로 fallback
            - 1시간 캐싱으로 성능 최적화
        """
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "주소 변환 성공",
                content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = AddressInfo.class),
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "fullAddress": "서울특별시 강남구 역삼동 테헤란로 152",
                            "shortAddress": "강남구 역삼동",
                            "sido": "서울특별시",
                            "sigungu": "강남구", 
                            "dong": "역삼동",
                            "roadName": "테헤란로",
                            "buildingNumber": "152",
                            "latitude": 37.5665,
                            "longitude": 126.9780
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
                description = "주소 변환 서비스 오류"
            )
        }
    )
    public ResponseEntity<ApiResponse<AddressInfo>> reverseGeocode(
            @Parameter(description = "위도 (optional when MOCK_LATITUDE is set)", required = false, example = "37.5665")
            @RequestParam(required = false) Double lat,
            @Parameter(description = "경도 (optional when MOCK_LONGITUDE is set)", required = false, example = "126.9780")
            @RequestParam(required = false) Double lon,
            HttpServletRequest httpRequest) {
        try {
            // ENV에 위치가 설정되어 있으면 강제로 사용 (파라미터 무시)
            double latitude;
            double longitude;

            if (locationProperties.getDefaultLatitude() != null && locationProperties.getDefaultLongitude() != null) {
                // ENV에 설정된 값 강제 사용 (개발 환경 테스트용)
                latitude = locationProperties.getDefaultLatitude();
                longitude = locationProperties.getDefaultLongitude();
                logger.info("Using configured mock location from ENV: lat={}, lon={}", latitude, longitude);
            } else {
                // ENV에 없으면 파라미터 사용 (기존 로직)
                if (lat == null || lon == null) {
                    return ResponseEntity.badRequest().body(
                        ApiResponse.error(
                            "MISSING_PARAMETERS",
                            "위도/경도 파라미터가 필요합니다",
                            httpRequest.getRequestURI()
                        )
                    );
                }
                latitude = lat;
                longitude = lon;
                logger.info("Using user-provided location: lat={}, lon={}", latitude, longitude);
            }

            logger.info("Received reverse geocoding request: lat={}, lon={}", latitude, longitude);

            // Validate coordinates
            if (latitude < -90 || latitude > 90) {
                logger.warn("Invalid latitude: {}", latitude);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        "INVALID_COORDINATES",
                        "위도는 -90에서 90 사이의 값이어야 합니다",
                        httpRequest.getRequestURI()
                    )
                );
            }

            if (longitude < -180 || longitude > 180) {
                logger.warn("Invalid longitude: {}", longitude);
                return ResponseEntity.badRequest().body(
                    ApiResponse.error(
                        "INVALID_COORDINATES",
                        "경도는 -180에서 180 사이의 값이어야 합니다",
                        httpRequest.getRequestURI()
                    )
                );
            }

            logger.info("Converting coordinates to address: lat={}, lon={}", latitude, longitude);
            AddressInfo addressInfo = addressService.getAddressFromCoordinates(latitude, longitude);
            
            logger.info("Address conversion completed: {}", addressInfo.getShortAddress());
            return ResponseEntity.ok(ApiResponse.success(addressInfo));
            
        } catch (Exception e) {
            logger.error("Failed to convert coordinates to address", e);
            return ResponseEntity.status(500).body(
                ApiResponse.error(
                    "ADDRESS_CONVERSION_ERROR",
                    "주소 변환에 실패했습니다: " + e.getMessage(),
                    httpRequest.getRequestURI()
                )
            );
        }
    }

    @GetMapping("/test")
    @Operation(
        summary = "주소 서비스 테스트",
        description = "주소 서비스가 정상 동작하는지 테스트합니다."
    )
    public ResponseEntity<ApiResponse<String>> testAddress(HttpServletRequest httpRequest) {
        try {
            logger.info("Address service test endpoint called");
            String data = "Address service is working";
            ApiResponse<String> response = ApiResponse.success(data, "Address service test completed");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Address service test failed", e);
            return ResponseEntity.status(500).body(ApiResponse.error("TEST_FAILED", "Test failed: " + e.getMessage(), httpRequest.getRequestURI()));
        }
    }
}