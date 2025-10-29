package com.mohe.spring.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 카테고리 정보 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryDto {
    /** 카테고리 키 (예: "cafe", "restaurant") */
    private String key;

    /** 카테고리 표시명 (예: "카페", "맛집") */
    private String displayName;

    /** 카테고리 설명 */
    private String description;

    /** 카테고리 이모지 */
    private String emoji;
}
