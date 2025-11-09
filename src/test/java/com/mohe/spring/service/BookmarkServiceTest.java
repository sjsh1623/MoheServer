package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.Bookmark;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.BookmarkRepository;
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
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("북마크 서비스 테스트")
class BookmarkServiceTest {

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SimilarityCalculationService similarityCalculationService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BookmarkService bookmarkService;

    private User testUser;
    private Place testPlace;
    private Bookmark testBookmark;
    private UserPrincipal userPrincipal;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setEmail("test@example.com");
        testUser.setNickname("테스터");

        testPlace = new Place();
        testPlace.setId(100L);
        testPlace.setName("테스트 카페");
        testPlace.setRoadAddress("서울시 강남구");
        testPlace.setRating(new java.math.BigDecimal("4.5"));

        testBookmark = new Bookmark();
        testBookmark.setId(1L);
        testBookmark.setUser(testUser);
        testBookmark.setPlace(testPlace);
        testBookmark.setCreatedAt(OffsetDateTime.now());

        userPrincipal = new UserPrincipal(1L, "test@example.com", "password", "테스터", Collections.emptyList());

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("북마크 토글 - 추가")
    void toggleBookmark_Add() {
        // Given
        BookmarkToggleRequest request = new BookmarkToggleRequest("100");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.findByUserAndPlace(testUser, testPlace)).thenReturn(Optional.empty());
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(testBookmark);

        // When
        BookmarkToggleResponse response = bookmarkService.toggleBookmark(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isTrue();
        assertThat(response.getMessage()).contains("추가");
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 토글 - 제거")
    void toggleBookmark_Remove() {
        // Given
        BookmarkToggleRequest request = new BookmarkToggleRequest("100");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.findByUserAndPlace(testUser, testPlace)).thenReturn(Optional.of(testBookmark));

        // When
        BookmarkToggleResponse response = bookmarkService.toggleBookmark(request);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isFalse();
        assertThat(response.getMessage()).contains("제거");
        verify(bookmarkRepository).deleteByUserAndPlace(testUser, testPlace);
    }

    @Test
    @DisplayName("북마크 추가 성공")
    void addBookmark_Success() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.existsByUserAndPlace(testUser, testPlace)).thenReturn(false);
        when(bookmarkRepository.save(any(Bookmark.class))).thenReturn(testBookmark);

        // When
        BookmarkToggleResponse response = bookmarkService.addBookmark("100");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isTrue();
        verify(bookmarkRepository).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 추가 실패 - 이미 북마크됨")
    void addBookmark_AlreadyExists() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.existsByUserAndPlace(testUser, testPlace)).thenReturn(true);

        // When
        BookmarkToggleResponse response = bookmarkService.addBookmark("100");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isTrue();
        assertThat(response.getMessage()).contains("이미");
        verify(bookmarkRepository, never()).save(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 제거 성공")
    void removeBookmark_Success() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.findByUserAndPlace(testUser, testPlace)).thenReturn(Optional.of(testBookmark));

        // When
        BookmarkToggleResponse response = bookmarkService.removeBookmark("100");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isFalse();
        verify(bookmarkRepository).deleteByUserAndPlace(testUser, testPlace);
    }

    @Test
    @DisplayName("북마크 제거 실패 - 북마크되지 않음")
    void removeBookmark_NotExists() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.findByUserAndPlace(testUser, testPlace)).thenReturn(Optional.empty());

        // When
        BookmarkToggleResponse response = bookmarkService.removeBookmark("100");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isFalse();
        assertThat(response.getMessage()).contains("북마크되지 않은");
        verify(bookmarkRepository, never()).delete(any(Bookmark.class));
    }

    @Test
    @DisplayName("북마크 상태 확인 - 존재함")
    void getBookmarkStatus_Exists() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.existsByUserAndPlace(testUser, testPlace)).thenReturn(true);

        // When
        BookmarkStatusResponse response = bookmarkService.getBookmarkStatus("100");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isTrue();
    }

    @Test
    @DisplayName("북마크 상태 확인 - 존재하지 않음")
    void getBookmarkStatus_NotExists() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(bookmarkRepository.existsByUserAndPlace(testUser, testPlace)).thenReturn(false);

        // When
        BookmarkStatusResponse response = bookmarkService.getBookmarkStatus("100");

        // Then
        assertThat(response).isNotNull();
        assertThat(response.isBookmarked()).isFalse();
    }

    @Test
    @DisplayName("북마크 목록 조회")
    void getBookmarks() {
        // Given
        List<Bookmark> bookmarks = Arrays.asList(testBookmark);
        Page<Bookmark> bookmarksPage = new PageImpl<>(bookmarks);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookmarkRepository.findByUserOrderByCreatedAtDesc(eq(testUser), any(Pageable.class)))
            .thenReturn(bookmarksPage);

        // When
        BookmarkListResponse response = bookmarkService.getBookmarks(0, 10);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getBookmarks()).hasSize(1);
        assertThat(response.getBookmarks().get(0).getPlace().getName()).isEqualTo("테스트 카페");
    }

    @Test
    @DisplayName("장소 ID 유효성 검증 - 잘못된 형식")
    void resolvePlace_InvalidFormat() {
        // Given
        BookmarkToggleRequest request = new BookmarkToggleRequest("invalid");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> bookmarkService.toggleBookmark(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("유효하지 않은 장소 ID");
    }

    @Test
    @DisplayName("장소 찾기 실패")
    void resolvePlace_NotFound() {
        // Given
        BookmarkToggleRequest request = new BookmarkToggleRequest("999");

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> bookmarkService.toggleBookmark(request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("장소를 찾을 수 없습니다");
    }
}
