package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/app")
@SecurityRequirements
@Tag(name = "앱 정보", description = "앱 버전 및 시스템 정보 API")
public class AppController {

    @Value("${app.version:1.0.0}")
    private String appVersion;

    @Value("${app.release-date:2025-01-01}")
    private String releaseDate;

    @GetMapping("/version")
    @Operation(
        summary = "앱 버전 정보 조회",
        description = "현재 앱의 버전 정보를 조회합니다. 인증이 필요하지 않습니다."
    )
    @ApiResponses(
        value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "버전 정보 조회 성공",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                        {
                          "success": true,
                          "data": {
                            "version": "1.0.0",
                            "releaseDate": "2025-01-01"
                          }
                        }
                        """
                    )
                )
            )
        }
    )
    public ResponseEntity<ApiResponse<Map<String, String>>> getVersion(
            HttpServletRequest httpRequest) {
        try {
            Map<String, String> versionInfo = Map.of(
                "version", appVersion,
                "releaseDate", releaseDate
            );
            return ResponseEntity.ok(ApiResponse.success(versionInfo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(
                    "VERSION_ERROR",
                    e.getMessage() != null ? e.getMessage() : "버전 정보 조회에 실패했습니다",
                    httpRequest.getRequestURI()
                )
            );
        }
    }
}
