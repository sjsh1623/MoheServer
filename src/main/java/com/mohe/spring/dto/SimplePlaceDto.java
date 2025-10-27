package com.mohe.spring.dto;

import java.util.List;

/**
 * Simple PlaceDto matching frontend expectations
 */
public class SimplePlaceDto {
    private String id;
    private String name;
    private String category;
    private Double rating;
    private Integer reviewCount;
    private String location;
    private String address;
    private String shortAddress; // 구 + 동 (e.g., "강남구 역삼동")
    private String fullAddress;  // 전체 도로명 주소
    private String imageUrl;
    private List<String> images;
    private Double distance;
    private Boolean isBookmarked;
    private String description;
    private String phone;
    private String websiteUrl;
    private String operatingHours;
    
    // Transportation info
    private String carTime;
    private String busTime;
    
    // Additional fields for recommendations
    private Double similarityScore;
    private String explanation;
    private String recommendationReason;
    
    // Debug field
    private Boolean isDemo;

    public SimplePlaceDto() {}

    public SimplePlaceDto(String id, String name, String category, Double rating,
                          String location, String imageUrl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.location = location;
        this.imageUrl = imageUrl;
        this.isBookmarked = false;
        this.isDemo = false;
    }

    // Full constructor
    public SimplePlaceDto(String id, String name, String category, Double rating,
                          Integer reviewCount, String location, String address, String shortAddress,
                          String fullAddress, String imageUrl, List<String> images, Double distance,
                          Boolean isBookmarked, String description, String phone, String websiteUrl,
                          String operatingHours, String carTime, String busTime, Double similarityScore,
                          String explanation, String recommendationReason, Boolean isDemo) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.rating = rating;
        this.reviewCount = reviewCount;
        this.location = location;
        this.address = address;
        this.shortAddress = shortAddress;
        this.fullAddress = fullAddress;
        this.imageUrl = imageUrl;
        this.images = images;
        this.distance = distance;
        this.isBookmarked = isBookmarked;
        this.description = description;
        this.phone = phone;
        this.websiteUrl = websiteUrl;
        this.operatingHours = operatingHours;
        this.carTime = carTime;
        this.busTime = busTime;
        this.similarityScore = similarityScore;
        this.explanation = explanation;
        this.recommendationReason = recommendationReason;
        this.isDemo = isDemo;
    }

    // Getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Double getRating() { return rating; }
    public void setRating(Double rating) { this.rating = rating; }

    public Integer getReviewCount() { return reviewCount; }
    public void setReviewCount(Integer reviewCount) { this.reviewCount = reviewCount; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getShortAddress() { return shortAddress; }
    public void setShortAddress(String shortAddress) { this.shortAddress = shortAddress; }

    public String getFullAddress() { return fullAddress; }
    public void setFullAddress(String fullAddress) { this.fullAddress = fullAddress; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }

    public Boolean getIsBookmarked() { return isBookmarked; }
    public void setIsBookmarked(Boolean isBookmarked) { this.isBookmarked = isBookmarked; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsiteUrl() { return websiteUrl; }
    public void setWebsiteUrl(String websiteUrl) { this.websiteUrl = websiteUrl; }

    public String getOperatingHours() { return operatingHours; }
    public void setOperatingHours(String operatingHours) { this.operatingHours = operatingHours; }

    public String getCarTime() { return carTime; }
    public void setCarTime(String carTime) { this.carTime = carTime; }

    public String getBusTime() { return busTime; }
    public void setBusTime(String busTime) { this.busTime = busTime; }

    public Double getSimilarityScore() { return similarityScore; }
    public void setSimilarityScore(Double similarityScore) { this.similarityScore = similarityScore; }

    public String getExplanation() { return explanation; }
    public void setExplanation(String explanation) { this.explanation = explanation; }

    public String getRecommendationReason() { return recommendationReason; }
    public void setRecommendationReason(String recommendationReason) { this.recommendationReason = recommendationReason; }

    public Boolean getIsDemo() { return isDemo; }
    public void setIsDemo(Boolean isDemo) { this.isDemo = isDemo; }
}