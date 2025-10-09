package com.mohe.spring.entity;

import com.vladmihalcea.hibernate.type.array.ListArrayType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "places")
@Getter
@Setter
public class Place {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "road_address", columnDefinition = "TEXT")
    private String roadAddress;

    @Column(name = "website_url", columnDefinition = "TEXT")
    private String websiteUrl;

    private BigDecimal rating;

    private Integer reviewCount;

    @Type(ListArrayType.class)
    @Column(name = "category", columnDefinition = "varchar[]")
    private List<String> category;

    @Type(ListArrayType.class)
    @Column(name = "keyword", columnDefinition = "varchar[]")
    private List<String> keyword;

    @Column(name = "keyword_vector", columnDefinition = "TEXT")
    private String keywordVector;

    private Boolean parkingAvailable;

    private Boolean petFriendly;

    private Boolean ready;

    @Column(name = "crawler_found")
    private Boolean crawlerFound;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceDescription> descriptions = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceBusinessHour> businessHours = new ArrayList<>();

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PlaceSns> sns = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
