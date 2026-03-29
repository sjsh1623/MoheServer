package com.mohe.spring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Set;

/**
 * 평시 자동 크롤링 스케줄러
 *
 * 전체 파이프라인:
 * 1. MoheCrawler 큐 수집     - Kakao API로 장소 기본 데이터 수집 (queue-based)
 * 2. UpdateCrawledDataJob    - 네이버 스크래핑 + OpenAI 요약 생성
 * 3. VectorEmbeddingJob      - 키워드 벡터 임베딩
 * 4. ImageUpdateJob          - 이미지 다운로드 + ready=true 최종화
 */
@Service
public class BatchSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(BatchSchedulerService.class);

    private final JobLauncher asyncJobLauncher;
    private final JobExplorer jobExplorer;
    private final Job updateCrawledDataJob;
    private final Job vectorEmbeddingJob;
    private final Job imageUpdateJob;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${batch.scheduler.enabled:true}")
    private boolean schedulerEnabled;

    @Value("${batch.collector.url:http://mohe-batch-crawler:4001}")
    private String batchCollectorUrl;

    public BatchSchedulerService(
            JobLauncher asyncJobLauncher,
            JobExplorer jobExplorer,
            @Qualifier("updateCrawledDataJob") Job updateCrawledDataJob,
            @Qualifier("vectorEmbeddingJob") Job vectorEmbeddingJob,
            @Qualifier("imageUpdateJob") Job imageUpdateJob) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.jobExplorer = jobExplorer;
        this.updateCrawledDataJob = updateCrawledDataJob;
        this.vectorEmbeddingJob = vectorEmbeddingJob;
        this.imageUpdateJob = imageUpdateJob;
    }

    /**
     * 앱 시작 30초 후 MoheCrawler 큐 수집 자동 시작
     * Kakao API로 pending 지역의 장소 데이터를 수집
     */
    @PostConstruct
    public void init() {
        if (!schedulerEnabled) return;
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(30000); // 30초 대기 (크롤러 준비 시간)
                startQueueCollection();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 2시간마다 큐 수집 상태 확인 → 중지되어 있으면 재시작
     */
    @Scheduled(fixedDelayString = "${batch.scheduler.queue-check-interval-ms:7200000}", initialDelay = 300000)
    public void ensureQueueCollectionRunning() {
        if (!schedulerEnabled) return;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchCollectorUrl + "/api/batch/status"))
                    .timeout(Duration.ofSeconds(5))
                    .GET().build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            String body = response.body();

            // 큐 수집이 멈춰있으면 재시작
            // API 응답 형식: {"data":{"batch_running":false,"scheduler_running":false},"success":true}
            if (body.contains("\"batch_running\":false") || body.contains("\"scheduler_running\":false")
                    || body.contains("\"status\":\"idle\"") || body.contains("\"status\":\"completed\"")) {
                logger.info("🔄 [Scheduler] 큐 수집이 중지 상태 - 재시작");
                startQueueCollection();
            }
        } catch (Exception e) {
            logger.warn("⚠️ [Scheduler] 큐 수집 상태 확인 실패: {}", e.getMessage());
        }
    }

    /**
     * 30분마다 크롤링 + OpenAI 요약 파이프라인 실행
     * 미처리 장소(crawl_status=null)를 네이버 스크래핑 → OpenAI 요약 생성
     */
    @Scheduled(fixedDelayString = "${batch.scheduler.crawl-interval-ms:1800000}", initialDelay = 60000)
    public void scheduleCrawlingJob() {
        if (!schedulerEnabled) return;
        launchJobIfIdle(updateCrawledDataJob, "updateCrawledDataJob");
    }

    /**
     * 45분마다 벡터 임베딩 실행
     * 크롤링 완료(crawl_status=COMPLETED, embed_status=PENDING) 장소의 키워드 벡터화
     */
    @Scheduled(fixedDelayString = "${batch.scheduler.embed-interval-ms:2700000}", initialDelay = 120000)
    public void scheduleEmbeddingJob() {
        if (!schedulerEnabled) return;
        launchJobIfIdle(vectorEmbeddingJob, "vectorEmbeddingJob");
    }

    /**
     * 60분마다 이미지 업데이트 + 최종화 실행
     * crawler_found=true, ready=false인 장소의 이미지 다운로드 → ready=true
     */
    @Scheduled(fixedDelayString = "${batch.scheduler.image-interval-ms:3600000}", initialDelay = 180000)
    public void scheduleImageUpdateJob() {
        if (!schedulerEnabled) return;
        launchJobIfIdle(imageUpdateJob, "imageUpdateJob");
    }

    /**
     * MoheCrawler 큐 수집 시작 호출
     */
    private void startQueueCollection() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(batchCollectorUrl + "/api/batch/start-queue"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(10))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                logger.info("🚀 [Scheduler] MoheCrawler 큐 기반 장소 수집 시작됨");
            } else {
                logger.warn("⚠️ [Scheduler] 큐 수집 시작 응답: {} - {}", response.statusCode(), response.body());
            }
        } catch (Exception e) {
            logger.warn("⚠️ [Scheduler] 큐 수집 시작 실패: {}", e.getMessage());
        }
    }

    /**
     * 동일 Job이 이미 실행 중이면 스킵, 아니면 실행
     */
    private void launchJobIfIdle(Job job, String jobName) {
        try {
            Set<JobExecution> running = jobExplorer.findRunningJobExecutions(jobName);
            if (!running.isEmpty()) {
                logger.info("⏭️ [Scheduler] {} 이미 실행 중 - 스킵", jobName);
                return;
            }

            JobParameters params = new JobParametersBuilder()
                    .addLong("scheduledAt", System.currentTimeMillis())
                    .toJobParameters();

            asyncJobLauncher.run(job, params);
            logger.info("🚀 [Scheduler] {} 자동 실행 시작", jobName);

        } catch (Exception e) {
            logger.error("❌ [Scheduler] {} 실행 실패: {}", jobName, e.getMessage());
        }
    }
}
