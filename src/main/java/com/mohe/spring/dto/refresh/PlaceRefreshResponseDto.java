package com.mohe.spring.dto.refresh;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 장소 데이터 새로고침 응답 DTO
 */
@Getter
@Builder
public class PlaceRefreshResponseDto {
    private Long placeId;
    private String placeName;
    private int imageCount;
    private int newReviewCount;
    private int totalReviewCount;
    private int menuCount;
    private int menuWithImageCount;
    private List<String> imageUrls;
    private List<String> newReviews;
    private List<MenuDto> menus;
    private String message;

    /**
     * 메뉴 정보 DTO
     */
    @Getter
    @Builder
    public static class MenuDto {
        private String name;
        private String price;
        private String description;
        private String imageUrl;
        private String imagePath;
        private boolean isPopular;
    }
}
