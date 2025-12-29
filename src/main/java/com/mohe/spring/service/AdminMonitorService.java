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
        info.setProcessedCount(getLong(data, "processedCount"));
        info.setFailedCount(getLong(data, "failedCount"));
        info.setLastHeartbeat(getString(data, "lastHeartbeat"));
        info.setStatus(getString(data, "status"));
        info.setCurrentTask(getString(data, "currentTask"));
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
