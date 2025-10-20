package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.*;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.service.ActivityService;
import org.junit.jupiter.api.BeforeEach;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("사용자 활동 컨트롤러 테스트")
class ActivityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ActivityService activityService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private RecentViewRequest recentViewRequest;

    @BeforeEach
    void setUp() {
        recentViewRequest = new RecentViewRequest("1");
    }

    @Test
    @DisplayName("최근 조회한 장소 목록 조회 성공 테스트")
    @WithMockUser(roles = "USER")
    void testGetRecentPlacesSuccess() throws Exception {
        // Given
        RecentPlacesResponse response = new RecentPlacesResponse(List.of(), 0);
        when(activityService.getRecentPlaces()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/user/recent-places"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recent_places").exists());
    }

    @Test
    @DisplayName("최근 조회한 장소 기록 성공 테스트")
    @WithMockUser(roles = "USER")
    void testRecordRecentPlaceSuccess() throws Exception {
        // Given
        doNothing().when(activityService).recordRecentPlaceView(anyString());

        // When & Then
        mockMvc.perform(post("/api/user/recent-places")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recentViewRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("최근 조회 이력이 저장되었습니다."));
    }

    @Test
    @DisplayName("내가 등록한 장소 목록 조회 성공 테스트")
    @WithMockUser(roles = "USER")
    void testGetMyPlacesSuccess() throws Exception {
        // Given
        MyPlacesResponse response = new MyPlacesResponse(List.of(), 0, 0, 10, false);
        when(activityService.getMyPlaces()).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/user/my-places"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.places").exists());
    }

    @Test
    @DisplayName("인증 없이 최근 조회 장소 접근 실패 테스트")
    void testGetRecentPlacesWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/recent-places"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 장소 조회 기록 실패 테스트")
    void testRecordRecentPlaceWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/user/recent-places")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recentViewRequest)))
                .andDo(print())
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("인증 없이 내 장소 접근 실패 테스트")
    void testGetMyPlacesWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/user/my-places"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
