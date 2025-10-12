package com.mohe.spring.service.crawling;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.crawling.CrawledDataDto;
import com.mohe.spring.dto.crawling.CrawlingResponse;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class CrawlingService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public CrawlingService(WebClient.Builder webClientBuilder, ObjectMapper objectMapper,
                          @Value("${crawler.base-url:http://host.docker.internal:4000}") String baseUrl) {
        // HttpClient 설정: 타임아웃 증가 및 연결 풀 설정
        // 크롤러는 Selenium으로 실제 브라우저를 구동하므로 매우 긴 타임아웃 필요
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMinutes(10))  // 응답 타임아웃 10분
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)  // 연결 타임아웃 60초
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(600, TimeUnit.SECONDS))  // 읽기 타임아웃 10분
                        .addHandlerLast(new WriteTimeoutHandler(120, TimeUnit.SECONDS)));  // 쓰기 타임아웃 2분

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
                });
    }
}
