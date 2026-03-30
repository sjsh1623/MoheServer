package com.mohe.spring.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Set;

/**
 * 평시 자동 크롤링 스케줄러
 *
 * 좀비 잡 방지:
 * - 앱 시작 시 STARTED 잡 자동 FAILED 마킹
 * - 1시간 이상 STARTED 잡 자동 FAILED 마킹
 * - 앱 종료 시 실행 중 잡 FAILED 마킹
 */
@Service
public class BatchSchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(BatchSchedulerService.class);
    private static final long ZOMBIE_THRESHOLD_HOURS = 1; // 1시간 이상 실행 중이면 좀비로 판단

    private final JobLauncher asyncJobLauncher;
    private final JobExplorer jobExplorer;
    private final JobRepository jobRepository;
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

    private static final String[] ALL_JOB_NAMES = {
        "updateCrawledDataJob", "vectorEmbeddingJob", "imageUpdateJob"
    };

    public BatchSchedulerService(
            JobLauncher asyncJobLauncher,
            JobExplorer jobExplorer,
            JobRepository jobRepository,
            @Qualifier("updateCrawledDataJob") Job updateCrawledDataJob,
            @Qualifier("vectorEmbeddingJob") Job vectorEmbeddingJob,
            @Qualifier("imageUpdateJob") Job imageUpdateJob) {
        this.asyncJobLauncher = asyncJobLauncher;
        this.jobExplorer = jobExplorer;
        this.jobRepository = jobRepository;
        this.updateCrawledDataJob = updateCrawledDataJob;
        this.vectorEmbeddingJob = vectorEmbeddingJob;
        this.imageUpdateJob = imageUpdateJob;
    }

    /**
     * 앱 시작 시: 좀비 잡 정리 + 큐 수집 시작
     */
    @PostConstruct
    public void init() {
        // 1. 좀비 잡 정리 (이전 실행에서 남은 STARTED 잡)
        cleanupZombieJobs("startup");

        if (!schedulerEnabled) return;

        // 2. 큐 수집 시작 (30초 후)
        Thread.ofVirtual().start(() -> {
            try {
                Thread.sleep(30000);
                startQueueCollection();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * 앱 종료 시: 실행 중 잡 FAILED 마킹
     */
    @EventListener(ContextClosedEvent.class)
    public void onShutdown() {
        logger.info("🛑 [Scheduler] 앱 종료 - 실행 중 잡 정리");
        cleanupZombieJobs("shutdown");
    }

    /**
     * 좀비 잡 자동 정리
     * - STARTED 상태인 모든 잡을 FAILED로 변경
     * - context: "startup" (앱 시작) 또는 "shutdown" (앱 종료) 또는 "periodic" (주기적)
     */
    private void cleanupZombieJobs(String context) {
        int cleaned = 0;
        for (String jobName : ALL_JOB_NAMES) {
            try {
                Set<JobExecution> running = jobExplorer.findRunningJobExecutions(jobName);
                for (JobExecution execution : running) {
                    // periodic 정리는 1시간 이상된 잡만
                    if ("periodic".equals(context)) {
                        LocalDateTime startTime = execution.getStartTime();
                        if (startTime != null &&
                            Duration.between(startTime, LocalDateTime.now()).toHours() < ZOMBIE_THRESHOLD_HOURS) {
                            continue; // 아직 1시간 안 됨 — 정상 실행 중일 수 있음
                        }
                    }

                    execution.setStatus(BatchStatus.FAILED);
                    execution.setExitStatus(ExitStatus.FAILED.addExitDescription(
                            "Cleaned up by scheduler (" + context + ")"));
                    execution.setEndTime(LocalDateTime.now());

                    // Step executions도 정리
                    for (StepExecution step : execution.getStepExecutions()) {
                        if (step.getStatus() == BatchStatus.STARTED || step.getStatus() == BatchStatus.STARTING) {
                            step.setStatus(BatchStatus.FAILED);
                            step.setExitStatus(ExitStatus.FAILED);
                            step.setEndTime(LocalDateTime.now());
                            jobRepository.update(step);
                        }
                    }

                    jobRepository.update(execution);
                    cleaned++;
                    logger.info("🧹 [Scheduler] 좀비 잡 정리 ({}): {} (executionId={})",
                            context, jobName, execution.getId());
                }
            } catch (Exception e) {
                logger.warn("⚠️ [Scheduler] 좀비 잡 정리 실패 ({}): {} - {}", context, jobName, e.getMessage());
            }
        }
        if (cleaned > 0) {
            logger.info("🧹 [Scheduler] 총 {} 좀비 잡 정리 완료 ({})", cleaned, context);
        }
    }

    // ============================================
    // 스케줄러
    // ============================================

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

            if (body.contains("\"batch_running\":false") || body.contains("\"scheduler_running\":false")
                    || body.contains("\"status\":\"idle\"") || body.contains("\"status\":\"completed\"")) {
                logger.info("🔄 [Scheduler] 큐 수집이 중지 상태 - 재시작");
                startQueueCollection();
            }
        } catch (Exception e) {
            logger.warn("⚠️ [Scheduler] 큐 수집 상태 확인 실패: {}", e.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${batch.scheduler.crawl-interval-ms:1800000}", initialDelay = 60000)
    public void scheduleCrawlingJob() {
        if (!schedulerEnabled) return;
        launchJobIfIdle(updateCrawledDataJob, "updateCrawledDataJob");
    }

    @Scheduled(fixedDelayString = "${batch.scheduler.embed-interval-ms:2700000}", initialDelay = 120000)
    public void scheduleEmbeddingJob() {
        if (!schedulerEnabled) return;
        launchJobIfIdle(vectorEmbeddingJob, "vectorEmbeddingJob");
    }

    @Scheduled(fixedDelayString = "${batch.scheduler.image-interval-ms:3600000}", initialDelay = 180000)
    public void scheduleImageUpdateJob() {
        if (!schedulerEnabled) return;
        launchJobIfIdle(imageUpdateJob, "imageUpdateJob");
    }

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
     * 1시간 이상 STARTED면 좀비로 간주하고 정리 후 새로 실행
     */
    private void launchJobIfIdle(Job job, String jobName) {
        try {
            Set<JobExecution> running = jobExplorer.findRunningJobExecutions(jobName);

            if (!running.isEmpty()) {
                // 1시간 이상된 좀비 잡 확인
                boolean hasZombie = false;
                for (JobExecution exec : running) {
                    LocalDateTime startTime = exec.getStartTime();
                    if (startTime != null &&
                        Duration.between(startTime, LocalDateTime.now()).toHours() >= ZOMBIE_THRESHOLD_HOURS) {
                        hasZombie = true;
                        break;
                    }
                }

                if (hasZombie) {
                    logger.warn("🧹 [Scheduler] {} 좀비 잡 감지 (1시간+) - 정리 후 재실행", jobName);
                    cleanupZombieJobs("periodic");
                } else {
                    logger.info("⏭️ [Scheduler] {} 이미 실행 중 - 스킵", jobName);
                    return;
                }
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
