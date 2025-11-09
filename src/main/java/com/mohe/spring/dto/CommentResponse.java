package com.mohe.spring.dto;

import java.time.LocalDateTime;

/**
 * 댓글 응답 DTO
 */
public class CommentResponse {

    private Long id;
    private Long placeId;
    private String placeName;
    private Long userId;
    private String userNickname;
    private String userProfileImage;
    private String content;
    private Double rating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean isAuthor; // 요청자가 작성자인지 여부

    public CommentResponse() {}

    public CommentResponse(Long id, Long placeId, String placeName, Long userId, String userNickname,
                          String userProfileImage, String content, Double rating,
                          LocalDateTime createdAt, LocalDateTime updatedAt, boolean isAuthor) {
        this.id = id;
        this.placeId = placeId;
        this.placeName = placeName;
        this.userId = userId;
        this.userNickname = userNickname;
        this.userProfileImage = userProfileImage;
        this.content = content;
        this.rating = rating;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.isAuthor = isAuthor;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPlaceId() {
        return placeId;
    }

    public void setPlaceId(Long placeId) {
        this.placeId = placeId;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserNickname() {
        return userNickname;
    }

    public void setUserNickname(String userNickname) {
        this.userNickname = userNickname;
    }

    public String getUserProfileImage() {
        return userProfileImage;
    }

    public void setUserProfileImage(String userProfileImage) {
        this.userProfileImage = userProfileImage;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Double getRating() {
        return rating;
    }

    public void setRating(Double rating) {
        this.rating = rating;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isAuthor() {
        return isAuthor;
    }

    public void setAuthor(boolean author) {
        isAuthor = author;
    }
}
