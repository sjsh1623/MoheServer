package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.Comment;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.CommentRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.UserRepository;
import com.mohe.spring.security.UserPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("댓글 서비스 테스트")
class CommentServiceTest {

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private CommentService commentService;

    private User testUser;
    private Place testPlace;
    private Comment testComment;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNickname("테스터");
        testUser.setProfileImageUrl("https://example.com/profile.jpg");

        testPlace = new Place();
        testPlace.setId(100L);
        testPlace.setName("테스트 카페");

        testComment = Comment.builder()
            .id(1L)
            .user(testUser)
            .place(testPlace)
            .content("좋은 장소입니다!")
            .rating(4.5)
            .build();
        testComment.setCreatedAt(LocalDateTime.now());
        testComment.setUpdatedAt(LocalDateTime.now());

        userPrincipal = new UserPrincipal(1L, "test@example.com", "password", "테스터", Collections.emptyList());

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("댓글 작성 성공")
    void createComment_Success() {
        // Given
        CommentCreateRequest request = new CommentCreateRequest("좋은 장소입니다!", 4.5);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentResponse response = commentService.createComment(100L, request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getContent()).isEqualTo("좋은 장소입니다!");
        assertThat(response.getRating()).isEqualTo(4.5);
        assertThat(response.getPlaceId()).isEqualTo(100L);
        assertThat(response.getUserId()).isEqualTo(1L);
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 작성 실패 - 장소를 찾을 수 없음")
    void createComment_PlaceNotFound() {
        // Given
        CommentCreateRequest request = new CommentCreateRequest("좋은 장소입니다!", 4.5);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.createComment(999L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("장소를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("장소 댓글 목록 조회 성공")
    void getCommentsByPlaceId_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> comments = Arrays.asList(testComment);
        Page<Comment> commentsPage = new PageImpl<>(comments, pageable, 1);

        when(placeRepository.existsById(100L)).thenReturn(true);
        when(commentRepository.findByPlaceIdOrderByCreatedAtDesc(eq(100L), any(Pageable.class)))
            .thenReturn(commentsPage);
        when(commentRepository.getAverageRatingByPlaceId(100L)).thenReturn(4.5);
        when(commentRepository.countByPlaceId(100L)).thenReturn(1L);

        // When
        CommentListResponse response = commentService.getCommentsByPlaceId(100L, 0, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getAverageRating()).isEqualTo(4.5);
        assertThat(response.getTotalComments()).isEqualTo(1);
    }

    @Test
    @DisplayName("장소 댓글 목록 조회 실패 - 장소를 찾을 수 없음")
    void getCommentsByPlaceId_PlaceNotFound() {
        // Given
        when(placeRepository.existsById(999L)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> commentService.getCommentsByPlaceId(999L, 0, 10))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("장소를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("내 댓글 목록 조회 성공")
    void getMyComments_Success() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);
        List<Comment> comments = Arrays.asList(testComment);
        Page<Comment> commentsPage = new PageImpl<>(comments, pageable, 1);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByUserIdOrderByCreatedAtDesc(eq(1L), any(Pageable.class)))
            .thenReturn(commentsPage);

        // When
        CommentListResponse response = commentService.getMyComments(0, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getComments()).hasSize(1);
        assertThat(response.getTotalElements()).isEqualTo(1);
    }

    @Test
    @DisplayName("댓글 수정 성공")
    void updateComment_Success() {
        // Given
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용", 5.0);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdWithUserAndPlace(1L)).thenReturn(Optional.of(testComment));
        when(commentRepository.save(any(Comment.class))).thenReturn(testComment);

        // When
        CommentResponse response = commentService.updateComment(1L, request);

        // Then
        assertThat(response).isNotNull();
        verify(commentRepository).save(any(Comment.class));
    }

    @Test
    @DisplayName("댓글 수정 실패 - 댓글을 찾을 수 없음")
    void updateComment_CommentNotFound() {
        // Given
        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용", 5.0);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdWithUserAndPlace(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(999L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("댓글 수정 실패 - 본인이 작성한 댓글이 아님")
    void updateComment_NotAuthor() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);
        otherUser.setEmail("other@example.com");

        Comment otherComment = Comment.builder()
            .id(2L)
            .user(otherUser)
            .place(testPlace)
            .content("다른 사람 댓글")
            .rating(3.0)
            .build();

        CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용", 5.0);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdWithUserAndPlace(2L)).thenReturn(Optional.of(otherComment));

        // When & Then
        assertThatThrownBy(() -> commentService.updateComment(2L, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("본인이 작성한 댓글만 수정할 수 있습니다");
    }

    @Test
    @DisplayName("댓글 삭제 성공")
    void deleteComment_Success() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdWithUserAndPlace(1L)).thenReturn(Optional.of(testComment));

        // When
        commentService.deleteComment(1L);

        // Then
        verify(commentRepository).delete(testComment);
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 댓글을 찾을 수 없음")
    void deleteComment_CommentNotFound() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdWithUserAndPlace(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("댓글을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("댓글 삭제 실패 - 본인이 작성한 댓글이 아님")
    void deleteComment_NotAuthor() {
        // Given
        User otherUser = new User();
        otherUser.setId(2L);

        Comment otherComment = Comment.builder()
            .id(2L)
            .user(otherUser)
            .place(testPlace)
            .content("다른 사람 댓글")
            .rating(3.0)
            .build();

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(commentRepository.findByIdWithUserAndPlace(2L)).thenReturn(Optional.of(otherComment));

        // When & Then
        assertThatThrownBy(() -> commentService.deleteComment(2L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("본인이 작성한 댓글만 삭제할 수 있습니다");
    }
}
