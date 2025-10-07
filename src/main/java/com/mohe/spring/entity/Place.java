package com.mohe.spring.entity;

import com.fasterxml.jackson.databind.JsonNode;
import com.vladmihalcea.hibernate.type.json.JsonType;
import jakarta.persistence.*;
import org.hibernate.annotations.Type;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "road_address")
    private String roadAddress;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    private String category;
    private String description;

    @Column(precision = 3, scale = 2)
    private BigDecimal rating = BigDecimal.ZERO;

    @Column(name = "review_count")
    private Integer reviewCount = 0;

    private Integer popularity = 0;

    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();

    // Essential enhanced fields
    @Column(name = "naver_place_id", unique = true)
    private String naverPlaceId;

    private String phone;

    @Column(name = "website_url")
    private String websiteUrl;

    @Type(JsonType.class)
    @Column(name = "opening_hours", columnDefinition = "jsonb")
    private JsonNode openingHours;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "keyword_vector", columnDefinition = "text")
    private String keywordVector; // Stored as JSON array string format

    // New fields for enhanced batch processing
    @Column(name = "description_vector", columnDefinition = "text")
    private String descriptionVector; // Ollama embedding vector as JSON string

    @Column(name = "keywords", columnDefinition = "text")
    private String keywords; // OpenAI extracted keywords as comma-separated string

    @Column(name = "search_query")
    private String searchQuery; // The query used to find this place (location + category)

    // Relationships
    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Bookmark> bookmarks = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Prompt> prompts = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RecentView> recentViews = new ArrayList<>();

    // Default constructor for JPA
    public Place() {}

    // Constructor with required fields
    public Place(String name) {
        this.name = name;
    }

    // Helper methods
    public boolean shouldBeRecommended() {
        // Recommend if rating >= 3.0
        return rating != null && rating.doubleValue() >= 3.0;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRoadAddress() {
        return roadAddress;
    }

    public void setRoadAddress(String roadAddress) {
        this.roadAddress = roadAddress;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getRating() {
        return rating;
    }

    public void setRating(BigDecimal rating) {
        this.rating = rating;
    }

    public Integer getReviewCount() {
        return reviewCount;
    }

    public void setReviewCount(Integer reviewCount) {
        this.reviewCount = reviewCount;
    }

    public Integer getPopularity() {
        return popularity;
    }

    public void setPopularity(Integer popularity) {
        this.popularity = popularity;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getNaverPlaceId() {
        return naverPlaceId;
    }

    public void setNaverPlaceId(String naverPlaceId) {
        this.naverPlaceId = naverPlaceId;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public JsonNode getOpeningHours() {
        return openingHours;
    }

    public void setOpeningHours(JsonNode openingHours) {
        this.openingHours = openingHours;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getKeywordVector() {
        return keywordVector;
    }

    public void setKeywordVector(String keywordVector) {
        this.keywordVector = keywordVector;
    }

    public String getDescriptionVector() {
        return descriptionVector;
    }

    public void setDescriptionVector(String descriptionVector) {
        this.descriptionVector = descriptionVector;
    }

    public String getKeywords() {
        return keywords;
    }

    public void setKeywords(String keywords) {
        this.keywords = keywords;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public void setSearchQuery(String searchQuery) {
        this.searchQuery = searchQuery;
    }

    public List<Bookmark> getBookmarks() {
        return bookmarks;
    }

    public void setBookmarks(List<Bookmark> bookmarks) {
        this.bookmarks = bookmarks;
    }

    public List<Prompt> getPrompts() {
        return prompts;
    }

    public void setPrompts(List<Prompt> prompts) {
        this.prompts = prompts;
    }

    public List<RecentView> getRecentViews() {
        return recentViews;
    }

    public void setRecentViews(List<RecentView> recentViews) {
        this.recentViews = recentViews;
    }
}
