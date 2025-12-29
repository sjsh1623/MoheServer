package com.mohe.spring.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class AdminPlaceDto {
    private Long id;
    private String name;
    private String roadAddress;
    private List<String> category;
    private BigDecimal rating;
    private Integer reviewCount;
    private String crawlStatus;   // PENDING, COMPLETED, FAILED, NOT_FOUND
    private String embedStatus;   // PENDING, COMPLETED, FAILED
    private int imageCount;
    private int menuCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
