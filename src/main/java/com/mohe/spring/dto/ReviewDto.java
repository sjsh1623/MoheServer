package com.mohe.spring.dto;

import java.time.LocalDateTime;

/**
 * DTO for place reviews
 */
public class ReviewDto {
    private Long id;
    private String reviewText;
    private String authorName;
    private LocalDateTime createdAt;

    public ReviewDto() {}

    public ReviewDto(Long id, String reviewText, String authorName, LocalDateTime createdAt) {
        this.id = id;
        this.reviewText = reviewText;
        this.authorName = authorName;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReviewText() { return reviewText; }
    public void setReviewText(String reviewText) { this.reviewText = reviewText; }

    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
