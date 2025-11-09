package com.mohe.spring.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.DecimalMax;

/**
 * 댓글 수정 요청 DTO
 */
public class CommentUpdateRequest {

    @NotBlank(message = "댓글 내용은 필수입니다")
    @Size(min = 1, max = 1000, message = "댓글은 1-1000자 사이여야 합니다")
    private String content;

    @DecimalMin(value = "1.0", message = "평점은 1.0 이상이어야 합니다")
    @DecimalMax(value = "5.0", message = "평점은 5.0 이하여야 합니다")
    private Double rating;

    public CommentUpdateRequest() {}

    public CommentUpdateRequest(String content, Double rating) {
        this.content = content;
        this.rating = rating;
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
}
