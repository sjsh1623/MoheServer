package com.mohe.spring.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceSearchRequest {
    private String keyword;
    private String status; // all, ready, crawled, pending, failed
    private int page = 0;
    private int size = 20;
}
