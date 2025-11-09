package com.mohe.spring.repository;

import com.mohe.spring.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 댓글 Repository
 */
@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 특정 장소의 댓글 목록 조회 (페이징, 최신순)
     */
    @Query("SELECT c FROM Comment c WHERE c.place.id = :placeId ORDER BY c.createdAt DESC")
    Page<Comment> findByPlaceIdOrderByCreatedAtDesc(@Param("placeId") Long placeId, Pageable pageable);

    /**
     * 특정 사용자가 작성한 댓글 목록 조회 (페이징, 최신순)
     */
    @Query("SELECT c FROM Comment c WHERE c.user.id = :userId ORDER BY c.createdAt DESC")
    Page<Comment> findByUserIdOrderByCreatedAtDesc(@Param("userId") Long userId, Pageable pageable);

    /**
     * 댓글 ID로 조회 (User, Place Eager Loading)
     */
    @Query("SELECT c FROM Comment c " +
           "LEFT JOIN FETCH c.user " +
           "LEFT JOIN FETCH c.place " +
           "WHERE c.id = :commentId")
    Optional<Comment> findByIdWithUserAndPlace(@Param("commentId") Long commentId);

    /**
     * 특정 장소의 댓글 개수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.place.id = :placeId")
    long countByPlaceId(@Param("placeId") Long placeId);

    /**
     * 특정 사용자의 댓글 개수
     */
    @Query("SELECT COUNT(c) FROM Comment c WHERE c.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    /**
     * 특정 장소의 평균 평점
     */
    @Query("SELECT AVG(c.rating) FROM Comment c WHERE c.place.id = :placeId AND c.rating IS NOT NULL")
    Double getAverageRatingByPlaceId(@Param("placeId") Long placeId);
}
