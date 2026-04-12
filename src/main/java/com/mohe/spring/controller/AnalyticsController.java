package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.service.AnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analytics", description = "Visitor tracking and analytics API")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    // ===== Public endpoint (no auth needed) =====

    @PostMapping("/api/analytics/pageview")
    @Operation(summary = "Record a pageview", description = "Track a page visit with session and device info")
    public ResponseEntity<ApiResponse<Void>> recordPageview(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request
    ) {
        String sessionId = (String) body.get("sessionId");
        String pagePath = (String) body.get("pagePath");
        String referrer = (String) body.get("referrer");
        Long userId = body.get("userId") != null ? ((Number) body.get("userId")).longValue() : null;

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        analyticsService.recordPageview(sessionId, ipAddress, userAgent, pagePath, referrer, userId);

        return ResponseEntity.ok(ApiResponse.success("Pageview recorded"));
    }

    // ===== Admin endpoints =====

    @GetMapping("/api/admin/analytics/summary")
    @Operation(summary = "Get analytics summary", description = "Returns today/thisWeek/thisMonth pageviews and unique visitors")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getSummary()));
    }

    @GetMapping("/api/admin/analytics/hourly")
    @Operation(summary = "Get hourly stats", description = "Returns pageview counts per hour for the last 24 hours")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getHourlyStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getHourlyStats()));
    }

    @GetMapping("/api/admin/analytics/devices")
    @Operation(summary = "Get device type stats", description = "Returns device type breakdown for the last 30 days")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDeviceStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getDeviceStats()));
    }

    @GetMapping("/api/admin/analytics/browsers")
    @Operation(summary = "Get browser stats", description = "Returns browser breakdown for the last 30 days")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBrowserStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getBrowserStats()));
    }

    @GetMapping("/api/admin/analytics/os")
    @Operation(summary = "Get OS stats", description = "Returns OS breakdown for the last 30 days")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOsStats() {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getOsStats()));
    }

    @GetMapping("/api/admin/analytics/pages")
    @Operation(summary = "Get top pages", description = "Returns most visited pages for the last 30 days")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopPages(
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getTopPages(limit)));
    }

    @GetMapping("/api/admin/analytics/visitors")
    @Operation(summary = "Get recent visitors", description = "Returns paginated list of recent visitor logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRecentVisitors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.getRecentVisitors(page, size)));
    }

    /**
     * Extract client IP considering X-Forwarded-For header (for reverse proxy like Caddy)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
