package com.mohe.spring.controller;

import com.mohe.spring.dto.*;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.service.PlaceService;
import com.mohe.spring.service.VectorSearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("장소 컨트롤러 테스트")
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PlaceService placeService;

    @MockBean
    private VectorSearchService vectorSearchService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("장소 추천 조회 성공 테스트")
    void testGetRecommendationsSuccess() throws Exception {
        // Given
        PlaceRecommendationsResponse response = createMockRecommendationsResponse();
        when(placeService.getRecommendations(anyDouble(), anyDouble())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/recommendations")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("장소 목록 조회 성공 테스트")
    void testGetPlacesSuccess() throws Exception {
        // Given
        PlaceListResponse response = createMockPlaceListResponse();
        when(placeService.getPlaces(anyInt(), anyInt(), any(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places")
                .param("page", "1")
                .param("limit", "20")
                .param("sort", "rating"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("장소 상세 정보 조회 성공 테스트")
    void testGetPlaceDetailSuccess() throws Exception {
        // Given
        PlaceDetailResponse response = createMockPlaceDetailResponse();
        when(placeService.getPlaceDetail(anyString())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("장소 상세 정보 조회 실패 테스트 - 존재하지 않는 장소")
    void testGetPlaceDetailNotFound() throws Exception {
        // Given
        when(placeService.getPlaceDetail(anyString()))
            .thenThrow(new RuntimeException("장소를 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(get("/api/places/9999"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("장소 검색 성공 테스트")
    void testSearchPlacesSuccess() throws Exception {
        // Given
        PlaceSearchResponse response = createMockSearchResponse();
        when(placeService.searchPlaces(anyString(), any(), any(), any()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/search")
                .param("q", "카페")
                .param("location", "성수동")
                .param("weather", "hot")
                .param("time", "afternoon"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.searchResults").exists());
    }

    @Test
    @DisplayName("주변 장소 조회 성공 테스트")
    void testGetNearbyPlacesSuccess() throws Exception {
        // Given
        PlaceListResponse response = createMockPlaceListResponse();
        when(placeService.getNearbyPlaces(anyDouble(), anyDouble(), anyDouble(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/nearby")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("radius", "3000")
                .param("limit", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("디버그 정보 조회 성공 테스트")
    void testDebugPlacesSuccess() throws Exception {
        // Given
        Map<String, Object> debugInfo = Map.of(
            "totalPlaces", 100,
            "readyPlaces", 80,
            "notReadyPlaces", 20
        );
        when(placeService.getDebugInfo()).thenReturn(debugInfo);

        // When & Then
        mockMvc.perform(get("/api/places/debug"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalPlaces").value(100));
    }

    @Test
    @DisplayName("인기 장소 목록 조회 성공 테스트")
    void testGetPopularPlacesSuccess() throws Exception {
        // Given
        PlaceListResponse response = createMockPlaceListResponse();
        when(placeService.getPopularPlaces(anyDouble(), anyDouble(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/popular")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("limit", "20"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("현재 시간 추천 장소 조회 성공 테스트")
    void testGetCurrentTimePlacesSuccess() throws Exception {
        // Given
        CurrentTimeRecommendationsResponse response = createMockCurrentTimeResponse();
        when(placeService.getCurrentTimePlaces(any(), any(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/current-time")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("일반 장소 목록 조회 성공 테스트")
    void testGetPlacesListSuccess() throws Exception {
        // Given
        PlaceListResponse response = createMockPlaceListResponse();
        when(placeService.getPlacesList(anyInt(), anyInt(), anyString()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/list")
                .param("page", "0")
                .param("limit", "10")
                .param("sort", "popularity"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("벡터 유사도 기반 장소 검색 성공 테스트")
    @WithMockUser(username = "test@mohe.com", roles = "USER")
    void testVectorSearchPlacesSuccess() throws Exception {
        // Given
        VectorSimilarityResponse response = createMockVectorSimilarityResponse();
        when(vectorSearchService.searchWithVectorSimilarity(anyString(), anyString(), anyDouble(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/vector-search")
                .param("query", "조용한 카페")
                .param("threshold", "0.3")
                .param("limit", "15"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("벡터 검색 인증 실패 테스트")
    void testVectorSearchWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/places/vector-search")
                .param("query", "조용한 카페"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("새로운 장소 추천 조회 성공 테스트")
    void testGetNewRecommendationsSuccess() throws Exception {
        // Given
        PlaceRecommendationsResponse response = createMockRecommendationsResponse();
        when(placeService.getRecommendations(anyDouble(), anyDouble())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/places/new")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    // Helper methods to create mock responses
    private PlaceRecommendationsResponse createMockRecommendationsResponse() {
        return new PlaceRecommendationsResponse(List.of(), 0, "default");
    }

    private PlaceListResponse createMockPlaceListResponse() {
        return new PlaceListResponse(List.of(), 0, 1, 20, 0);
    }

    private PlaceDetailResponse createMockPlaceDetailResponse() {
        SimplePlaceDto placeDto = new SimplePlaceDto();
        placeDto.setId("1");
        placeDto.setName("테스트 카페");
        placeDto.setAddress("서울 성수동");
        placeDto.setRating(4.7);

        return new PlaceDetailResponse(
            placeDto,
            List.of("https://example.com/image.jpg"),
            false,
            List.of()
        );
    }

    private PlaceSearchResponse createMockSearchResponse() {
        return new PlaceSearchResponse(
            List.of(),
            Map.of("weather", "더운 날씨", "time", "오후 2시", "recommendation", "지금은 멀지 않고 실내 장소들을 추천드릴께요."),
            0,
            "카페"
        );
    }

    private CurrentTimeRecommendationsResponse createMockCurrentTimeResponse() {
        return new CurrentTimeRecommendationsResponse(
            List.of(),
            Map.of("time", "오후", "weather", "맑음", "recommendation", "현재 시간에 적합한 장소를 추천합니다."),
            "맑음",
            "오후"
        );
    }

    private VectorSimilarityResponse createMockVectorSimilarityResponse() {
        return new VectorSimilarityResponse(List.of(), 1L, null, 0, 0.3, "keyword", 10L);
    }
}
