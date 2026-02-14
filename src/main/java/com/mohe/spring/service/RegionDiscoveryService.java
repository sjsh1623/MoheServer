package com.mohe.spring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RegionDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(RegionDiscoveryService.class);

    private final WebClient webClient;

    // 동일 지역 중복 요청 방지 (key: "lat_lon" 반올림, value: 마지막 요청 시각)
    private final ConcurrentHashMap<String, Long> recentRequests = new ConcurrentHashMap<>();
    private static final long COOLDOWN_MS = 30 * 60 * 1000; // 30분

    public RegionDiscoveryService(
            @Value("${crawler.base-url:http://localhost:4000}") String crawlerBaseUrl
    ) {
        this.webClient = WebClient.builder()
                .baseUrl(crawlerBaseUrl)
                .build();
    }

    public void discoverPlaces(double latitude, double longitude, double radiusKm) {
        // 소수점 2자리로 반올림하여 같은 지역 판별
        String regionKey = String.format("%.2f_%.2f", latitude, longitude);

        Long lastRequest = recentRequests.get(regionKey);
        long now = System.currentTimeMillis();

        if (lastRequest != null && (now - lastRequest) < COOLDOWN_MS) {
            log.debug("[RegionDiscovery] Skipping - recent request for region {}", regionKey);
            return;
        }

        recentRequests.put(regionKey, now);

        // 오래된 항목 정리 (1000개 초과 시)
        if (recentRequests.size() > 1000) {
            recentRequests.entrySet().removeIf(entry -> (now - entry.getValue()) > COOLDOWN_MS);
        }

        log.info("[RegionDiscovery] Requesting discover for lat={}, lon={}, radius={}km", latitude, longitude, radiusKm);

        try {
            Map<String, Object> body = Map.of(
                    "latitude", latitude,
                    "longitude", longitude,
                    "radius_km", radiusKm
            );

            String response = webClient.post()
                    .uri("/api/v1/discover")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMinutes(5))
                    .block();

            log.info("[RegionDiscovery] Discover response: {}", response);

        } catch (Exception e) {
            log.warn("[RegionDiscovery] Failed to call discover API: {}", e.getMessage());
        }
    }
}
