package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.ApiResponse;
import com.mohe.spring.dto.admin.*;
import com.mohe.spring.service.AdminMonitorService;
import org.springframework.beans.factory.annotation.Value;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
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
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${batch.collector.url:http://localhost:4001}")
    private String batchCollectorUrl;

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

    @GetMapping("/docker/containers")
    @Operation(summary = "Get Docker containers", description = "Returns list of all Docker containers with status from local server")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDockerContainers() {
        log.debug("Fetching Docker containers from local");
        List<Map<String, Object>> containers = adminMonitorService.getDockerContainers();
        return ResponseEntity.ok(ApiResponse.success(containers));
    }

    @GetMapping("/docker/containers/{serverName}")
    @Operation(summary = "Get Docker containers from specific server", description = "Returns list of all Docker containers from specified server via Docker TCP API")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getDockerContainersFromServer(
            @PathVariable String serverName
    ) {
        log.debug("Fetching Docker containers from server: {}", serverName);
        List<Map<String, Object>> containers = adminMonitorService.getDockerContainersFromServer(serverName);
        return ResponseEntity.ok(ApiResponse.success(containers));
    }

    @GetMapping("/docker/logs/{containerName}")
    @Operation(summary = "Get Docker logs", description = "Returns logs from specified Docker container on local server")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDockerLogs(
            @PathVariable String containerName,
            @RequestParam(defaultValue = "100") int lines
    ) {
        log.debug("Fetching Docker logs for {} (last {} lines)", containerName, lines);
        Map<String, Object> logs = adminMonitorService.getDockerLogs(containerName, lines);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/docker/logs/{serverName}/{containerName}")
    @Operation(summary = "Get Docker logs from specific server", description = "Returns logs from specified Docker container on specified server via Docker TCP API")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDockerLogsFromServer(
            @PathVariable String serverName,
            @PathVariable String containerName,
            @RequestParam(defaultValue = "100") int lines
    ) {
        log.debug("Fetching Docker logs for {} from server {} (last {} lines)", containerName, serverName, lines);
        Map<String, Object> logs = adminMonitorService.getDockerLogsFromServer(serverName, containerName, lines);
        return ResponseEntity.ok(ApiResponse.success(logs));
    }

    @GetMapping("/batch/config/{serverName}")
    @Operation(summary = "Get server config", description = "Returns server configuration including max workers")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getServerConfig(@PathVariable String serverName) {
        Map<String, Object> config = adminMonitorService.getServerConfig(serverName);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @GetMapping("/batch/current-jobs/{serverName}")
    @Operation(summary = "Get current running jobs", description = "Returns currently running jobs on specified server including type, description, and processing details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentJobs(@PathVariable String serverName) {
        Map<String, Object> currentJobs = adminMonitorService.getCurrentJobs(serverName);
        return ResponseEntity.ok(ApiResponse.success(currentJobs));
    }

    @GetMapping("/pipeline/recent-crawls")
    @Operation(summary = "Get recently crawled places", description = "Returns places updated in the last N minutes with location data")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentCrawls(
            @RequestParam(defaultValue = "5") int minutes,
            @RequestParam(defaultValue = "50") int limit
    ) {
        List<Map<String, Object>> places = adminMonitorService.getRecentCrawls(minutes, limit);
        return ResponseEntity.ok(ApiResponse.success(places));
    }

    @GetMapping("/pipeline/stats")
    @Operation(summary = "Get full pipeline statistics", description = "Returns comprehensive pipeline stats: places, crawling, AI, embedding, images, reviews")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPipelineStats() {
        Map<String, Object> stats = adminMonitorService.getPipelineStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/pipeline/recent-activity")
    @Operation(summary = "Get recent pipeline activity", description = "Returns hourly place creation counts for the last 24 hours")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentActivity() {
        List<Map<String, Object>> activity = adminMonitorService.getRecentActivity();
        return ResponseEntity.ok(ApiResponse.success(activity));
    }

    @PostMapping("/pipeline/jobs/{jobName}/trigger")
    @Operation(summary = "Manually trigger a batch job", description = "Triggers updateCrawledDataJob, vectorEmbeddingJob, or imageUpdateJob")
    public ResponseEntity<ApiResponse<Map<String, String>>> triggerJob(@PathVariable String jobName) {
        String result = adminMonitorService.triggerJob(jobName);
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", result)));
    }

    /**
     * 지역 크롤링 현황 지도 데이터 (Phase 5)
     * batch_collector의 queue-monitoring을 프록시
     */
    @GetMapping("/crawling/map")
    @Operation(summary = "Get crawling map data", description = "Returns region crawl queue status for map visualization")
    public ResponseEntity<ApiResponse<Object>> getCrawlingMapData() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchCollectorUrl + "/api/batch/queue-monitoring"))
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Object data = objectMapper.readValue(response.body(), Object.class);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.warn("Failed to fetch crawling map data from batch collector: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("BATCH_COLLECTOR_UNAVAILABLE",
                    "배치 콜렉터에 연결할 수 없습니다: " + e.getMessage()));
        }
    }

    /**
     * 큐 기반 전국 자동 순환 수집 시작 (Phase 4)
     */
    @PostMapping("/crawling/start-queue")
    @Operation(summary = "Start queue-based crawling", description = "Start nationwide automatic crawling from the priority queue")
    public ResponseEntity<ApiResponse<Object>> startQueueCrawling() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchCollectorUrl + "/api/batch/start-queue"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Object data = objectMapper.readValue(response.body(), Object.class);
            return ResponseEntity.ok(ApiResponse.success(data));
        } catch (Exception e) {
            log.error("Failed to start queue crawling: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.error("BATCH_COLLECTOR_UNAVAILABLE", e.getMessage()));
        }
    }

    // ===== Place Delete APIs =====

    @DeleteMapping("/places/{id}")
    @Operation(summary = "Delete a place", description = "Delete a place by ID with all related data (cascade)")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> deletePlace(@PathVariable Long id) {
        log.info("Deleting place ID: {}", id);
        try {
            adminMonitorService.deletePlace(id);
            Map<String, Object> result = new HashMap<>();
            result.put("deletedId", id);
            return ResponseEntity.ok(ApiResponse.success(result, "Place deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.error("NOT_FOUND", e.getMessage()));
        }
    }

    @PostMapping("/places/batch-delete")
    @Operation(summary = "Batch delete places", description = "Delete multiple places by IDs")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> batchDeletePlaces(@RequestBody Map<String, List<Long>> body) {
        List<Long> placeIds = body.get("placeIds");
        if (placeIds == null || placeIds.isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("BAD_REQUEST", "placeIds is required and must not be empty"));
        }
        log.info("Batch deleting {} places: {}", placeIds.size(), placeIds);
        int deleted = adminMonitorService.deletePlaces(placeIds);

        Map<String, Object> result = new HashMap<>();
        result.put("requestedCount", placeIds.size());
        result.put("deletedCount", deleted);
        return ResponseEntity.ok(ApiResponse.success(result, "Deleted " + deleted + " places"));
    }

    // ===== Pipeline Progress API =====

    @GetMapping("/pipeline/progress")
    @Operation(summary = "Get pipeline progress", description = "Returns description, embedding, and image processing progress")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPipelineProgress() {
        Map<String, Object> progress = adminMonitorService.getPipelineProgress();
        return ResponseEntity.ok(ApiResponse.success(progress));
    }
}
