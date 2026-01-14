package com.mohe.spring.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "social_accounts", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"provider", "provider_id"})
})
public class SocialAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String provider; // "kakao", "google"

    @Column(name = "provider_id", nullable = false)
    private String providerId; // User's ID from the provider

    @Column(name = "provider_email")
    private String providerEmail;

    @Column(name = "provider_name")
    private String providerName;

    @Column(name = "access_token", length = 2048)
    private String accessToken;

    @Column(name = "refresh_token", length = 2048)
    private String refreshToken;

    @Column(name = "token_expires_at")
    private OffsetDateTime tokenExpiresAt;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt = OffsetDateTime.now();

    // Constructors
    public SocialAccount() {}

    public SocialAccount(User user, String provider, String providerId, String providerEmail, String providerName) {
        this.user = user;
        this.provider = provider;
        this.providerId = providerId;
        this.providerEmail = providerEmail;
        this.providerName = providerName;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getProviderEmail() {
        return providerEmail;
    }

    public void setProviderEmail(String providerEmail) {
        this.providerEmail = providerEmail;
    }

    public String getProviderName() {
        return providerName;
    }

    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public OffsetDateTime getTokenExpiresAt() {
        return tokenExpiresAt;
    }

    public void setTokenExpiresAt(OffsetDateTime tokenExpiresAt) {
        this.tokenExpiresAt = tokenExpiresAt;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
