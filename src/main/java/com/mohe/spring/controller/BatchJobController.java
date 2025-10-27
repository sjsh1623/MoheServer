package com.mohe.spring.controller;

import com.mohe.spring.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.launch.NoSuchJobExecutionException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/batch/jobs")
@Tag(name = "Batch Jobs", description = "Spring Batch Job 실행 API")
public class BatchJobController {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobController.class);

    private final JobLauncher asyncJobLauncher;
    private final JobOperator jobOperator;
    private final JobExplorer jobExplorer;
    private final Job placeCollectionJob;
    private final Job updateCrawledDataJob;
    private final Job vectorEmbeddingJob;
    private final Job imageUpdateJob;

    public BatchJobController(
            JobLauncher asyncJobLauncher,
            JobOperator jobOperator,
            JobExplorer jobExplorer,
            @org.springframework.beans.factory.annotation.Qualifier("placeCollectionJob") Job placeCollectionJob,
            @org.springframework.beans.factory.annotation.Qualifier("updateCrawledDataJob") Job updateCrawledDataJob,
            @org.springframework.beans.factory.annotation.Qualifier("vectorEmbeddingJob") Job vectorEmbeddingJob,
            @org.springframework.beans.factory.annotation.Qualifier("imageUpdateJob") Job imageUpdateJob) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.jobOperator = jobOperator;
        this.jobExplorer = jobExplorer;
        this.placeCollectionJob = placeCollectionJob;
        this.updateCrawledDataJob = updateCrawledDataJob;
        this.vectorEmbeddingJob = vectorEmbeddingJob;
        this.imageUpdateJob = imageUpdateJob;
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
        description = "Kakao API를 통해 특정 지역의 장소 데이터를 수집하여 DB에 저장합니다. " +
                      "배치 작업은 백그라운드에서 비동기로 실행됩니다. " +
                      "지역: seoul (서울), jeju (제주), yongin (용인)"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runPlaceCollectionJobWithRegion(
            @PathVariable(required = false) String region) {
        try {
            String regionName = region != null ? region : "ALL";
            long startTime = System.currentTimeMillis();

            logger.info("🚀 Triggering Place Collection Batch Job (Region: {})", regionName);

            JobParametersBuilder builder = new JobParametersBuilder()
                    .addLong("startTime", startTime);

            if (region != null && !region.trim().isEmpty()) {
                builder.addString("region", region.toLowerCase());
            }

            JobParameters jobParameters = builder.toJobParameters();

            // 비동기 실행 - 즉시 반환
            asyncJobLauncher.run(placeCollectionJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "STARTED");
            result.put("message", "Place Collection Batch Job has been triggered and is running in the background");
            result.put("region", regionName);
            result.put("startTime", startTime);

            logger.info("✅ Place Collection Batch Job triggered for region: {}", regionName);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to trigger Place Collection Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to trigger batch job: " + e.getMessage());
            error.put("region", region != null ? region : "ALL");

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", error.get("message").toString(), "/api/batch/jobs/place-collection"));
        }
    }

    @PostMapping("/update-crawled-data")
    @Operation(
        summary = "크롤링 데이터 업데이트 배치 실행",
        description = "Crawling 서버와 연동하여 장소 데이터를 업데이트하고 DB에 저장합니다. " +
                      "배치 작업은 백그라운드에서 비동기로 실행됩니다. " +
                      "이 작업은 description만 생성하고 crawler_found=true, ready=false로 설정합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runUpdateCrawledDataJob() {
        try {
            long startTime = System.currentTimeMillis();

            logger.info("🕷️ Triggering Update Crawled Data Batch Job");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startTime", startTime)
                    .toJobParameters();

            // 비동기 실행 - 즉시 반환
            asyncJobLauncher.run(updateCrawledDataJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "STARTED");
            result.put("message", "Update Crawled Data Job has been triggered and is running in the background");
            result.put("startTime", startTime);

            logger.info("✅ Update Crawled Data Job triggered");
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to trigger Update Crawled Data Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to trigger batch job: " + e.getMessage());

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", error.get("message").toString(), "/api/batch/jobs/update-crawled-data"));
        }
    }

    @PostMapping("/vector-embedding")
    @Operation(
        summary = "벡터 임베딩 배치 실행",
        description = "mohe_description을 기반으로 키워드 생성 및 벡터화를 수행합니다. " +
                      "배치 작업은 백그라운드에서 비동기로 실행됩니다. " +
                      "조건: crawler_found=true, ready=false, mohe_description IS NOT NULL"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runVectorEmbeddingJob() {
        try {
            long startTime = System.currentTimeMillis();

            logger.info("🧮 Triggering Vector Embedding Batch Job");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startTime", startTime)
                    .toJobParameters();

            // 비동기 실행 - 즉시 반환
            asyncJobLauncher.run(vectorEmbeddingJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "STARTED");
            result.put("message", "Vector Embedding Job has been triggered and is running in the background");
            result.put("startTime", startTime);

            logger.info("✅ Vector Embedding Job triggered");
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to trigger Vector Embedding Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to trigger batch job: " + e.getMessage());

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", error.get("message").toString(), "/api/batch/jobs/vector-embedding"));
        }
    }

    @GetMapping("/running")
    @Operation(
        summary = "실행 중인 배치 작업 조회",
        description = "현재 실행 중인 모든 배치 작업의 정보를 반환합니다"
    )
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRunningJobs() {
        try {
            Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(null);

            List<Map<String, Object>> runningJobs = runningExecutions.stream()
                .map(execution -> {
                    Map<String, Object> jobInfo = new HashMap<>();
                    jobInfo.put("jobName", execution.getJobInstance().getJobName());
                    jobInfo.put("executionId", execution.getId());
                    jobInfo.put("status", execution.getStatus().name());
                    jobInfo.put("startTime", execution.getStartTime());
                    jobInfo.put("createTime", execution.getCreateTime());

                    // Add step information
                    Collection<StepExecution> stepExecutions = execution.getStepExecutions();
                    if (!stepExecutions.isEmpty()) {
                        List<Map<String, Object>> steps = stepExecutions.stream()
                            .map(step -> {
                                Map<String, Object> stepInfo = new HashMap<>();
                                stepInfo.put("stepName", step.getStepName());
                                stepInfo.put("status", step.getStatus().name());
                                stepInfo.put("readCount", step.getReadCount());
                                stepInfo.put("writeCount", step.getWriteCount());
                                return stepInfo;
                            })
                            .collect(Collectors.toList());
                        jobInfo.put("steps", steps);
                    }

                    return jobInfo;
                })
                .collect(Collectors.toList());

            logger.info("📊 Found {} running batch jobs", runningJobs.size());
            return ResponseEntity.ok(ApiResponse.success(runningJobs));

        } catch (Exception e) {
            logger.error("❌ Failed to get running batch jobs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", "Failed to get running jobs: " + e.getMessage(), "/api/batch/jobs/running"));
        }
    }

    @PostMapping("/stop/{executionId}")
    @Operation(
        summary = "배치 작업 중지",
        description = "실행 중인 배치 작업을 중지합니다. executionId는 /running 엔드포인트에서 조회할 수 있습니다"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> stopJob(@PathVariable Long executionId) {
        try {
            logger.info("🛑 Attempting to stop batch job with executionId: {}", executionId);

            // Stop the job
            boolean stopped = jobOperator.stop(executionId);

            Map<String, Object> result = new HashMap<>();
            result.put("executionId", executionId);
            result.put("stopped", stopped);

            if (stopped) {
                result.put("status", "STOPPING");
                result.put("message", "Batch job stop request sent successfully. The job will stop after completing the current chunk.");
                logger.info("✅ Successfully sent stop request for executionId: {}", executionId);
            } else {
                result.put("status", "FAILED");
                result.put("message", "Failed to stop the batch job. It may have already completed or stopped.");
                logger.warn("⚠️ Failed to stop job with executionId: {}", executionId);
            }

            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (NoSuchJobExecutionException e) {
            logger.error("❌ Job execution not found: {}", executionId);
            Map<String, Object> error = new HashMap<>();
            error.put("executionId", executionId);
            error.put("message", "Job execution not found");
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("JOB_NOT_FOUND", "Job execution not found: " + executionId, "/api/batch/jobs/stop/" + executionId));
        } catch (Exception e) {
            logger.error("❌ Failed to stop batch job: {}", executionId, e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", "Failed to stop batch job: " + e.getMessage(), "/api/batch/jobs/stop/" + executionId));
        }
    }

    @PostMapping("/stop-all")
    @Operation(
        summary = "모든 실행 중인 배치 작업 중지",
        description = "현재 실행 중인 모든 배치 작업을 중지합니다"
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> stopAllRunningJobs() {
        try {
            logger.info("🛑 Attempting to stop all running batch jobs");

            Set<JobExecution> runningExecutions = jobExplorer.findRunningJobExecutions(null);
            int totalJobs = runningExecutions.size();
            int stoppedCount = 0;
            List<Long> stoppedExecutionIds = new ArrayList<>();
            List<Long> failedExecutionIds = new ArrayList<>();

            for (JobExecution execution : runningExecutions) {
                try {
                    boolean stopped = jobOperator.stop(execution.getId());
                    if (stopped) {
                        stoppedCount++;
                        stoppedExecutionIds.add(execution.getId());
                        logger.info("✅ Stopped job: {} (executionId: {})",
                            execution.getJobInstance().getJobName(), execution.getId());
                    } else {
                        failedExecutionIds.add(execution.getId());
                        logger.warn("⚠️ Failed to stop job executionId: {}", execution.getId());
                    }
                } catch (Exception e) {
                    failedExecutionIds.add(execution.getId());
                    logger.error("❌ Error stopping job executionId: {}", execution.getId(), e);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("totalJobs", totalJobs);
            result.put("stoppedCount", stoppedCount);
            result.put("stoppedExecutionIds", stoppedExecutionIds);
            result.put("failedExecutionIds", failedExecutionIds);
            result.put("message", String.format("Stop request sent to %d out of %d running jobs", stoppedCount, totalJobs));

            logger.info("✅ Stop request sent to {}/{} running jobs", stoppedCount, totalJobs);
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to stop all batch jobs", e);
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", "Failed to stop all batch jobs: " + e.getMessage(), "/api/batch/jobs/stop-all"));
        }
    }

    @PostMapping("/image-update")
    @Operation(
        summary = "이미지 업데이트 배치 실행",
        description = "crawlerFound=true이고 ready=false인 장소들의 이미지만 다시 크롤링하여 업데이트합니다. " +
                      "배치 작업은 백그라운드에서 비동기로 실행됩니다. " +
                      "기존 이미지는 삭제되고 새로운 이미지로 대체됩니다. " +
                      "완료 후 ready=true로 업데이트됩니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> runImageUpdateJob() {
        try {
            long startTime = System.currentTimeMillis();

            logger.info("🖼️ Triggering Image Update Batch Job");

            JobParameters jobParameters = new JobParametersBuilder()
                    .addLong("startTime", startTime)
                    .toJobParameters();

            // 비동기 실행 - 즉시 반환
            asyncJobLauncher.run(imageUpdateJob, jobParameters);

            Map<String, Object> result = new HashMap<>();
            result.put("status", "STARTED");
            result.put("message", "Image Update Batch Job has been triggered and is running in the background");
            result.put("startTime", startTime);

            logger.info("✅ Image Update Batch Job triggered successfully");
            return ResponseEntity.ok(ApiResponse.success(result));

        } catch (Exception e) {
            logger.error("❌ Failed to trigger Image Update Batch Job", e);

            Map<String, Object> error = new HashMap<>();
            error.put("status", "FAILED");
            error.put("message", "Failed to trigger batch job: " + e.getMessage());

            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("BATCH_JOB_ERROR", error.get("message").toString(), "/api/batch/jobs/image-update"));
        }
    }
}
