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

@RestController
@RequestMapping("/api/batch/jobs")
@Tag(name = "Batch Jobs", description = "Spring Batch Job 실행 API")
public class BatchJobController {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobController.class);

    private final JobLauncher jobLauncher;
    private final Job placeCollectionJob;
    private final Job updateCrawledDataJob;

    public BatchJobController(JobLauncher jobLauncher, Job placeCollectionJob, Job updateCrawledDataJob) {
        this.jobLauncher = jobLauncher;
        this.placeCollectionJob = placeCollectionJob;
        this.updateCrawledDataJob = updateCrawledDataJob;
    }

    @PostMapping("/place-collection")
    @Operation(
        summary = "장소 수집 배치 실행 (전체 지역)",
        description = "Naver API를 통해 모든 지역의 장소 데이터를 수집하여 DB에 저장합니다"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runPlaceCollectionJob() {
        return runPlaceCollectionJobWithRegion(null);
    }

    @PostMapping("/place-collection/{region}")
    @Operation(
        summary = "장소 수집 배치 실행 (특정 지역)",
        description = "Naver API를 통해 특정 지역의 장소 데이터를 수집하여 DB에 저장합니다. " +
                      "지역: seoul (서울), jeju (제주), yongin (용인)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runPlaceCollectionJobWithRegion(
            @PathVariable(required = false) String region) {
        try {
            String regionName = region != null ? region : "ALL";
            logger.info("🚀 Starting Place Collection Batch Job (Region: {})", regionName);

            JobParametersBuilder builder = new JobParametersBuilder()
                    .addLong("startTime", System.currentTimeMillis());

            if (region != null && !region.trim().isEmpty()) {
                builder.addString("region", region.toLowerCase());
            }

            JobParameters jobParameters = builder.toJobParameters();
            jobLauncher.run(placeCollectionJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "Place Collection Job started successfully");
            result.put("region", regionName);
            result.put("startTime", System.currentTimeMillis());

            logger.info("✅ Place Collection Batch Job started for region: {}", regionName);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to start Place Collection Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to start batch job: " + e.getMessage());
            error.put("region", region != null ? region : "ALL");

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success(error));
        }
    }

    @PostMapping("/update-crawled-data")
    @Operation(
        summary = "크롤링 데이터 업데이트 배치 실행",
        description = "Crawling 서버와 연동하여 장소 데이터를 업데이트하고 DB에 저장합니다"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runUpdateCrawledDataJob() {
        try {
            logger.info("🕷️ Starting Update Crawled Data Batch Job");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startTime", System.currentTimeMillis())
                    .toJobParameters();

            jobLauncher.run(updateCrawledDataJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("message", "Update Crawled Data Job started successfully");
            result.put("startTime", System.currentTimeMillis());

            logger.info("✅ Update Crawled Data Job started");
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to start Update Crawled Data Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to start batch job: " + e.getMessage());

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.success(error));
        }
    }
}
