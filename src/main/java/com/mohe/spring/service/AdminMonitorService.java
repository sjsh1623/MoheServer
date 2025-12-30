package com.mohe.spring.service;

import com.mohe.spring.config.BatchServerConfig;
import com.mohe.spring.dto.admin.*;
import com.mohe.spring.entity.CrawlStatus;
import com.mohe.spring.entity.EmbedStatus;
import com.mohe.spring.entity.Place;
import com.mohe.spring.repository.PlaceRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class AdminMonitorService {

    private final PlaceRepository placeRepository;
    private final WebClient webClient;
    private final BatchServerConfig batchServerConfig;

    public AdminMonitorService(
            PlaceRepository placeRepository,
            WebClient webClient,
            BatchServerConfig batchServerConfig
    ) {
        this.placeRepository = placeRepository;
        this.webClient = webClient;
        this.batchServerConfig = batchServerConfig;
    }

    /**
     * Get list of available batch servers
     */
    public List<Map<String, Object>> getBatchServers() {
        List<Map<String, Object>> servers = new ArrayList<>();
        for (BatchServerConfig.RemoteServer server : batchServerConfig.getRemoteServers()) {
            Map<String, Object> serverInfo = new HashMap<>();
            serverInfo.put("name", server.getName());
            serverInfo.put("url", server.getUrl());
            serverInfo.put("enabled", server.isEnabled());
            serverInfo.put("status", checkServerHealth(server.getUrl()) ? "online" : "offline");
            serverInfo.put("dockerHost", server.getDockerHost());
            serverInfo.put("dockerPort", server.getDockerPort());
            servers.add(serverInfo);
        }
        return servers;
    }

    /**
     * Check if a batch server is online
     */
    private boolean checkServerHealth(String serverUrl) {
        try {
            webClient.get()
                    .uri(serverUrl + "/health")
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(3))
                    .block();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get batch service URL by server name
     */
    private String getServerUrl(String serverName) {
        if (serverName == null || serverName.isEmpty()) {
            return batchServerConfig.getService().getUrl();
        }
        return batchServerConfig.getRemoteServers().stream()
                .filter(s -> s.getName().equals(serverName))
                .findFirst()
                .map(BatchServerConfig.RemoteServer::getUrl)
                .orElse(batchServerConfig.getService().getUrl());
    }

    /**
     * Get place statistics
     */
    public PlaceStatsResponse getPlaceStats() {
        long totalCount = placeRepository.count();
        long embeddedCount = placeRepository.countByEmbedStatus(EmbedStatus.COMPLETED);
        long crawledCount = placeRepository.countByCrawlStatus(CrawlStatus.COMPLETED);
        long pendingCount = placeRepository.countByCrawlStatus(CrawlStatus.PENDING);
        long failedCount = placeRepository.countByCrawlStatus(CrawlStatus.FAILED);
        long notFoundCount = placeRepository.countByCrawlStatus(CrawlStatus.NOT_FOUND);

        return PlaceStatsResponse.builder()
                .totalCount(totalCount)
                .embeddedCount(embeddedCount)
                .crawledCount(crawledCount)
                .pendingCount(pendingCount)
                .failedCount(failedCount)
                .notFoundCount(notFoundCount)
                .build();
    }

    /**
     * Search places for admin
     */
    public AdminPlaceSearchResponse searchPlaces(PlaceSearchRequest request) {
        PageRequest pageRequest = PageRequest.of(request.getPage(), request.getSize());
        String status = request.getStatus() != null ? request.getStatus() : "all";
        String keyword = request.getKeyword();

        Page<Place> placePage = placeRepository.searchPlacesForAdmin(keyword, status, pageRequest);

        List<AdminPlaceDto> places = placePage.getContent().stream()
                .map(this::toAdminPlaceDto)
                .collect(Collectors.toList());

        return AdminPlaceSearchResponse.builder()
                .places(places)
                .page(placePage.getNumber())
                .size(placePage.getSize())
                .totalElements(placePage.getTotalElements())
                .totalPages(placePage.getTotalPages())
                .hasNext(placePage.hasNext())
                .hasPrevious(placePage.hasPrevious())
                .build();
    }

    /**
     * Get batch queue statistics from MoheBatch
     */
    public BatchStatsResponse getBatchStats(String serverName) {
        String serverUrl = getServerUrl(serverName);
        try {
            Map<String, Object> response = webClient.get()
                    .uri(serverUrl + "/batch/queue/stats")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                BatchStatsResponse stats = new BatchStatsResponse();
                stats.setPendingCount(getLong(data, "pendingCount"));
                stats.setPriorityCount(getLong(data, "priorityCount"));
                stats.setProcessingCount(getLong(data, "processingCount"));
                stats.setCompletedCount(getLong(data, "completedCount"));
                stats.setFailedCount(getLong(data, "failedCount"));
                stats.setActiveWorkers(getInt(data, "activeWorkers"));
                stats.setServerName(serverName != null ? serverName : "local");
                return stats;
            }
        } catch (Exception e) {
            log.warn("Failed to fetch batch stats from MoheBatch ({}): {}", serverUrl, e.getMessage());
        }

        // Return empty stats on error
        BatchStatsResponse emptyStats = new BatchStatsResponse();
        emptyStats.setActiveWorkers(0);
        emptyStats.setServerName(serverName != null ? serverName : "local");
        return emptyStats;
    }

    /**
     * Get batch queue statistics from default server
     */
    public BatchStatsResponse getBatchStats() {
        return getBatchStats(null);
    }

    /**
     * Get active workers from MoheBatch
     */
    public Map<String, WorkerStatusResponse.WorkerInfo> getWorkers(String serverName) {
        String serverUrl = getServerUrl(serverName);
        try {
            Map<String, Object> response = webClient.get()
                    .uri(serverUrl + "/batch/queue/workers")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, Object>> data = (Map<String, Map<String, Object>>) response.get("data");
                return data.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> mapToWorkerInfo(entry.getValue())
                        ));
            }
        } catch (Exception e) {
            log.warn("Failed to fetch workers from MoheBatch ({}): {}", serverUrl, e.getMessage());
        }

        return Collections.emptyMap();
    }

    /**
     * Get active workers from default server
     */
    public Map<String, WorkerStatusResponse.WorkerInfo> getWorkers() {
        return getWorkers(null);
    }

    /**
     * Get combined dashboard data
     */
    public DashboardResponse getDashboard() {
        PlaceStatsResponse placeStats = getPlaceStats();
        BatchStatsResponse batchStats = getBatchStats();
        Map<String, WorkerStatusResponse.WorkerInfo> workers = getWorkers();

        return DashboardResponse.builder()
                .placeStats(placeStats)
                .batchStats(batchStats)
                .workers(workers)
                .timestamp(LocalDateTime.now())
                .build();
    }

    /**
     * Start batch worker on specific server
     */
    public boolean startWorker(String serverName) {
        String serverUrl = getServerUrl(serverName);
        try {
            webClient.post()
                    .uri(serverUrl + "/batch/queue/worker/start")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Started worker on server: {}", serverName != null ? serverName : "local");
            return true;
        } catch (Exception e) {
            log.error("Failed to start worker on {}: {}", serverUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Start batch worker on default server
     */
    public boolean startWorker() {
        return startWorker(null);
    }

    /**
     * Stop batch worker on specific server
     */
    public boolean stopWorker(String serverName) {
        String serverUrl = getServerUrl(serverName);
        try {
            webClient.post()
                    .uri(serverUrl + "/batch/queue/worker/stop")
                    .retrieve()
                    .bodyToMono(Void.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            log.info("Stopped worker on server: {}", serverName != null ? serverName : "local");
            return true;
        } catch (Exception e) {
            log.error("Failed to stop worker on {}: {}", serverUrl, e.getMessage());
            return false;
        }
    }

    /**
     * Stop batch worker on default server
     */
    public boolean stopWorker() {
        return stopWorker(null);
    }

    /**
     * Push all places to queue on specific server
     */
    public int pushAllToQueue(String serverName, boolean menus, boolean images, boolean reviews) {
        String serverUrl = getServerUrl(serverName);
        try {
            Map<String, Object> response = webClient.post()
                    .uri(serverUrl + "/batch/queue/push-all?menus=" + menus + "&images=" + images + "&reviews=" + reviews)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                int count = getInt(data, "pushedCount");
                log.info("Pushed {} places to queue on server: {}", count, serverName != null ? serverName : "local");
                return count;
            }
        } catch (Exception e) {
            log.error("Failed to push all to queue on {}: {}", serverUrl, e.getMessage());
        }
        return 0;
    }

    /**
     * Push all places to queue on default server
     */
    public int pushAllToQueue(boolean menus, boolean images, boolean reviews) {
        return pushAllToQueue(null, menus, images, reviews);
    }

    /**
     * Get Docker container logs
     */
    public Map<String, Object> getDockerLogs(String containerName, int lines) {
        Map<String, Object> result = new HashMap<>();
        result.put("containerName", containerName);
        result.put("lines", lines);
        result.put("timestamp", LocalDateTime.now());

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "logs", "--tail", String.valueOf(lines), containerName
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            int exitCode = process.waitFor();
            result.put("success", exitCode == 0);
            result.put("logs", output.toString());
            result.put("exitCode", exitCode);

        } catch (Exception e) {
            log.error("Failed to get Docker logs for {}: {}", containerName, e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("logs", "");
        }

        return result;
    }

    /**
     * Get list of Docker containers
     */
    public List<Map<String, Object>> getDockerContainers() {
        List<Map<String, Object>> containers = new ArrayList<>();

        try {
            ProcessBuilder pb = new ProcessBuilder(
                "docker", "ps", "-a", "--format", "{{.Names}}\t{{.Status}}\t{{.Image}}"
            );
            Process process = pb.start();

            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split("\t");
                    if (parts.length >= 3) {
                        Map<String, Object> container = new HashMap<>();
                        container.put("name", parts[0]);
                        container.put("status", parts[1]);
                        container.put("image", parts[2]);
                        container.put("running", parts[1].toLowerCase().contains("up"));
                        containers.add(container);
                    }
                }
            }

            process.waitFor();
        } catch (Exception e) {
            log.error("Failed to list Docker containers: {}", e.getMessage());
        }

        return containers;
    }

    /**
     * Get Docker containers from specific server via Docker TCP API
     */
    public List<Map<String, Object>> getDockerContainersFromServer(String serverName) {
        // For local server, use Docker CLI via socket
        if ("local".equalsIgnoreCase(serverName)) {
            return getDockerContainers();
        }

        BatchServerConfig.RemoteServer server = findServer(serverName);
        if (server == null || server.getDockerHost() == null) {
            log.warn("Server {} not found or no Docker host configured", serverName);
            return getDockerContainers(); // Fallback to local
        }

        String dockerUrl = String.format("http://%s:%d", server.getDockerHost(), server.getDockerPort());
        List<Map<String, Object>> containers = new ArrayList<>();

        try {
            List<Map<String, Object>> response = webClient.get()
                    .uri(dockerUrl + "/containers/json?all=true")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<List<Map<String, Object>>>() {})
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response != null) {
                for (Map<String, Object> container : response) {
                    Map<String, Object> containerInfo = new HashMap<>();

                    // Extract container name (remove leading /)
                    @SuppressWarnings("unchecked")
                    List<String> names = (List<String>) container.get("Names");
                    String name = names != null && !names.isEmpty()
                            ? names.get(0).replaceFirst("^/", "")
                            : (String) container.get("Id");

                    containerInfo.put("name", name);
                    containerInfo.put("id", container.get("Id"));
                    containerInfo.put("image", container.get("Image"));
                    containerInfo.put("status", container.get("Status"));
                    containerInfo.put("state", container.get("State"));
                    containerInfo.put("running", "running".equalsIgnoreCase((String) container.get("State")));
                    containers.add(containerInfo);
                }
            }
            log.debug("Fetched {} containers from {}", containers.size(), dockerUrl);
        } catch (Exception e) {
            log.error("Failed to fetch containers from {}: {}", dockerUrl, e.getMessage());
        }

        return containers;
    }

    /**
     * Get Docker logs from specific server via Docker TCP API
     */
    public Map<String, Object> getDockerLogsFromServer(String serverName, String containerName, int lines) {
        // For local server, use Docker CLI via socket
        if ("local".equalsIgnoreCase(serverName)) {
            return getDockerLogs(containerName, lines);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("containerName", containerName);
        result.put("serverName", serverName);
        result.put("lines", lines);
        result.put("timestamp", LocalDateTime.now());

        BatchServerConfig.RemoteServer server = findServer(serverName);
        if (server == null || server.getDockerHost() == null) {
            log.warn("Server {} not found or no Docker host configured, using local", serverName);
            return getDockerLogs(containerName, lines);
        }

        String dockerUrl = String.format("http://%s:%d", server.getDockerHost(), server.getDockerPort());

        try {
            // Docker API uses container ID or name
            String logsUrl = String.format("%s/containers/%s/logs?stdout=true&stderr=true&tail=%d&timestamps=false",
                    dockerUrl, containerName, lines);

            String logs = webClient.get()
                    .uri(logsUrl)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            // Docker logs API returns raw bytes with stream headers, clean them up
            String cleanedLogs = cleanDockerLogs(logs);

            result.put("success", true);
            result.put("logs", cleanedLogs);
            log.debug("Fetched {} bytes of logs for {} from {}",
                    cleanedLogs != null ? cleanedLogs.length() : 0, containerName, dockerUrl);
        } catch (Exception e) {
            log.error("Failed to fetch logs for {} from {}: {}", containerName, dockerUrl, e.getMessage());
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("logs", "Error: " + e.getMessage());
        }

        return result;
    }

    /**
     * Clean Docker log output (remove stream headers)
     */
    private String cleanDockerLogs(String logs) {
        if (logs == null) return "";

        StringBuilder cleaned = new StringBuilder();
        int i = 0;
        while (i < logs.length()) {
            // Docker multiplexed stream format: 8-byte header + payload
            // Header: [stream_type(1), 0, 0, 0, size(4 bytes big-endian)]
            if (i + 8 <= logs.length()) {
                char streamType = logs.charAt(i);
                // Check if this looks like a Docker stream header
                if ((streamType == 1 || streamType == 2) &&
                    logs.charAt(i + 1) == 0 && logs.charAt(i + 2) == 0 && logs.charAt(i + 3) == 0) {
                    // Read size from bytes 4-7
                    int size = ((logs.charAt(i + 4) & 0xFF) << 24) |
                               ((logs.charAt(i + 5) & 0xFF) << 16) |
                               ((logs.charAt(i + 6) & 0xFF) << 8) |
                               (logs.charAt(i + 7) & 0xFF);
                    i += 8; // Skip header
                    if (size > 0 && i + size <= logs.length()) {
                        cleaned.append(logs, i, i + size);
                        i += size;
                        continue;
                    }
                }
            }
            // Not a stream header, just append the character
            cleaned.append(logs.charAt(i));
            i++;
        }
        return cleaned.toString();
    }

    /**
     * Find server by name
     */
    private BatchServerConfig.RemoteServer findServer(String serverName) {
        if (serverName == null) return null;
        return batchServerConfig.getRemoteServers().stream()
                .filter(s -> serverName.equals(s.getName()))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get server configuration including max workers
     */
    public Map<String, Object> getServerConfig(String serverName) {
        String serverUrl = getServerUrl(serverName);
        Map<String, Object> config = new HashMap<>();
        config.put("serverName", serverName != null ? serverName : "local");
        config.put("serverUrl", serverUrl);
        config.put("maxWorkers", 10); // Support up to 10 workers

        try {
            Map<String, Object> response = webClient.get()
                    .uri(serverUrl + "/batch/config")
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofSeconds(5))
                    .block();

            if (response != null && response.containsKey("data")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                config.putAll(data);
            }
        } catch (Exception e) {
            log.debug("Could not fetch server config from {}: {}", serverUrl, e.getMessage());
            // Return default config
            config.put("totalWorkers", 10);
            config.put("threadsPerWorker", 1);
            config.put("chunkSize", 10);
        }

        return config;
    }

    /**
     * Execute any batch endpoint on specific server
     */
    public Object executeBatchEndpoint(String serverName, String method, String path, Map<String, Object> body) {
        String serverUrl = getServerUrl(serverName);
        try {
            Map<String, Object> response;
            if ("GET".equalsIgnoreCase(method)) {
                response = webClient.get()
                        .uri(serverUrl + path)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(30))
                        .block();
            } else if (body != null) {
                response = webClient.post()
                        .uri(serverUrl + path)
                        .bodyValue(body)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(30))
                        .block();
            } else {
                response = webClient.post()
                        .uri(serverUrl + path)
                        .retrieve()
                        .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                        .timeout(Duration.ofSeconds(30))
                        .block();
            }

            if (response != null && response.containsKey("data")) {
                return response.get("data");
            }
            return response;
        } catch (Exception e) {
            log.error("Failed to execute batch endpoint {} on {}: {}", path, serverUrl, e.getMessage());
            throw new RuntimeException("Failed to execute: " + e.getMessage());
        }
    }

    private AdminPlaceDto toAdminPlaceDto(Place place) {
        return AdminPlaceDto.builder()
                .id(place.getId())
                .name(place.getName())
                .roadAddress(place.getRoadAddress())
                .category(place.getCategory())
                .rating(place.getRating())
                .reviewCount(place.getReviewCount())
                .crawlStatus(place.getCrawlStatus() != null ? place.getCrawlStatus().name() : null)
                .embedStatus(place.getEmbedStatus() != null ? place.getEmbedStatus().name() : null)
                .imageCount(place.getImages() != null ? place.getImages().size() : 0)
                .menuCount(place.getMenus() != null ? place.getMenus().size() : 0)
                .createdAt(place.getCreatedAt())
                .updatedAt(place.getUpdatedAt())
                .build();
    }

    private WorkerStatusResponse.WorkerInfo mapToWorkerInfo(Map<String, Object> data) {
        WorkerStatusResponse.WorkerInfo info = new WorkerStatusResponse.WorkerInfo();
        info.setWorkerId(getString(data, "workerId"));
        info.setThreads(getInt(data, "threads"));
        // MoheBatch uses 'tasksProcessed', fallback to 'processedCount'
        long processed = getLong(data, "tasksProcessed");
        if (processed == 0) {
            processed = getLong(data, "processedCount");
        }
        info.setProcessedCount(processed);
        // MoheBatch uses 'tasksFailed', fallback to 'failedCount'
        long failed = getLong(data, "tasksFailed");
        if (failed == 0) {
            failed = getLong(data, "failedCount");
        }
        info.setFailedCount(failed);
        info.setLastHeartbeat(getString(data, "lastHeartbeat"));
        info.setStatus(getString(data, "status"));
        // MoheBatch uses 'currentTaskId', fallback to 'currentTask'
        String task = getString(data, "currentTaskId");
        if (task == null) {
            task = getString(data, "currentTask");
        }
        info.setCurrentTask(task);
        return info;
    }

    private long getLong(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return 0L;
    }

    private int getInt(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return 0;
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        return value != null ? value.toString() : null;
    }
}
