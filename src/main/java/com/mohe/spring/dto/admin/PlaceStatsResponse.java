package com.mohe.spring.dto.admin;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PlaceStatsResponse {
    private long totalCount;
    private long embeddedCount;   // embed_status = COMPLETED
    private long crawledCount;    // crawl_status = COMPLETED
    private long pendingCount;    // crawl_status = PENDING
    private long failedCount;     // crawl_status = FAILED
    private long notFoundCount;   // crawl_status = NOT_FOUND
}
