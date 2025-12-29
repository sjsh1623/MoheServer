package com.mohe.spring.dto.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class BatchStatsResponse {
    private String serverName;
    private long pendingCount;
    private long priorityCount;
    private long processingCount;
    private long completedCount;
    private long failedCount;
    private int activeWorkers;
}
