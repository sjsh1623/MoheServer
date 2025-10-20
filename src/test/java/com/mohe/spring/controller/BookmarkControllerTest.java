package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.*;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.service.BookmarkService;
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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("북마크 컨트롤러 테스트")
class BookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private BookmarkService bookmarkService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private BookmarkToggleRequest bookmarkToggleRequest;

    @BeforeEach
    void setUp() {
        bookmarkToggleRequest = new BookmarkToggleRequest("1");
    }

    @Test
    @DisplayName("북마크 토글 성공 테스트 - 추가")
    @WithMockUser(roles = "USER")
    void testToggleBookmarkAddSuccess() throws Exception {
        // Given
        BookmarkToggleResponse response = new BookmarkToggleResponse(true, "북마크가 추가되었습니다.");
        when(bookmarkService.toggleBookmark(any(BookmarkToggleRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/bookmarks/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookmarkToggleRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookmarked").value(true))
                .andExpect(jsonPath("$.data.message").value("북마크가 추가되었습니다."));
    }

    @Test
    @DisplayName("북마크 토글 성공 테스트 - 제거")
    @WithMockUser(roles = "USER")
    void testToggleBookmarkRemoveSuccess() throws Exception {
        // Given
        BookmarkToggleResponse response = new BookmarkToggleResponse(false, "북마크가 제거되었습니다.");
        when(bookmarkService.toggleBookmark(any(BookmarkToggleRequest.class)))
            .thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/bookmarks/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookmarkToggleRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookmarked").value(false))
                .andExpect(jsonPath("$.data.message").value("북마크가 제거되었습니다."));
    }

    @Test
    @DisplayName("북마크 토글 실패 테스트 - 존재하지 않는 장소")
    @WithMockUser(roles = "USER")
    void testToggleBookmarkNotFound() throws Exception {
        // Given
        when(bookmarkService.toggleBookmark(any(BookmarkToggleRequest.class)))
            .thenThrow(new RuntimeException("장소를 찾을 수 없습니다"));

        // When & Then
        mockMvc.perform(post("/api/bookmarks/toggle")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookmarkToggleRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("북마크 추가 성공 테스트")
    @WithMockUser(roles = "USER")
    void testAddBookmarkSuccess() throws Exception {
        // Given
        BookmarkToggleResponse response = new BookmarkToggleResponse(true, "북마크가 추가되었습니다.");
        when(bookmarkService.addBookmark(anyString())).thenReturn(response);

        // When & Then
        mockMvc.perform(post("/api/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(bookmarkToggleRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("북마크 제거 성공 테스트")
    @WithMockUser(roles = "USER")
    void testRemoveBookmarkSuccess() throws Exception {
        // Given
        BookmarkToggleResponse response = new BookmarkToggleResponse(false, "북마크가 제거되었습니다.");
        when(bookmarkService.removeBookmark(anyString())).thenReturn(response);

        // When & Then
        mockMvc.perform(delete("/api/bookmarks/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookmarked").value(false));
    }

    @Test
    @DisplayName("북마크 목록 조회 성공 테스트")
    @WithMockUser(roles = "USER")
    void testGetBookmarksSuccess() throws Exception {
        // Given
        BookmarkListResponse response = new BookmarkListResponse(List.of());
        when(bookmarkService.getBookmarks(anyInt(), anyInt())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/bookmarks")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    @DisplayName("북마크 여부 확인 성공 테스트")
    @WithMockUser(roles = "USER")
    void testGetBookmarkStatusSuccess() throws Exception {
        // Given
        BookmarkStatusResponse response = new BookmarkStatusResponse(true);
        when(bookmarkService.getBookmarkStatus(anyString())).thenReturn(response);

        // When & Then
        mockMvc.perform(get("/api/bookmarks/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.bookmarked").value(true));
    }

    @Test
    @DisplayName("인증 없이 북마크 접근 실패 테스트")
    void testBookmarkWithoutAuth() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/bookmarks"))
                .andDo(print())
                .andExpect(status().isForbidden());
    }
}
