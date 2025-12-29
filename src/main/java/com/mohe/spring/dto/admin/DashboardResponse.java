package com.mohe.spring.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Builder
public class DashboardResponse {
    private PlaceStatsResponse placeStats;
    private BatchStatsResponse batchStats;
    private Map<String, WorkerStatusResponse.WorkerInfo> workers;
    private LocalDateTime timestamp;
}
