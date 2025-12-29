package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.admin.*;
import com.mohe.spring.service.AdminMonitorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/monitor")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin Monitor", description = "Admin monitoring dashboard API")
public class AdminMonitorController {

    private final AdminMonitorService adminMonitorService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get dashboard overview", description = "Returns combined dashboard data including place stats, batch stats, and worker status")
    public ResponseEntity<ApiResponse<DashboardResponse>> getDashboard() {
        log.debug("Fetching dashboard data");
        DashboardResponse dashboard = adminMonitorService.getDashboard();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    @GetMapping("/batch/servers")
    @Operation(summary = "Get batch servers", description = "Returns list of available batch servers with status")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBatchServers() {
        List<Map<String, Object>> servers = adminMonitorService.getBatchServers();
        return ResponseEntity.ok(ApiResponse.success(servers));
    }

    @GetMapping("/places/stats")
    @Operation(summary = "Get place statistics", description = "Returns counts of places by status")
    public ResponseEntity<ApiResponse<PlaceStatsResponse>> getPlaceStats() {
        PlaceStatsResponse stats = adminMonitorService.getPlaceStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/places/search")
    @Operation(summary = "Search places", description = "Search places with filters for admin")
    public ResponseEntity<ApiResponse<AdminPlaceSearchResponse>> searchPlaces(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "all") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PlaceSearchRequest request = new PlaceSearchRequest();
        request.setKeyword(keyword);
        request.setStatus(status);
        request.setPage(page);
        request.setSize(size);

        AdminPlaceSearchResponse response = adminMonitorService.searchPlaces(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/batch/stats")
    @Operation(summary = "Get batch queue statistics", description = "Returns queue statistics from default MoheBatch service")
    public ResponseEntity<ApiResponse<BatchStatsResponse>> getBatchStats() {
        BatchStatsResponse stats = adminMonitorService.getBatchStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/batch/stats/{serverName}")
    @Operation(summary = "Get batch queue statistics from specific server", description = "Returns queue statistics from specified MoheBatch server")
    public ResponseEntity<ApiResponse<BatchStatsResponse>> getBatchStats(@PathVariable String serverName) {
        BatchStatsResponse stats = adminMonitorService.getBatchStats(serverName);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/batch/workers")
    @Operation(summary = "Get active workers", description = "Returns list of active batch workers from default server")
    public ResponseEntity<ApiResponse<Map<String, WorkerStatusResponse.WorkerInfo>>> getWorkers() {
        Map<String, WorkerStatusResponse.WorkerInfo> workers = adminMonitorService.getWorkers();
        return ResponseEntity.ok(ApiResponse.success(workers));
    }

    @GetMapping("/batch/workers/{serverName}")
    @Operation(summary = "Get active workers from specific server", description = "Returns list of active batch workers from specified server")
    public ResponseEntity<ApiResponse<Map<String, WorkerStatusResponse.WorkerInfo>>> getWorkers(@PathVariable String serverName) {
        Map<String, WorkerStatusResponse.WorkerInfo> workers = adminMonitorService.getWorkers(serverName);
        return ResponseEntity.ok(ApiResponse.success(workers));
    }

    @PostMapping("/batch/worker/start")
    @Operation(summary = "Start batch worker", description = "Start the batch worker on default MoheBatch service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startWorker() {
        return startWorkerOnServer(null);
    }

    @PostMapping("/batch/worker/start/{serverName}")
    @Operation(summary = "Start batch worker on specific server", description = "Start the batch worker on specified MoheBatch server")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startWorkerOnServer(@PathVariable String serverName) {
        log.info("Starting batch worker on server: {}", serverName != null ? serverName : "default");
        boolean success = adminMonitorService.startWorker(serverName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("action", "start");
        result.put("server", serverName != null ? serverName : "local");

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(result, "Worker started on " + (serverName != null ? serverName : "local")));
        } else {
            return ResponseEntity.ok(ApiResponse.error("WORKER_START_FAILED", "Failed to start worker on " + (serverName != null ? serverName : "local")));
        }
    }

    @PostMapping("/batch/worker/stop")
    @Operation(summary = "Stop batch worker", description = "Stop the batch worker on default MoheBatch service")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stopWorker() {
        return stopWorkerOnServer(null);
    }

    @PostMapping("/batch/worker/stop/{serverName}")
    @Operation(summary = "Stop batch worker on specific server", description = "Stop the batch worker on specified MoheBatch server")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stopWorkerOnServer(@PathVariable String serverName) {
        log.info("Stopping batch worker on server: {}", serverName != null ? serverName : "default");
        boolean success = adminMonitorService.stopWorker(serverName);

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("action", "stop");
        result.put("server", serverName != null ? serverName : "local");

        if (success) {
            return ResponseEntity.ok(ApiResponse.success(result, "Worker stopped on " + (serverName != null ? serverName : "local")));
        } else {
            return ResponseEntity.ok(ApiResponse.error("WORKER_STOP_FAILED", "Failed to stop worker on " + (serverName != null ? serverName : "local")));
        }
    }

    @PostMapping("/batch/push-all")
    @Operation(summary = "Push all places to queue", description = "Push all unprocessed places to the batch queue on default server")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pushAllToQueue(
            @RequestParam(defaultValue = "true") boolean menus,
            @RequestParam(defaultValue = "true") boolean images,
            @RequestParam(defaultValue = "true") boolean reviews
    ) {
        return pushAllToQueueOnServer(null, menus, images, reviews);
    }

    @PostMapping("/batch/push-all/{serverName}")
    @Operation(summary = "Push all places to queue on specific server", description = "Push all unprocessed places to the batch queue on specified server")
    public ResponseEntity<ApiResponse<Map<String, Object>>> pushAllToQueueOnServer(
            @PathVariable String serverName,
            @RequestParam(defaultValue = "true") boolean menus,
            @RequestParam(defaultValue = "true") boolean images,
            @RequestParam(defaultValue = "true") boolean reviews
    ) {
        log.info("Pushing all places to queue on server {}: menus={}, images={}, reviews={}",
                serverName != null ? serverName : "default", menus, images, reviews);
        int count = adminMonitorService.pushAllToQueue(serverName, menus, images, reviews);

        Map<String, Object> result = new HashMap<>();
        result.put("pushedCount", count);
        result.put("server", serverName != null ? serverName : "local");
        result.put("menus", menus);
        result.put("images", images);
        result.put("reviews", reviews);

        return ResponseEntity.ok(ApiResponse.success(result, "Pushed " + count + " places to queue on " + (serverName != null ? serverName : "local")));
    }

    @RequestMapping(value = "/batch/execute/{serverName}", method = {RequestMethod.GET, RequestMethod.POST})
    @Operation(summary = "Execute batch endpoint", description = "Execute any batch endpoint on specified server")
    public ResponseEntity<ApiResponse<Object>> executeBatchEndpoint(
            @PathVariable String serverName,
            @RequestParam String path,
            @RequestParam(defaultValue = "POST") String method,
            @RequestBody(required = false) Map<String, Object> body
    ) {
        log.info("Executing batch endpoint on {} [{}]: {}", serverName, method, path);
        try {
            Object result = adminMonitorService.executeBatchEndpoint(serverName, method, path, body);
            return ResponseEntity.ok(ApiResponse.success(result));
        } catch (Exception e) {
            log.error("Failed to execute batch endpoint: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("BATCH_EXECUTE_FAILED", e.getMessage()));
        }
    }
}
