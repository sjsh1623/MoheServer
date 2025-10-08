package com.mohe.spring.dto.crawling;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CrawlingRequest {
    private String searchQuery;
    private String placeName;
}
