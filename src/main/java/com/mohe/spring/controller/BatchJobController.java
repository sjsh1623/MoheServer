package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Spring Batch Job 실행 컨트롤러
 *
 * Batch Job을 수동으로 트리거하는 API 제공
 */
@RestController
@RequestMapping("/api/batch/jobs")
@Tag(name = "Batch Jobs", description = "Spring Batch Job 실행 API")
public class BatchJobController {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobController.class);

    private final JobLauncher jobLauncher;
    private final Job placeCollectionJob;

    public BatchJobController(JobLauncher jobLauncher, Job placeCollectionJob) {
        this.jobLauncher = jobLauncher;
        this.placeCollectionJob = placeCollectionJob;
    }

    /**
     * Place 수집 Batch Job 실행
     */
    @PostMapping("/place-collection")
    @Operation(
        summary = "장소 수집 배치 실행",
        description = "Naver API를 통해 장소 데이터를 수집하여 DB에 저장합니다"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runPlaceCollectionJob() {
        try {
            logger.info("🚀 Starting Place Collection Batch Job");

            // Job Parameters (실행 시각을 파라미터로 추가하여 재실행 가능하게 함)
            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startTime", System.currentTimeMillis())
                    .toJobParameters();

            // Job 실행
            jobLauncher.run(placeCollectionJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "Place Collection Job started successfully");
            result.put("startTime", System.currentTimeMillis());

            logger.info("✅ Place Collection Batch Job started");
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to start Place Collection Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to start batch job: " + e.getMessage());

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success(error));
        }
    }
}
