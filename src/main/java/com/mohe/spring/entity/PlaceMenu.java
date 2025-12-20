package com.mohe.spring.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 장소 메뉴 엔티티
 *
 * <p>장소(Place)에 속한 메뉴 정보를 저장합니다.</p>
 *
 * <h3>저장 정보</h3>
 * <ul>
 *   <li>메뉴명 (name)</li>
 *   <li>가격 (price)</li>
 *   <li>설명 (description)</li>
 *   <li>원본 이미지 URL (imageUrl)</li>
 *   <li>저장된 이미지 경로 (imagePath)</li>
 *   <li>인기 메뉴 여부 (isPopular)</li>
 *   <li>표시 순서 (displayOrder)</li>
 * </ul>
 */
@Entity
@Table(name = "place_menus")
@Getter
@Setter
@NoArgsConstructor
public class PlaceMenu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    /**
     * 메뉴명
     */
    @Column(name = "name", nullable = false, length = 255)
    private String name;

    /**
     * 가격 (예: "10,000원", "시가")
     */
    @Column(name = "price", length = 50)
    private String price;

    /**
     * 메뉴 설명
     */
    @Column(name = "description", length = 1000)
    private String description;

    /**
     * 원본 이미지 URL (크롤링한 URL)
     */
    @Column(name = "image_url", length = 2048)
    private String imageUrl;

    /**
     * 저장된 이미지 경로 (예: /images/menu/123_menuName_uuid.jpg)
     */
    @Column(name = "image_path", length = 2048)
    private String imagePath;

    /**
     * 인기 메뉴 여부
     */
    @Column(name = "is_popular")
    private Boolean isPopular = false;

    /**
     * 표시 순서
     */
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    @Column(name = "created_at")
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
     * 편의 생성자
     */
    public PlaceMenu(Place place, String name, String price) {
        this.place = place;
        this.name = name;
        this.price = price;
    }

    /**
     * 이미지가 있는지 확인
     */
    public boolean hasImage() {
        return imageUrl != null && !imageUrl.isEmpty();
    }

    /**
     * 이미지가 다운로드되었는지 확인
     */
    public boolean isImageDownloaded() {
        return imagePath != null && !imagePath.isEmpty();
    }
}
