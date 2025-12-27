package com.mohe.spring.dto;

import java.util.List;

/**
 * 장소 메뉴 목록 응답 DTO
 */
public class PlaceMenuListResponse {
    private List<PlaceMenuDto> menus;
    private int totalCount;

    public PlaceMenuListResponse() {}

    public PlaceMenuListResponse(List<PlaceMenuDto> menus) {
        this.menus = menus;
        this.totalCount = menus != null ? menus.size() : 0;
    }

    public List<PlaceMenuDto> getMenus() {
        return menus;
    }

    public void setMenus(List<PlaceMenuDto> menus) {
        this.menus = menus;
        this.totalCount = menus != null ? menus.size() : 0;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
    }
}
