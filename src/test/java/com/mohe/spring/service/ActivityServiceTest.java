package com.mohe.spring.service;

import com.mohe.spring.dto.MyPlacesResponse;
import com.mohe.spring.dto.RecentPlacesResponse;
import com.mohe.spring.entity.Bookmark;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.RecentView;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.BookmarkRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.RecentViewRepository;
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
@DisplayName("활동 서비스 테스트")
class ActivityServiceTest {

    @Mock
    private RecentViewRepository recentViewRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PlaceRepository placeRepository;

    @Mock
    private BookmarkRepository bookmarkRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private ActivityService activityService;

    private User testUser;
    private Place testPlace;
    private RecentView testRecentView;
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
        testPlace.setCategory(Arrays.asList("카페"));
        testPlace.setReviewCount(100);

        testRecentView = new RecentView(testUser, testPlace);
        testRecentView.setViewedAt(OffsetDateTime.now());

        testBookmark = new Bookmark();
        testBookmark.setId(1L);
        testBookmark.setUser(testUser);
        testBookmark.setPlace(testPlace);
        testBookmark.setCreatedAt(OffsetDateTime.now());

        userPrincipal = new UserPrincipal(1L, "test@example.com", "password", "테스터", Collections.emptyList());

        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("최근 조회한 장소 목록 조회 성공")
    void getRecentPlaces_Success() {
        // Given
        List<RecentView> recentViews = Arrays.asList(testRecentView);
        Page<RecentView> recentViewsPage = new PageImpl<>(recentViews);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recentViewRepository.findByUserOrderByViewedAtDesc(eq(testUser), any(Pageable.class)))
            .thenReturn(recentViewsPage);

        // When
        RecentPlacesResponse response = activityService.getRecentPlaces();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRecentPlaces()).hasSize(1);
        assertThat(response.getRecentPlaces().get(0).getTitle()).isEqualTo("테스트 카페");
        assertThat(response.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("최근 조회한 장소 목록 조회 - 빈 목록")
    void getRecentPlaces_EmptyList() {
        // Given
        Page<RecentView> emptyPage = new PageImpl<>(Collections.emptyList());

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recentViewRepository.findByUserOrderByViewedAtDesc(eq(testUser), any(Pageable.class)))
            .thenReturn(emptyPage);

        // When
        RecentPlacesResponse response = activityService.getRecentPlaces();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getRecentPlaces()).isEmpty();
        assertThat(response.getTotalCount()).isEqualTo(0);
    }

    @Test
    @DisplayName("내가 등록한 장소 목록 조회 성공")
    void getMyPlaces_Success() {
        // Given
        List<Bookmark> bookmarks = Arrays.asList(testBookmark);

        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(bookmarkRepository.findByUserOrderByCreatedAtDesc(testUser)).thenReturn(bookmarks);

        // When
        MyPlacesResponse response = activityService.getMyPlaces();

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getPlaces()).hasSize(1);
        assertThat(response.getPlaces().get(0).getName()).isEqualTo("테스트 카페");
        assertThat(response.getPlaces().get(0).getIsBookmarked()).isTrue();
        assertThat(response.getTotalCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("최근 장소 조회 기록 - 새로운 장소")
    void recordRecentPlaceView_NewPlace() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(recentViewRepository.findByUserIdAndPlaceId(1L, 100L)).thenReturn(Optional.empty());
        when(recentViewRepository.save(any(RecentView.class))).thenReturn(testRecentView);

        // When
        activityService.recordRecentPlaceView("100");

        // Then
        verify(recentViewRepository).save(any(RecentView.class));
        verify(recentViewRepository).deleteOldViewsByUser(eq(1L), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("최근 장소 조회 기록 - 기존 장소 업데이트")
    void recordRecentPlaceView_ExistingPlace() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(100L)).thenReturn(Optional.of(testPlace));
        when(recentViewRepository.findByUserIdAndPlaceId(1L, 100L))
            .thenReturn(Optional.of(testRecentView));
        when(recentViewRepository.save(any(RecentView.class))).thenReturn(testRecentView);

        // When
        activityService.recordRecentPlaceView("100");

        // Then
        verify(recentViewRepository).save(testRecentView);
        verify(recentViewRepository).deleteOldViewsByUser(eq(1L), any(OffsetDateTime.class));
    }

    @Test
    @DisplayName("최근 장소 조회 기록 실패 - 유효하지 않은 장소 ID")
    void recordRecentPlaceView_InvalidPlaceId() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // When & Then
        assertThatThrownBy(() -> activityService.recordRecentPlaceView("invalid"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("유효하지 않은 장소 ID");
    }

    @Test
    @DisplayName("최근 장소 조회 기록 실패 - 장소를 찾을 수 없음")
    void recordRecentPlaceView_PlaceNotFound() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(placeRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityService.recordRecentPlaceView("999"))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("장소를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("사용자 조회 실패")
    void getCurrentUser_NotFound() {
        // Given
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> activityService.getRecentPlaces())
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("사용자를 찾을 수 없습니다");
    }
}
