package com.mohe.spring.entity;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * 사용자가 장소에 작성하는 댓글 엔티티
 */
@Entity
@Table(name = "comments", indexes = {
    @Index(name = "idx_comments_place_id", columnList = "place_id"),
    @Index(name = "idx_comments_user_id", columnList = "user_id"),
    @Index(name = "idx_comments_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    /**
     * 댓글과 함께 제공하는 평점 (1.0 ~ 5.0)
     * null 가능 (평점 없이 댓글만 작성 가능)
     */
    @Column(name = "rating")
    private Double rating;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    /**
     * 댓글 작성자 확인
     */
    public boolean isAuthor(Long userId) {
        return this.user != null && this.user.getId().equals(userId);
    }
}
