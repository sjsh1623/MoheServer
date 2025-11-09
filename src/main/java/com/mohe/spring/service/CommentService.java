package com.mohe.spring.service;

import com.mohe.spring.dto.*;
import com.mohe.spring.entity.Comment;
import com.mohe.spring.entity.Place;
import com.mohe.spring.entity.User;
import com.mohe.spring.repository.CommentRepository;
import com.mohe.spring.repository.PlaceRepository;
import com.mohe.spring.repository.UserRepository;
import com.mohe.spring.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 댓글 서비스
 */
@Service
@Transactional
public class CommentService {

    private final CommentRepository commentRepository;
    private final PlaceRepository placeRepository;
    private final UserRepository userRepository;

    public CommentService(CommentRepository commentRepository,
                         PlaceRepository placeRepository,
                         UserRepository userRepository) {
        this.commentRepository = commentRepository;
        this.placeRepository = placeRepository;
        this.userRepository = userRepository;
    }

    /**
     * 댓글 작성
     */
    public CommentResponse createComment(Long placeId, CommentCreateRequest request) {
        User currentUser = getCurrentUser();

        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new RuntimeException("장소를 찾을 수 없습니다 (ID: " + placeId + ")"));

        Comment comment = Comment.builder()
            .user(currentUser)
            .place(place)
            .content(request.getContent())
            .rating(request.getRating())
            .build();

        Comment savedComment = commentRepository.save(comment);

        return convertToResponse(savedComment, currentUser.getId());
    }

    /**
     * 특정 장소의 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public CommentListResponse getCommentsByPlaceId(Long placeId, int page, int size) {
        // 장소 존재 여부 확인
        if (!placeRepository.existsById(placeId)) {
            throw new RuntimeException("장소를 찾을 수 없습니다 (ID: " + placeId + ")");
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentsPage = commentRepository.findByPlaceIdOrderByCreatedAtDesc(placeId, pageable);

        Long currentUserId = getCurrentUserIdOrNull();

        List<CommentResponse> comments = commentsPage.getContent().stream()
            .map(comment -> convertToResponse(comment, currentUserId))
            .collect(Collectors.toList());

        // 평균 평점 및 총 댓글 수
        Double averageRating = commentRepository.getAverageRatingByPlaceId(placeId);
        long totalComments = commentRepository.countByPlaceId(placeId);

        return new CommentListResponse(
            comments,
            commentsPage.getTotalPages(),
            commentsPage.getTotalElements(),
            page,
            size,
            averageRating,
            totalComments
        );
    }

    /**
     * 내가 작성한 댓글 목록 조회
     */
    @Transactional(readOnly = true)
    public CommentListResponse getMyComments(int page, int size) {
        User currentUser = getCurrentUser();

        Pageable pageable = PageRequest.of(page, size);
        Page<Comment> commentsPage = commentRepository.findByUserIdOrderByCreatedAtDesc(
            currentUser.getId(), pageable);

        List<CommentResponse> comments = commentsPage.getContent().stream()
            .map(comment -> convertToResponse(comment, currentUser.getId()))
            .collect(Collectors.toList());

        return new CommentListResponse(
            comments,
            commentsPage.getTotalPages(),
            commentsPage.getTotalElements(),
            page,
            size,
            null, // 내 댓글 목록에서는 평균 평점 불필요
            commentsPage.getTotalElements()
        );
    }

    /**
     * 댓글 수정
     */
    public CommentResponse updateComment(Long commentId, CommentUpdateRequest request) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findByIdWithUserAndPlace(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다 (ID: " + commentId + ")"));

        // 작성자 확인
        if (!comment.isAuthor(currentUser.getId())) {
            throw new RuntimeException("본인이 작성한 댓글만 수정할 수 있습니다");
        }

        comment.setContent(request.getContent());
        comment.setRating(request.getRating());

        Comment updatedComment = commentRepository.save(comment);

        return convertToResponse(updatedComment, currentUser.getId());
    }

    /**
     * 댓글 삭제
     */
    public void deleteComment(Long commentId) {
        User currentUser = getCurrentUser();

        Comment comment = commentRepository.findByIdWithUserAndPlace(commentId)
            .orElseThrow(() -> new RuntimeException("댓글을 찾을 수 없습니다 (ID: " + commentId + ")"));

        // 작성자 확인
        if (!comment.isAuthor(currentUser.getId())) {
            throw new RuntimeException("본인이 작성한 댓글만 삭제할 수 있습니다");
        }

        commentRepository.delete(comment);
    }

    /**
     * Comment -> CommentResponse 변환
     */
    private CommentResponse convertToResponse(Comment comment, Long currentUserId) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setPlaceId(comment.getPlace().getId());
        response.setPlaceName(comment.getPlace().getName());
        response.setUserId(comment.getUser().getId());
        response.setUserNickname(comment.getUser().getNickname());
        response.setUserProfileImage(comment.getUser().getProfileImageUrl());
        response.setContent(comment.getContent());
        response.setRating(comment.getRating());
        response.setCreatedAt(comment.getCreatedAt());
        response.setUpdatedAt(comment.getUpdatedAt());
        response.setAuthor(currentUserId != null && comment.isAuthor(currentUserId));
        return response;
    }

    /**
     * 현재 인증된 사용자 가져오기
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
            || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new RuntimeException("인증이 필요한 서비스입니다");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userRepository.findById(userPrincipal.getId())
            .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));
    }

    /**
     * 현재 사용자 ID 가져오기 (인증되지 않은 경우 null 반환)
     */
    private Long getCurrentUserIdOrNull() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
                UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
                return userPrincipal.getId();
            }
        } catch (Exception e) {
            // 인증 정보가 없거나 오류 발생 시 null 반환
        }
        return null;
    }
}
