package com.mohe.spring.dto;

import java.util.List;

/**
 * 댓글 목록 응답 DTO
 */
public class CommentListResponse {

    private List<CommentResponse> comments;
    private int totalPages;
    private long totalElements;
    private int currentPage;
    private int pageSize;
    private Double averageRating; // 해당 장소의 평균 평점
    private long totalComments; // 총 댓글 수

    public CommentListResponse() {}

    public CommentListResponse(List<CommentResponse> comments, int totalPages, long totalElements,
                              int currentPage, int pageSize, Double averageRating, long totalComments) {
        this.comments = comments;
        this.totalPages = totalPages;
        this.totalElements = totalElements;
        this.currentPage = currentPage;
        this.pageSize = pageSize;
        this.averageRating = averageRating;
        this.totalComments = totalComments;
    }

    // Getters and Setters
    public List<CommentResponse> getComments() {
        return comments;
    }

    public void setComments(List<CommentResponse> comments) {
        this.comments = comments;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public void setTotalElements(long totalElements) {
        this.totalElements = totalElements;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public void setCurrentPage(int currentPage) {
        this.currentPage = currentPage;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public Double getAverageRating() {
        return averageRating;
    }

    public void setAverageRating(Double averageRating) {
        this.averageRating = averageRating;
    }

    public long getTotalComments() {
        return totalComments;
    }

    public void setTotalComments(long totalComments) {
        this.totalComments = totalComments;
    }
}
