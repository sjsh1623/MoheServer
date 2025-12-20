package com.mohe.spring.dto.refresh;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/**
 * 배치 새로고침 응답 DTO
 *
 * <p>전체 Places 배치 새로고침 결과를 담습니다.</p>
 */
@Getter
@Builder
public class BatchRefreshResponseDto {
    private int totalPlaces;
    private int successCount;
    private int failedCount;
    private List<PlaceRefreshSummary> results;
    private String message;
    private long elapsedTimeMs;

    /**
     * 개별 장소 새로고침 요약
     */
    @Getter
    @Builder
    public static class PlaceRefreshSummary {
        private Long placeId;
        private String placeName;
        private boolean success;
        private int imageCount;
        private int reviewCount;
        private int menuCount;
        private String errorMessage;
    }
}
