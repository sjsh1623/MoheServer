package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.*;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.BookmarkRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.UserRepository;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.security.UserPrincipal;
import com.mohe.spring.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("추천 컨트롤러 테스트")
class RecommendationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private EnhancedRecommendationService enhancedRecommendationService;

    @MockBean
    private ContextualRecommendationService contextualRecommendationService;

    @MockBean
    private WeatherService weatherService;

    @MockBean
    private PlaceService placeService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private PlaceRepository placeRepository;

    @MockBean
    private BookmarkRepository bookmarkRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @Test
    @DisplayName("강화된 추천 조회 성공 테스트")
    void testGetEnhancedRecommendationsSuccess() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@mohe.com");
        mockUser.setPasswordHash("password");
        mockUser.setNickname("testnick");

        UserPrincipal userPrincipal = UserPrincipal.create(mockUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("algorithm", "mbti_similarity");
        EnhancedRecommendationsResponse response = new EnhancedRecommendationsResponse(
            List.of(), metadata, "MBTI 기반 추천", 0
        );
        when(enhancedRecommendationService.getEnhancedRecommendations(any(User.class), anyInt(), anyBoolean()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/recommendations/enhanced")
                .with(user(userPrincipal))
                .param("limit", "15")
                .param("excludeBookmarked", "true"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("강화된 추천 조회 실패 테스트 - 잘못된 limit")
    @WithMockUser(username = "test@mohe.com", roles = "USER")
    void testGetEnhancedRecommendationsInvalidLimit() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/recommendations/enhanced")
                .param("limit", "100"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("MBTI별 추천 조회 성공 테스트")
    void testGetMbtiSpecificRecommendationsSuccess() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@mohe.com");
        mockUser.setPasswordHash("password");
        mockUser.setNickname("testnick");

        UserPrincipal userPrincipal = UserPrincipal.create(mockUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mbti", "INTJ");
        EnhancedRecommendationsResponse response = new EnhancedRecommendationsResponse(
            List.of(), metadata, "INTJ 추천", 0
        );
        when(enhancedRecommendationService.getEnhancedRecommendations(any(User.class), anyInt(), anyBoolean()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/recommendations/mbti/INTJ")
                .with(user(userPrincipal))
                .param("limit", "15"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("MBTI별 추천 조회 실패 테스트 - 잘못된 MBTI")
    @WithMockUser(username = "test@mohe.com", roles = "USER")
    void testGetMbtiSpecificRecommendationsInvalidMbti() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/recommendations/mbti/INVALID")
                .param("limit", "15"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("추천 설명 조회 성공 테스트")
    void testGetRecommendationExplanationSuccess() throws Exception {
        // Given
        User mockUser = new User();
        mockUser.setId(1L);
        mockUser.setEmail("test@mohe.com");
        mockUser.setMbti("ENTJ");
        mockUser.setPasswordHash("password");
        mockUser.setNickname("testnick");

        UserPrincipal userPrincipal = UserPrincipal.create(mockUser);
        when(userRepository.findById(1L)).thenReturn(Optional.of(mockUser));

        // When & Then
        mockMvc.perform(get("/api/recommendations/explanation")
                .with(user(userPrincipal)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.algorithm").value("mbti_similarity_based"))
                .andExpect(jsonPath("$.data.userMbti").value("ENTJ"));
    }

    @Test
    @DisplayName("컨텍스트 기반 추천 조회 성공 테스트 - 인증된 사용자")
    @WithMockUser(username = "test@mohe.com", roles = "USER")
    void testGetContextualRecommendationsAuthenticatedSuccess() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("weather", "맑음");
        context.put("time", "오후");
        ContextualRecommendationResponse response = new ContextualRecommendationResponse(
            List.of(),
            context,
            "맑음",
            "오후",
            "현재 날씨와 시간에 적합한 장소입니다"
        );
        when(contextualRecommendationService.getContextualRecommendations(anyString(), anyDouble(), anyDouble(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/recommendations/contextual")
                .param("lat", "37.5665")
                .param("lon", "126.9780")
                .param("query", "카페")
                .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("컨텍스트 기반 추천 조회 성공 테스트 - 게스트")
    void testGetContextualRecommendationsGuestSuccess() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("weather", "맑음");
        context.put("time", "오후");
        ContextualRecommendationResponse response = new ContextualRecommendationResponse(
            List.of(),
            context,
            "맑음",
            "오후",
            "현재 날씨와 시간에 적합한 장소입니다"
        );
        when(contextualRecommendationService.getContextualRecommendations(anyString(), anyDouble(), anyDouble(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/recommendations/contextual")
                .param("lat", "37.5665")
                .param("lon", "126.9780")
                .param("query", "카페")
                .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("컨텍스트 기반 추천 조회 실패 테스트 - 잘못된 limit")
    void testGetContextualRecommendationsInvalidLimit() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/recommendations/contextual")
                .param("lat", "37.5665")
                .param("lon", "126.9780")
                .param("limit", "100"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_LIMIT"));
    }

    @Test
    @DisplayName("쿼리 기반 추천 조회 성공 테스트 (레거시)")
    @WithMockUser(username = "test@mohe.com", roles = "USER")
    void testQueryRecommendationsSuccess() throws Exception {
        // Given
        Map<String, Object> context = new HashMap<>();
        context.put("weather", "맑음");
        context.put("time", "오후");
        ContextualRecommendationResponse response = new ContextualRecommendationResponse(
            List.of(),
            context,
            "맑음",
            "오후",
            "현재 날씨와 시간에 적합한 장소입니다"
        );
        when(contextualRecommendationService.getContextualRecommendations(anyString(), anyDouble(), anyDouble(), anyInt()))
            .thenReturn(response);

        Map<String, Object> requestBody = Map.of(
            "lat", 37.5665,
            "lon", 126.9780,
            "query", "카페",
            "limit", 10
        );

        // When & Then
        mockMvc.perform(post("/api/recommendations/query")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(requestBody)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("현재 시간 추천 조회 성공 테스트")
    void testGetCurrentTimeRecommendationsSuccess() throws Exception {
        // Given
        Map<String, Object> timeContext = new HashMap<>();
        timeContext.put("time", "오후");
        timeContext.put("weather", "맑음");
        CurrentTimeRecommendationsResponse response = new CurrentTimeRecommendationsResponse(
            List.of(),
            timeContext,
            "맑음",
            "오후"
        );
        when(placeService.getCurrentTimePlaces(any(), any(), anyInt()))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/recommendations/current-time")
                .param("latitude", "37.5665")
                .param("longitude", "126.9780")
                .param("limit", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("북마크 기반 추천 조회 성공 테스트")
    void testGetBookmarkBasedRecommendationsSuccess() throws Exception {
        // Given
        when(bookmarkRepository.findMostBookmarkedPlaces(any()))
            .thenReturn(List.of());

        // When & Then
        mockMvc.perform(get("/api/recommendations/bookmark-based")
                .param("limit", "15"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("강화된 추천 인증 실패 테스트")
    void testGetEnhancedRecommendationsWithoutAuth() throws Exception {
        // When & Then
        // Without authentication, the controller will throw NPE when accessing userPrincipal.getId()
        // This results in 400/500 error
        mockMvc.perform(get("/api/recommendations/enhanced")
                .param("limit", "15"))
                .andDo(print())
                .andExpect(status().is4xxClientError());  // Expecting error due to missing authentication
    }
}
