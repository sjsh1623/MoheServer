package com.mohe.spring.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mohe.spring.dto.*;
import com.mohe.spring.security.JwtTokenProvider;
import com.mohe.spring.service.CommentService;
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

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("댓글 컨트롤러 테스트")
class CommentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CommentService commentService;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    private CommentCreateRequest createRequest;
    private CommentUpdateRequest updateRequest;
    private CommentResponse commentResponse;
    private CommentListResponse commentListResponse;

    @BeforeEach
    void setUp() {
        // 댓글 작성 요청 데이터
        createRequest = new CommentCreateRequest("정말 좋은 장소입니다!", 4.5);

        // 댓글 수정 요청 데이터
        updateRequest = new CommentUpdateRequest("수정된 댓글 내용입니다.", 5.0);

        // 댓글 응답 데이터
        commentResponse = new CommentResponse(
            1L,
            123L,
            "카페 무브먼트랩",
            10L,
            "홍길동",
            "https://example.com/profile.jpg",
            "정말 좋은 장소입니다!",
            4.5,
            LocalDateTime.now(),
            LocalDateTime.now(),
            true
        );

        // 댓글 목록 응답 데이터
        List<CommentResponse> comments = Arrays.asList(commentResponse);
        commentListResponse = new CommentListResponse(
            comments,
            1,
            1L,
            0,
            10,
            4.5,
            1L
        );
    }

    @Test
    @DisplayName("댓글 작성 성공 테스트")
    @WithMockUser(roles = "USER")
    void testCreateCommentSuccess() throws Exception {
        // Given
        when(commentService.createComment(anyLong(), any(CommentCreateRequest.class)))
            .thenReturn(commentResponse);

        // When & Then
        mockMvc.perform(post("/api/places/123/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.placeId").value(123))
                .andExpect(jsonPath("$.data.content").value("정말 좋은 장소입니다!"))
                .andExpect(jsonPath("$.data.rating").value(4.5))
                .andExpect(jsonPath("$.data.isAuthor").value(true));
    }

    @Test
    @DisplayName("댓글 작성 실패 테스트 - 장소를 찾을 수 없음")
    @WithMockUser(roles = "USER")
    void testCreateCommentNotFound() throws Exception {
        // Given
        when(commentService.createComment(anyLong(), any(CommentCreateRequest.class)))
            .thenThrow(new RuntimeException("장소를 찾을 수 없습니다 (ID: 999)"));

        // When & Then
        mockMvc.perform(post("/api/places/999/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(createRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 작성 실패 테스트 - 유효성 검증 실패 (빈 내용)")
    @WithMockUser(roles = "USER")
    void testCreateCommentValidationFailed() throws Exception {
        // Given
        CommentCreateRequest invalidRequest = new CommentCreateRequest("", 4.5);

        // When & Then
        mockMvc.perform(post("/api/places/123/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("장소 댓글 목록 조회 성공 테스트")
    void testGetCommentsByPlaceSuccess() throws Exception {
        // Given
        when(commentService.getCommentsByPlaceId(anyLong(), anyInt(), anyInt()))
            .thenReturn(commentListResponse);

        // When & Then
        mockMvc.perform(get("/api/places/123/comments")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.comments").isArray())
                .andExpect(jsonPath("$.data.comments[0].id").value(1))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.averageRating").value(4.5))
                .andExpect(jsonPath("$.data.totalComments").value(1));
    }

    @Test
    @DisplayName("장소 댓글 목록 조회 실패 테스트 - 장소를 찾을 수 없음")
    void testGetCommentsByPlaceNotFound() throws Exception {
        // Given
        when(commentService.getCommentsByPlaceId(anyLong(), anyInt(), anyInt()))
            .thenThrow(new RuntimeException("장소를 찾을 수 없습니다 (ID: 999)"));

        // When & Then
        mockMvc.perform(get("/api/places/999/comments")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("내가 작성한 댓글 목록 조회 성공 테스트")
    @WithMockUser(roles = "USER")
    void testGetMyCommentsSuccess() throws Exception {
        // Given
        when(commentService.getMyComments(anyInt(), anyInt()))
            .thenReturn(commentListResponse);

        // When & Then
        mockMvc.perform(get("/api/user/comments")
                .param("page", "0")
                .param("size", "10"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.comments").isArray())
                .andExpect(jsonPath("$.data.comments[0].id").value(1));
    }

    @Test
    @DisplayName("댓글 수정 성공 테스트")
    @WithMockUser(roles = "USER")
    void testUpdateCommentSuccess() throws Exception {
        // Given
        CommentResponse updatedResponse = new CommentResponse(
            1L, 123L, "카페 무브먼트랩", 10L, "홍길동",
            "https://example.com/profile.jpg",
            "수정된 댓글 내용입니다.", 5.0,
            LocalDateTime.now(), LocalDateTime.now(), true
        );
        when(commentService.updateComment(anyLong(), any(CommentUpdateRequest.class)))
            .thenReturn(updatedResponse);

        // When & Then
        mockMvc.perform(put("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").value("수정된 댓글 내용입니다."))
                .andExpect(jsonPath("$.data.rating").value(5.0));
    }

    @Test
    @DisplayName("댓글 수정 실패 테스트 - 댓글을 찾을 수 없음")
    @WithMockUser(roles = "USER")
    void testUpdateCommentNotFound() throws Exception {
        // Given
        when(commentService.updateComment(anyLong(), any(CommentUpdateRequest.class)))
            .thenThrow(new RuntimeException("댓글을 찾을 수 없습니다 (ID: 999)"));

        // When & Then
        mockMvc.perform(put("/api/comments/999")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 수정 실패 테스트 - 본인이 작성한 댓글이 아님")
    @WithMockUser(roles = "USER")
    void testUpdateCommentForbidden() throws Exception {
        // Given
        when(commentService.updateComment(anyLong(), any(CommentUpdateRequest.class)))
            .thenThrow(new RuntimeException("본인이 작성한 댓글만 수정할 수 있습니다"));

        // When & Then
        mockMvc.perform(put("/api/comments/1")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest)))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }

    @Test
    @DisplayName("댓글 삭제 성공 테스트")
    @WithMockUser(roles = "USER")
    void testDeleteCommentSuccess() throws Exception {
        // Given
        doNothing().when(commentService).deleteComment(anyLong());

        // When & Then
        mockMvc.perform(delete("/api/comments/1"))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("댓글이 삭제되었습니다"));
    }

    @Test
    @DisplayName("댓글 삭제 실패 테스트 - 댓글을 찾을 수 없음")
    @WithMockUser(roles = "USER")
    void testDeleteCommentNotFound() throws Exception {
        // Given
        doThrow(new RuntimeException("댓글을 찾을 수 없습니다 (ID: 999)"))
            .when(commentService).deleteComment(anyLong());

        // When & Then
        mockMvc.perform(delete("/api/comments/999"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    @DisplayName("댓글 삭제 실패 테스트 - 본인이 작성한 댓글이 아님")
    @WithMockUser(roles = "USER")
    void testDeleteCommentForbidden() throws Exception {
        // Given
        doThrow(new RuntimeException("본인이 작성한 댓글만 삭제할 수 있습니다"))
            .when(commentService).deleteComment(anyLong());

        // When & Then
        mockMvc.perform(delete("/api/comments/1"))
                .andDo(print())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("ACCESS_DENIED"));
    }
}
