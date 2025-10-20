package com.mohe.spring.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "contact_messages")
public class ContactMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String message;

    @Column(name = "category")
    private String category; // "bug", "feature", "inquiry", "other"

    @Column(name = "status")
    private String status = "pending"; // "pending", "in_progress", "resolved"

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Default constructor for JPA
    public ContactMessage() {}

    // Constructor with required fields
    public ContactMessage(User user, String message) {
        this.user = user;
        this.message = message;
    }

    // Full constructor
    public ContactMessage(Long id, User user, String message, String category, String status, OffsetDateTime createdAt) {
        this.id = id;
        this.user = user;
        this.message = message;
        this.category = category;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
