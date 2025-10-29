package com.mohe.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 추천 카테고리 응답 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SuggestedCategoriesResponse {
    /** 현재 시간대 (예: "아침", "저녁") */
    private String timeSlot;

    /** 현재 날씨 상태 (예: "맑음", "비") */
    private String weather;

    /** 추천 이유 */
    private String reason;

    /** 추천 카테고리 목록 (5개) */
    private List<CategoryDto> suggestedCategories;

    /** 위치 정보 */
    private LocationInfo location;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private Double latitude;
        private Double longitude;
    }
}
