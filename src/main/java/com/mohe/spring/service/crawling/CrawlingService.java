package com.mohe.spring.service.crawling;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.dto.crawling.CrawlingResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CrawlingService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CrawlingService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                          @Value("${crawler.base-url:http://localhost:4000}") String baseUrl) {
        // HttpClient 설정: 타임아웃 증가 및 연결 풀 설정
        // 크롤러는 Selenium으로 실제 브라우저를 구동하므로 매우 긴 타임아웃 필요
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(15))  // 응답 타임아웃 15분으로 증가
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 120000)  // 연결 타임아웃 2분으로 증가
                .option(ChannelOption.SO_KEEPALIVE, true)  // Keep-Alive 활성화
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(900, TimeUnit.SECONDS))  // 읽기 타임아웃 15분으로 증가
                        .addHandlerLast(new WriteTimeoutHandler(180, TimeUnit.SECONDS)));  // 쓰기 타임아웃 3분으로 증가

        this.webClient = webClientBuilder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
        this.objectMapper = objectMapper;
    }

    public Mono<CrawlingResponse<CrawledDataDto>> crawlPlaceData(String searchQuery, String placeName) {
        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("searchQuery", searchQuery + " " + placeName);
        requestBody.put("placeName", placeName);

        return webClient.post()
                .uri("/api/v1/place")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .map(responseMap -> {
                    CrawlingResponse<CrawledDataDto> response = new CrawlingResponse<>();
                    response.setSuccess((Boolean) responseMap.get("success"));
                    response.setMessage((String) responseMap.get("message"));
                    CrawledDataDto data = objectMapper.convertValue(responseMap.get("data"), new TypeReference<CrawledDataDto>() {});
                    response.setData(data);
                    return response;
                })
                .onErrorResume(error -> {
                    // Handle 404, 500, and other errors gracefully
                    CrawlingResponse<CrawledDataDto> errorResponse = new CrawlingResponse<>();
                    errorResponse.setSuccess(false);
                    errorResponse.setMessage(error.getMessage());
                    errorResponse.setData(null);
                    return Mono.just(errorResponse);
                });
    }

    /**
     * 장소 이미지만 크롤링
     *
     * @param placeName 장소명
     * @param location 주소
     * @return 이미지 URL 리스트를 포함한 Map
     */
    public Map<String, Object> fetchPlaceImages(String placeName, String location) {
        try {
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("searchQuery", String.format("%s %s", location != null ? location : "", placeName).trim());
            requestBody.put("placeName", placeName);

            // 재시도 로직 추가: 최대 3번 시도
            Map<String, Object> response = webClient.post()
                    .uri("/api/v1/place/images")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .retry(2)  // 실패 시 2번 더 재시도 (총 3번 시도)
                    .block(Duration.ofMinutes(15)); // 최대 15분 대기

            if (response == null || !Boolean.TRUE.equals(response.get("success"))) {
                return null;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> data = (Map<String, Object>) response.get("data");
            if (data == null) {
                return null;
            }

            @SuppressWarnings("unchecked")
            var imageUrls = (List<String>) data.getOrDefault("image_urls", data.get("images"));

            if (imageUrls == null || imageUrls.isEmpty()) {
                return null;
            }

            // Normalize key so downstream processors can always find "images"
            data.put("images", imageUrls);
            return data;
        } catch (Exception e) {
            return null; // Skip this place on error
        }
    }
}
