package com.mohe.spring.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Distributed Job Lock Entity
 *
 * <p>여러 컴퓨터에서 동시에 배치 작업을 실행할 때
 * 중복 처리를 방지하기 위한 락(Lock) 엔티티입니다.</p>
 *
 * <h3>사용 시나리오</h3>
 * <ul>
 *   <li>Mac Mini, MacBook Pro 등 여러 머신에서 동시에 크롤링</li>
 *   <li>각 머신이 다른 청크(chunk)를 처리하도록 보장</li>
 *   <li>데드 워커(dead worker) 자동 감지 및 복구</li>
 * </ul>
 */
@Entity
@Table(name = "distributed_job_lock")
public class DistributedJobLock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 배치 작업 이름 (예: updateCrawledDataJob)
     */
    @Column(name = "job_name", nullable = false, length = 100)
    private String jobName;

    /**
     * 청크 식별자 (예: place_1-10, place_11-20)
     */
    @Column(name = "chunk_id", nullable = false)
    private String chunkId;

    /**
     * 워커 식별자 (hostname + UUID)
     */
    @Column(name = "worker_id", nullable = false, length = 100)
    private String workerId;

    /**
     * 워커 호스트네임
     */
    @Column(name = "worker_hostname")
    private String workerHostname;

    /**
     * 락 상태: LOCKED, PROCESSING, COMPLETED, FAILED
     */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /**
     * 락 획득 시간
     */
    @Column(name = "locked_at", nullable = false)
    private LocalDateTime lockedAt;

    /**
     * 처리 시작 시간
     */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /**
     * 처리 완료 시간
     */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /**
     * 락 만료 시간 (타임아웃)
     */
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /**
     * 재시도 횟수
     */
    @Column(name = "retry_count")
    private Integer retryCount = 0;

    /**
     * 최대 재시도 횟수
     */
    @Column(name = "max_retries")
    private Integer maxRetries = 3;

    /**
     * 마지막 에러 메시지
     */
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * 메타데이터 (JSON)
     */
    @Column(name = "metadata", columnDefinition = "JSONB")
    private String metadata;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (lockedAt == null) {
            lockedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = "LOCKED";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobName() {
        return jobName;
    }

    public void setJobName(String jobName) {
        this.jobName = jobName;
    }

    public String getChunkId() {
        return chunkId;
    }

    public void setChunkId(String chunkId) {
        this.chunkId = chunkId;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getWorkerHostname() {
        return workerHostname;
    }

    public void setWorkerHostname(String workerHostname) {
        this.workerHostname = workerHostname;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getLockedAt() {
        return lockedAt;
    }

    public void setLockedAt(LocalDateTime lockedAt) {
        this.lockedAt = lockedAt;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getMetadata() {
        return metadata;
    }

    public void setMetadata(String metadata) {
        this.metadata = metadata;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
