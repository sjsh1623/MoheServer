package com.mohe.spring.repository;

import com.mohe.spring.entity.Comment;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@DisplayName("댓글 레포지토리 테스트")
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private TestEntityManager entityManager;

    private User testUser;
    private Place testPlace;
    private Comment testComment;

    @BeforeEach
    void setUp() {
        // User 생성
        testUser = new User();
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashedpassword");
        testUser.setNickname("테스터");
        testUser = entityManager.persist(testUser);

        // Place 생성
        testPlace = new Place();
        testPlace.setName("테스트 카페");
        testPlace.setRoadAddress("서울시 강남구");
        testPlace = entityManager.persist(testPlace);

        // Comment 생성
        testComment = Comment.builder()
            .user(testUser)
            .place(testPlace)
            .content("좋은 장소입니다!")
            .rating(4.5)
            .build();
        testComment = commentRepository.save(testComment);

        entityManager.flush();
    }

    @Test
    @DisplayName("장소별 댓글 조회 - 최신순 정렬")
    void findByPlaceIdOrderByCreatedAtDesc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Comment> comments = commentRepository.findByPlaceIdOrderByCreatedAtDesc(
            testPlace.getId(), pageable);

        // Then
        assertThat(comments).isNotEmpty();
        assertThat(comments.getContent()).hasSize(1);
        assertThat(comments.getContent().get(0).getContent()).isEqualTo("좋은 장소입니다!");
    }

    @Test
    @DisplayName("사용자별 댓글 조회 - 최신순 정렬")
    void findByUserIdOrderByCreatedAtDesc() {
        // Given
        Pageable pageable = PageRequest.of(0, 10);

        // When
        Page<Comment> comments = commentRepository.findByUserIdOrderByCreatedAtDesc(
            testUser.getId(), pageable);

        // Then
        assertThat(comments).isNotEmpty();
        assertThat(comments.getContent()).hasSize(1);
        assertThat(comments.getContent().get(0).getUser().getEmail()).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("댓글 ID로 조회 - User와 Place Eager Loading")
    void findByIdWithUserAndPlace() {
        // When
        Optional<Comment> comment = commentRepository.findByIdWithUserAndPlace(testComment.getId());

        // Then
        assertThat(comment).isPresent();
        assertThat(comment.get().getUser()).isNotNull();
        assertThat(comment.get().getPlace()).isNotNull();
        assertThat(comment.get().getUser().getNickname()).isEqualTo("테스터");
        assertThat(comment.get().getPlace().getName()).isEqualTo("테스트 카페");
    }

    @Test
    @DisplayName("장소별 댓글 개수 조회")
    void countByPlaceId() {
        // When
        long count = commentRepository.countByPlaceId(testPlace.getId());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("사용자별 댓글 개수 조회")
    void countByUserId() {
        // When
        long count = commentRepository.countByUserId(testUser.getId());

        // Then
        assertThat(count).isEqualTo(1);
    }

    @Test
    @DisplayName("장소별 평균 평점 조회")
    void getAverageRatingByPlaceId() {
        // Given - 추가 댓글 생성
        Comment comment2 = Comment.builder()
            .user(testUser)
            .place(testPlace)
            .content("또 다른 댓글")
            .rating(3.5)
            .build();
        commentRepository.save(comment2);
        entityManager.flush();

        // When
        Double avgRating = commentRepository.getAverageRatingByPlaceId(testPlace.getId());

        // Then
        assertThat(avgRating).isNotNull();
        assertThat(avgRating).isEqualTo(4.0); // (4.5 + 3.5) / 2
    }

    @Test
    @DisplayName("평점 없는 댓글만 있을 때 평균 평점 조회")
    void getAverageRatingByPlaceId_NoRatings() {
        // Given
        Comment commentWithoutRating = Comment.builder()
            .user(testUser)
            .place(testPlace)
            .content("평점 없는 댓글")
            .rating(null)
            .build();
        commentRepository.save(commentWithoutRating);
        entityManager.flush();

        // When
        Double avgRating = commentRepository.getAverageRatingByPlaceId(testPlace.getId());

        // Then
        assertThat(avgRating).isNotNull(); // testComment의 4.5가 있음
    }

    @Test
    @DisplayName("여러 댓글 페이징 조회")
    void findByPlaceIdOrderByCreatedAtDesc_Pagination() {
        // Given - 10개 댓글 추가 생성
        for (int i = 0; i < 10; i++) {
            Comment comment = Comment.builder()
                .user(testUser)
                .place(testPlace)
                .content("댓글 " + i)
                .rating(4.0)
                .build();
            commentRepository.save(comment);
        }
        entityManager.flush();

        // When - 페이지 크기 5로 첫 페이지 조회
        Pageable pageable = PageRequest.of(0, 5);
        Page<Comment> comments = commentRepository.findByPlaceIdOrderByCreatedAtDesc(
            testPlace.getId(), pageable);

        // Then
        assertThat(comments.getContent()).hasSize(5);
        assertThat(comments.getTotalElements()).isEqualTo(11); // 초기 1개 + 10개
        assertThat(comments.getTotalPages()).isEqualTo(3); // 11개를 5개씩
    }

    @Test
    @DisplayName("댓글 삭제 후 개수 확인")
    void deleteAndCount() {
        // Given
        long initialCount = commentRepository.countByPlaceId(testPlace.getId());

        // When
        commentRepository.delete(testComment);
        entityManager.flush();

        // Then
        long afterCount = commentRepository.countByPlaceId(testPlace.getId());
        assertThat(afterCount).isEqualTo(initialCount - 1);
    }
}
