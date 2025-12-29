package com.mohe.spring.dto.admin;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
public class WorkerStatusResponse {
    private Map<String, WorkerInfo> workers;

    @Getter
    @Setter
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkerInfo {
        private String workerId;
        private int threads;
        private long processedCount;
        private long failedCount;
        private String lastHeartbeat;
        private String status;
        private String currentTask;
    }
}
