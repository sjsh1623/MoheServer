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
 * Spring Batch Job 실행 REST 컨트롤러
 *
 * <p>Spring Batch Job을 HTTP API를 통해 수동으로 트리거할 수 있는
 * 엔드포인트를 제공합니다.</p>
 *
 * <h3>주요 기능</h3>
 * <ul>
 *   <li>배치 Job의 수동 실행</li>
 *   <li>실행 파라미터 설정 (타임스탬프 자동 추가)</li>
 *   <li>실행 결과 응답 (성공/실패)</li>
 * </ul>
 *
 * <h3>Job 자동 실행 방지</h3>
 * <p>application.yml에서 {@code spring.batch.job.enabled: false}로 설정하여
 * 애플리케이션 시작 시 자동 실행을 방지합니다. 오직 API 호출을 통해서만 실행됩니다.</p>
 *
 * <h3>API 엔드포인트</h3>
 * <ul>
 *   <li><b>POST</b> /api/batch/jobs/place-collection - 장소 수집 배치 실행</li>
 * </ul>
 *
 * <h3>사용 예시</h3>
 * <pre>
 * curl -X POST http://localhost:8080/api/batch/jobs/place-collection
 * </pre>
 *
 * @author Andrew Lim
 * @since 1.0
 * @see org.springframework.batch.core.launch.JobLauncher
 * @see com.mohe.spring.batch.job.PlaceCollectionJobConfig
 */
@RestController
@RequestMapping("/api/batch/jobs")
@Tag(name = "Batch Jobs", description = "Spring Batch Job 실행 API")
public class BatchJobController {

    private static final Logger logger = LoggerFactory.getLogger(BatchJobController.class);

    /** Spring Batch Job을 실행하는 런처 */
    private final JobLauncher jobLauncher;

    /** 장소 수집 배치 Job (PlaceCollectionJobConfig에서 정의) */
    private final Job placeCollectionJob;

    /**
     * BatchJobController 생성자
     *
     * @param jobLauncher Spring Batch Job 실행 런처
     * @param placeCollectionJob 장소 수집 배치 Job 인스턴스
     */
    public BatchJobController(JobLauncher jobLauncher, Job placeCollectionJob) {
        this.jobLauncher = jobLauncher;
        this.placeCollectionJob = placeCollectionJob;
    }

    /**
     * 장소 수집 Batch Job을 수동으로 실행합니다
     *
     * <p>Naver API를 통해 전국의 장소 데이터를 수집하여
     * 데이터베이스에 저장하는 배치 작업을 시작합니다.</p>
     *
     * <h3>처리 흐름</h3>
     * <ol>
     *   <li>현재 시각을 파라미터로 추가 (재실행 가능하도록)</li>
     *   <li>JobLauncher를 통해 placeCollectionJob 실행</li>
     *   <li>성공 시 200 OK 응답</li>
     *   <li>실패 시 500 Internal Server Error 응답</li>
     * </ol>
     *
     * <h3>Job 파라미터</h3>
     * <p>매 실행마다 고유한 파라미터를 생성하여 동일한 Job을 재실행할 수 있습니다:</p>
     * <ul>
     *   <li><b>startTime</b>: 실행 시작 타임스탬프 (밀리초)</li>
     * </ul>
     *
     * <h3>응답 예시</h3>
     * <pre>
     * {
     *   "success": true,
     *   "data": {
     *     "status": "SUCCESS",
     *     "message": "Place Collection Job started successfully",
     *     "startTime": 1696502400000
     *   }
     * }
     * </pre>
     *
     * <p><b>비동기 실행:</b> Job은 백그라운드에서 실행되며,
     * API는 즉시 응답을 반환합니다. 실행 상태는 배치 메타데이터 테이블에서 확인 가능합니다.</p>
     *
     * @return 배치 실행 시작 결과 (성공 시 200, 실패 시 500)
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
